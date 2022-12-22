package me.banana.entity_builder.mixin;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

interface SpriteImageAccesor {
    NativeImage getOriginalImage();
}

@Mixin(Sprite.class)
public class SpriteMixin implements SpriteImageAccesor {
    @Final
    @Shadow
    protected NativeImage[] images;

    /**
     * gets original and thus largest image
     * <p>
     * always at index 0 because of {@link net.minecraft.client.texture.MipmapHelper#getMipmapLevelsImages(NativeImage, int) getMipmapLevelsImages}}
     */
    public NativeImage getOriginalImage() {
        return images[0];
    }
}