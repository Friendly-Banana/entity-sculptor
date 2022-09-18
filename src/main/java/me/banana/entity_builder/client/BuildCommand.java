package me.banana.entity_builder.client;


import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import io.netty.buffer.Unpooled;
import me.banana.entity_builder.EntityBuilder;
import me.banana.entity_builder.Utils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.resource.ResourceManager;
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

import static me.banana.entity_builder.client.EntityBuilderClient.colorMatcher;
import static net.minecraft.server.command.CommandManager.literal;

public class BuildCommand {

    public static void register() {
        float scale = 10;
        boolean noFallingBlocks = true, noCreativeBlocks = true;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("build")
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), context.getSource().getPosition(), scale, noFallingBlocks, noCreativeBlocks))
                        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), scale, noFallingBlocks, noCreativeBlocks))
                                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), FloatArgumentType.getFloat(context, "scale"), noFallingBlocks, noCreativeBlocks))
                                        .then(CommandManager.argument("noFallingBlocks", BoolArgumentType.bool())
                                                .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), FloatArgumentType.getFloat(context, "scale"), BoolArgumentType.getBool(context, "noFallingBlocks"), noCreativeBlocks))
                                                .then(CommandManager.argument("noCreativeBlocks", BoolArgumentType.bool())
                                                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), FloatArgumentType.getFloat(context, "scale"), BoolArgumentType.getBool(context, "fallingBlocks"), BoolArgumentType.getBool(context, "noCreativeBlocks")))
                                                )))))
        ));
    }

    public static int execute(ServerCommandSource commandSource, Entity entity, Vec3d statueOrigin, float scale, boolean noFallingBlocks, boolean noCreativeBlocks) {
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
                Vertex a = vertices.get(i), b = vertices.get(i + 1), c = vertices.get(i + 2), d = vertices.get(i + 3);
                List<Vertex> ver = List.of(a, b, c, d);
                /*// remap to 0 - 1
                List<Vertex> sortedU = ver.stream().sorted((f, e) -> Float.compare(f.uv.x, e.uv.x)).toList();
                List<Vertex> sortedV = ver.stream().sorted((f, e) -> Float.compare(f.uv.y, e.uv.y)).toList();
                float minU = sortedU.get(0).uv.x, maxU = sortedU.get(sortedU.size() - 1).uv.x;
                float minV = sortedV.get(0).uv.y, maxV = sortedV.get(sortedV.size() - 1).uv.y;
                for (Vertex v : ver) {
                    v.uv = new Vec2f((v.uv.x - minU) / (maxU - minU), (v.uv.y - minV) / (maxV - minV));
                }*/
                Optional<Vertex> min = ver.stream().min((f, e) -> Float.compare(f.uv.x, e.uv.x) + Float.compare(f.uv.y, e.uv.y));
                Optional<Vertex> max = ver.stream().max((f, e) -> Float.compare(f.uv.x, e.uv.x) + Float.compare(f.uv.y, e.uv.y));
                a = min.get();
                c = max.get();
                Direction.Axis unmappedAxis = getUnmappedAxis(a);
                Utils.log(unmappedAxis);
                //Utils.log(a.uv.x, a.uv.y, c.uv.x, c.uv.y);
                int u1 = Math.round(a.uv.x * image.getWidth()), v1 = Math.round(a.uv.y * image.getHeight());
                int u2 = Math.round(c.uv.x * image.getWidth()), v2 = Math.round(c.uv.y * image.getHeight());
                Utils.log(u1, u2, v1, v2);
                for (int u = u1; u <= u2; u++) {
                    double uProgress = MathHelper.getLerpProgress((double) u, u1, u2);
                    for (int v = v1; v <= v2; v++) {
                        double vProgress = MathHelper.getLerpProgress((double) v, v1, v2);
                        double progress = 0.5;//(uProgress + vProgress) / 2;MathHelper.getLerpProgress(u+v, u1+v1, u2+v2)
                        double x, y, z;
                        if (unmappedAxis == Direction.Axis.Y) {
                            x = MathHelper.clampedLerpFromProgress(u, u1, u2, a.position.x, c.position.x);
                            z = MathHelper.clampedLerpFromProgress(v, v1, v2, a.position.z, c.position.z);
                            y = MathHelper.lerp(a.position.y, c.position.y, progress);
                        } else if (unmappedAxis == Direction.Axis.Z) {
                            x = MathHelper.clampedLerpFromProgress(u, u1, u2, a.position.x, c.position.x);
                            y = MathHelper.clampedLerpFromProgress(v, v1, v2, a.position.y, c.position.y);
                            z = MathHelper.lerp(a.position.z, c.position.z, progress);
                        } else {
                            z = MathHelper.clampedLerpFromProgress(u, u1, u2, a.position.z, c.position.z);
                            y = MathHelper.clampedLerpFromProgress(v, v1, v2, a.position.y, c.position.y);
                            x = MathHelper.lerp(a.position.x, c.position.x, progress);
                        }
                        //Utils.log(u, v, image.getWidth(), image.getHeight());
                        statue.put(new Vec3d(x, y, z).multiply(scale), colorMatcher.NearestBlock(image.getRGB(u % image.getWidth(), v % image.getHeight()), noFallingBlocks, noCreativeBlocks));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Utils.LOGGER.warn("Missing texture for " + renderer.getTexture(entity));
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*List<Vertex> ver = List.of(a, b, c, d);
        Map<Double, String> F = new HashMap<>();
        for (int j = 0; j < ver.size(); j++) {
            for (int g = 0; g < ver.size(); g++) {
                if (j != g) {
                    Vec3d l = ver.get(j).position.relativize(ver.get(g).position);
                    F.put(l.lengthSquared(), j + " to " + g);
                }
            }
        }*/

        // edges
        vertices.forEach(v -> statue.put(v.position.multiply(scale), new Identifier("diamond_block")));

        // place blocks
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeMap(statue, (buf, v) -> buf.writeBlockPos(new BlockPos(statueOrigin.add(v))), PacketByteBuf::writeIdentifier);
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(EntityBuilder.BUILD_CHANGES, data));

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
     * u v  side    normal  unmapped axis
     * x z	top     0 1 0   Direction.Axis.Y
     * x y	front   0 0 1   Direction.Axis.Z
     * z y	right   1 0 0   Direction.Axis.X
     */
    private static Direction.Axis getUnmappedAxis(Vertex a) {
        float x = Math.abs(a.normal.getX()), y = Math.abs(a.normal.getY()), z = Math.abs(a.normal.getZ());
        if (x > y && x > z) return Direction.Axis.Y;
        else if (y > x && y > z) return Direction.Axis.Z;
        else if (z > x && z > y) return Direction.Axis.X;
        return Direction.Axis.Y;
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