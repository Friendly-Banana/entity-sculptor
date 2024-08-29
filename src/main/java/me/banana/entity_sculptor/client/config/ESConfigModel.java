package me.banana.entity_sculptor.client.config;

import io.wispforest.owo.config.annotation.*;
import me.banana.entity_sculptor.EntitySculptor;
import me.banana.entity_sculptor.SetBlockMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
@Modmenu(modId = EntitySculptor.MOD_ID)
@Config(name = EntitySculptor.MOD_ID, wrapperName = "ESConfig")
public class ESConfigModel {
    public static Set<Identifier> blockTags = new HashSet<>();

    @SectionHeader("statue")
    public double defaultScale = 16;
    public SetBlockMode setBlockMode = SetBlockMode.CustomPacket;

    public boolean skipWaterlogged = true;
    @Hook
    public boolean nonSolidBlocks;
    @Hook
    public boolean fallingBlocks;
    @Hook
    public boolean creativeBlocks;
    @Hook
    @PredicateConstraint("validBlockIDs")
    public List<String> excludedBlockIDs = new ArrayList<>();

    public static boolean validBlockIDs(List<String> ids) {
        for (String id : ids) {
            if (!Identifier.isValid(id)) {
                EntitySculptor.LOGGER.info("{} is not a valid identifier.", id);
                return false;
            } else if (!Registry.BLOCK.containsId(new Identifier(id))) {
                EntitySculptor.LOGGER.warn("{} is not a registered block.", id);
                return false;
            }
        }
        return true;
    }

    @Hook
    @PredicateConstraint("validBlockTags")
    public List<String> excludedBlockTags = new ArrayList<>() {{
        addAll(Stream.of(BlockTags.STAIRS, BlockTags.SLABS, BlockTags.FENCES, BlockTags.FENCE_GATES, BlockTags.BEDS, BlockTags.DOORS, BlockTags.WOOL_CARPETS, BlockTags.PRESSURE_PLATES)
            .map(tag -> tag.id().toString())
            .collect(Collectors.toUnmodifiableSet()));
    }};

    public static boolean validBlockTags(List<String> tags) {
        for (String tag : tags) {
            if (!Identifier.isValid(tag)) {
                EntitySculptor.LOGGER.info("{} is not a valid identifier.", tag);
                return false;
            } else if (!blockTags.contains(new Identifier(tag))) {
                EntitySculptor.LOGGER.warn("{} is not a registered block tag.", tag);
                return false;
            }
        }
        return true;
    }

    @SectionHeader("debug")
    public boolean showOrigin;
    public boolean showVertices;

    @SectionHeader("matchColor")
    public int amountOfSuggestions = 3;
}