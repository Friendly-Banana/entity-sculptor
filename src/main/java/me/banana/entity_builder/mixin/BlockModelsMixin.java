package me.banana.entity_builder.mixin;

import me.banana.entity_builder.client.ColorMatcher;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(BlockModels.class)
public class BlockModelsMixin {
    @Inject(method = "getModelId(Lnet/minecraft/util/Identifier;Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/util/ModelIdentifier;", at = @At("HEAD"))
    private static void catchBlockStateIDForReversing(Identifier id, BlockState state, CallbackInfoReturnable<ModelIdentifier> cir) {
        StringBuilder states = new StringBuilder();
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getEntries().entrySet()) {
            states.append('_');
            Property<?> property = entry.getKey();
            states.append(property.getName());
            states.append(propertyValueToString(property, entry.getValue()));
        }

        ColorMatcher.idToblockstate.put(id.getNamespace() + ":" + "block/" + id.getPath() + states, state);
    }

    private static <T extends Comparable<T>> String propertyValueToString(Property<T> property, Comparable<?> value) {
        return property.name((T) value);
    }
}
