package io.github.togar2.fluids;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;

public class FluidPlacementRule extends BlockPlacementRule {
	public FluidPlacementRule(@NotNull Block block) {
		super(block);
	}
	
	@Override
	public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
		String waterlogged = updateState.currentBlock().properties().get("waterlogged");
		if (waterlogged == null || waterlogged.equals("true")) {
			MinestomFluids.scheduleTick(
					(Instance) updateState.instance(), new BlockVec(updateState.blockPosition()),
					FluidState.of(updateState.currentBlock())
			);
		}
		return super.blockUpdate(updateState);
	}
	
	@Override
	public @NotNull Block blockPlace(@NotNull PlacementState placementState) {
		String waterlogged = placementState.block().properties().get("waterlogged");
		if (waterlogged == null || waterlogged.equals("true")) {
			MinestomFluids.scheduleTick(
					(Instance) placementState.instance(), new BlockVec(placementState.placePosition()),
					FluidState.of(placementState.block())
			);
		}
		return placementState.block();
	}
}
