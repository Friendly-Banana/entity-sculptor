package me.banana.entity_builder.client;

import com.google.common.collect.Maps;
import me.banana.entity_builder.EntityBuilder;
import me.banana.entity_builder.Utils;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarrotsBlock;
import net.minecraft.block.FireBlock;
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
    public static Map<Identifier, Identifier> ids = Maps.newHashMap();

    // supplied by mixins
    public static final Map<String, BlockState> idToblockstate = new HashMap<>();
    public static final Map<Identifier, List<Identifier>> blockstateToTextures = new HashMap<>();
    private final Map<Direction, Map<Color, BlockState>> palette = new HashMap<>();

    private static Identifier getTexturePath(Identifier id) {
        return new Identifier(id.getNamespace(), String.format("textures/%s%s", id.getPath(), ".png"));
    }

    /**
     * finds the best suited color
     */
    public BlockState nearestBlock(int intColor, Direction unmappedAxis) {
        float minDistance = Float.MAX_VALUE;
        BlockState matching = Registry.BLOCK.get(new Identifier("stone")).getDefaultState();
        Color color = new Color(intColor, true);
        for (Color c : palette.get(unmappedAxis).keySet()) {
            float distance = Math.abs(c.getRed() - color.getRed()) + Math.abs(c.getGreen() - color.getGreen()) + Math.abs(c.getBlue() - color.getBlue()) + Math.abs(c.getAlpha() - color.getAlpha());
            if (distance < minDistance) {
                BlockState blockState = palette.get(unmappedAxis).get(c);
                if (EntityBuilder.CONFIG.excludedBlockIDs().contains(Registry.BLOCK.getId(blockState.getBlock()).toString())) continue;
                minDistance = distance;
                matching = blockState;
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

        var dirs = Arrays.stream(Direction.values()).toList();
        for (Direction direction : dirs) {
            palette.put(direction, new HashMap<>());
        }
        int counter = 0;
        for (Identifier stateID : blockstateToTextures.keySet()) {
            // blockstateToTextures "minecraft:block/fire_up0"
            Utils.log(ids);
            // TODO this is assuming the textures for the faces are in the same order
            int dir = 0;
            for (Iterator<Identifier> iterator = blockstateToTextures.get(stateID).iterator(); iterator.hasNext() && dir < dirs.size(); dir++) {
                Identifier textureId = iterator.next();
                float r = 0, g = 0, b = 0, a = 0;
                try (InputStream inputStream = manager.open(getTexturePath(textureId))) {
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
                palette.get(dirs.get(dir)).put(new Color((int) r, (int) g, (int) b, (int) a), id(stateID));
            }
            counter++;
        }
        LOGGER.info("Added " + counter + " block textures.");
    }

    BlockState id(Identifier stateID) {
        Blocks.FIRE.getDefaultState().with(FireBlock.AGE, 7);
        Blocks.CARROTS.getDefaultState().with(CarrotsBlock.AGE, 7);
        String path = stateID.getPath();
        Identifier a = new Identifier(stateID.getNamespace());
        while (!Registry.BLOCK.containsId(a) && path.contains("_")) {
            a = new Identifier(stateID.getNamespace(), path.substring(0, path.lastIndexOf("_")));
        }

        return Registry.BLOCK.get(a).getDefaultState();
    }
}
