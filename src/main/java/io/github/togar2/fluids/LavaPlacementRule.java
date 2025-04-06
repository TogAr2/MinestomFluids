package io.github.togar2.fluids;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LavaPlacementRule extends FluidPlacementRule {
	public LavaPlacementRule(@NotNull Block block) {
		super(block);
	}
	
	@Override
	public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
		Instance instance = (Instance) updateState.instance();
		BlockVec blockVec = new BlockVec(updateState.blockPosition());
		
		FluidState state = FluidState.of(updateState.currentBlock());
		Block interaction = handleInteraction(instance, blockVec, state);
		if (interaction != null) return interaction;
		
		return super.blockUpdate(updateState);
	}
	
	@Override
	public @NotNull Block blockPlace(@NotNull PlacementState placementState) {
		Instance instance = (Instance) placementState.instance();
		BlockVec blockVec = new BlockVec(placementState.placePosition());
		
		FluidState state = FluidState.of(placementState.block());
		Block interaction = handleInteraction(instance, blockVec, state);
		if (interaction != null) return interaction;
		
		return super.blockPlace(placementState);
	}
	
	private static final BlockFace[] FLOW_DIRECTIONS = new BlockFace[] {
			BlockFace.BOTTOM, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST
	};
	
	/**
	 * Handles interaction with other fluids.
	 * @return the block, if any, that should replace the fluid
	 */
	private @Nullable Block handleInteraction(Instance instance, BlockVec point, FluidState state) {
		boolean soil = instance.getBlock(point.sub(0, 1, 0)).compare(Block.SOUL_SOIL);
		
		for (BlockFace direction : FLOW_DIRECTIONS) {
			FluidState directionState = FluidState.of(instance.getBlock(point.relative(direction.getOppositeFace())));
			if (directionState.isWater()) {
				LavaFluid.fizz(instance, point);
				return state.isSource() ? Block.OBSIDIAN : Block.COBBLESTONE;
			}
			
			if (soil && directionState.block().compare(Block.BLUE_ICE)) {
				LavaFluid.fizz(instance, point);
				return Block.BASALT;
			}
		}
		
		return null;
	}
}
