package io.github.togar2.fluids;

import net.minestom.server.instance.block.Block;

public record FluidState(Block block, Fluid fluid) {
	public static FluidState of(Block block) {
		return new FluidState(block, MinestomFluids.get(block));
	}
	
	public boolean isSource() {
		return isSource(block);
	}
	
	public int getLevel() {
		return getLevel(block);
	}
	
	public boolean isFalling() {
		return isFalling(block);
	}
	
	public boolean isEmpty() {
		return fluid.isEmpty();
	}
	
	public boolean isWater() {
		return fluid == MinestomFluids.WATER;
	}
	
	public boolean sameFluid(FluidState other) {
		return fluid == other.fluid;
	}
	
	public boolean sameFluid(Block other) {
		return fluid == MinestomFluids.get(other);
	}
	
	public boolean isWaterlogged() {
		return MinestomFluids.isWaterlogged(block);
	}
	
	public FluidState setWaterlogged(boolean waterlogged) {
		return FluidState.of(MinestomFluids.setWaterlogged(block, waterlogged));
	}
	
	public WaterlogHandler getWaterlogHandler() {
		return MinestomFluids.getWaterlog(block);
	}
	
	public FluidState asFlowing(int level, boolean falling) {
		return new FluidState(block.withProperty("level", String.valueOf((falling ? 8 : 0) + (level == 0 ? 0 : 8 - level))), fluid);
	}
	
	public FluidState asSource(boolean falling) {
		return new FluidState(block.withProperty("level", falling ? "8" : "0"), fluid);
	}
	
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
