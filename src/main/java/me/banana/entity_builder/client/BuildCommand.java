package me.banana.entity_builder.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import me.banana.entity_builder.EntityBuilder;
import me.banana.entity_builder.Utils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static me.banana.entity_builder.client.EntityBuilderClient.COLOR_MATCHER;
import static net.minecraft.server.command.CommandManager.literal;

public class BuildCommand {
    public static void register() {
        double scale = EntityBuilder.CONFIG.defaultScale();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("build").then(CommandManager.argument("entity", EntityArgumentType.entity()).executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), context.getSource().getPosition(), scale)).then(CommandManager.argument("pos", Vec3ArgumentType.vec3()).executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), scale)).then(CommandManager.argument("scale", DoubleArgumentType.doubleArg()).executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), DoubleArgumentType.getDouble(context, "scale"))))))));
    }

    static double quadrupleLerp(double lerp1, Vertex v1, Vertex v2, double lerp2, Vertex v3, Vertex v4, Function<Vertex, Double> getter) {
        double x1 = getter.apply(v1), x2 = getter.apply(v2), x3 = getter.apply(v3), x4 = getter.apply(v4);
        return (lerp1 * x1 + (1 - lerp1) * x2 + lerp2 * x3 + (1 - lerp2) * x4) / 4;
    }

    static Vec3d lerp(double v1, double s1, double e1, double v2, double s2, double e2, Vertex[] vertices) {
        double p1 = MathHelper.getLerpProgress(v1, s1, e1), p2 = MathHelper.getLerpProgress(v2, s2, e2);
        Vec3d a = vertices[1].getPosition(), e = vertices[0].getPosition().subtract(a);
        Vec3d c = vertices[3].getPosition(), f = vertices[2].getPosition().subtract(c);

        return a.add(e.multiply(p1)).add(c.add(f.multiply(p1))).multiply(p2);
        //return vertices[0].position.multiply((1 - p1) * (1 - p2)).add(vertices[1].position.multiply(p1 * (1 - p2))).add(vertices[2].position.multiply((1 - p1) * p2)).add(vertices[3].position.multiply(p1 * p2));
    }

    public static int execute(ServerCommandSource commandSource, Entity entity, Vec3d statueOrigin, double scale) {
        var vc = new CollectingVertexConsumerProvider();

        EntityRenderer<? super Entity> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) {
            commandSource.sendFeedback(Text.literal("Renderer is null."), false);
            return -1;
        }
        renderer.render(entity, entity.prevYaw, 0, new MatrixStack(), vc, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        // convert vertices into statue
        Map<Vec3d, BlockState> statue = new HashMap<>();
        List<Vertex> vertices = vc.allVertices().toList();

        /*Function<Object, String> vec3Pr = v -> v.toString().substring(1, v.toString().length() - 1);
        String s = EntityType.getId(entity.getType()).getPath();
        try (FileWriter myWriter = new FileWriter(s)) {
            myWriter.write(vertices.stream().map(v -> vec3Pr.apply(v.position.toString()) + "|" + vec3Pr.apply(v.normal.toString()) + "|" + v.uv.x + ", " + v.uv.y).collect(Collectors.joining("\n")));
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        try (InputStream inputStream = resourceManager.getResourceOrThrow(renderer.getTexture(entity)).getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            for (int i = 0; i < vertices.size(); i += 4) {
                Vertex[] ver = new Vertex[]{vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 3)};
                // get opposite edges
                Vertex minUV = ver[1];
                Vertex maxUV = ver[3];
                //Vertex minUV = ver.stream().min((f, e) -> Float.compare(f.uv.x, e.uv.x) + Float.compare(f.uv.y, e.uv.y)).get();
                //Vertex maxUV = ver.stream().max((f, e) -> Float.compare(f.uv.x, e.uv.x) + Float.compare(f.uv.y, e.uv.y)).get();
                Direction unmappedAxis = minUV.getUnmappedAxis();
                //Utils.log(unmappedAxis);
                int u1 = Math.round(minUV.getTextureU() * image.getWidth()), v1 = Math.round(minUV.getTextureV() * image.getHeight());
                int u2 = Math.round(maxUV.getTextureU() * image.getWidth()), v2 = Math.round(maxUV.getTextureV() * image.getHeight());
                //Utils.log(u1, u2, v1, v2);

                // scale so one block equals one pixel
                int uDiff = u2 - u1;
                double xDiff = switch (unmappedAxis) {
                    case EAST, WEST -> maxUV.getPosition().z - minUV.getPosition().z;
                    default -> maxUV.getPosition().x - minUV.getPosition().x;
                };
                double uvScale = Math.round(Math.abs(uDiff / xDiff));
                Utils.log(uvScale);

                for (int u = u1; u <= u2; u++) {
                    for (int v = v1; v <= v2; v++) {
                        double x, y, z;
                        switch (unmappedAxis) {
                            case DOWN, UP -> {
                                /*double uP = MathHelper.getLerpProgress(u, u1, u2);
                                double vP = MathHelper.getLerpProgress(v, v1, v2);
                                x = quadrupleLerp(uP, ver[0], ver[1], vP, ver[2], ver[3], h -> h.position.x);*/
                                x = MathHelper.clampedLerpFromProgress(u, u1, u2, minUV.getPosition().x, maxUV.getPosition().x);
                                z = MathHelper.clampedLerpFromProgress(v, v1, v2, minUV.getPosition().z, maxUV.getPosition().z);
                                y = MathHelper.clampedLerpFromProgress(u + v, u1 + v1, u2 + v2, minUV.getPosition().y, maxUV.getPosition().y);
                            }
                            case NORTH, SOUTH -> {
                                x = MathHelper.clampedLerpFromProgress(u, u1, u2, minUV.getPosition().x, maxUV.getPosition().x);
                                y = MathHelper.clampedLerpFromProgress(v, v1, v2, minUV.getPosition().y, maxUV.getPosition().y);
                                z = MathHelper.clampedLerpFromProgress(u + v, u1 + v1, u2 + v2, minUV.getPosition().z, maxUV.getPosition().z);
                            }
                            default -> {
                                z = MathHelper.clampedLerpFromProgress(u, u1, u2, minUV.getPosition().z, maxUV.getPosition().z);
                                y = MathHelper.clampedLerpFromProgress(v, v1, v2, minUV.getPosition().y, maxUV.getPosition().y);
                                x = MathHelper.clampedLerpFromProgress(u + v, u1 + v1, u2 + v2, minUV.getPosition().x, maxUV.getPosition().x);
                            }
                        }//lerp(u, u1, u2, v, v1, v2, ver)
                        statue.put(new Vec3d(x, y, z).multiply(uvScale * scale), COLOR_MATCHER.nearestBlock(image.getRGB(u % image.getWidth(), v % image.getHeight()), unmappedAxis));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Utils.LOGGER.warn("Missing texture for " + renderer.getTexture(entity));
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (EntityBuilder.CONFIG.showVertices()) {
            vertices.forEach(v -> statue.put(v.getPosition().multiply(scale), Registry.BLOCK.get(new Identifier("diamond_block")).getDefaultState()));
        }

        if (!EntityBuilderClient.installedOnServer) {
            commandSource.sendFeedback(MutableText.of(new LiteralTextContent("Please install this mod on the server.")), false);
            return 0;
        }
        // place statue
        PacketByteBuf data = PacketByteBufs.create();
        data.writeEnumConstant(EntityBuilder.CONFIG.setBlockMode());
        data.writeMap(statue, (buf, vec3d) -> buf.writeBlockPos(new BlockPos(statueOrigin.add(vec3d))), (buf, state) -> buf.writeRegistryValue(Block.STATE_IDS, state));
        switch (EntityBuilder.CONFIG.setBlockMode()) {
            case CustomPacket, WorldEdit -> ClientPlayNetworking.send(EntityBuilder.BUILD_CHANGES, data);
        }

        commandSource.sendFeedback(MutableText.of(new LiteralTextContent("Building ")).append(entity.getDisplayName()).append("..."), false);
        return 1;
    }
}