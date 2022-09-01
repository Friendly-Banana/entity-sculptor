package me.banana.entity_builder.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;

import java.util.*;

import static net.minecraft.server.command.CommandManager.literal;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class EntityBuilderClient implements ClientModInitializer {
    private static double AbsMin(double a, double b) {
        return Math.min(a, Math.abs(b));
    }

    private static int execute(ServerCommandSource commandSource, Entity entity, Vec3d statueOrigin, boolean noFallingBlocks, boolean noCreativeBlocks) {
        MyVertexConsumer myVertexConsumer = new MyVertexConsumer();

        EntityRenderer<? super Entity> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        renderer.render(entity, entity.prevYaw, 0, new MatrixStack(), layer -> myVertexConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE);

        double smallest = Double.MAX_VALUE;
        for (Vertex a : myVertexConsumer.vertices) {
            smallest = AbsMin(smallest, a.position.x);
            smallest = AbsMin(smallest, a.position.y);
            smallest = AbsMin(smallest, a.position.z);
        }

        Map<Vec3d, Block> statue = new HashMap<>();
        for (Vertex vertex : myVertexConsumer.vertices) {
            // scale positions
            statue.put(vertex.position.multiply(1 / smallest), ColorMatcher.NearestBlock(vertex.color, noFallingBlocks, noCreativeBlocks));
        }

        // place blocks
        ServerWorld world = commandSource.getWorld();
        for (Vec3d pos : statue.keySet()) {
            world.setBlockState(new BlockPos(statueOrigin.add(pos)), statue.get(pos).getDefaultState());
        }

        commandSource.sendFeedback(Text.literal("Built " + entity.getName() + "."), false);
        return 1;
    }

    @Override
    public void onInitializeClient() {
        ColorMatcher.init();

        //WorldRenderEvents.AFTER_ENTITIES.register(context -> {context.matrixStack()});
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("build")
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), context.getSource().getPosition(), true, true)))
                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), true, true)))
                .then(CommandManager.argument("noFallingBlocks", BoolArgumentType.bool())
                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), BoolArgumentType.getBool(context, "noFallingBlocks"), true)))
                .then(CommandManager.argument("noCreativeBlocks", BoolArgumentType.bool())
                        .executes(context -> execute(context.getSource(), EntityArgumentType.getEntity(context, "entity"), Vec3ArgumentType.getVec3(context, "pos"), BoolArgumentType.getBool(context, "fallingBlocks"), BoolArgumentType.getBool(context, "noCreativeBlocks"))))
        ));
    }

    private static final class Vertex {
        public Vec3d position;
        public Vec3f normal;
        public Vector4f color;
        public Vec2f uv;

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Vertex) obj;
            return Objects.equals(this.position, that.position) &&
                    Objects.equals(this.normal, that.normal) &&
                    Objects.equals(this.color, that.color) &&
                    Objects.equals(this.uv, that.uv);
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, normal, color, uv);
        }

        @Override
        public String toString() {
            return "Vertex[" +
                    "position=" + position + ", " +
                    "normal=" + normal + ", " +
                    "color=" + color + ", " +
                    "uv=" + uv + ']';
        }
    }

    private static class MyVertexConsumer extends FixedColorVertexConsumer implements VertexConsumer {
        Set<Vertex> vertices = new HashSet<>();
        private Vertex current = new Vertex();

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            current.position = new Vec3d(x, y, z);
            System.out.println(new Vec3d(x, y, z));
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            current.color = new Vector4f(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            current.uv = new Vec2f(u, v);
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
}
