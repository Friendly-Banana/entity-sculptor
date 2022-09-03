package me.banana.entity_builder.client;

import me.banana.entity_builder.MovingBlockEntity;
import me.banana.entity_builder.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class EntityBuilderClient implements ClientModInitializer {
    public static final EntityModelLayer MOVING_BLOCK_LAYER = new EntityModelLayer(Utils.NewIdentifier("moving_block"), "main");
    public final static EntityType<MovingBlockEntity> MOVING_BLOCK = FabricEntityTypeBuilder.createLiving().entityFactory(MovingBlockEntity::new).dimensions(EntityDimensions.fixed(1f, 1f)).build();

    @Override
    public void onInitializeClient() {
        BuildCommand.register();
        RegistryEntryAddedCallback.event(Registry.BLOCK).register((rawId, id, block) -> ColorMatcher.AddBlock(id, block));

        EntityModelLayerRegistry.registerModelLayer(MOVING_BLOCK_LAYER, MovingBlockModel::getTexturedModelData);
        EntityRendererRegistry.register(MOVING_BLOCK, MovingBlockRenderer::new);

        // potentially server stuff
        Registry.register(Registry.ENTITY_TYPE, Utils.NewIdentifier("moving_block"), MOVING_BLOCK);

    }
}
