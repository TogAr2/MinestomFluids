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
	public static final Fluid LAVA = new LavaFluid();
	public static final Fluid EMPTY = new EmptyFluid();
	
	public static final FluidState AIR_STATE = new FluidState(Block.AIR, EMPTY);
	
	private static final Map<Integer, WaterlogHandler> WATERLOG_HANDLERS = new ConcurrentHashMap<>();
	
	private static final Tag<Map<Long, Set<BlockVec>>> TICK_UPDATES = Tag.Transient("fluid-tick-updates");
	
	public static Fluid get(Block block) {
		if (block.compare(Block.WATER) || FluidState.isWaterlogged(block)) {
			return WATER;
		} else if (block.compare(Block.LAVA)) {
			return LAVA;
		} else {
			return EMPTY;
		}
	}
	
	public static void tick(InstanceTickEvent event) {
		Instance instance = event.getInstance();
		long age = instance.getWorldAge();
		
		var updates = instance.getTag(TICK_UPDATES);
		if (updates == null) {
			updates = new ConcurrentHashMap<>();
			instance.setTag(TICK_UPDATES, updates);
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
		scheduleTick(instance, point, state.fluid().getNextTickDelay(instance, point));
	}
	
	public static void scheduleTick(Instance instance, BlockVec point, int tickDelay) {
		if (tickDelay == -1) return;
		
		var updates = instance.getTag(TICK_UPDATES);
		if (updates == null) {
			updates = new ConcurrentHashMap<>();
			instance.setTag(TICK_UPDATES, updates);
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
	
	public static void init() {
		MinecraftServer.getBlockManager().registerBlockPlacementRule(new FluidPlacementRule(Block.WATER));
		MinecraftServer.getBlockManager().registerBlockPlacementRule(new LavaPlacementRule(Block.LAVA));
		
		for (Block block : Block.values()) {
			if (FluidState.canBeWaterlogged(block)) {
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
