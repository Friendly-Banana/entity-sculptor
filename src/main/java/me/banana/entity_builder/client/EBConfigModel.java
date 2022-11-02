package me.banana.entity_builder.client;

import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Expanded;
import io.wispforest.owo.config.annotation.Hook;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.component.ConfigTextBox;
import io.wispforest.owo.config.ui.component.OptionComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.UISounds;
import io.wispforest.owo.util.ReflectionUtils;
import me.banana.entity_builder.SetBlockMode;
import me.banana.entity_builder.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

    public List<Block> excludedBlocks = List.of();
}

class EBConfigScreen extends ConfigScreen {
    OptionComponentFactory<List<Block>> BLOCKLIST = (model, option) -> {
        var layout = new BlockOptionContainer(option);
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

class BlockOptionContainer extends CollapsibleContainer implements OptionComponent {
    protected final Option<List<Block>> backingOption;
    protected final List<Block> backingList;

    protected final List<Component> optionContainers = new ArrayList<>();
    protected final ButtonWidget resetButton;

    @SuppressWarnings("unchecked")
    public BlockOptionContainer(Option<List<Block>> option) {
        super(Sizing.fill(100), Sizing.content(), Text.translatable("text.config." + option.configName() + ".option." + option.key().asString()), option.backingField().field().isAnnotationPresent(Expanded.class));

        this.backingOption = option;
        this.backingList = new ArrayList<>(option.value());

        this.padding(this.padding.get().add(0, 5, 0, 0));
        this.titleLayout.padding(Insets.top(5));

        this.titleLayout.horizontalSizing(Sizing.fill(100));
        this.titleLayout.verticalSizing(Sizing.fixed(30));
        this.titleLayout.verticalAlignment(VerticalAlignment.CENTER);

        if (!option.detached()) {
            var addButton = Components.label(Text.literal("Add entry").formatted(Formatting.GRAY));
            addButton.cursorStyle(CursorStyle.HAND);
            addButton.mouseEnter().subscribe(() -> addButton.text(addButton.text().copy().styled(style -> style.withColor(Formatting.YELLOW))));
            addButton.mouseLeave().subscribe(() -> addButton.text(addButton.text().copy().styled(style -> style.withColor(Formatting.GRAY))));
            addButton.mouseDown().subscribe((mouseX, mouseY, button) -> {
                this.backingList.add(Blocks.AIR);
                this.refreshOptions();
                UISounds.playInteractionSound();

                return true;
            });
            this.titleLayout.child(addButton.margins(Insets.of(5)));
        }

        this.resetButton = Components.button(Text.literal("⇄"), button -> {
            this.backingList.clear();

            this.refreshOptions();
            button.active = false;
        });
        this.resetButton.margins(Insets.right(10));
        this.resetButton.positioning(Positioning.relative(100, 50));
        this.refreshResetButton();
        this.titleLayout.child(resetButton);

        this.refreshOptions();
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    protected void refreshOptions() {
        this.collapsibleChildren.removeAll(this.optionContainers);
        this.children.removeAll(this.optionContainers);
        this.optionContainers.clear();

        for (int i = 0; i < this.backingList.size(); i++) {
            var container = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            container.verticalAlignment(VerticalAlignment.CENTER);

            int optionIndex = i;
            final var label = Components.label(Text.literal("- ").formatted(Formatting.GRAY));
            label.margins(Insets.left(10));
            if (!this.backingOption.detached()) {
                label.cursorStyle(CursorStyle.HAND);
                label.mouseEnter().subscribe(() -> label.text(Text.literal("x ").formatted(Formatting.GRAY)));
                label.mouseLeave().subscribe(() -> label.text(Text.literal("- ").formatted(Formatting.GRAY)));
                label.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    this.backingList.remove(optionIndex);
                    this.refreshResetButton();
                    this.refreshOptions();
                    UISounds.playInteractionSound();

                    return true;
                });
            }
            container.child(label);

            final var box = new ConfigTextBox();
            box.setText(Registry.BLOCK.getId(this.backingList.get(i)).toString());
            box.setCursorToStart();
            box.setDrawsBackground(false);
            box.margins(Insets.vertical(2));
            box.horizontalSizing(Sizing.fill(95));
            box.verticalSizing(Sizing.fixed(8));

            if (!this.backingOption.detached()) {
                box.setChangedListener(s -> {
                    if (!box.isValid()) return;

                    this.backingList.set(optionIndex, Registry.BLOCK.get(new Identifier((String) box.parsedValue())));
                    this.refreshResetButton();
                });
            } else {
                box.active = false;
            }
            box.applyPredicate(b -> Registry.BLOCK.containsId(new Identifier(b)) && !this.backingList.contains(Registry.BLOCK.get(new Identifier(b))));

            container.child(box);
            this.optionContainers.add(container);
        }

        this.children(this.optionContainers);
        this.refreshResetButton();
    }

    protected void refreshResetButton() {
        this.resetButton.active = !this.backingOption.detached() && !this.backingList.equals(this.backingOption.defaultValue());
    }

    @Override
    public boolean shouldDrawTooltip(double mouseX, double mouseY) {
        return ((mouseY - this.y) <= this.titleLayout.height()) && super.shouldDrawTooltip(mouseX, mouseY);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Object parsedValue() {
        return this.backingList;
    }
}