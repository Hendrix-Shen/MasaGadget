package com.plusls.MasaGadget.util;

import com.plusls.MasaGadget.ModInfo;
import com.plusls.MasaGadget.config.Configs;
import fi.dy.masa.minihud.renderer.shapes.ShapeBase;
import fi.dy.masa.minihud.renderer.shapes.ShapeDespawnSphere;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SearchMobSpawnPointUtil {
    @Nullable
    private static ShapeDespawnSphere getShapeDespawnSphere() {
        ShapeDespawnSphere ret = null;
        for (ShapeBase shapeBase : ShapeManager.INSTANCE.getAllShapes()) {
            if (shapeBase.isEnabled() && shapeBase instanceof ShapeDespawnSphere) {
                if (ret == null) {
                    ret = (ShapeDespawnSphere) shapeBase;
                } else {
                    Objects.requireNonNull(MinecraftClient.getInstance().player).sendMessage(
                            new TranslatableText("masa_gadget_mod.message.onlySupportOneDespawnShape").formatted(Formatting.RED), false);
                    return null;
                }
            }
        }
        if (ret == null) {
            Objects.requireNonNull(MinecraftClient.getInstance().player).sendMessage(
                    new TranslatableText("masa_gadget_mod.message.canNotFindDespawnShape").formatted(Formatting.RED), false);
        }
        return ret;
    }

    public static void search() {
        ClientWorld world = MinecraftClient.getInstance().world;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (world == null || player == null) {
            return;
        }
        ShapeDespawnSphere shapeDespawnSphere = getShapeDespawnSphere();
        if (shapeDespawnSphere == null) {
            return;
        }
        Vec3d centerPos = shapeDespawnSphere.getCenter();
        BlockPos pos = new BlockPos((int) centerPos.x, (int) centerPos.y, (int) centerPos.z);
        ModInfo.LOGGER.warn("shape: {}", shapeDespawnSphere.getCenter());
        BlockPos spawnPos = null;
        int maxX = pos.getX() + 129;
        int maxZ = pos.getZ() + 129;
        BlockPos.Mutable currentPos = new BlockPos.Mutable();
        int maxSpawnLightLevel = fi.dy.masa.minihud.config.Configs.Generic.LIGHT_LEVEL_THRESHOLD.getIntegerValue();
        LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
        EntityType<?> entityType = world.getDimension().isUltrawarm() ? EntityType.ZOMBIFIED_PIGLIN : EntityType.CREEPER;
        EntityType<?> entityType2 = world.getDimension().isUltrawarm() ? null : EntityType.SPIDER;

        for (int x = pos.getX() - 129; x <= maxX; ++x) {
            for (int z = pos.getZ() - 129; z <= maxZ; ++z) {
                WorldChunk chunk = world.getChunk(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                int maxY = Math.min(pos.getY() + 129, chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z) + 1);
                for (int y = Math.max(pos.getY() - 129, world.getBottomY() + 1); y <= maxY; ++y) {
                    if (squaredDistanceTo(x, y, z, centerPos) > 16384) {
                        if (y > centerPos.y) {
                            break;
                        } else {
                            continue;
                        }
                    } else if (spawnPos != null && player.squaredDistanceTo(x, y, z) >
                            player.squaredDistanceTo(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ())) {
                        continue;
                    }
                    currentPos.set(x, y, z);
                    if (SpawnHelper.canSpawn(SpawnRestriction.getLocation(entityType), world, currentPos, entityType) &&
                            lightingProvider.get(LightType.BLOCK).getLightLevel(currentPos) < maxSpawnLightLevel) {
                        Block block = world.getBlockState(currentPos.down()).getBlock();
                        String blockId = Registry.BLOCK.getId(world.getBlockState(currentPos.down()).getBlock()).toString();
                        String blockName = block.getName().getString();
                        if (Configs.Generic.SEARCH_MOB_SPAWN_POINT_BLACK_LIST.getStrings().stream().noneMatch(s -> blockId.contains(s) || blockName.contains(s))) {
                            if (world.isSpaceEmpty(entityType.createSimpleBoundingBox(currentPos.getX() + 0.5D, currentPos.getY(), currentPos.getZ() + 0.5D))) {
                                spawnPos = currentPos.mutableCopy();
                            } else if (entityType2 != null && world.isSpaceEmpty(entityType2.createSimpleBoundingBox(currentPos.getX() + 0.5D, currentPos.getY(), currentPos.getZ() + 0.5D))) {
                                spawnPos = currentPos.mutableCopy();
                            }
                        }
                    }
                }
            }
        }
        Text text;
        if (spawnPos == null) {
            text = new TranslatableText("masa_gadget_mod.message.noBlockCanSpawn").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GREEN)));
        } else {
            // for ommc parser
            text = new LiteralText(I18n.translate("masa_gadget_mod.message.spawnPos", spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()));
            player.sendChatMessage(String.format("/highlightWaypoint %d %d %d", spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()));
        }
        Objects.requireNonNull(MinecraftClient.getInstance().player).sendMessage(text, false);
    }

    private static double squaredDistanceTo(int x, int y, int z, Vec3d vec3d) {
        double d = vec3d.x - x;
        double e = vec3d.y - y;
        double f = vec3d.z - z;
        return d * d + e * e + f * f;
    }
}
