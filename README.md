# MinestomFluids

[![license](https://img.shields.io/github/license/TogAr2/MinestomFluids.svg?style=flat-square)](LICENSE)
[![platform](https://img.shields.io/badge/platform-Minestom-ff69b4?style=flat-square)](https://github.com/Minestom/Minestom)

MinestomFluids is a simple library for Minestom, which adds fluid mechanics.
It currently supports both water and lava, and also has support for waterlogging.

The maven repository is available on [jitpack](https://jitpack.io/#TogAr2/MinestomFluids).

## Usage

To use the library, add it as a dependency to your project.

Before using it, you should call `MinestomFluids.init()`.
This will register the custom `BlockPlacementRule` that allows the fluids to detect when a neighbour changes.
After you've initialized the extension, you can get an `EventNode` containing listeners which provide fluid ticking using `MinestomBlocks.events()`.
By adding this node as a child to any other node, you enable the fluids in that scope.

Example:
```java
MinestomFluids.init();
MinecraftServer.getGlobalEventHandler().addChild(MinestomFluids.events());
```

When placing a fluid by code, you should use `instance.placeBlock()` instead of `setBlock()`, because it will cause the block handler of the fluid to trigger. Otherwise, it will not start to flow.

You can register a custom waterlog handler for certain blocks using `MinestomFluids.registerWaterlog(Block, WaterLogHandler)`.
By default, every waterloggable block has the same handler.

## Events

- `FluidBlockBreakEvent`: called upon breaking a block, which can be cancelled or can be used to set the new fluid state.
- `LavaSolidifyEvent`: called when lava turns into stone/cobblestone/obsidian when in contact with water. This event can be cancelled or can be used to change the resulting block.

## Known issues

Fluids may get updated too quickly in some cases, if there are multiple neighbour changes in a short timespan.
This is especially noticeable with cobblestone generators: breaking the cobblestone block will cause a fluid update, but generating the cobblestone block back will trigger a fluid update as well.
This will result in the cobblestone often generating back too quickly.

Sadly, it is very difficult to solve this issue without using the complicated ticking and neighbour update mechanics present in vanilla.

## Contributing

You are welcome to open an issue or pull request.
