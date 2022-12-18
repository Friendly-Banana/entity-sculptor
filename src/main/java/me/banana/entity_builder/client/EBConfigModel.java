package me.banana.entity_builder.client;

import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Hook;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.util.ReflectionUtils;
import me.banana.entity_builder.SetBlockMode;
import me.banana.entity_builder.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

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

    public List<String> excludedBlocks = List.of();
}

class EBConfigScreen extends ConfigScreen {
    OptionComponentFactory<List<Block>> BLOCKLIST = (model, option) -> {
        var layout = new GenericStringConvertibleList<>(option, Blocks.AIR, block -> Registry.BLOCK.getId(block).toString(), string -> Registry.BLOCK.get(new Identifier(string)));
        return new OptionComponentFactory.Result(layout, layout);
    };

    public EBConfigScreen(ConfigWrapper<?> config, @Nullable Screen parent) {
        super(DEFAULT_MODEL_ID, config, parent);
        extraFactories.put(option -> isBlockList(option.backingField().field()), BLOCKLIST);
    }

    private static boolean isBlockList(Field field) {
        if (field.getType() != List.class) return false;

        var listType = ReflectionUtils.getTypeArgument(field.getGenericType(), 0);
        if (listType == null) return false;

        return Block.class == listType;
    }
}