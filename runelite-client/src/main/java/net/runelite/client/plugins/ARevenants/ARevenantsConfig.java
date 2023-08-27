package net.runelite.client.plugins.ARevenants;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ARevenants")
public interface ARevenantsConfig extends Config
{
	@ConfigItem(
			keyName = "foodID",
			name = "Food ID",
			description = "ID of the food plugin should use.",
			position = 0
	)
	default int foodID() { return 3144; }

	@ConfigItem(
			keyName = "sanfewSerums",
			name = "Enable to use Sanfew Serums, disable to use Super Restores.",
			description = "",
			position = 0
	)
	default boolean sanfewSerums() { return false; }

	@ConfigItem(
			keyName = "staminaPots",
			name = "Enable to use Stamina potions.",
			description = "",
			position = 0
	)
	default boolean staminaPots() { return false; }

	@ConfigItem(
			keyName = "energyThreshold",
			name = "Energy to Drink Stam.",
			description = "Minimum run energy before toggling run, and drinking Stamina potion.",
			position = 0
	)
	default int energyThreshold() { return 30; }

	@ConfigItem(
			keyName = "pickupFood",
			name = "Loot Blighted Items",
			description = "",
			position = 0
	)
	default boolean pickupFood() { return false; }

	@ConfigItem(
			keyName = "hopWorlds",
			name = "Hop Worlds",
			description = "Enable to hop worlds after escaping or being PKed",
			position = 0
	)
	default boolean hopWorlds() { return true; }

	@ConfigItem(
			keyName = "dhideBodies",
			name = "Dhide Body",
			description = "Enable to equip Dragonhide Body.",
			position = 0
	)
	default boolean dhideBodies() { return true; }

	@ConfigItem(
			keyName = "seedPod",
			name = "Royal Seed Pod",
			description = "Skips ring of wealth, using seed pod instead.",
			position = 1
	)
	default boolean seedPod() { return false; }

	@ConfigItem(
			keyName = "avarice",
			name = "Amulet of Avarice",
			description = "If disabled, will use Amulet of Glory (6).",
			position = 2
	)
	default boolean avarice() { return true; }

	@ConfigItem(
			keyName = "avoidRevs",
			name = "Hide from revs to Escape",
			description = "If enabled, will hide from revenants before trying to escape.",
			position = 3
	)
	default boolean avoidRevs() { return false; }

	@ConfigItem(
			keyName = "attackingOnly",
			name = "Only tele from Attacks",
			description = "If enabled, only teleports if PKers are trying to attack you.",
			position = 4
	)
	default boolean attackingOnly() { return true; }

	@ConfigItem(
			keyName = "weaponsOnly",
			name = "Only tele from PK Weapons",
			description = "If enabled, only teleports if PKers are wearing PK weapons.",
			position = 5
	)
	default boolean weaponsOnly() { return true; }

	@ConfigItem(
			keyName = "type",
			name = "Type of Revs",
			description = "",
			position = 6
	)
	default Type type() { return Type.PYREFIENDS; }

	@ConfigItem(
			keyName = "blackDhide",
			name = "Use black d'hide",
			description = "If disabled, will use red d'hide instead.",
			position = 7
	)
	default boolean blackDhide() { return true; }

	@ConfigItem(
			keyName = "amethyst",
			name = "Use Amethyst Arrows",
			description = "If disabled, will use rune arrows instead.",
			position = 8
	)
	default boolean amethyst() { return true; }

	@ConfigItem(
			keyName = "prayerThreshold",
			name = "Min. Prayer Points",
			description = "Minimum amount of prayer points remaining until a potion should be drank.",
			position = 9
	)
	default int prayerThreshold() { return 25; }

	@ConfigItem(
			keyName = "lootThreshold",
			name = "Loot Threshold",
			description = "Minimum amount of prayer points remaining until a potion should be drank.",
			position = 9
	)
	default int lootThreshold() { return 300000; }

	@ConfigItem(
			keyName = "worldDelay",
			name = "World Hop Tick Delay",
			description = "",
			position = 10
	)
	default int worldDelay() { return 30; }

	@ConfigItem(
			keyName = "world1",
			name = "World 1",
			description = "",
			position = 11
	)
	default int world1() { return 302; }

	@ConfigItem(
			keyName = "world2",
			name = "World 2",
			description = "",
			position = 12
	)
	default int world2() { return 303; }

	@ConfigItem(
			keyName = "world3",
			name = "World 3",
			description = "",
			position = 13
	)
	default int world3() { return 304; }

	@ConfigItem(
			keyName = "world4",
			name = "World 4",
			description = "",
			position = 14
	)
	default int world4() { return 305; }

	@ConfigItem(
			keyName = "world5",
			name = "World 5",
			description = "",
			position = 15
	)
	default int world5() { return 306; }

	@ConfigItem(
			keyName = "world6",
			name = "World 6",
			description = "",
			position = 16
	)
	default int world6() { return 307; }

	@ConfigItem(
			keyName = "world7",
			name = "World 7",
			description = "",
			position = 17
	)
	default int world7() { return 309; }

	@ConfigItem(
			keyName = "world8",
			name = "World 8",
			description = "",
			position = 18
	)
	default int world8() { return 310; }

	@ConfigItem(
			keyName = "world9",
			name = "World 9",
			description = "",
			position = 19
	)
	default int world9() { return 311; }

	@ConfigItem(
			keyName = "world10",
			name = "World 10",
			description = "",
			position = 20
	)
	default int world10() { return 312; }

}