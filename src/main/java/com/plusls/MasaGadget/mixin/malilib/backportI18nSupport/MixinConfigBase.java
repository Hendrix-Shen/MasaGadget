package com.plusls.MasaGadget.mixin.malilib.backportI18nSupport;

import com.plusls.MasaGadget.config.Configs;
import com.plusls.MasaGadget.util.MiscUtil;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ConfigBase.class, remap = false)
public abstract class MixinConfigBase implements IConfigBase {

    @Shadow
    private String comment;

    @Inject(method = "getComment", at = @At(value = "HEAD"), cancellable = true)
    private void useI18nComment(CallbackInfoReturnable<String> cir) {
        if (Configs.Malilib.BACKPORT_I18N_SUPPORT.getBooleanValue()) {
            cir.setReturnValue(MiscUtil.getTranslatedOrFallback("config.comment." + this.getName().toLowerCase(), this.comment));
        }
    }

    @Override
    public String getConfigGuiDisplayName() {
        if (Configs.Malilib.BACKPORT_I18N_SUPPORT.getBooleanValue()) {
            return MiscUtil.getTranslatedOrFallback("config.name." + this.getName().toLowerCase(), this.getName());
        } else {
            return this.getName();
        }
    }
}
