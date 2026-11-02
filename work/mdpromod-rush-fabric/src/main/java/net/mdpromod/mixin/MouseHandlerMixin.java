package net.mdpromod.mixin;

import net.mdpromod.ClientForgeEvents;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void mdpromod$onMouseButton(long window, int button, int action,
                                         int modifiers, CallbackInfo callback) {
        if (ClientForgeEvents.onMouseButton(button, action)) {
            callback.cancel();
        }
    }
}
