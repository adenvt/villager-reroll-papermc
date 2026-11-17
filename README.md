# VillagerReroll

VillagerReroll is a Paper plugin that lets players generate fresh villager trades from the villager’s trade screen. It uses Minecraft’s own trade generation and leaves the villager’s profession and workstation connection intact.

## How to reroll trades

1. Right-click a villager to open its trade screen.
2. Select the **Reroll Trades** offer at the bottom of the list.
3. The villager’s offers are replaced with freshly generated trades.

The offer uses a placeholder emerald ingredient, but clicking it does not consume an emerald. The plugin refreshes the open trade screen when possible and reopens it if Minecraft has already closed it.

## Which villagers can be rerolled?

By default, the reroll offer is available only for villagers that have never been traded with. This follows vanilla’s behavior: once a villager has been traded with, changing its workstation does not generate a new set of offers.

To allow rerolling for all villagers, set this option in `plugins/VillagerReroll/config.yml`:

```yaml
reroll:
  only-fresh-villagers: false
```

Restart the server after changing the setting.

## Installation

You’ll need Java 21 and a Paper 1.21.6 server.

1. Build the plugin:

   ```bash
   ./gradlew build
   ```

2. Copy the JAR from `build/libs/` into your server’s `plugins` folder.
3. Restart the server.

## How it works

When a player rerolls, the plugin creates a temporary villager with the same profession and villager type. Minecraft generates that villager’s trades, which the plugin then applies to the original villager. The temporary villager is removed afterward.

The original villager keeps its profession and workstation connection. The plugin also resets its trade level and experience as part of the reroll. During a clean shutdown, it removes the placeholder offer from loaded villagers.

## License

This repository does not currently include a license file. Ask the project owner about reuse and distribution terms.
