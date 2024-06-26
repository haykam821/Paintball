package io.github.haykam821.paintball.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.decoration.ArmorStandEntity;

@Mixin(ArmorStandEntity.class)
public interface ArmorStandEntityAccessor {
	@Accessor("disabledSlots")
	public void paintball$setDisabledSlots(int disabledSlots);
}
