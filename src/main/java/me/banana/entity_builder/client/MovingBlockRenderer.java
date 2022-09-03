package me.banana.entity_builder.client;

import me.banana.entity_builder.MovingBlockEntity;
import me.banana.entity_builder.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/*
 * A renderer is used to provide an entity model, shadow size, and texture.
 */
@Environment(EnvType.CLIENT)
public class MovingBlockRenderer extends MobEntityRenderer<MovingBlockEntity, MovingBlockModel> {
    private static final Identifier TEXTURE_LOCATION = Utils.NewIdentifier("textures/entity/block.png");

    public MovingBlockRenderer(Context context) {
        super(context, new MovingBlockModel(context.getPart(EntityBuilderClient.MOVING_BLOCK_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(MovingBlockEntity entity) {
        return TEXTURE_LOCATION;
    }
}
