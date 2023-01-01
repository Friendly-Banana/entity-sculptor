package me.banana.entity_builder.client;

import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class CollectingVertexConsumer extends FixedColorVertexConsumer implements VertexConsumer {
    private final List<Vertex> vertices = new ArrayList<>();
    private Vertex current;

    public Stream<Vertex> getVertices() {
        return vertices.stream();
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        current = new Vertex();
        current.setPosition(new Vec3d(x, y, z));
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        current.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        current.setTexture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        current.setOverlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        current.setLight(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        current.setNormal(new Vec3f(x, y, z));
        return this;
    }

    @Override
    public void next() {
        vertices.add(current);
        current = null;
    }
}
