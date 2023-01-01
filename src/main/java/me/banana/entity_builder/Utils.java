package me.banana.entity_builder;

import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {
    public static final String MOD_ID = "entity_builder";
    public static final Logger LOGGER = LogManager.getLogger("EntityBuilder");

    public static void log(Object... objects) {
        LOGGER.info(Arrays.stream(objects).map(Object::toString).collect(Collectors.joining(", ")));
    }

    public static Identifier Id(String name) {
        return new Identifier(MOD_ID, name);
    }
}