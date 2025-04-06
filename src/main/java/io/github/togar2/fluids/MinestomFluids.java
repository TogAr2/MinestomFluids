package io.github.togar2.fluids;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MinestomFluids {
	public static final Fluid WATER = new WaterFluid();
	public static final Fluid EMPTY = new EmptyFluid();
	
	private static final Map<Integer, WaterlogHandler> WATERLOG_HANDLERS = new ConcurrentHashMap<>();
	
	private static final Tag<Map<Long, Set<BlockVec>>> UPDATES = Tag.Transient("fluid-updates");
	
	public static Fluid get(Block block) {
		if (block.compare(Block.WATER) || isWaterlogged(block)) {
			return WATER;
		} else if (block.compare(Block.LAVA)) {
			return EMPTY;
		} else {
			return EMPTY;
		}
	}
	
	public static void tick(InstanceTickEvent event) {
		Instance instance = event.getInstance();
		long age = instance.getWorldAge();
		
		var updates = instance.getTag(UPDATES);
		if (updates == null) {
			updates = new ConcurrentHashMap<>();
			instance.setTag(UPDATES, updates);
		}
		
		Set<BlockVec> currentUpdate = updates.remove(age);
		if (currentUpdate == null) return;
		
		for (BlockVec point : currentUpdate) {
			tick(event.getInstance(), point);
		}
	}
	
	public static void tick(Instance instance, BlockVec point) {
		FluidState state = FluidState.of(instance.getBlock(point));
		state.fluid().onTick(instance, point, state);
	}
	
	public static void scheduleTick(Instance instance, BlockVec point, FluidState state) {
		int tickDelay = state.fluid().getNextTickDelay(instance, point);
		if (tickDelay == -1) return;
		
		var updates = instance.getTag(UPDATES);
		if (updates == null) {
			updates = new ConcurrentHashMap<>();
			instance.setTag(UPDATES, updates);
		}
		
		long newAge = instance.getWorldAge() + tickDelay;
		updates.computeIfAbsent(newAge, l -> new HashSet<>()).add(point);
	}
	
	public static void registerWaterlog(Block block, WaterlogHandler handler) {
		WATERLOG_HANDLERS.put(block.id(), handler);
	}
	
	public static WaterlogHandler getWaterlog(Block block) {
		return WATERLOG_HANDLERS.get(block.id());
	}
	
	public static boolean canBeWaterlogged(Block block) {
		return block.properties().containsKey("waterlogged");
	}
	
	public static boolean isWaterlogged(Block block) {
		String waterlogged = block.getProperty("waterlogged");
		return waterlogged != null && waterlogged.equals("true");
	}
	
	public static Block setWaterlogged(Block block, boolean waterlogged) {
		return block.withProperty("waterlogged", waterlogged ? "true" : "false");
	}
	
	public static void init() {
		MinecraftServer.getBlockManager().registerBlockPlacementRule(new FluidPlacementRule(Block.WATER));
		
		for (Block block : Block.values()) {
			if (canBeWaterlogged(block)) {
				registerWaterlog(block, WaterlogHandler.DEFAULT);
				MinecraftServer.getBlockManager().registerBlockPlacementRule(new FluidPlacementRule(block));
			}
		}
	}
	
	public static EventNode<Event> events() {
		EventNode<Event> node = EventNode.all("fluid-events");
		node.addListener(InstanceTickEvent.class, MinestomFluids::tick);
		return node;
	}
}
