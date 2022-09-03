package me.banana.entity_builder;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

public class MovingBlockEntity extends HostileEntity {
    public MovingBlockEntity(EntityType<? extends MovingBlockEntity> entityType, World world) {
        super(entityType, world);
    }
}