package me.banana.entity_sculptor.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class CollectingVertexConsumerProvider implements VertexConsumerProvider {
    private final Map<RenderLayer, CollectingVertexConsumer> perLayer = new HashMap<>();

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        perLayer.putIfAbsent(layer, new CollectingVertexConsumer());
        return perLayer.get(layer);
    }

    public Stream<Vertex> onLayer(RenderLayer layer) {
        return perLayer.containsKey(layer) ? perLayer.get(layer).getVertices() : Stream.empty();
    }

    public Stream<Vertex> allVertices() {
        return perLayer.values().stream().flatMap(CollectingVertexConsumer::getVertices);
    }

    public void clear() {
        perLayer.clear();
    }
}
