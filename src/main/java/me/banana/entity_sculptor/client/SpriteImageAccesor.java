package me.banana.entity_sculptor.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;

@Environment(EnvType.CLIENT)
public interface SpriteImageAccesor {
    NativeImage getOriginalImage();
}
