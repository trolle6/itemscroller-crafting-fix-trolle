package fi.dy.masa.itemscroller.mixin.network;

import fi.dy.masa.itemscroller.util.ClickPacketBuffer;
import fi.dy.masa.itemscroller.villager.VillagerFavoriteTradeHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinClientPlayerInteractionManager
{
    @Inject(method = "interact", at = @At("RETURN"))
    private void itemscroller$onVillagerUseInteract(Player player, Entity entity, InteractionHand hand,
                                                    CallbackInfoReturnable<InteractionResult> cir)
    {
        if (entity instanceof AbstractVillager villager &&
            cir.getReturnValue() != InteractionResult.FAIL)
        {
            VillagerFavoriteTradeHandler.onVillagerUseInteract(villager);
        }
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void cancelWindowClicksWhileReplayingBufferedPackets(CallbackInfo ci)
    {
        if (ClickPacketBuffer.shouldCancelWindowClicks())
        {
            ci.cancel();
        }
    }

    @Redirect(method = "handleInventoryMouseClick",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void bufferClickPacketsAndCancel(ClientPacketListener netHandler, Packet<?> packet)
    {
        /*
        if (packet instanceof ClickSlotC2SPacket clickPacket)
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            System.out.printf("clickPacket: type: %s button: %d, slot: %d, (after) cursor item: %s\n", clickPacket.getActionType(), clickPacket.getButton(), clickPacket.getSlot(), clickPacket.getStack());
            clickPacket.getModifiedStacks().forEach((integer, stack) -> System.out.printf("%d = %s, ", integer, stack));
            System.out.println();
        }
         */
        if (ClickPacketBuffer.shouldBufferClickPackets())
        {
            ClickPacketBuffer.bufferPacket(packet);
            return;
        }

        netHandler.send(packet);
    }
}
