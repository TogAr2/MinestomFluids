package io.github.togar2.fluids;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;

public interface WaterlogHandler {
	WaterlogHandler DEFAULT = new WaterlogHandler() {};
	
	default boolean canPlaceFluid(Instance instance, BlockVec point, Block block, Fluid fluid, Block flowing) {
		return fluid == MinestomFluids.WATER && Fluid.isSource(flowing);
	}
	
	default boolean canRemoveFluid(Instance instance, BlockVec point, Block block, Fluid fluid) {
		return true;
	}
	
	default boolean placeFluid(Instance instance, BlockVec point, Block block, Fluid fluid, Block flowing) {
		if (!canPlaceFluid(instance, point, block, fluid, flowing)) return false;
		if (MinestomFluids.isWaterlogged(block)) return false;
		
		instance.placeBlock(new BlockHandler.Placement(MinestomFluids.setWaterlogged(block, true), instance, point));
		MinestomFluids.scheduleTick(instance, point, block);
		return true;
	}
}
