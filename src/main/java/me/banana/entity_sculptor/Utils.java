package me.banana.entity_sculptor;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {
    public static final String MOD_ID = "entity_sculptor";
    public static final Logger LOGGER = LoggerFactory.getLogger("EntitySculptor");

    public static void log(Object... objects) {
        LOGGER.info(Arrays.stream(objects).map(Object::toString).collect(Collectors.joining(", ")));
    }

    public static Identifier Id(String name) {
        return new Identifier(MOD_ID, name);
    }
}