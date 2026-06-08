package fi.dy.masa.itemscroller.villager;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.util.ClickPacketBuffer;
import fi.dy.masa.itemscroller.util.InventoryUtils;

public class VillagerInstantTrade
{
    private static boolean tradeRequested;
    private static int tradeCooldown;

    public static void requestTrade()
    {
        tradeRequested = true;
    }

    public static void clearTradeRequest()
    {
        tradeRequested = false;
    }

    public static boolean isInstantTradeEnabled()
    {
        return Configs.Generic.VILLAGER_TRADE_FAVORITES_INSTANT.getBooleanValue();
    }

    public static boolean isTradeRequestActive()
    {
        return tradeRequested ||
               Configs.Generic.VILLAGER_TRADE_FAVORITES_HOLD.getBooleanValue() ||
               Hotkeys.VILLAGER_TRADE_FAVORITES.getKeybind().isKeybindHeld();
    }

    public static boolean shouldProcessMerchantScreen(MerchantScreen screen)
    {
        return Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue() &&
               isInstantTradeEnabled() &&
               isTradeRequestActive() &&
               hasTradeableOffers(screen.getMenu());
    }

    @Nullable
    public static AbstractVillager getLookedAtVillager(Minecraft mc)
    {
        if (mc.player == null || mc.hitResult == null ||
            mc.hitResult.getType() != HitResult.Type.ENTITY)
        {
            return null;
        }

        Entity entity = ((EntityHitResult) mc.hitResult).getEntity();

        if (entity instanceof AbstractVillager villager && villager.isAlive() && mc.player.distanceTo(villager) <= 6.0D)
        {
            return villager;
        }

        return null;
    }

    public static boolean tryOpenLookedAtVillager(Minecraft mc)
    {
        AbstractVillager villager = getLookedAtVillager(mc);

        if (villager == null || mc.gameMode == null)
        {
            return false;
        }

        VillagerDataStorage.getInstance().setLastInteractedUUID(villager.getUUID());
        mc.gameMode.interact(mc.player, villager, InteractionHand.MAIN_HAND);
        return true;
    }

    public static boolean tradeFavoritesAndMaybeClose(Minecraft mc, boolean closeAfter)
    {
        if (InventoryUtils.villagerTradeEverythingPossibleWithAllFavoritedTrades() == false)
        {
            return false;
        }

        if (closeAfter)
        {
            closeMerchantScreen(mc);
        }

        return true;
    }

    public static void closeMerchantScreen(Minecraft mc)
    {
        if (mc.player != null)
        {
            mc.player.closeContainer();
        }

        mc.setScreen(null);
    }

    public static void onClientTick(Minecraft mc)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue() == false ||
            isInstantTradeEnabled() == false ||
            isTradeRequestActive() == false ||
            mc.player == null || mc.level == null)
        {
            tradeCooldown = 0;
            return;
        }

        if (isRepeatingTradeRequest())
        {
            if (++tradeCooldown < Configs.Generic.VILLAGER_TRADE_INTERVAL.getIntegerValue())
            {
                return;
            }

            tradeCooldown = 0;
        }

        Screen screen = mc.screen;

        if (screen instanceof MerchantScreen merchantScreen)
        {
            if (shouldProcessMerchantScreen(merchantScreen))
            {
                if (Configs.Generic.RATE_LIMIT_CLICK_PACKETS.getBooleanValue())
                {
                    ClickPacketBuffer.setShouldBufferClickPackets(true);
                }

                tradeFavoritesAndMaybeClose(mc, true);

                if (tradeRequested)
                {
                    tradeRequested = false;
                }

                if (Configs.Generic.RATE_LIMIT_CLICK_PACKETS.getBooleanValue())
                {
                    ClickPacketBuffer.setShouldBufferClickPackets(false);
                }
            }
        }
        else if (screen == null)
        {
            tryOpenLookedAtVillager(mc);
        }
    }

    private static boolean isRepeatingTradeRequest()
    {
        return Configs.Generic.VILLAGER_TRADE_FAVORITES_HOLD.getBooleanValue() ||
               Hotkeys.VILLAGER_TRADE_FAVORITES.getKeybind().isKeybindHeld();
    }

    private static boolean hasTradeableOffers(MerchantMenu menu)
    {
        try
        {
            return menu.getOffers().isEmpty() == false;
        }
        catch (Exception ignored)
        {
            return false;
        }
    }
}
