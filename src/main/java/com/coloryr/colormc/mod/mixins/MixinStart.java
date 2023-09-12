package com.coloryr.colormc.mod.mixins;

import com.coloryr.colormc.mod.SocketDisplay;
import net.minecraftforge.fml.client.SplashProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashProgress.class)
public abstract class MixinStart {
    @Unique
    private static SocketDisplay colorMC_Mod$display;

    @Inject(
            method = "start",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraftforge/fml/client/SplashProgress;createResourcePack(Ljava/io/File;)Lnet/minecraft/client/resources/IResourcePack;",
                    remap = false
            )
    )
    private static void start_inject(CallbackInfo ci) {
        colorMC_Mod$display = new SocketDisplay();
    }

    @Inject(method = "finish", remap = false, at = @At(value = "HEAD" , remap = false))
    private static void onDone(CallbackInfo ci)
    {
        colorMC_Mod$display.finish();
    }
}
