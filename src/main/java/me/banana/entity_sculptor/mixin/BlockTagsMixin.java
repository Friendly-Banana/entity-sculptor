package me.banana.entity_sculptor.mixin;

import me.banana.entity_sculptor.client.ESConfigModel;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TagKey.class)
public class BlockTagsMixin {
    @Inject(method = "of", at = @At("HEAD"))
    private static <T> void collectBlockTags(RegistryKey<? extends Registry<T>> registry, Identifier id, CallbackInfoReturnable<TagKey<T>> cir) {
        if (registry.getRegistry() == Registry.BLOCK_KEY.getRegistry()) ESConfigModel.blockTags.add(id);
    }
}
