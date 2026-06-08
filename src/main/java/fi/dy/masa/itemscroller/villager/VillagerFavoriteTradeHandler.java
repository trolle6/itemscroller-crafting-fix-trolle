package fi.dy.masa.itemscroller.villager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.MerchantMenu;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.util.ClickPacketBuffer;
import fi.dy.masa.itemscroller.util.InventoryUtils;

public class VillagerFavoriteTradeHandler
{
    private static boolean tradeOnNextMerchantOpen;

    public static boolean isEnabled()
    {
        return Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue() &&
               Configs.Generic.VILLAGER_TRADE_FAVORITES_ON_USE.getBooleanValue();
    }

    public static void onVillagerUseInteract(AbstractVillager villager)
    {
        if (isEnabled() == false)
        {
            return;
        }

        VillagerDataStorage.getInstance().setLastInteractedUUID(villager.getUUID());
        tradeOnNextMerchantOpen = true;
    }

    public static void reset()
    {
        tradeOnNextMerchantOpen = false;
    }

    public static void onClientTick(Minecraft mc)
    {
        if (tradeOnNextMerchantOpen == false || isEnabled() == false || mc.player == null)
        {
            return;
        }

        if (mc.screen instanceof MerchantScreen == false)
        {
            return;
        }

        MerchantMenu menu = ((MerchantScreen) mc.screen).getMenu();

        if (hasOffers(menu) == false)
        {
            return;
        }

        tradeOnNextMerchantOpen = false;

        if (hasFavoritedTrades(menu) == false)
        {
            // No favorites for this villager — leave the normal GUI open.
            return;
        }

        if (Configs.Generic.RATE_LIMIT_CLICK_PACKETS.getBooleanValue())
        {
            ClickPacketBuffer.setShouldBufferClickPackets(true);
        }

        InventoryUtils.villagerTradeEverythingPossibleWithAllFavoritedTrades();
        closeMerchantScreen(mc);

        if (Configs.Generic.RATE_LIMIT_CLICK_PACKETS.getBooleanValue())
        {
            ClickPacketBuffer.setShouldBufferClickPackets(false);
        }
    }

    private static boolean hasFavoritedTrades(MerchantMenu menu)
    {
        return VillagerDataStorage.getInstance().getFavoritesForCurrentVillager(menu).favorites().isEmpty() == false;
    }

    private static boolean hasOffers(MerchantMenu menu)
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

    private static void closeMerchantScreen(Minecraft mc)
    {
        mc.player.closeContainer();
        mc.setScreen(null);
    }
}
