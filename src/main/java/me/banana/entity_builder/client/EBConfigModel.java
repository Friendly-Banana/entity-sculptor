package me.banana.entity_builder.client;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Hook;
import io.wispforest.owo.config.annotation.Modmenu;
import me.banana.entity_builder.SetBlockMode;
import me.banana.entity_builder.Utils;

import java.util.List;

@Modmenu(modId = Utils.MOD_ID)
@Config(name = Utils.MOD_ID, wrapperName = "EBConfig")
public class EBConfigModel {
    public SetBlockMode setBlockMode = SetBlockMode.SetBlock;
    public double defaultScale = 1;
    @Hook
    public boolean solidBlocks;
    @Hook
    public boolean fallingBlocks;
    @Hook
    public boolean creativeBlocks;

    public List<String> excludedBlockIDs = List.of();
}