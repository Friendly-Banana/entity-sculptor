package me.banana.entity_builder.client;

import me.banana.entity_builder.EntityBuilder;
import me.banana.entity_builder.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.InfestedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Predicate;


@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class EntityBuilderClient implements ClientModInitializer {
    public static final ColorMatcher COLOR_MATCHER = new ColorMatcher();
    public static final EntityModelLayer MOVING_BLOCK_LAYER = new EntityModelLayer(Utils.Id("moving_block"), "main");
    public static final me.banana.entity_builder.client.EBConfig CONFIG = me.banana.entity_builder.client.EBConfig.createAndLoad();
    public static final Predicate<Block> SOLID_BLOCK = block -> block.getDefaultState().getMaterial().isSolid();
    public static final Predicate<Block> FALLING_BLOCK = block -> block instanceof FallingBlock;
    public static final Predicate<Block> CREATIVE_BLOCK = block -> block.getHardness() == -1.0f || block instanceof InfestedBlock;
    public static boolean installedOnServer = false;

    static {
        EntityBuilderClient.CONFIG.subscribeToCreativeBlocks(exclude -> EntityBuilderClient.filterBlocks(exclude, EntityBuilderClient.CREATIVE_BLOCK));
        EntityBuilderClient.CONFIG.subscribeToFallingBlocks(exclude -> EntityBuilderClient.filterBlocks(exclude, EntityBuilderClient.FALLING_BLOCK));
        EntityBuilderClient.CONFIG.subscribeToNonSolidBlocks(exclude -> EntityBuilderClient.filterBlocks(exclude, EntityBuilderClient.SOLID_BLOCK.negate()));

        EntityBuilderClient.CONFIG.subscribeToExcludedBlockIDs(i -> MinecraftClient.getInstance().reloadResources());
        EntityBuilderClient.CONFIG.subscribeToExcludedBlockTags(i -> MinecraftClient.getInstance().reloadResources());
    }

    public static void filterBlocks(boolean exclude, Predicate<Block> blockPredicate) {
        var excludedBlocks = CONFIG.excludedBlockIDs();
        if (exclude) {
            Registry.BLOCK.getIds().stream().filter(id -> blockPredicate.test(Registry.BLOCK.get(id))).map(Identifier::toString).filter(id -> !excludedBlocks.contains(id)).forEach(excludedBlocks::add);
        } else {
            excludedBlocks.removeIf(id -> blockPredicate.test(Registry.BLOCK.get(new Identifier(id))));
        }
        CONFIG.excludedBlockIDs(excludedBlocks);
    }

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
