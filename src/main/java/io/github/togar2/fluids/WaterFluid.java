package io.github.togar2.fluids;

import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.Material;

public class WaterFluid extends FlowableFluid {
	public WaterFluid() {
		super(Block.WATER, Material.WATER_BUCKET);
	}
	
	@Override
	protected boolean isInfinite() {
		return true;
	}
	
	@Override
	protected boolean onBreakingBlock(Instance instance, BlockVec point, Block block) {
		WaterBlockBreakEvent event = new WaterBlockBreakEvent(instance, point, block);
		return !event.isCancelled();
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
	protected boolean canBeReplacedWith(Instance instance, BlockVec point, Fluid other, BlockFace direction) {
		return direction == BlockFace.BOTTOM && this == other;
	}
	
	@Override
	protected double getBlastResistance() {
		return 100;
	}
}
