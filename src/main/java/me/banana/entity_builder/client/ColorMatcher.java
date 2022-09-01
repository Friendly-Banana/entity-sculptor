package me.banana.entity_builder.client;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.InfestedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vector4f;
import net.minecraft.util.registry.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ColorMatcher {
    static Map<Vector4f, Block> palette = new HashMap<>();
    static Predicate<Block> fallingBlock = block -> block instanceof FallingBlock;
    static Predicate<Block> creativeBlock = block -> block.getHardness() == -1.0f || block instanceof InfestedBlock;

    public static void init() {
        DefaultResourcePack resourcePack = MinecraftClient.getInstance().getResourcePackProvider().getPack();

        for (Identifier identifier : Registry.BLOCK.getIds()) {
            if (!resourcePack.contains(ResourceType.CLIENT_RESOURCES, identifier)) continue;
            try {
                InputStream inputStream = resourcePack.open(ResourceType.CLIENT_RESOURCES, identifier);
                inputStream.readAllBytes();

                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            palette.put(new Vector4f(), Registry.BLOCK.get(identifier));
        }
    }

    public static Block NearestBlock(Vector4f color, boolean noFallingBlocks, boolean noCreativeBlocks) {
        float distance = Float.MAX_VALUE;
        Block matching = Blocks.AIR;
        for (var c : palette.keySet()) {
            float d = Math.abs(c.getW() - color.getW()) + Math.abs(c.getX() - color.getX()) + Math.abs(c.getY() - color.getY()) + Math.abs(c.getZ() - color.getZ());
            if (d < distance) {
                Block block = palette.get(c);
                if (noFallingBlocks && fallingBlock.test(block)) continue;
                if (noCreativeBlocks && creativeBlock.test(block)) continue;
                distance = d;
                matching = block;
            }
        }
        return matching;
    }
}
