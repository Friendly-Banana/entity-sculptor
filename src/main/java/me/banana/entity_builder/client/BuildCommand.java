package me.banana.entity_builder.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import me.banana.entity_builder.EntityBuilder;
import me.banana.entity_builder.Utils;
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

import static me.banana.entity_builder.client.EntityBuilderClient.COLOR_MATCHER;
import static net.minecraft.server.command.CommandManager.literal;

public class BuildCommand {
    public static void register() {
        double scale = EntityBuilder.CONFIG.defaultScale();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("build").then(CommandManager.argument("entity", EntityArgumentType.entity()).executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), context.getSource().getPosition(), scale)).then(CommandManager.argument("pos", Vec3ArgumentType.vec3()).executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), scale)).then(CommandManager.argument("scale", DoubleArgumentType.doubleArg()).executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), DoubleArgumentType.getDouble(context, "scale"))))))));
    }

    public static int execute(ServerCommandSource commandSource, Entity entity, Vec3d statueOrigin, double scale) {
        /*if (!EntityBuilderClient.installedOnServer) {
            commandSource.sendFeedback(MutableText.of(new LiteralTextContent("Please install this mod on the server.")), false);
            return -1;
        }*/

        EntityRenderer<? super Entity> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) {
            commandSource.sendFeedback(Text.literal("Renderer is null."), false);
            return -1;
        }

        commandSource.sendFeedback(MutableText.of(new LiteralTextContent("Building ")).append(entity.getDisplayName()).append("..."), false);
        var vc = new CollectingVertexConsumerProvider();
        renderer.render(entity, entity.prevYaw, 0, new MatrixStack(), vc, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        // convert vertices into statue
        Map<Vec3d, BlockState> statue = new HashMap<>();
        List<Vertex> vertices = vc.allVertices().toList();

        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        try (InputStream inputStream = resourceManager.getResourceOrThrow(renderer.getTexture(entity)).getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            for (int i = 0; i < vertices.size(); i += 4) {
                Vertex[] ver = new Vertex[]{vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 3)};
                // get opposite edges
                Vertex minUV = ver[1];
                Vertex maxUV = ver[3];

                Direction unmappedAxis = minUV.getUnmappedAxis();
                int u1 = Math.round(minUV.getTextureU() * image.getWidth()), v1 = Math.round(minUV.getTextureV() * image.getHeight());
                int u2 = Math.round(maxUV.getTextureU() * image.getWidth()), v2 = Math.round(maxUV.getTextureV() * image.getHeight());

                // scale so one block equals one pixel
                int uDiff = u2 - u1;
                double xDiff = switch (unmappedAxis) {
                    case EAST, WEST -> maxUV.getPosition().z - minUV.getPosition().z;
                    default -> maxUV.getPosition().x - minUV.getPosition().x;
                };
                double uvScale = Math.round(Math.abs(uDiff / xDiff));
                if (uvScale == 0) {
                    continue;
                }
                minUV.setPosition(minUV.getPosition().multiply(uvScale * scale));
                maxUV.setPosition(maxUV.getPosition().multiply(uvScale * scale));

                for (int u = u1; u <= u2; u++) {
                    for (int v = v1; v <= v2; v++) {
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
                        statue.put(new Vec3d(x, y, z), COLOR_MATCHER.bestBlockState(image.getRGB(u % image.getWidth(), v % image.getHeight()), unmappedAxis));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Utils.LOGGER.warn("Missing texture for " + renderer.getTexture(entity));
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (EntityBuilder.CONFIG.showOrigin()) {
            statue.put(statueOrigin, Blocks.OBSIDIAN.getDefaultState());
        }
        if (EntityBuilder.CONFIG.showVertices()) {
            vertices.forEach(v -> statue.put(v.getPosition().multiply(scale), Blocks.DIAMOND_BLOCK.getDefaultState()));
        }

        // place statue
        PacketByteBuf data = PacketByteBufs.create();
        data.writeEnumConstant(EntityBuilder.CONFIG.setBlockMode());
        data.writeMap(statue, (buf, vec3d) -> buf.writeBlockPos(new BlockPos(statueOrigin.add(vec3d))), (buf, state) -> buf.writeRegistryValue(Block.STATE_IDS, state));
        switch (EntityBuilder.CONFIG.setBlockMode()) {
            case CustomPacket, WorldEdit -> ClientPlayNetworking.send(EntityBuilder.BUILD_CHANGES, data);
        }

        commandSource.sendFeedback(MutableText.of(new LiteralTextContent("Sent " + statue.size() + " blocks to server")), false);
        return 1;
    }

    private static double clampedLerpFromProgress(int progress, int progressStart, int progressEnd, double lerpStart, double lerpEnd) {
        return MathHelper.clampedLerp(lerpStart, lerpEnd, MathHelper.getLerpProgress((double) progress, progressStart, progressEnd));
    }
}