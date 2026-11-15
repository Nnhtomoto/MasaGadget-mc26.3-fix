package com.plusls.MasaGadget.mixin.feature.entityInfo;

//#if MC >= 26.1
import com.plusls.MasaGadget.impl.feature.entityInfo.EntityInfoNameTag;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Inject(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At("TAIL"))
    private void masa_gadget_mod$appendEntityInfo(Entity entity, EntityRenderState renderState, float partialTick,
                                                  double nameTagDistance, double belowNameDistance, CallbackInfo ci) {
        EntityInfoNameTag.append(entity, renderState, partialTick);
    }
}
//#else
import top.hendrixshen.magiclib.api.preprocess.DummyClass;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(DummyClass.class)
public abstract class MixinEntityRenderer {
}
//#endif
