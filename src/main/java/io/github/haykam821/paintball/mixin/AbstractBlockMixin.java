package io.github.haykam821.paintball.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.haykam821.paintball.Main;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BarrierBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
	@Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
	public void removePaintballBarrierCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> ci) {
		if (!(state.getBlock() instanceof BarrierBlock)) return;

		if (!(context instanceof EntityShapeContext entityContext)) return;
		if (!(entityContext.getEntity() instanceof ProjectileEntity entity)) return;

		GameSpace gameSpace = GameSpaceManager.get().byWorld(entity.getWorld());
		if (gameSpace == null) return;
		if (gameSpace.getBehavior().testRule(Main.PROJECTILE_BARRIER_COLLISION) != EventResult.DENY) return;

		ci.setReturnValue(VoxelShapes.empty());
	}
}
