# MinestomFluids

[![license](https://img.shields.io/github/license/TogAr2/MinestomFluids.svg?style=flat-square)](LICENSE)
[![platform](https://img.shields.io/badge/platform-Minestom-ff69b4?style=flat-square)](https://github.com/Minestom/Minestom)

MinestomFluids is a simple library for Minestom, which adds fluid mechanics.

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

MinestomFluids will call a `WaterBlockBreakEvent` upon breaking a block, which can be cancelled to prevent the action.

## Contributing

You are welcome to open an issue or pull request.
