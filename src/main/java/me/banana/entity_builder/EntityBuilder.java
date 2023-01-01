package me.banana.entity_builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.session.SessionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.Map;

public class EntityBuilder implements ModInitializer {
    public final static EntityType<MovingBlockEntity> MOVING_BLOCK = FabricEntityTypeBuilder.createMob().defaultAttributes(MobEntity::createMobAttributes).entityFactory(MovingBlockEntity::new).dimensions(EntityDimensions.fixed(1f, 1f)).build();
    public static final Identifier BUILD_CHANGES = Utils.Id("build_changes");
    public static final Identifier MOD_INSTALLED = Utils.Id("mod_installed");

    private static void receiveStatueToBuild(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        SetBlockMode setBlockMode = buf.readEnumConstant(SetBlockMode.class);
        Map<BlockPos, BlockState> statue = buf.readMap(PacketByteBuf::readBlockPos, buffer -> buffer.readRegistryValue(Block.STATE_IDS));

        // All operations on the server or world must be executed on the server thread
        server.execute(() -> {
            int changedBlockCount = 0;
            if (setBlockMode == SetBlockMode.WorldEdit && FabricLoader.getInstance().isModLoaded("worldedit")) {
                Player actor = FabricAdapter.adaptPlayer(player);
                SessionManager manager = WorldEdit.getInstance().getSessionManager();
                LocalSession localSession = manager.get(actor);

                try (EditSession editSession = localSession.createEditSession(actor)) {
                    if (localSession.getBlockChangeLimit() > -1 && statue.size() > editSession.getBlockChangeLimit()) {
                        player.sendMessage(Text.of("Change blockChangeLimit, statue has " + statue.size() + " blocks."));
                        return;
                    }
                    for (var entry : statue.entrySet()) {
                        if (editSession.setBlock(FabricAdapter.adapt(entry.getKey()), FabricAdapter.adapt(entry.getValue()))) {
                            changedBlockCount++;
                        }
                    }
                    localSession.remember(editSession);
                } catch (MaxChangedBlocksException e) {
                    player.sendMessage(Text.of(e.toString()));
                }
            } else {
                for (BlockPos blockPos : statue.keySet()) {
                    player.getWorld().setBlockState(blockPos, statue.get(blockPos));
                    changedBlockCount++;
                }
            }
            player.sendMessage(Text.of("Built statue, changed " + changedBlockCount + " blocks."));
        });
    }

    @Override
    public void onInitialize() {
        Registry.register(Registry.ENTITY_TYPE, Utils.Id("moving_block"), MOVING_BLOCK);
        ServerPlayNetworking.registerGlobalReceiver(BUILD_CHANGES, EntityBuilder::receiveStatueToBuild);
        ServerPlayConnectionEvents.JOIN.register((networkHandler, sender, server) -> sender.sendPacket(MOD_INSTALLED, PacketByteBufs.empty()));
    }
}
