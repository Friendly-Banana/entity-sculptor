package me.banana.entity_builder.client;

import me.banana.entity_builder.Utils;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.InfestedBlock;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static me.banana.entity_builder.Utils.LOGGER;

public class ColorMatcher implements SimpleSynchronousResourceReloadListener {
    final static Predicate<Block> fallingBlock = block -> block instanceof FallingBlock;
    final static Predicate<Block> creativeBlock = block -> block.getHardness() == -1.0f || block instanceof InfestedBlock;
    final Map<Color, Identifier> palette = new HashMap<>();

    public Identifier NearestBlock(int intColor, boolean noFallingBlocks, boolean noCreativeBlocks) {
        float distance = Float.MAX_VALUE;
        Identifier matching = new Identifier("stone");//Registry.BLOCK.getDefaultId();
        Color color = new Color(intColor, true);
        for (Color c : palette.keySet()) {
            float d = Math.abs(c.getRed() - color.getRed()) + Math.abs(c.getGreen() - color.getGreen()) + Math.abs(c.getBlue() - color.getBlue()) + Math.abs(c.getAlpha() - color.getAlpha());
            if (d < distance) {
                Block block = Registry.BLOCK.get(palette.get(c));
                if (noFallingBlocks && fallingBlock.test(block)) continue;
                if (noCreativeBlocks && creativeBlock.test(block)) continue;
                distance = d;
                matching = palette.get(c);
            }
        }
        return matching;
    }

    @Override
    public Identifier getFabricId() {
        return Utils.NewIdentifier("color_matcher");
    }

    @Override
    public Collection<Identifier> getFabricDependencies() {
        return Collections.singletonList(ResourceReloadListenerKeys.TEXTURES);
    }

    @Override
    public void reload(ResourceManager manager) {
        palette.clear();
        int counter = 0;
        for (Identifier id : Registry.BLOCK.getIds()) {
            float r = 0, g = 0, b = 0, a = 0;
            try (InputStream inputStream = manager.getResourceOrThrow(new Identifier(id.getNamespace(), "textures/block/" + id.getPath() + ".png")).getInputStream()) {
                BufferedImage image = ImageIO.read(inputStream);
                boolean hasAlpha = image.getColorModel().hasAlpha();
                float alphaWeight = 1;
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        Color color = new Color(image.getRGB(x, y), hasAlpha);
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
            } catch (FileNotFoundException e) {
                LOGGER.warn("Missing texture for " + id);
                continue;
            } catch (IOException e) {
                LOGGER.error("Couldn't open texture: " + e);
                continue;
            }
            palette.put(new Color((int)r, (int)g, (int)b, (int)a), id);
            counter++;
        }
        LOGGER.info("Added " + counter + " block textures.");
    }
}
