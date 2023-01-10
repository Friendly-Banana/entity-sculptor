package me.banana.entity_sculptor;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectionsArgumentType implements ArgumentType<Direction[]> {
    public static final Direction[] DIRECTIONS = Direction.values();
    public static final String SEPARATOR = ",";
    private static final String ALL = "all";
    private static final Collection<String> EXAMPLES = Arrays.asList("north", "up,down,east,west", ALL);
    private static final Map<String, Direction> DIRECTION_BY_NAME = Arrays.stream(DIRECTIONS)
        .collect(Collectors.toUnmodifiableMap(Direction::getName, direction -> direction));

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
            return DIRECTIONS;
        }
        return Arrays.stream(arg.split(SEPARATOR))
            .filter(DIRECTION_BY_NAME::containsKey)
            .distinct()
            .map(DIRECTION_BY_NAME::get)
            .toArray(Direction[]::new);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String input = context.getInput();
        String dirsAlreadyThere = input.contains(SEPARATOR) ? input
            .substring(input.lastIndexOf(" ") + 1, input.lastIndexOf(SEPARATOR) + 1) : "";
        return CommandSource.suggestMatching(Stream.concat(Stream.of(ALL), Arrays.stream(Direction.values())
            .map(direction -> dirsAlreadyThere + direction.getName())), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
