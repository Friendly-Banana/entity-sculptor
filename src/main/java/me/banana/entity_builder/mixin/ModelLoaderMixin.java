package me.banana.entity_builder.mixin;

import com.google.common.collect.Sets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import me.banana.entity_builder.Utils;
import me.banana.entity_builder.client.ColorMatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashSet;
import java.util.Map;

@Mixin(ModelLoader.class)
public class ModelLoaderMixin {
    LinkedHashSet<Pair<String, String>> set = Sets.newLinkedHashSet();

    @Shadow
    public UnbakedModel getOrLoadModel(Identifier id) {
        throw new UnsupportedOperationException();
    }

    @WrapOperation(at = @At(value = "INVOKE", ordinal = 1, target = "java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), method = "addModel")
    private Object getModel(Map<Identifier, UnbakedModel> instance, Object oModelId, Object oUnbakedModel, Operation<Object> original) {
        ModelIdentifier modelId = (ModelIdentifier) oModelId;
        UnbakedModel unbakedModel = (UnbakedModel) oUnbakedModel;
        unbakedModel.getTextureDependencies(this::getOrLoadModel, set).stream().map(SpriteIdentifier::getTextureId).forEach(texture -> ColorMatcher.ids.put(modelId, texture));
        Utils.log(modelId.toString(), unbakedModel.getTextureDependencies(this::getOrLoadModel, set).stream().map(SpriteIdentifier::getTextureId).map(Identifier::toString));
        return original.call(instance, oModelId, oUnbakedModel);
    }
}
