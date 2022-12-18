package me.banana.entity_builder.client;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.Expanded;
import io.wispforest.owo.config.ui.component.ConfigTextBox;
import io.wispforest.owo.config.ui.component.OptionComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.UISounds;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.ReflectionUtils;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings({"UnstableApiUsage"})
class GenericStringConvertibleList<T> extends CollapsibleContainer implements OptionComponent {

    protected final Option<List<T>> backingOption;
    protected final List<T> backingList;

    protected final List<Component> optionContainers = new ArrayList<>();
    protected final ButtonWidget resetButton;
    private final Function<T, String> customToString;
    private final Function<String, T> fromString;

    public GenericStringConvertibleList(Option<List<T>> option, T defaultNewValue, Function<T, String> customToString, Function<String, T> fromString) {
        super(Sizing.fill(100), Sizing.content(), Text.translatable("text.config." + option.configName() + ".option." + option.key().asString()), option.backingField().field().isAnnotationPresent(Expanded.class));

        this.customToString = customToString;
        this.fromString = fromString;
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
                this.backingList.add(defaultNewValue);
                this.refreshOptions();
                UISounds.playInteractionSound();

                return true;
            });
            this.titleLayout.child(addButton.margins(Insets.of(5)));
        }

        this.resetButton = Components.button(Text.literal("⇄"), (button) -> {
            this.backingList.clear();
            this.backingList.addAll(option.defaultValue());

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

        var listType = ReflectionUtils.getTypeArgument(this.backingOption.backingField().field().getGenericType(), 0);
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
            box.setText(this.customToString.apply(this.backingList.get(i)));
            box.setCursorToStart();
            box.setDrawsBackground(false);
            box.margins(Insets.vertical(2));
            box.horizontalSizing(Sizing.fill(95));
            box.verticalSizing(Sizing.fixed(8));

            if (!this.backingOption.detached()) {
                box.setChangedListener(s -> {
                    if (!box.isValid()) return;

                    this.backingList.set(optionIndex, this.fromString.apply((String) box.parsedValue()));
                    this.refreshResetButton();
                });
            } else {
                box.active = false;
            }

            if (NumberReflection.isNumberType(listType)) {
                box.configureForNumber((Class<? extends Number>) listType);
            } else if (this.backingOption.constraint() != null) {
                box.applyPredicate(this.backingOption.constraint().predicate());
            }

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
