package me.banana.entity_sculptor;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.session.SessionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class EntitySculptor implements ModInitializer {
    public static final String MOD_ID = "entity_sculptor";
    public static final Logger LOGGER = LoggerFactory.getLogger("EntitySculptor");
    public static final Identifier BUILD_CHANGES = Id("build_changes");
    public static final Identifier MOD_INSTALLED = Id("mod_installed");
    public final static EntityType<MovingBlockEntity> MOVING_BLOCK = FabricEntityTypeBuilder.createMob()
        .defaultAttributes(MobEntity::createMobAttributes)
        .entityFactory(MovingBlockEntity::new)
        .dimensions(EntityDimensions.fixed(1f, 1f))
        .build();

    public static void log(Object... objects) {
        LOGGER.info(Arrays.stream(objects).map(Object::toString).collect(Collectors.joining(", ")));
    }

    public static Identifier Id(String name) {
        return new Identifier(MOD_ID, name);
    }

    @Override
    public void onInitialize() {
        Registry.register(Registry.ENTITY_TYPE, Id("moving_block"), MOVING_BLOCK);

        ArgumentTypeRegistry.registerArgumentType(Id("direction"), DirectionsArgumentType.class, ConstantArgumentSerializer.of(DirectionsArgumentType::directions));

        ServerPlayNetworking.registerGlobalReceiver(BUILD_CHANGES, EntitySculptor::receiveStatueToBuild);
        ServerPlayConnectionEvents.JOIN.register((networkHandler, sender, server) -> sender.sendPacket(MOD_INSTALLED, PacketByteBufs.empty()));
    }

    private static void receiveStatueToBuild(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        if (!player.hasPermissionLevel(server.getOpPermissionLevel())) {
            player.sendMessage(Text.of("You need to be an operator to build statues."));
            return;
        }
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
}
