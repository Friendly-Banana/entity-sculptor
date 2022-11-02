package me.banana.entity_builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.session.SessionManager;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

import static me.banana.entity_builder.EntityBuilder.BUILD_CHANGES;
import static me.banana.entity_builder.EntityBuilder.MOD_INSTALLED;

public class EntityBuilderServer implements DedicatedServerModInitializer {
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

                EditSession editSession = localSession.createEditSession(actor);
                if (localSession.getBlockChangeLimit() > -1 && statue.size() > localSession.getBlockChangeLimit()) {
                    player.sendMessage(Text.of("Change blockChangeLimit, statue has " + statue.size() + " blocksMap."));
                    editSession.close();
                    return;
                }
                for (BlockPos blockPos : statue.keySet()) {
                    try {
                        if (editSession.setBlock(FabricAdapter.adapt(blockPos), FabricAdapter.adapt(statue.get(blockPos)))) {
                            changedBlockCount++;
                        }
                    } catch (MaxChangedBlocksException e) {
                        player.sendMessage(Text.of("MaxChangedBlocksException: " + e.getMessage()));
                        break;
                    }
                }
                editSession.close();
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
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(BUILD_CHANGES, EntityBuilderServer::receiveStatueToBuild);
        ServerPlayConnectionEvents.JOIN.register((networkHandler, sender, server) -> sender.sendPacket(MOD_INSTALLED, PacketByteBufs.empty()));
    }
}
