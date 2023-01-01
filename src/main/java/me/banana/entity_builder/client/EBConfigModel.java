package me.banana.entity_builder.client;

import io.wispforest.owo.config.annotation.*;
import me.banana.entity_builder.SetBlockMode;
import me.banana.entity_builder.Utils;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Modmenu(modId = Utils.MOD_ID)
@Config(name = Utils.MOD_ID, wrapperName = "EBConfig")
public class EBConfigModel {
    public static Set<Identifier> blockTags = new HashSet<>();

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
    @Hook
    @PredicateConstraint("validBlockTags")
    public List<String> excludedBlockTags = new ArrayList<>() {{
        addAll(Stream.of(BlockTags.STAIRS, BlockTags.SLABS, BlockTags.FENCES, BlockTags.FENCE_GATES, BlockTags.BEDS, BlockTags.DOORS, BlockTags.PRESSURE_PLATES).map(tag -> tag.id().toString()).collect(Collectors.toUnmodifiableSet()));
    }};

    @SectionHeader("debug")
    public boolean showOrigin;
    public boolean showVertices;

    public static boolean validBlockIDs(List<String> ids) {
        try {
            return ids.stream().allMatch(id -> Registry.BLOCK.containsId(new Identifier(id)));
        } catch (InvalidIdentifierException exception) {
            return false;
        }
    }

    public static boolean validBlockTags(List<String> tags) {
        try {
            return tags.stream().allMatch(tag -> blockTags.contains(new Identifier(tag)));
        } catch (InvalidIdentifierException exception) {
            return false;
        }
    }
}