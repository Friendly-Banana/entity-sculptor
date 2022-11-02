package me.banana.entity_builder.mixin;

import com.google.common.collect.Sets;
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

@Mixin(ModelLoader.class)
public class ModelLoaderMixin {
    LinkedHashSet<Pair<String, String>> set = Sets.newLinkedHashSet();

    @Shadow
    public UnbakedModel getOrLoadModel(Identifier id) {
        throw new UnsupportedOperationException();
    }

    @WrapOperation(at = @At(value = "INVOKE", ordinal = 1, target = "java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), method = "addModel")
    void getModel(ModelLoader instance, ModelIdentifier modelId, UnbakedModel unbakedModel) {
        unbakedModel.getTextureDependencies(this::getOrLoadModel, set).stream().map(SpriteIdentifier::getTextureId).forEach(texture -> ColorMatcher.ids.put(modelId, texture));
        Utils.log(modelId.toString(), unbakedModel.getTextureDependencies(this::getOrLoadModel, set).stream().map(SpriteIdentifier::getTextureId).map(Identifier::toString));
    }

    @WrapOperation(at = @At(value = "INVOKE", ordinal = 1, target = "java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), method = "addModel")
    void getModel(ModelIdentifier modelId, UnbakedModel unbakedModel) {
        unbakedModel.getTextureDependencies(this::getOrLoadModel, set).stream().map(SpriteIdentifier::getTextureId).forEach(texture -> ColorMatcher.ids.put(modelId, texture));
        Utils.log(modelId.toString(), unbakedModel.getTextureDependencies(this::getOrLoadModel, set).stream().map(SpriteIdentifier::getTextureId).map(Identifier::toString));
    }
}
