package net.mdpromod.mixin;

import net.mdpromod.ClientDuelUiAudio;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void mdpromod$replaceDuelButtonSound(SoundManager soundManager,
                                                  CallbackInfo callback) {
        if (ClientDuelUiAudio.replaceButtonSound((AbstractWidget) (Object) this)) {
            callback.cancel();
        }
    }
}
