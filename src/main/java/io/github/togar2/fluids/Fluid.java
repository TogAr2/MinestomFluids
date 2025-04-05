package io.github.togar2.fluids;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public abstract class Fluid {
	protected final Block defaultBlock;
	private final ItemStack bucket;
	
	public Fluid(Block block, Material bucket) {
		this.defaultBlock = block;
		this.bucket = ItemStack.of(bucket);
	}
	
	public Block getDefaultBlock() {
		return defaultBlock;
	}
	
	public ItemStack getBucket() {
		return bucket;
	}
	
	protected abstract boolean canBeReplacedWith(Instance instance, BlockVec point,
	                                             Fluid other, BlockFace direction);
	
	public abstract int getNextTickDelay(Instance instance, BlockVec point, Block block);
	
	public void onTick(Instance instance, BlockVec point, Block block) {}
	
	protected boolean isEmpty() {
		return false;
	}
	
	protected abstract double getBlastResistance();
	
	public abstract double getHeight(Block block, Instance instance, BlockVec point);
	public abstract double getHeight(Block block);
	
	public static boolean isSource(Block block) {
		if (MinestomFluids.isWaterlogged(block)) return true;
		String levelStr = block.getProperty("level");
		return levelStr != null && Integer.parseInt(levelStr) == 0;
	}
	
	public static int getLevel(Block block) {
		if (MinestomFluids.isWaterlogged(block)) return 8;
		String levelStr = block.getProperty("level");
		if (levelStr == null) return 0;
		int level = Integer.parseInt(levelStr);
		if (level >= 8) return 8; // Falling water
		return 8 - level;
	}
	
	public static boolean isFalling(Block block) {
		String levelStr = block.getProperty("level");
		if (levelStr == null) return false;
		return Integer.parseInt(levelStr) >= 8;
	}
}
