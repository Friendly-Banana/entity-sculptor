package me.banana.entity_builder.client;

import com.google.common.collect.Maps;
import me.banana.entity_builder.EntityBuilder;
import me.banana.entity_builder.Utils;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;

import static me.banana.entity_builder.Utils.LOGGER;

public class ColorMatcher implements SimpleSynchronousResourceReloadListener {
    public static List<Identifier> oldIds = new ArrayList<>();
    public static Map<Identifier, Identifier> ids = Maps.newHashMap();
    private final Map<Color, BlockState> palette = new HashMap<>();

    // TODO use axis
    public BlockState NearestBlock(int intColor, Direction unmappedAxis) {
        float distance = Float.MAX_VALUE;
        BlockState matching = Registry.BLOCK.get(new Identifier("bedrock")).getDefaultState();
        Color color = new Color(intColor, true);
        for (Color c : palette.keySet()) {
            float d = Math.abs(c.getRed() - color.getRed()) + Math.abs(c.getGreen() - color.getGreen()) + Math.abs(c.getBlue() - color.getBlue()) + Math.abs(c.getAlpha() - color.getAlpha());
            if (d < distance) {
                Block block = palette.get(c).getBlock();
                if (EntityBuilder.CONFIG.excludedBlocks().contains(block)) continue;
                distance = d;
                matching = palette.get(c);
            }
        }
        return matching;
    }

    @Override
    public Identifier getFabricId() {
        return Utils.Id("color_matcher");
    }

    @Override
    public Collection<Identifier> getFabricDependencies() {
        return Collections.singletonList(ResourceReloadListenerKeys.TEXTURES);
    }

    @Override
    public void reload(ResourceManager manager) {
        palette.clear();
        int counter = 0;
        for (Identifier blockId : ids.keySet()) {
            Identifier textureId = ids.get(blockId);
            float r = 0, g = 0, b = 0, a = 0;
            try (InputStream inputStream = manager.open(textureId)) {
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
                LOGGER.warn("Missing texture for " + textureId);
                continue;
            } catch (IOException e) {
                LOGGER.error("Couldn't open texture: " + e);
                continue;
            }
            palette.put(new Color((int) r, (int) g, (int) b, (int) a), Registry.BLOCK.get(blockId).getDefaultState());
            if (Registry.BLOCK.get(blockId).getDefaultState().isAir()) Utils.log("fdsasf");
            counter++;
        }
        LOGGER.info("Added " + counter + " block textures.");
    }
}
