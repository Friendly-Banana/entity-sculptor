package me.banana.entity_builder;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {
    public static final String MOD_ID = "entity_builder";
    public static final int GOLD = 0xffd700, SAND = 0xffe11f, ICE = 0x80e5ef;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static int counter = 0;

    public static void Debug() {
        counter++;
        LOGGER.debug(StackWalker.getInstance().walk(stream -> stream.skip(1).findFirst().get()).getMethodName() + ": " + counter);
    }

    public static void Log(Object... objects) {
        LOGGER.info(Arrays.stream(objects).map(Object::toString).collect(Collectors.joining(", ")));
    }

    public static <T> T NotNull(@Nullable T one, T fallback) {
        return one != null ? one : fallback;
    }

    public static Identifier NewIdentifier(String name) {
        return new Identifier(MOD_ID, name);
    }

    public static boolean IsSurvival(PlayerEntity player) {
        return !player.isCreative() && !player.isSpectator();
    }
}