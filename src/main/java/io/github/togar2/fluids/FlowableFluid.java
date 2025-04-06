package io.github.togar2.fluids;

import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.gamedata.tags.TagManager;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Direction;

import java.util.EnumMap;
import java.util.Map;

public abstract class FlowableFluid extends Fluid {
	public FlowableFluid(Block defaultBlock, Material bucket) {
		super(defaultBlock, bucket);
	}
	
	@Override
	public void onTick(Instance instance, BlockVec point, FluidState state) {
		if (!state.isSource()) {
			FluidState updated = getUpdatedState(instance, point, state);
			if (updated.isEmpty()) {
				state = updated;
				instance.setBlock(point, Block.AIR);
			} else if (!updated.equals(state)) {
				state = updated;
				instance.setBlock(point, updated.block());
				MinestomFluids.scheduleTick(instance, point, updated);
			}
		}
		trySpread(instance, point, state);
	}
	
	protected void trySpread(Instance instance, BlockVec point, FluidState state) {
		if (state.isEmpty()) return;
		
		BlockVec down = point.add(0, -1, 0);
		FluidState downState = FluidState.of(instance.getBlock(down));
		if (canFlowThrough(instance, down, state, downState, BlockFace.BOTTOM)) {
			FluidState updatedDownState = getUpdatedState(instance, down, downState);
			
			if (downState.fluid().canBeReplacedWith(instance, down, updatedDownState.fluid(), BlockFace.BOTTOM)) {
				flow(instance, down, updatedDownState, BlockFace.BOTTOM);
				
				if (getAdjacentSourceCount(instance, point) >= 3)
					flowSides(instance, point, state);
			}
		} else if (state.isSource() || !isWaterHole(instance, state, down)) {
			flowSides(instance, point, state);
		}
	}
	
	/**
	 * Flows to the sides whenever possible, or to a hole if found
	 */
	private void flowSides(Instance instance, BlockVec point, FluidState flowing) {
		int newLevel = flowing.getLevel() - getLevelDecreasePerBlock(instance);
		if (flowing.isFalling()) newLevel = 7;
		if (newLevel <= 0) return;
		
		Map<BlockFace, FluidState> map = getSpread(instance, point, flowing);
		for (Map.Entry<BlockFace, FluidState> entry : map.entrySet()) {
			BlockFace direction = entry.getKey();
			FluidState newState = entry.getValue();
			
			BlockVec offset = point.relative(direction);
			FluidState currentState = FluidState.of(instance.getBlock(offset));
			if (!canFlowTo(instance, offset, flowing, newState, currentState, direction)) continue;
			flow(instance, offset, newState, direction);
		}
	}
	
	private static final BlockFace[] HORIZONTAL = new BlockFace[] {
			BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST
	};
	
	/**
	 * Gets the updated state of a fluid block by taking into account its surrounding blocks.
	 */
	protected FluidState getUpdatedState(Instance instance, BlockVec point, FluidState original) {
		int highestLevel = 0;
		int stillCount = 0;
		
		for (BlockFace face : HORIZONTAL) {
			FluidState directionState = FluidState.of(instance.getBlock(point.relative(face)));
			if (directionState.fluid() != this || !receivesFlow(face, original, directionState))
				continue;
			
			if (directionState.isSource()) stillCount++;
			highestLevel = Math.max(highestLevel, directionState.getLevel());
		}
		
		if (isInfinite() && stillCount >= 2) {
			// If there's 2 or more still fluid blocks around
			// and below is still or a solid block, make this block still
			Block downBlock = instance.getBlock(point.add(0, -1, 0));
			if (downBlock.isSolid() || isMatchingAndStill(FluidState.of(downBlock))) {
				return defaultState.asSource(false);
			}
		}
		
		BlockVec above = point.add(0, 1, 0);
		FluidState aboveState = FluidState.of(instance.getBlock(above));
		if (!aboveState.isEmpty() && aboveState.fluid() == this && receivesFlow(BlockFace.TOP, original, aboveState))
			return defaultState.asFlowing(8, true);
		
		int newLevel = highestLevel - getLevelDecreasePerBlock(instance);
		if (newLevel <= 0) return FluidState.of(Block.AIR);
		return defaultState.asFlowing(newLevel, false);
	}
	
	@SuppressWarnings("UnstableApiUsage")
	private boolean receivesFlow(BlockFace face, FluidState from, FluidState to) {
		// Check if both block faces merged occupy the whole square
		return !from.block().registry().collisionShape().isOccluded(to.block().registry().collisionShape(), face);
	}
	
	/**
	 * Creates a unique id based on the relation between point and point2
	 */
	private static short getID(BlockVec point, BlockVec point2) {
		int i = (int) (point2.x() - point.x());
		int j = (int) (point2.z() - point.z());
		return (short) ((i + 128 & 0xFF) << 8 | j + 128 & 0xFF);
	}
	
	/**
	 * Returns a map with the directions the water can flow in and the block the water will become in that direction.
	 * If a hole is found within {@code getHoleRadius()} blocks, the water will only flow in that direction.
	 * A weight is used to determine which hole is the closest.
	 */
	protected Map<BlockFace, FluidState> getSpread(Instance instance, BlockVec point, FluidState flowing) {
		int weight = 1000;
		EnumMap<BlockFace, FluidState> map = new EnumMap<>(BlockFace.class);
		Short2BooleanOpenHashMap holeMap = new Short2BooleanOpenHashMap();
		
		for (BlockFace direction : HORIZONTAL) {
			BlockVec directionPoint = point.relative(direction);
			FluidState directionState = FluidState.of(instance.getBlock(directionPoint));
			short id = FlowableFluid.getID(point, directionPoint);
			
			if (!canFlowThrough(instance, directionPoint, flowing, directionState, direction))
				continue;
			
			FluidState newState = getUpdatedState(instance, directionPoint, directionState);
			
			boolean down = holeMap.computeIfAbsent(id, s -> {
				BlockVec downPoint = directionPoint.add(0, -1, 0);
				return isWaterHole(instance, defaultState.asFlowing(newState.getLevel(), false), downPoint);
			});
			
			int newWeight = down ? 0 : getWeight(instance, directionPoint, 1,
					direction.getOppositeFace(), directionState, point, holeMap);
			if (newWeight < weight) map.clear();
			
			if (newWeight <= weight) {
				if (directionState.fluid().canBeReplacedWith(instance, directionPoint, newState.fluid(), direction)) {
					map.put(direction, newState);
				}
				
				weight = newWeight;
			}
		}
		
		return map;
	}
	
	protected int getWeight(Instance instance, BlockVec point, int initialWeight, BlockFace skipCheck,
	                        FluidState flowing, BlockVec originalPoint, Short2BooleanMap short2BooleanMap) {
		// NOTE: flowing will often be air
		
		int weight = 1000;
		for (BlockFace direction : HORIZONTAL) {
			if (direction == skipCheck) continue;
			BlockVec directionPoint = point.relative(direction);
			FluidState directionState = FluidState.of(instance.getBlock(directionPoint));
			short id = FlowableFluid.getID(originalPoint, directionPoint);
			
			if (!canFlowThrough(instance, directionPoint, defaultState.asFlowing(7, false), directionState, direction))
				continue;
			
			boolean down = short2BooleanMap.computeIfAbsent(id, s -> {
				BlockVec downPoint = directionPoint.add(0, -1, 0);
				return isWaterHole(instance, defaultState.asFlowing(7, false), downPoint);
			});
			if (down) return initialWeight;
			
			if (initialWeight < getHoleRadius(instance)) {
				int newWeight = getWeight(instance, directionPoint, initialWeight + 1,
						direction.getOppositeFace(), directionState, originalPoint, short2BooleanMap);
				if (newWeight < weight) weight = newWeight;
			}
		}
		return weight;
	}
	
	private int getAdjacentSourceCount(Instance instance, BlockVec point) {
		int i = 0;
		for (Direction direction : Direction.HORIZONTAL) {
			BlockVec currentPoint = point.add(direction.normalX(), direction.normalY(), direction.normalZ());
			Block block = instance.getBlock(currentPoint);
			if (!isMatchingAndStill(FluidState.of(block))) continue;
			++i;
		}
		return i;
	}
	
	/**
	 * Returns whether the fluid can flow through a specific block
	 */
	private boolean canFill(Instance instance, BlockVec point, Block block, FluidState flowing) {
		WaterlogHandler handler = MinestomFluids.getWaterlog(block);
		if (handler != null) return handler.canPlaceFluid(instance, point, block, flowing);
		
		TagManager tags = MinecraftServer.getTagManager();
		if (block.compare(Block.LADDER)
				|| block.compare(Block.SUGAR_CANE)
				|| block.compare(Block.BUBBLE_COLUMN)
				|| block.compare(Block.NETHER_PORTAL)
				|| block.compare(Block.END_PORTAL)
				|| block.compare(Block.END_GATEWAY)
				|| tags.getTag(Tag.BasicType.BLOCKS, "minecraft:signs").contains(block.key())
				|| block.name().contains("door")
				|| block.name().contains("coral")) {
			return false;
		}
		return !block.isSolid();
	}
	
	private boolean isWaterHole(Instance instance, FluidState flowing, BlockVec flowTo) {
		FluidState flowToState = FluidState.of(instance.getBlock(flowTo));
		if (!receivesFlow(BlockFace.BOTTOM, flowing, flowToState)) return false; // Don't flow down if the path is obstructed
		if (flowing.sameFluid(flowToState)) return true; // Always flow down when the fluid is the same
		return canFill(instance, flowTo, flowToState.block(), flowing); // Flow down when the block beneath can be filled
	}
	
	private boolean canFlowThrough(Instance instance, BlockVec flowTo,
	                               FluidState flowing, FluidState state,
	                               BlockFace face) {
		return !isMatchingAndStill(state) // Don't flow through if matching and still
				&& receivesFlow(face, flowing, state) // Only flow through when the path is not obstructed
				&& canFill(instance, flowTo, state.block(), flowing); // Only flow through when the block can be filled
	}
	
	protected boolean canFlowTo(Instance instance, BlockVec flowTo,
	                            FluidState flowing, FluidState newState, FluidState currentState,
	                            BlockFace flowFace) {
		return currentState.fluid().canBeReplacedWith(instance, flowTo, newState.fluid(), flowFace)
				&& receivesFlow(flowFace, flowing, currentState) // Only flow when the path is not obstructed
				&& canFill(instance, flowTo, currentState.block(), newState); // Only flow when the block can be filled
	}
	
	/**
	 * Puts the new block at the position, executing {@code onBreakingBlock()} before breaking any non-air block.
	 */
	protected void flow(Instance instance, BlockVec point, FluidState newState, BlockFace direction) {
		if (point.y() < MinecraftServer.getDimensionTypeRegistry().get(instance.getDimensionType()).minY())
			return; // Prevent errors when flowing into the void
		
		Block currentBlock = instance.getBlock(point);
		WaterlogHandler handler = MinestomFluids.getWaterlog(currentBlock);
		if (handler != null) {
			handler.placeFluid(instance, point, newState);
		} else {
			if (currentBlock.equals(newState.block())) return; // Prevent unnecessary updates
			
			if (!currentBlock.isAir() && !onBreakingBlock(instance, point, currentBlock)) {
				// Event has been cancelled
				return;
			}
			
			instance.setBlock(point, newState.block());
			MinestomFluids.scheduleTick(instance, point, newState);
		}
	}
	
	private boolean isMatchingAndStill(FluidState state) {
		return state.fluid() == this && state.isSource();
	}
	
	protected abstract boolean isInfinite();

	protected abstract int getLevelDecreasePerBlock(Instance instance);

	protected abstract int getHoleRadius(Instance instance);
	
	/**
	 * Returns whether the block can be broken
	 */
	protected abstract boolean onBreakingBlock(Instance instance, BlockVec point, Block block);
	
	private static boolean isFluidAboveEqual(Block block, Instance instance, Point point) {
		return MinestomFluids.get(block) == MinestomFluids.get(instance.getBlock(point.add(0, 1, 0)));
	}
	
	@Override
	public double getHeight(Instance instance, BlockVec point) {
		Block block = instance.getBlock(point);
		return isFluidAboveEqual(block, instance, point) ? 1 : getHeight(FluidState.of(block));
	}
	
	@Override
	public double getHeight(FluidState state) {
		return state.getLevel() / 9.0;
	}
}
