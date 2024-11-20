package io.github.haykam821.paintball.game.map;

import io.github.haykam821.paintball.Main;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.util.ColoredBlocks;

public final class BlockStaining {
	private BlockStaining() {
		return;
	}

	private static BlockState getStainedState(BlockState state, DyeColor color) {
		if (state.isIn(Main.STAINABLE_TERRACOTTA)) {
			Block stainedBlock = ColoredBlocks.terracotta(color);
			return stainedBlock.getStateWithProperties(state);
		} else {
			return state;
		}
	}

	public static void setStainedState(World world, BlockPos pos, DyeColor color) {
		BlockState state = world.getBlockState(pos);
		world.setBlockState(pos, BlockStaining.getStainedState(state, color));
	}
}
