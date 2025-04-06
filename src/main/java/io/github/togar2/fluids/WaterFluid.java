package io.github.togar2.fluids;

import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

public class WaterFluid extends FlowableFluid {
	public WaterFluid() {
		super(Block.WATER, Material.WATER_BUCKET);
	}
	
	@Override
	protected boolean isInfinite() {
		return true;
	}
	
	@Override
	protected @Nullable FluidState onBreakingBlock(Instance instance, BlockVec point,
	                                               BlockFace direction, Block block, FluidState newState) {
		FluidBlockBreakEvent event = new FluidBlockBreakEvent(instance, point, direction, block, newState);
		return event.isCancelled() ? null : event.getNewState();
	}
	
	@Override
	protected int getHoleRadius(Instance instance) {
		return 4;
	}
	
	@Override
	public int getLevelDecreasePerBlock(Instance instance) {
		return 1;
	}
	
	@Override
	public int getNextTickDelay(Instance instance, BlockVec point) {
		return 5 * (ServerFlag.SERVER_TICKS_PER_SECOND / 20);
	}
	
	@Override
	protected boolean canBeReplacedWith(Instance instance, BlockVec point, FluidState currentState,
	                                    FluidState newState, BlockFace direction) {
		return direction == BlockFace.BOTTOM && !newState.isWater();
	}
	
	@Override
	protected double getBlastResistance() {
		return 100;
	}
}
