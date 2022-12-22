package me.banana.entity_builder.client;

import me.banana.entity_builder.EntityBuilder;
import me.banana.entity_builder.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.resource.ResourceType;


@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class EntityBuilderClient implements ClientModInitializer {
    public static final ColorMatcher COLOR_MATCHER = new ColorMatcher();
    public static final EntityModelLayer MOVING_BLOCK_LAYER = new EntityModelLayer(Utils.Id("moving_block"), "main");
    public static boolean installedOnServer = false;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EntityBuilder.MOD_INSTALLED, (client, handler, buf, responseSender) -> installedOnServer = true);
        ClientPlayConnectionEvents.JOIN.register((networkHandler, sender, client) -> installedOnServer = false);

        BuildCommand.register();

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(COLOR_MATCHER);

        EntityModelLayerRegistry.registerModelLayer(MOVING_BLOCK_LAYER, MovingBlockModel::getTexturedModelData);
        EntityRendererRegistry.register(EntityBuilder.MOVING_BLOCK, MovingBlockRenderer::new);
    }
}
