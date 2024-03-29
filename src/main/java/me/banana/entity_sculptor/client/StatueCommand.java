package me.banana.entity_sculptor.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.banana.entity_sculptor.EntitySculptor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.banana.entity_sculptor.client.EntitySculptorClient.COLOR_MATCHER;
import static net.minecraft.server.command.CommandManager.literal;

@Environment(EnvType.CLIENT)
public class StatueCommand {
    private static final BlockState ORIGIN_BLOCK = Blocks.OBSIDIAN.getDefaultState();
    private static final BlockState VERTEX_BLOCK = Blocks.DIAMOND_BLOCK.getDefaultState();
    private static final SimpleCommandExceptionType modNotOnServer = new SimpleCommandExceptionType(Text.literal("Please install this mod on the server."));
    private static final DynamicCommandExceptionType missingTexture = new DynamicCommandExceptionType(texture -> Text.literal("Missing texture " + texture));
    private static final Dynamic2CommandExceptionType fileError = new Dynamic2CommandExceptionType((texture, exception) -> Text.literal("Could not open %s: %s".formatted(texture, exception)));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            final LiteralCommandNode<ServerCommandSource> statue = dispatcher.register(literal("entitysculptor statue").then(CommandManager.argument("entity", EntityArgumentType.entity())
                .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), context.getSource()
                    .getPosition(), EntitySculptorClient.CONFIG.defaultScale()))
                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                    .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), EntitySculptorClient.CONFIG.defaultScale()))
                    .then(CommandManager.argument("scale", DoubleArgumentType.doubleArg())
                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), DoubleArgumentType.getDouble(context, "scale")))))));
            dispatcher.register(literal("statue").redirect(statue));
        });
    }

    public static int execute(ServerCommandSource commandSource, Entity entity, Vec3d statueOrigin, double scale) throws CommandSyntaxException {
        /* // TODO get working or wait for Fabric API
        if (!EntitySculptorClient.installedOnServer) {
            throw modNotOnServer.create();
        }*/

        Map<Vec3d, BlockState> statue = new HashMap<>();
        commandSource.sendFeedback(MutableText.of(new LiteralTextContent("Building "))
            .append(entity.getDisplayName())
            .append("..."), false);

        EntityRenderer<? super Entity> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) {
            throw new SimpleCommandExceptionType(Text.literal("Renderer is null.")).create();
        }

        var vc = new CollectingVertexConsumerProvider();
        renderer.render(entity, entity.prevYaw, 0, new MatrixStack(), vc, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        List<Vertex> vertices = vc.allVertices().toList();

        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        try (InputStream inputStream = resourceManager.getResourceOrThrow(renderer.getTexture(entity)).getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            for (int i = 0; i < vertices.size(); i += 4) {
                // get opposite edges
                Vertex minUV = vertices.get(i + 1);
                Vertex maxUV = vertices.get(i + 3);

                Direction unmappedAxis = minUV.getUnmappedAxis();
                // TODO adjust per-direction to prevent pixel bleeding from opposite face
                int u1 = Math.round(minUV.getTextureU() * image.getWidth()), v1 = Math.round((unmappedAxis == Direction.DOWN ? maxUV : minUV).getTextureV() * image.getHeight());
                int u2 = Math.round(maxUV.getTextureU() * image.getWidth()), v2 = Math.round((unmappedAxis == Direction.DOWN ? minUV : maxUV).getTextureV() * image.getHeight());

                for (int u = u1; u <= u2; u++) {
                    for (int v = v1; v <= v2; v++) {
                        ColorMatcher.RGBA color = new ColorMatcher.RGBA(image.getRGB(u % image.getWidth(), v % image.getHeight()));
                        // skip transparent pixels
                        if (color.a() == 0) continue;
                        // add potential tint
                        color = color.tint(minUV.getColor());

                        double x, y, z;
                        switch (unmappedAxis) {
                            case DOWN, UP -> {
                                x = clampedLerpFromProgress(u, u1, u2, minUV.getPosition().x, maxUV.getPosition().x);
                                z = clampedLerpFromProgress(v, v1, v2, minUV.getPosition().z, maxUV.getPosition().z);
                                y = clampedLerpFromProgress(u + v, u1 + v1, u2 + v2, minUV.getPosition().y, maxUV.getPosition().y);
                            }
                            case NORTH, SOUTH -> {
                                x = clampedLerpFromProgress(u, u1, u2, minUV.getPosition().x, maxUV.getPosition().x);
                                y = clampedLerpFromProgress(v, v1, v2, minUV.getPosition().y, maxUV.getPosition().y);
                                z = clampedLerpFromProgress(u + v, u1 + v1, u2 + v2, minUV.getPosition().z, maxUV.getPosition().z);
                            }
                            default -> {
                                z = clampedLerpFromProgress(u, u1, u2, minUV.getPosition().z, maxUV.getPosition().z);
                                y = clampedLerpFromProgress(v, v1, v2, minUV.getPosition().y, maxUV.getPosition().y);
                                x = clampedLerpFromProgress(u + v, u1 + v1, u2 + v2, minUV.getPosition().x, maxUV.getPosition().x);
                            }
                        }
                        statue.put(new Vec3d(x, y, z).multiply(scale), COLOR_MATCHER.bestBlockState(color, unmappedAxis));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw missingTexture.create(renderer.getTexture(entity));
        } catch (IOException exception) {
            throw fileError.create(renderer.getTexture(entity), exception);
        }
        if (EntitySculptorClient.CONFIG.showVertices()) {
            vertices.forEach(v -> statue.put(v.getPosition().multiply(scale), VERTEX_BLOCK));
        }

        if (EntitySculptorClient.CONFIG.showOrigin()) {
            statue.put(Vec3d.ZERO, ORIGIN_BLOCK);
        }

        // place statue
        PacketByteBuf data = PacketByteBufs.create();
        data.writeEnumConstant(EntitySculptorClient.CONFIG.setBlockMode());
        data.writeMap(statue, (buf, vec3d) -> buf.writeBlockPos(new BlockPos(statueOrigin.add(vec3d))), (buf, state) -> buf.writeRegistryValue(Block.STATE_IDS, state));
        switch (EntitySculptorClient.CONFIG.setBlockMode()) {
            case CustomPacket, WorldEdit -> ClientPlayNetworking.send(EntitySculptor.BUILD_CHANGES, data);
        }

        commandSource.sendFeedback(MutableText.of(new LiteralTextContent("Sent " + statue.size() + " blocks to server")), false);
        return statue.size();
    }

    private static double clampedLerpFromProgress(int progress, int progressStart, int progressEnd, double lerpStart, double lerpEnd) {
        return MathHelper.clampedLerp(lerpStart, lerpEnd, MathHelper.getLerpProgress((double) progress, progressStart, progressEnd));
    }
}