package me.banana.entity_sculptor.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.banana.entity_sculptor.DirectionsArgumentType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;

import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.server.command.CommandManager.literal;

@Environment(EnvType.CLIENT)
public class MatchColorCommand {
    private static final int DEFAULT_ALPHA = 255;

    /**
     * matchcolor r g b [a|directions | limit]
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            final LiteralCommandNode<ServerCommandSource> match = dispatcher.register(literal("entitysculptor match").then(CommandManager.argument("r", DoubleArgumentType.doubleArg(0, 255))
                .then(CommandManager.argument("g", DoubleArgumentType.doubleArg(0, 255))
                    .then(CommandManager.argument("b", DoubleArgumentType.doubleArg(0, 255))
                        .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DEFAULT_ALPHA), DirectionsArgumentType.DIRECTIONS, EntitySculptorClient.CONFIG.defaultLimit()))
                        .then(CommandManager.argument("a", DoubleArgumentType.doubleArg(0, 255))
                            .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DoubleArgumentType.getDouble(context, "a")), DirectionsArgumentType.DIRECTIONS, EntitySculptorClient.CONFIG.defaultLimit()))
                            .then(CommandManager.argument("directions", DirectionsArgumentType.directions())
                                .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DoubleArgumentType.getDouble(context, "a")), DirectionsArgumentType.getDirections(context, "directions"), EntitySculptorClient.CONFIG.defaultLimit()))
                                .then(CommandManager.argument("limit", IntegerArgumentType.integer())
                                    .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DoubleArgumentType.getDouble(context, "a")), DirectionsArgumentType.getDirections(context, "directions"), IntegerArgumentType.getInteger(context, "limit"))))))
                        .then(CommandManager.argument("directions", DirectionsArgumentType.directions())
                            .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DEFAULT_ALPHA), DirectionsArgumentType.getDirections(context, "directions"), EntitySculptorClient.CONFIG.defaultLimit()))
                            .then(CommandManager.argument("limit", IntegerArgumentType.integer())
                                .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DEFAULT_ALPHA), DirectionsArgumentType.getDirections(context, "directions"), IntegerArgumentType.getInteger(context, "limit")))))
                        .then(CommandManager.argument("limit", IntegerArgumentType.integer())
                            .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DEFAULT_ALPHA), DirectionsArgumentType.DIRECTIONS, IntegerArgumentType.getInteger(context, "limit"))))))));
            dispatcher.register(literal("matchcolor").redirect(match));
        });
    }

    public static int execute(ServerCommandSource commandSource, ColorMatcher.RGBA color, Direction[] directions, int limit) {
        AtomicInteger order = new AtomicInteger(1);
        EntitySculptorClient.COLOR_MATCHER.bestBlockStates(color, directions, limit)
            .map(state -> Text.literal(order.getAndIncrement() + ". ")
                .append(Text.literal(formatBlockState(state))
                    .setStyle(Style.EMPTY.withColor(Formatting.BLUE)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Copy to clipboard")
                            .setStyle(Style.EMPTY.withColor(Formatting.GREEN))))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, formatBlockState(state))))))
            .forEach(text -> commandSource.sendFeedback(text, false));
        return Command.SINGLE_SUCCESS;
    }

    private static String formatBlockState(BlockState state) {
        final String beginning = "Block{";
        return state.toString().substring(beginning.length()).replaceFirst("}", "");
    }
}