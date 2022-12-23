package me.banana.entity_builder.client;

import io.wispforest.owo.config.annotation.*;
import me.banana.entity_builder.SetBlockMode;
import me.banana.entity_builder.Utils;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;

@Modmenu(modId = Utils.MOD_ID)
@Config(name = Utils.MOD_ID, wrapperName = "EBConfig")
public class EBConfigModel {
    public double defaultScale = 1;
    public SetBlockMode setBlockMode = SetBlockMode.CustomPacket;

    public boolean skipWaterlogged = true;
    @Hook
    public boolean nonSolidBlocks;
    @Hook
    public boolean fallingBlocks;
    @Hook
    public boolean creativeBlocks;
    @PredicateConstraint("validBlockIDs")
    public List<String> excludedBlockIDs = new ArrayList<>();

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
}