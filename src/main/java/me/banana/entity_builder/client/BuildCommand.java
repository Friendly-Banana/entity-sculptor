package me.banana.entity_builder.client;


import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import io.netty.buffer.Unpooled;
import me.banana.entity_builder.EntityBuilder;
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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

import java.util.*;

import static me.banana.entity_builder.Utils.log;
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
        for (int i = 0; i < vertices.size(); i += 4) {
            Vertex a = vertices.get(i), b = vertices.get(i + 1), c = vertices.get(i + 2), d = vertices.get(i + 3);
            List<Vertex> ver = List.of(a, b, c, d);
            Map<Double, String> F = new HashMap<>();
            for (int j = 0; j < ver.size(); j++) {
                for (int g = 0; g < ver.size(); g++) {
                    if (j != g) {
                        Vec3d l = ver.get(j).position.relativize(ver.get(g).position);
                        F.put(l.lengthSquared(), j + " to " + g);
                    }
                }
            }
            log(F.get(F.keySet().stream().max(Double::compare).get()));
            Vec3d f = d.position.relativize(b.position);
            for (int u = 0; u <= 10; u++) {
                for (int v = 0; v <= 10; v++) {
                    for (int zv = 0; zv <= 10; zv++) {
                        float x = u / 10f;
                        float y = v / 10f;
                        float z = zv / 10f;
                        statue.put(new Vec3d(d.position.x + f.x * x, d.position.y + f.y * y, d.position.z + f.z * z).multiply(scale), new Identifier("stone"));
                    }
                }
            }
        }

        /*
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        try (InputStream inputStream = resourceManager.getResourceOrThrow(renderer.getTexture(entity)).getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);

            // interpolate vertices for complete model
            List<Vertex> vertices = myVertexConsumer.getVertices();
            for (int i = 0; i < vertices.size(); i += 3) {
                Vertex a = vertices.get(i), b = vertices.get(i + 1), c = vertices.get(i + 2);
                Utils.log(a.position, b.position, c.position);
            }

            Vertex lastVertex = vertices.get(0);
            for (Vertex vertex : vertices) {
                int lx = (int) (lastVertex.uv.x * image.getWidth());
                int x = (int) (vertex.uv.x * image.getWidth());
                float scaleX = (lastVertex.uv.x - vertex.uv.x) / (lx - x);

                int y = (int) (vertex.uv.y * image.getHeight());

                /*int d = Math.abs(lx - x);
                for (int u = 0; u < d; u++) {
                    for (int v = (int) (lastVertex.uv.y * image.getHeight()); v < (int) (vertex.uv.y * image.getHeight()); v++) {
                        float p = (float)u / d;
                        lastVertex.uv.multiply(p).add(vertex.uv.multiply(1 - p));
                        var vertexColor = image.getRGB(u, v);
                        statue.put(vertex.position.multiply(scaleX), ColorMatcher.NearestBlock(vertexColor, noFallingBlocks, noCreativeBlocks));
                    }
                }
                Vertex n = new Vertex();
                n.position = lastVertex.position.add(vertex.position).multiply(0.5f);
                n.uv = lastVertex.uv.add(vertex.uv).multiply(0.5f);
                myVertexConsumer.getVertices().add(n);
                lastVertex = vertex;* /
            statue.put(vertex.position.multiply(scaleX * scale), colorMatcher.NearestBlock(image.getRGB(x, y), noFallingBlocks, noCreativeBlocks));
        }
        } catch (FileNotFoundException e) {
                LOGGER.warn("Missing texture for " + renderer.getTexture(entity));
                return -1;
                } catch (IOException e) {
                e.printStackTrace();
            }
        */

        // place blocks
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeMap(statue, (buf, v) -> buf.writeBlockPos(new BlockPos(statueOrigin.add(v))), PacketByteBuf::writeIdentifier);
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(EntityBuilder.BUILD_CHANGES, data));

        commandSource.sendFeedback(MutableText.of(new LiteralTextContent("Building ")).append(entity.getDisplayName()).append("."), false);
        return 1;
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