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
    public boolean showVertices;
    public SetBlockMode setBlockMode = SetBlockMode.CustomPacket;

    @Hook
    public boolean nonSolidBlocks;
    @Hook
    public boolean fallingBlocks;
    @Hook
    public boolean creativeBlocks;
    @Expanded
    @PredicateConstraint("validBlockID")
    public List<String> excludedBlockIDs = new ArrayList<>();

    boolean validBlockID(String id) {
        try {
            return Registry.BLOCK.containsId(new Identifier(id));
        } catch (InvalidIdentifierException ignored) {
            return false;
        }
    }
}