package me.banana.entity_sculptor.client;

import me.banana.entity_sculptor.EntitySculptor;
import me.banana.entity_sculptor.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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


@Environment(EnvType.CLIENT)
public class EntitySculptorClient implements ClientModInitializer {
    public static final ColorMatcher COLOR_MATCHER = new ColorMatcher();
    public static final EntityModelLayer MOVING_BLOCK_LAYER = new EntityModelLayer(Utils.Id("moving_block"), "main");
    public static final me.banana.entity_sculptor.client.ESConfig CONFIG = me.banana.entity_sculptor.client.ESConfig.createAndLoad();
    public static final Predicate<Block> SOLID_BLOCK = block -> block.getDefaultState().getMaterial().isSolid();
    public static final Predicate<Block> FALLING_BLOCK = block -> block instanceof FallingBlock;
    public static final Predicate<Block> CREATIVE_BLOCK = block -> block.getHardness() == -1.0f || block instanceof InfestedBlock;
    public static boolean installedOnServer = false;

    static {
        EntitySculptorClient.CONFIG.subscribeToCreativeBlocks(exclude -> EntitySculptorClient.filterBlocks(exclude, EntitySculptorClient.CREATIVE_BLOCK));
        EntitySculptorClient.CONFIG.subscribeToFallingBlocks(exclude -> EntitySculptorClient.filterBlocks(exclude, EntitySculptorClient.FALLING_BLOCK));
        EntitySculptorClient.CONFIG.subscribeToNonSolidBlocks(exclude -> EntitySculptorClient.filterBlocks(exclude, EntitySculptorClient.SOLID_BLOCK.negate()));

        EntitySculptorClient.CONFIG.subscribeToExcludedBlockIDs(i -> MinecraftClient.getInstance().reloadResources());
        EntitySculptorClient.CONFIG.subscribeToExcludedBlockTags(i -> MinecraftClient.getInstance().reloadResources());
    }

    public static void filterBlocks(boolean exclude, Predicate<Block> blockPredicate) {
        var excludedBlocks = CONFIG.excludedBlockIDs();
        if (exclude) {
            Registry.BLOCK.getIds()
                .stream()
                .filter(id -> blockPredicate.test(Registry.BLOCK.get(id)))
                .map(Identifier::toString)
                .filter(id -> !excludedBlocks.contains(id))
                .forEach(excludedBlocks::add);
        } else {
            excludedBlocks.removeIf(id -> blockPredicate.test(Registry.BLOCK.get(new Identifier(id))));
        }
        CONFIG.excludedBlockIDs(excludedBlocks);
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EntitySculptor.MOD_INSTALLED, (client, handler, buf, responseSender) -> installedOnServer = true);
        ClientPlayConnectionEvents.JOIN.register((networkHandler, sender, client) -> installedOnServer = false);

        StatueCommand.register();
        MatchColorCommand.register();

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(COLOR_MATCHER);

        EntityModelLayerRegistry.registerModelLayer(MOVING_BLOCK_LAYER, MovingBlockModel::getTexturedModelData);
        EntityRendererRegistry.register(EntitySculptor.MOVING_BLOCK, MovingBlockRenderer::new);
    }
}
