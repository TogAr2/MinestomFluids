package io.github.togar2.fluids;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.Material;

public class EmptyFluid extends Fluid {
	
	public EmptyFluid() {
		super(Block.AIR, Material.BUCKET);
	}
	
	@Override
	protected boolean canBeReplacedWith(Instance instance, BlockVec point, Fluid other, BlockFace direction) {
		return true;
	}
	
	@Override
	public int getNextTickDelay(Instance instance, BlockVec point) {
		return -1;
	}
	
	@Override
	protected boolean isEmpty() {
		return true;
	}
	
	@Override
	protected double getBlastResistance() {
		return 0;
	}
	
	@Override
	public double getHeight(Instance instance, BlockVec point) {
		return 0;
	}
	
	@Override
	public double getHeight(FluidState state) {
		return 0;
	}
}
