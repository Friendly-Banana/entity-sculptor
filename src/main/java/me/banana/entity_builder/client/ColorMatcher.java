package me.banana.entity_builder.client;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.InfestedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static me.banana.entity_builder.Utils.LOGGER;

public class ColorMatcher {
    static Predicate<Block> fallingBlock = block -> block instanceof FallingBlock;
    static Predicate<Block> creativeBlock = block -> block.getHardness() == -1.0f || block instanceof InfestedBlock;
    static DefaultResourcePack resourcePack;
    static Map<Color, Block> palette = new HashMap<>();

    public static void AddBlock(Identifier identifier, Block block) {
        if (resourcePack == null)
            resourcePack = MinecraftClient.getInstance().getResourcePackProvider().getPack();

        if (!resourcePack.contains(ResourceType.CLIENT_RESOURCES, identifier)) {
            LOGGER.warn("Missing texture for " + identifier);
            return;
        }
        float r = 0, g = 0, b = 0, a = 0;
        try {
            InputStream inputStream = resourcePack.open(ResourceType.CLIENT_RESOURCES, identifier);
            BufferedImage image = ImageIO.read(inputStream);
            float alphaWeight = 1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    Color color = new Color(pixel, true);
                    a += color.getAlpha();
                    if (!image.isAlphaPremultiplied()) {
                        alphaWeight = color.getAlpha() / 255.0f;
                    }
                    r += alphaWeight * color.getRed();
                    g += alphaWeight * color.getGreen();
                    b += alphaWeight * color.getBlue();
                }
            }
            // get average
            int pixels = image.getHeight() * image.getWidth();
            r /= pixels;
            g /= pixels;
            b /= pixels;
            a /= pixels;
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        palette.put(new Color(r, g, b, a), block);
        LOGGER.info("Added texture for " + identifier);
    }

    public static Block NearestBlock(int intColor, boolean noFallingBlocks, boolean noCreativeBlocks) {
        float distance = Float.MAX_VALUE;
        Block matching = Blocks.SMOOTH_STONE;
        Color color = new Color(intColor, true);
        for (Color c : palette.keySet()) {
            float d = Math.abs(c.getRed() - color.getRed()) + Math.abs(c.getGreen() - color.getGreen()) + Math.abs(c.getBlue() - color.getBlue()) + Math.abs(c.getAlpha() - color.getAlpha());
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
