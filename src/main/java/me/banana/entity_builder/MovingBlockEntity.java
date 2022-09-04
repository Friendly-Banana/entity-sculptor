package me.banana.entity_builder;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;

public class MovingBlockEntity extends MobEntity {
    public MovingBlockEntity(EntityType<? extends MovingBlockEntity> entityType, World world) {
        super(entityType, world);
    }
}