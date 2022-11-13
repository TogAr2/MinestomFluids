package io.github.togar2.fluids.test;

import io.github.togar2.fluids.MinestomFluids;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

public class Main {
	public static void main(String[] args) {
		MinecraftServer server = MinecraftServer.init();
		MinestomFluids.init();
		
		var dimension = DimensionType.builder(NamespaceID.from("test"))
				.ambientLight(2.0f).build();
		MinecraftServer.getDimensionTypeManager().addDimension(dimension);
		var instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);
		instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
		var spawn = new Pos(0, 40, 0);
		
		MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent.class, event -> {
			event.setSpawningInstance(instance);
			event.getPlayer().setRespawnPoint(spawn);
			event.getPlayer().setGameMode(GameMode.CREATIVE);
		});
		
		MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockInteractEvent.class, event -> {
			if (event.getPlayer().getItemInHand(event.getHand()).material() == Material.WATER_BUCKET) {
				event.getInstance().setBlock(event.getBlockPosition().relative(event.getBlockFace()), Block.WATER);
			}
		});
		
		MinecraftServer.getGlobalEventHandler().addChild(MinestomFluids.events());
		
		server.start("localhost", 25565);
	}
}
