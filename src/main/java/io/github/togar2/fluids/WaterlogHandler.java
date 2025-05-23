package io.github.togar2.fluids;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;

public interface WaterlogHandler {
	WaterlogHandler DEFAULT = new WaterlogHandler() {};
	
	default boolean canPlaceFluid(Instance instance, BlockVec point, Block block, FluidState state) {
		return state.isWater() && state.isSource();
	}
	
	default boolean canRemoveFluid(Instance instance, BlockVec point, FluidState state) {
		return true;
	}
	
	default boolean placeFluid(Instance instance, BlockVec point, FluidState state) {
		Block currentBlock = instance.getBlock(point);
		if (!canPlaceFluid(instance, point, currentBlock, state)) return false;
		if (state.isWaterlogged()) return false;
		
		// The placed state (waterlogged block) is different from the original fluid state (probably just water)
		FluidState placedState = FluidState.of(currentBlock).setWaterlogged(true);
		instance.placeBlock(new BlockHandler.Placement(placedState.block(), instance, point));
		MinestomFluids.scheduleTick(instance, point, placedState);
		return true;
	}
}
