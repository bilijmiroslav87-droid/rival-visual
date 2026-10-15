package com.collapse.mixin;

import com.collapse.combat.RotationManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Подменяем углы только на время отправки пакетов движения: серверу уходит
 * серверная ротация, а камера игрока не дёргается.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    private float collapse$sy, collapse$sp;
    private boolean collapse$swapped = false;

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void collapse$before(CallbackInfo ci) {
        if (RotationManager.isActive()) {
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            collapse$sy = self.getYaw();
            collapse$sp = self.getPitch();
            self.setYaw(RotationManager.yaw());
            self.setPitch(RotationManager.pitch());
            collapse$swapped = true;
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void collapse$after(CallbackInfo ci) {
        if (collapse$swapped) {
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            self.setYaw(collapse$sy);
            self.setPitch(collapse$sp);
            collapse$swapped = false;
        }
    }
}
