package me.banana.entity_sculptor.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static net.minecraft.server.command.CommandManager.literal;

@Environment(EnvType.CLIENT)
public class MatchBlockCommand {
    /**
     * matchblock r g b [a] [directions] [limit]
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            int limit = 3;
            final LiteralCommandNode<ServerCommandSource> match = dispatcher.register(literal("entitysculptor match")
                .then(CommandManager.argument("r", DoubleArgumentType.doubleArg(0, 255))
                                    .then(CommandManager.argument("g", DoubleArgumentType.doubleArg(0, 255))
                                                        .then(CommandManager.argument("b", DoubleArgumentType.doubleArg(0, 255))
                                                                            .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), 255), Direction.values(), limit)))))
                .then(CommandManager.argument("a", DoubleArgumentType.doubleArg(0, 255))
                                    .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DoubleArgumentType.getDouble(context, "a")), Direction.values(), limit)))
                .then(CommandManager.argument("directions", DirectionsArgumentType.directions())
                                    .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DoubleArgumentType.getDouble(context, "a")), DirectionsArgumentType.getDirections(context, "directions"), limit)))
                .then(CommandManager.argument("limit", IntegerArgumentType.integer())
                                    .executes(context -> execute(context.getSource(), new ColorMatcher.RGBA(DoubleArgumentType.getDouble(context, "r"), DoubleArgumentType.getDouble(context, "g"), DoubleArgumentType.getDouble(context, "b"), DoubleArgumentType.getDouble(context, "a")), DirectionsArgumentType.getDirections(context, "directions"), IntegerArgumentType.getInteger(context, "limit")))));
            dispatcher.register(literal("matchblock").redirect(match));
        });
    }

    public static int execute(ServerCommandSource commandSource, ColorMatcher.RGBA color, Direction[] directions, int limit) {
        EntitySculptorClient.COLOR_MATCHER.bestBlockStates(color, directions, limit)
                                          .map(state -> Text.literal(state.toString()))
                                          .forEach(text -> commandSource.sendFeedback(text, false));
        return Command.SINGLE_SUCCESS;
    }

    static class DirectionsArgumentType implements ArgumentType<Direction[]> {
        private static final String ALL = "all";
        private static final Collection<String> EXAMPLES = Arrays.asList("north", "up,down,east,west", ALL);

        public static DirectionsArgumentType directions() {
            return new DirectionsArgumentType();
        }

        public static <S> Direction[] getDirections(CommandContext<S> context, String name) {
            // Note that you should assume the CommandSource wrapped inside the CommandContext will always be a generic type.
            // If you need to access the ServerCommandSource make sure you verify the source is a server command source before casting.
            return context.getArgument(name, Direction[].class);
        }

        @Override
        public Direction[] parse(StringReader reader) {
            int argBeginning = reader.getCursor(); // The starting position of the cursor is at the beginning of the argument.
            if (!reader.canRead()) {
                reader.skip();
            }

            // Now we check the contents of the argument till either we hit the end of the command line (When canRead becomes false)
            // Otherwise we go till reach a space, which signifies the next argument
            while (reader.canRead() && reader.peek() != ' ') {
                reader.skip();
            }
            // Now we substring the specific part we want to see using the starting cursor position and the ends where the next argument starts.
            String arg = reader.getString().substring(argBeginning, reader.getCursor());
            if (arg.equals(ALL)) {
                return Direction.values();
            }
            var strings = arg.split(",");
            List<Direction> directions = new ArrayList<>();
            for (String s : strings) {
                directions.add(Direction.byName(s.toUpperCase()));
            }
            return directions.toArray(new Direction[0]);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return CommandSource.suggestMatching(Stream.concat(Stream.of(ALL), Arrays.stream(Direction.values())
                                                                                     .map(Direction::getName)), builder);
        }

        @Override
        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }
}