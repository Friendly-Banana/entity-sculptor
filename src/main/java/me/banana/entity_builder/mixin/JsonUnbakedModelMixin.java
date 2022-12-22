package me.banana.entity_builder.mixin;

import me.banana.entity_builder.client.ColorMatcher;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.function.Function;

@Mixin(JsonUnbakedModel.class)
public abstract class JsonUnbakedModelMixin {
    private ArrayList<Identifier> set;
    private boolean ignore = false;

    @Shadow
    public abstract SpriteIdentifier resolveSprite(String spriteName);

    @ModifyVariable(method = "bake(Lnet/minecraft/client/render/model/ModelLoader;Lnet/minecraft/client/render/model/json/JsonUnbakedModel;Ljava/util/function/Function;Lnet/minecraft/client/render/model/ModelBakeSettings;Lnet/minecraft/util/Identifier;Z)Lnet/minecraft/client/render/model/BakedModel;", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/model/json/ModelElementFace;textureId:Ljava/lang/String;"))
    private ModelElementFace catchTextures(ModelElementFace value) {
        if (!ignore) set.add(resolveSprite(value.textureId).getTextureId());
        return value;
    }

    @Inject(method = "bake(Lnet/minecraft/client/render/model/ModelLoader;Lnet/minecraft/client/render/model/json/JsonUnbakedModel;Ljava/util/function/Function;Lnet/minecraft/client/render/model/ModelBakeSettings;Lnet/minecraft/util/Identifier;Z)Lnet/minecraft/client/render/model/BakedModel;", at = @At(value = "HEAD"))
    private void catchBlockstate(ModelLoader loader, JsonUnbakedModel parent, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings settings, Identifier id, boolean hasDepth, CallbackInfoReturnable<BakedModel> cir) {
        // not interested in #inventory or missing#missing
        if (id.toString().contains("#")) {
            ignore = true;
        } else {
            ignore = false;
            set = new ArrayList<>();
            ColorMatcher.blockstateToTextures.put(id, set);
        }
    }
}
