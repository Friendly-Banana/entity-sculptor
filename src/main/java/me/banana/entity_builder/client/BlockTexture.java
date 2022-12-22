package me.banana.entity_builder.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockTexture {
    void a() {
        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        Map<BlockState, Map<Direction, List<Sprite>>> map = new HashMap<>();
        Random random = Random.create(42);
        for (var blockState : Block.STATE_IDS) {
            HashMap<Direction, List<Sprite>> directionMap = new HashMap<>();
            map.put(blockState, directionMap);
            // TODO figure out if BasicBakedModel.quads are important
            // direction null returns quads, anything else faceQuads
            random.setSeed(42);
            var sprites = blockRenderManager.getModel(blockState).getQuads(blockState, null, random).stream().map(BakedQuad::getSprite).toList();

            for (Direction direction : Direction.values()) {
                // use quads for all directions if not empty
                if (!sprites.isEmpty()) {
                    random.setSeed(42);
                    sprites = blockRenderManager.getModel(blockState).getQuads(blockState, direction, random).stream().map(BakedQuad::getSprite).toList();
                }
                directionMap.put(direction, sprites);
            }
        }
    }

    void b() {
        MatrixStack matrixStack = new MatrixStack();
        var vc = new CollectingVertexConsumerProvider();
        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        for (var blockState : Block.STATE_IDS) {
            blockRenderManager.renderBlockAsEntity(blockState, matrixStack, vc, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
            vc.clear();
        }
    }

    void c() {
        MatrixStack matrixStack = new MatrixStack();
        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        Random random = Random.create();
        BlockBufferBuilderStorage blockBufferBuilderStorage = MinecraftClient.getInstance().getBufferBuilders().getBlockBufferBuilders(); // new BufferBuilderStorage()
        for (var blockState : Block.STATE_IDS) {
            RenderLayer renderLayer = RenderLayers.getBlockLayer(blockState);
            BufferBuilder bufferBuilder = blockBufferBuilderStorage.get(renderLayer);
            blockRenderManager.renderBlock(blockState, BlockPos.ORIGIN, new BlockRenderView() {
                @Override
                public float getBrightness(Direction direction, boolean shaded) {
                    return 15;
                }

                @Override
                public LightingProvider getLightingProvider() {
                    return null;
                }

                @Override
                public int getColor(BlockPos pos, ColorResolver colorResolver) {
                    return 0;
                }

                @Nullable
                @Override
                public BlockEntity getBlockEntity(BlockPos pos) {
                    return null;
                }

                @Override
                public BlockState getBlockState(BlockPos pos) {
                    return null;
                }

                @Override
                public FluidState getFluidState(BlockPos pos) {
                    return null;
                }

                @Override
                public int getHeight() {
                    return 0;
                }

                @Override
                public int getBottomY() {
                    return 0;
                }
            }, matrixStack, bufferBuilder, false, random);
        }
    }
}
