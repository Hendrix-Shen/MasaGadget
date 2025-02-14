package com.plusls.MasaGadget.mixin.tweakeroo.inventoryPreviewSupportComparator;

import com.mojang.blaze3d.systems.RenderSystem;
import com.plusls.MasaGadget.ModInfo;
import com.plusls.MasaGadget.config.Configs;
import com.plusls.MasaGadget.mixin.Dependencies;
import com.plusls.MasaGadget.mixin.Dependency;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.config.Hotkeys;
import fi.dy.masa.tweakeroo.util.RayTraceUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ComparatorBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Dependencies(dependencyList = @Dependency(modId = ModInfo.TWEAKEROO_MOD_ID, version = "*"))
@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Inject(method = "render", at = @At(value = "RETURN"))
    private void postRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = WorldUtils.getBestWorld(mc);
        if (world == null || mc.player == null) {
            return;
        }
        Entity cameraEntity = world.getPlayerByUuid(mc.player.getUuid());
        if (cameraEntity == null) {
            cameraEntity = mc.player;
        }

        if (FeatureToggle.TWEAK_FREE_CAMERA.getBooleanValue()) {
            cameraEntity = mc.getCameraEntity();
        }

        if (!FeatureToggle.TWEAK_INVENTORY_PREVIEW.getBooleanValue() || !Hotkeys.INVENTORY_PREVIEW.getKeybind().isKeybindHeld() ||
                !Configs.Tweakeroo.INVENTORY_PREVIEW_SUPPORT_COMPARATOR.getBooleanValue() || cameraEntity == null) {
            return;
        }

        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.method_34425(matrices.peek().getModel());
        RenderSystem.applyModelViewMatrix();

        // 开始渲染

        HitResult trace = RayTraceUtils.getRayTraceFromEntity(world, cameraEntity, false);

        if (trace.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) trace).getBlockPos();

            // 绕过线程检查
            BlockEntity blockEntity = world.getWorldChunk(pos).getBlockEntity(pos);
            if (blockEntity instanceof ComparatorBlockEntity) {
                LiteralText literalText = new LiteralText(((ComparatorBlockEntity) blockEntity).getOutputSignal() + "");
                literalText.formatted(Formatting.GREEN);
                //literalText.formatted(Formatting.);

                VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                // 不加 1.17 渲染会有问题
                RenderSystem.disableDepthTest();

                matrixStack.push();
                matrixStack.translate(pos.getX() + 0.5 - camera.getPos().getX(), pos.getY() + 0.6 - camera.getPos().getY(), pos.getZ() + 0.5 - camera.getPos().getZ());
                matrixStack.method_34425(new Matrix4f(camera.getRotation()));
                matrixStack.scale(-0.04F, -0.04F, -0.04F);
                RenderSystem.applyModelViewMatrix();

                Matrix4f lv = AffineTransformation.identity().getMatrix();

                float xOffset = (float) (-mc.textRenderer.getWidth(literalText) / 2);
                float g = mc.options.getTextBackgroundOpacity(0.25F);
                int k = (int) (g * 255.0F) << 24;
                mc.textRenderer.draw(literalText, xOffset, 0, 553648127, false, lv, immediate, true, k, 0xf00000);
                immediate.draw();

                mc.textRenderer.draw(literalText, xOffset, 0, -1, false, lv, immediate, true, 0, 0xf00000);

                immediate.draw();
                matrixStack.pop();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.enableDepthTest();
            }
        }

        // 结束渲染
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
    }
}
