package dev.annoyingsounds.mixin;

import dev.annoyingsounds.AnnoyingSounds;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.client.sounds.ChannelAccess;
import dev.annoyingsounds.SoundEngineControl;
import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin implements SoundEngineControl {
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow protected abstract float calculateVolume(SoundInstance sound);
    @Override public void annoyingSounds$refreshVolumes() {
        instanceToChannel.forEach((sound, handle) -> {
            float volume = calculateVolume(sound);
            handle.execute(channel -> channel.setVolume(volume));
        });
    }
    @Inject(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"))
    private void annoyingSounds$record(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> ci) {
        AnnoyingSounds.heard(sound);
    }
    @ModifyExpressionValue(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float annoyingSounds$initialVolume(float original, SoundInstance sound) {
        return original * (float) AnnoyingSounds.gain(sound);
    }
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void annoyingSounds$volume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * (float) AnnoyingSounds.gain(sound));
    }
}
