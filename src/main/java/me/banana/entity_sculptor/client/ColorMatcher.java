package me.banana.entity_sculptor.client;

import me.banana.entity_sculptor.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockRenderView;

import java.util.*;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class ColorMatcher implements SimpleSynchronousResourceReloadListener {
    private final Map<Direction, HashMap<BlockState, RGBA>> palette = new TreeMap<>();
    private final BlockState fallbackState = Blocks.STONE.getDefaultState();

    /**
     * finds the best suited block state for the given color
     */
    public BlockState bestBlockState(RGBA color, Direction unmappedAxis) {
        return palette.get(unmappedAxis).entrySet().stream()
            .min(Comparator.comparing(e -> e.getValue().distance(color)))
            .orElse(Map.entry(fallbackState, RGBA.ZERO))
            .getKey();
    }

    public Stream<BlockState> bestBlockStates(RGBA color, Direction[] directions, int limit) {
        // TODO remove states only differing in orientation
        return Arrays.stream(directions).map(palette::get)
            .flatMap(map -> map.entrySet().stream())
            .sorted(Comparator.comparing(e -> e.getValue().distance(color)))
            .map(Map.Entry::getKey)
            .distinct()
            .limit(limit);
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
        int textures = 0, states = 0;

        final int seed = 42;
        Random random = Random.create(seed);
        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        for (Direction direction : Direction.values()) {
            HashMap<BlockState, RGBA> colorBlockStateMap = new HashMap<>();
            palette.put(direction, colorBlockStateMap);
            for (var state : Block.STATE_IDS) {
                boolean blockExcluded = EntitySculptorClient.CONFIG.excludedBlockIDs()
                    .contains(Registry.BLOCK.getId(state.getBlock()).toString());
                boolean tagExcluded = state.streamTags()
                    .anyMatch(blockTagKey -> EntitySculptorClient.CONFIG.excludedBlockTags().contains(blockTagKey.id().toString()));
                if (blockExcluded || tagExcluded || EntitySculptorClient.CONFIG.skipWaterlogged() && state.contains(Properties.WATERLOGGED) && state.get(Properties.WATERLOGGED)) {
                    continue;
                }
                // BasicBakedModel.quads and faceQuads are both important
                // 84132 block states used 17826 faceQuads vs 66306 quads
                // direction null returns quads, anything else faceQuads
                random.setSeed(seed);
                List<BakedQuad> sprites = blockRenderManager.getModel(state).getQuads(state, null, random);

                // use quads for all directions if not empty
                if (sprites.isEmpty()) {
                    random.setSeed(seed);
                    sprites = blockRenderManager.getModel(state).getQuads(state, direction, random);
                }

                // get weighted by area average of all sprites for a block state
                List<NativeImage> images = sprites.stream()
                    .map(bakedQuad -> ((SpriteImageAccesor) bakedQuad.getSprite()).getOriginalImage())
                    .toList();
                List<Double> weights = sprites.stream().map(bakedQuad -> minimalArea(direction, bakedQuad.getVertexData())).toList();

                // TODO investigate why full blocks have exactly one sprite with zero area
                if (weights.equals(List.of(0.0))) {
                    weights = List.of(1.0);
                }
                colorBlockStateMap.put(state, weightedAverage(weights, images));
                textures += images.size();
                states++;
            }
        }

        Utils.LOGGER.debug("Rebuilt palette: {}", palette);
        Utils.LOGGER.info("Added {} textures for {} block states.", textures, states);
    }

    private RGBA weightedAverage(List<Double> weights, List<NativeImage> images) {
        var avg = RGBA.ZERO;
        double totalWeight = weights.stream().mapToDouble(a -> a).sum();
        for (int i = 0; i < weights.size(); i++) {
            double weight = weights.get(i);
            if (weight == 0) continue;

            NativeImage image = images.get(i);
            var imageSum = RGBA.ZERO;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int color = image.getColor(x, y);
                    // don't count transparent pixels
                    if (NativeImage.getAlpha(color) == 0) {
                        continue;
                    }
                    imageSum = imageSum.add(NativeImage.getRed(color), NativeImage.getGreen(color), NativeImage.getBlue(color), NativeImage.getAlpha(color));
                }
            }
            int pixels = image.getHeight() * image.getWidth();
            avg = avg.add(imageSum.multiply(weight / pixels));
        }
        return avg.divide(totalWeight);
    }

    /**
     * gets the minimal area covered on one site
     * @see net.minecraft.client.render.block.BlockModelRenderer#getQuadDimensions(BlockRenderView, BlockState, BlockPos, int[], Direction, float[], BitSet)
     */
    private double minimalArea(Direction face, int[] vertexData) {
        float west = 32.0f;
        float down = 32.0f;
        float north = 32.0f;
        float east = -32.0f;
        float up = -32.0f;
        float south = -32.0f;
        for (int i = 0; i < 4; i++) {
            float x = Float.intBitsToFloat(vertexData[i * 8]);
            float y = Float.intBitsToFloat(vertexData[i * 8 + 1]);
            float z = Float.intBitsToFloat(vertexData[i * 8 + 2]);
            west = Math.min(west, x);
            down = Math.min(down, y);
            north = Math.min(north, z);
            east = Math.max(east, x);
            up = Math.max(up, y);
            south = Math.max(south, z);
        }
        return switch (face) {
            case WEST, EAST -> west - east;
            case DOWN, UP -> down - up;
            case NORTH, SOUTH -> north - south;
        };
    }

    /**
     * represents a color using four doubles in range [0-255]
     */
    record RGBA(double r, double g, double b, double a) {
        public static final RGBA ZERO = new RGBA(0, 0, 0, 0);

        public RGBA(int argb) {
            this(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >> 24 & 0xFF);
        }

        private static double subAndSquare(double a, double b) {
            return (a - b) * (a - b);
        }

        public RGBA add(double a, double b, double c, double d) {
            return new RGBA(a + this.r, b + this.g, c + this.b, d + this.a);
        }

        public RGBA add(RGBA other) {
            return new RGBA(r + other.r, g + other.g, b + other.b, a + other.a);
        }

        public RGBA multiply(double factor) {
            return new RGBA(r * factor, g * factor, b * factor, a * factor);
        }

        public RGBA divide(double factor) {
            return new RGBA(r / factor, g / factor, b / factor, a / factor);
        }

        public double distance(RGBA other) {
            return subAndSquare(r, other.r) + subAndSquare(g, other.g) + subAndSquare(b, other.b) + subAndSquare(a, other.a);
        }

        public RGBA tint(RGBA color) {
            return new RGBA(r * color.r / 255, g * color.g / 255, b * color.b / 255, a * color.a / 255);
        }
    }
}
