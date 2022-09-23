package me.banana.entity_builder.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import me.banana.entity_builder.EntityBuilder;
import me.banana.entity_builder.Utils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

import static me.banana.entity_builder.client.EntityBuilderClient.COLOR_MATCHER;
import static net.minecraft.server.command.CommandManager.literal;

public class BuildCommand {

    public static void register() {
        double scale = 1;
        boolean noFallingBlocks = true, noCreativeBlocks = true;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("build")
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), context.getSource().getPosition(), scale, noFallingBlocks, noCreativeBlocks))
                        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), scale, noFallingBlocks, noCreativeBlocks))
                                .then(CommandManager.argument("scale", DoubleArgumentType.doubleArg())
                                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), DoubleArgumentType.getDouble(context, "scale"), noFallingBlocks, noCreativeBlocks))
                                        .then(CommandManager.argument("noFallingBlocks", BoolArgumentType.bool())
                                                .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), DoubleArgumentType.getDouble(context, "scale"), BoolArgumentType.getBool(context, "noFallingBlocks"), noCreativeBlocks))
                                                .then(CommandManager.argument("noCreativeBlocks", BoolArgumentType.bool())
                                                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), DoubleArgumentType.getDouble(context, "scale"), BoolArgumentType.getBool(context, "fallingBlocks"), BoolArgumentType.getBool(context, "noCreativeBlocks")))
                                                )))))
        ));
    }

    static double quadrupleLerp(double lerp1, Vertex v1, Vertex v2, double lerp2, Vertex v3, Vertex v4, Function<Vertex, Double> getter) {
        double x1 = getter.apply(v1), x2 = getter.apply(v2), x3 = getter.apply(v3), x4 = getter.apply(v4);
        return (lerp1 * x1 + (1 - lerp1) * x2 + lerp2 * x3 + (1 - lerp2) * x4) / 4;
    }

    static Vec3d lerp(double v1, double s1, double e1, double v2, double s2, double e2, Vertex[] vertices) {
        double p1 = MathHelper.getLerpProgress(v1, s1, e1), p2 = MathHelper.getLerpProgress(v2, s2, e2);
        Vec3d a = vertices[1].position, e = vertices[0].position.subtract(a);
        Vec3d c = vertices[3].position, f = vertices[2].position.subtract(c);

        return a.add(e.multiply(p1)).add(c.add(f.multiply(p1))).multiply(p2);
        //return vertices[0].position.multiply((1 - p1) * (1 - p2)).add(vertices[1].position.multiply(p1 * (1 - p2))).add(vertices[2].position.multiply((1 - p1) * p2)).add(vertices[3].position.multiply(p1 * p2));
    }

    public static int execute(ServerCommandSource commandSource, Entity entity, Vec3d statueOrigin, double scale, boolean noFallingBlocks, boolean noCreativeBlocks) {
        Map<RenderLayer, MyVertexConsumer> l = new HashMap<>();

        MyVertexConsumer myVertexConsumer = new MyVertexConsumer();
        EntityRenderer<? super Entity> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) {
            commandSource.sendFeedback(Text.literal("Renderer is null."), false);
            return -1;
        }
        renderer.render(entity, entity.prevYaw, 0, new MatrixStack(), layer -> myVertexConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        // convert vertices into blocks
        Map<Vec3d, Identifier> statue = new HashMap<>();
        List<Vertex> vertices = myVertexConsumer.getVertices();

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
                Direction unmappedAxis = getUnmappedAxis(minUV);
                //Utils.log(unmappedAxis);
                int u1 = Math.round(minUV.uv.x * image.getWidth()), v1 = Math.round(minUV.uv.y * image.getHeight());
                int u2 = Math.round(maxUV.uv.x * image.getWidth()), v2 = Math.round(maxUV.uv.y * image.getHeight());
                //Utils.log(u1, u2, v1, v2);

                // scale to one block - one pixel
                int uDiff = u2 - u1;
                double xDiff = switch (unmappedAxis) {
                    case EAST, WEST -> maxUV.position.z - minUV.position.z;
                    default -> maxUV.position.x - minUV.position.x;
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
                                x = MathHelper.clampedLerpFromProgress(u, u1, u2, minUV.position.x, maxUV.position.x);
                                z = MathHelper.clampedLerpFromProgress(v, v1, v2, minUV.position.z, maxUV.position.z);
                                y = MathHelper.clampedLerpFromProgress(u + v, u1 + v1, u2 + v2, minUV.position.y, maxUV.position.y);
                            }
                            case NORTH, SOUTH -> {
                                x = MathHelper.clampedLerpFromProgress(u, u1, u2, minUV.position.x, maxUV.position.x);
                                y = MathHelper.clampedLerpFromProgress(v, v1, v2, minUV.position.y, maxUV.position.y);
                                z = MathHelper.clampedLerpFromProgress(u + v, u1 + v1, u2 + v2, minUV.position.z, maxUV.position.z);
                            }
                            default -> {
                                z = MathHelper.clampedLerpFromProgress(u, u1, u2, minUV.position.z, maxUV.position.z);
                                y = MathHelper.clampedLerpFromProgress(v, v1, v2, minUV.position.y, maxUV.position.y);
                                x = MathHelper.clampedLerpFromProgress(u + v, u1 + v1, u2 + v2, minUV.position.x, maxUV.position.x);
                            }
                        }//lerp(u, u1, u2, v, v1, v2, ver)
                        statue.put(new Vec3d(x, y, z).multiply(uvScale * scale), COLOR_MATCHER.NearestBlock(image.getRGB(u % image.getWidth(), v % image.getHeight()), noFallingBlocks, noCreativeBlocks));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Utils.LOGGER.warn("Missing texture for " + renderer.getTexture(entity));
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO config option: show vertices
        //vertices.forEach(v -> statue.put(v.position.multiply(scale), new Identifier("diamond_block")));

        // place blocks
        PacketByteBuf data = PacketByteBufs.create();
        data.writeEnumConstant(EntityBuilderClient.setBlockMode);
        data.writeMap(statue, (buf, v) -> buf.writeBlockPos(new BlockPos(statueOrigin.add(v))), PacketByteBuf::writeIdentifier);
        switch (EntityBuilderClient.setBlockMode) {
            case SetBlock -> {
            }
            case CustomPacket, Worldedit -> ClientPlayNetworking.send(EntityBuilder.BUILD_CHANGES, data);
        }

        commandSource.sendFeedback(MutableText.of(new LiteralTextContent("Building ")).append(entity.getDisplayName()).append("."), false);
        return 1;
    }

    /**
     * <table>
     * <tr> <td> u v </td> <td> side </td> <td> normal </td> <td> unmapped axis </td> </tr>
     * <tr> <td> x z </td> <td> top  </td> <td> 0 1 0 </td> <td> Direction.Axis.Y </td> </tr>
     * <tr> <td> x y </td> <td> front </td> <td> 0 0 1 </td> <td> Direction.Axis.Z </td> </tr>
     * <tr> <td> z y </td> <td> right </td> <td> 1 0 0 </td> <td> Direction.Axis.X </td> </tr>
     * </table>
     * u v  side    normal
     * x z	top     0 1 0
     * x y	front   0 0 1
     * z y	right   1 0 0
     */
    private static Direction getUnmappedAxis(Vertex a) {
        float x = Math.abs(a.normal.getX()), y = Math.abs(a.normal.getY()), z = Math.abs(a.normal.getZ());
        if (y > x && y > z) return a.normal.getY() > 0 ? Direction.UP : Direction.DOWN;
        else if (z > x && z > y) return a.normal.getZ() > 0 ? Direction.SOUTH : Direction.NORTH;
        else if (x > y && x > z) return a.normal.getX() > 0 ? Direction.EAST : Direction.WEST;
        return Direction.UP;
    }

    private static class MyVertexConsumer extends FixedColorVertexConsumer implements VertexConsumer {
        private final List<Vertex> vertices = new ArrayList<>();
        private Vertex current = new Vertex();

        public List<Vertex> getVertices() {
            return vertices;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            current.position = new Vec3d(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            current.uv = new Vec2f(u, v);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            current.normal = new Vec3f(x, y, z);
            return this;
        }

        @Override
        public void next() {
            vertices.add(current);
            current = new Vertex();
        }
    }

    private static final class Vertex {
        public Vec3d position;
        public Vec3f normal;
        public Vec2f uv;

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Vertex) obj;
            return Objects.equals(this.position, that.position) &&
                    Objects.equals(this.normal, that.normal) &&
                    Objects.equals(this.uv, that.uv);
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, normal, uv);
        }

        @Override
        public String toString() {
            return "Vertex[" +
                    "position=" + position + ", " +
                    "normal=" + normal + ", " +
                    "uv=" + uv + ']';
        }
    }
}