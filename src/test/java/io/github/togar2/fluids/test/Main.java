package io.github.togar2.fluids.test;

import io.github.togar2.fluids.Fluid;
import io.github.togar2.fluids.FluidState;
import io.github.togar2.fluids.MinestomFluids;
import io.github.togar2.fluids.WaterlogHandler;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.Material;
import net.minestom.server.world.DimensionType;

import java.util.Objects;

public class Main {
	public static void main(String[] args) {
		MinecraftServer server = MinecraftServer.init();
		MinestomFluids.init();
		
		var dimension = DimensionType.builder()
				.ambientLight(2.0f).build();
		MinecraftServer.getDimensionTypeRegistry().register(Key.key("test"), dimension);
		var instance = MinecraftServer.getInstanceManager().createInstanceContainer(Objects.requireNonNull(
				MinecraftServer.getDimensionTypeRegistry().getKey(dimension)));
		instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
		var spawn = new Pos(0, 40, 0);
		
		MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
			event.setSpawningInstance(instance);
			event.getPlayer().setRespawnPoint(spawn);
			event.getPlayer().setGameMode(GameMode.CREATIVE);
		});

		MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockInteractEvent.class, event -> {
			if (event.getPlayer().getItemInHand(event.getHand()).material() == Material.WATER_BUCKET) {
				WaterlogHandler handler = MinestomFluids.getWaterlog(event.getBlock());
				if (handler != null) {
					handler.placeFluid(instance, event.getBlockPosition(), MinestomFluids.WATER.getDefaultState());
				} else {
					event.getInstance().placeBlock(new BlockHandler.Placement(
							Block.WATER, event.getInstance(), event.getBlockPosition().relative(event.getBlockFace())));
				}
			} else if (event.getPlayer().getItemInHand(event.getHand()).material() == Material.BUCKET) {
				WaterlogHandler handler = MinestomFluids.getWaterlog(event.getBlock());
				if (handler != null && handler.canRemoveFluid(instance, event.getBlockPosition(), FluidState.of(event.getBlock()))) {
					event.getInstance().setBlock(event.getBlockPosition(), FluidState.setWaterlogged(event.getBlock(), false));
				} else if (event.getBlock().isLiquid()) {
					FluidState state = FluidState.of(event.getBlock());
					event.getPlayer().setItemInHand(event.getHand(), state.fluid().getBucket());
					event.getInstance().setBlock(event.getBlockPosition(), Block.AIR);
				}
			} else if (event.getPlayer().getItemInHand(event.getHand()).material() == Material.LAVA_BUCKET) {
				event.getInstance().placeBlock(new BlockHandler.Placement(
						Block.LAVA, event.getInstance(), event.getBlockPosition().relative(event.getBlockFace())));
			}
		});
		
		MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockBreakEvent.class, event -> {
			if (FluidState.isWaterlogged(event.getBlock())) {
				event.setResultBlock(Block.WATER);
			}
		});
		
		MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockPlaceEvent.class, event -> {
			Block originalBlock = event.getInstance().getBlock(event.getBlockPosition());
			Fluid fluid = MinestomFluids.get(originalBlock);
			if (fluid != MinestomFluids.EMPTY && FluidState.isSource(originalBlock) && FluidState.canBeWaterlogged(event.getBlock())) {
				event.setBlock(FluidState.setWaterlogged(event.getBlock(), true));
			}
		});
		
		MinecraftServer.getGlobalEventHandler().addChild(MinestomFluids.events());
		
		server.start("localhost", 25565);
	}
}
