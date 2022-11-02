package me.banana.entity_builder.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.banana.entity_builder.Utils;
import me.banana.entity_builder.client.ColorMatcher;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

@Mixin(SpriteAtlasTexture.class)
public class BlockTextureIds {
    @Shadow
    private Identifier getTexturePath(Identifier id) {
        throw new UnsupportedOperationException();
    }

    @ModifyVariable(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/SpriteAtlasTexture;loadSprites(Lnet/minecraft/resource/ResourceManager;Ljava/util/Set;)Ljava/util/Collection;"), method = "stitch")
    private Set<Identifier> beforeSpriteLoad(Set<Identifier> set) {
        // block atlas
        Optional<Identifier> a = set.stream().findFirst();
        if (a.isPresent() && a.get().getPath().startsWith("block")) {
            ColorMatcher.oldIds = set.stream().map(this::getTexturePath).toList();
        }
        return set;
    }

    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/Resource;getInputStream()Ljava/io/InputStream;"), method = "method_18160")
    private InputStream loadSprites(SpriteAtlasTexture instance, Identifier identifier, Operation<InputStream> original) {
        InputStream inputStream = original.call(identifier);
        Utils.log("xxxyyxys ", identifier);
        return inputStream;
    }

    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourceManager;open(Lnet/minecraft/util/Identifier;)Ljava/io/InputStream;"), method = "loadSprite")
    private InputStream loadSprite(SpriteAtlasTexture instance, Identifier identifier, Operation<InputStream> original) {
        InputStream inputStream = original.call(identifier);
        Utils.log("zxzyzxzy ", identifier);
        return inputStream;
    }
}
