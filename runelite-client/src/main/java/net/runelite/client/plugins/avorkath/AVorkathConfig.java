package net.runelite.client.plugins.avorkath;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;


@ConfigGroup("avork")
public interface AVorkathConfig extends Config
{
	@ConfigSection(
			//keyName = "Potions",
			name = "Pots/Food",
			description = "",
			position = 14
	)
	String Potions = "Potions";

	@ConfigSection(
			//keyName = "Spec",
			name = "Spec",
			description = "",
			position = 15
	)
	String Spec = "Spec";

	@ConfigSection(
			//keyName = "Other",
			name = "Other",
			description = "",
			position = 16
	)
	String Other = "Other";

	@ConfigItem(
			keyName = "supers",
			name = "Super Pots",
			description = "Enable to use Bastion potions for ranged, Super Combat for melee. Disable to use ranging potions for ranged, Combat potions for melee.",
			position = 55,
			section = Potions
	)
	default boolean supers() { return true; }

	@ConfigItem(
			keyName = "potThreshold",
			name = "Level to Drink",
			description = "Enter level to drink combat related potions, e.g set at 99, it will drink at or below 99",
			position = 56,
			section = Potions
	)
	default int potThreshold() { return 99; }


	@ConfigItem(
			keyName = "hp",
			name = "HP to eat",
			description = "Enter the HP threshold at which to eat.",
			position = 57,
			section = Potions
	)
	default int hp() { return 70; }

	@ConfigItem(
			keyName = "pray",
			name = "Prayer to drink",
			description = "Enter the prayer threshold at which to restore.",
			position = 58,
			section = Potions
	)
	default int pray() { return 45; }


	@ConfigItem(
			keyName = "useScrolls",
			name = "Lunar Isle Scrolls",
			description = "If disabled, uses mounted glory inside POH",
			position = 55,
			section = Other
	)
	default boolean useScrolls() { return true; }


	@ConfigItem(
			keyName = "useRanged",
			name = "Ranged Mode",
			description = "If disabled, uses melee",
			position = 56,
			section = Other
	)
	default boolean useRanged() { return true; }


	@ConfigItem(
			keyName = "autoBank",
			name = "Auto Restock",
			description = "If disabled, will only automate the kills and not bank for you.",
			position = 57,
			section = Other
	)
	default boolean autoBank() { return true; }

	@ConfigItem(
			keyName = "useBlowpipe",
			name = "Blowpipe",
			description = "If disabled, will attempt to use Bolts",
			position = 58,
			section = Other
	)
	default boolean useBlowpipe() { return false; }

	@ConfigItem(
			keyName = "gear",
			name = "Gear IDs",
			description = "Full gear set-up (for returning after death) separate with commas.",
			position = 59,
			section = Other
	)
	default String gear() { return ""; }

	/*@ConfigItem(
			keyName = "wooxWalk",
			name = "Woox Walk",
			description = "If disabled, still dodges acid but does not attack",
			position = 58,
			section = "Other"
	)
	default boolean wooxWalk() { return false; }

	@ConfigItem(
			keyName = "wwTicks",
			name = "WooxWalk Ticks",
			description = "How many ticks to wait between each attack when woox walking.",
			position = 59,
			section = "Other"
	)
	default int wwTicks() { return 2; }*/


	@ConfigItem(
			keyName = "lootNames",
			name = "Items to loot (separate with comma)",
			description = "Provide partial or full names of items you'd like to loot.",
			position = 65,
			section = Other
	)
	default String lootNames() {
		return "visage,lump,limb,scroll,key,med,legs,shield,shield,ore,stone,rune,bar,wrath,bolts,grimy,coins";
	}

	@ConfigItem(
			keyName = "superantifire",
			name = "Ext Super Antifire",
			description =  "Enable to use Extended Super Antifire. Disable to use regular antifire.",
			position = 66,
			section = Potions
	)
	default boolean superantifire()
	{
		return true;
	}

	@ConfigItem(
			keyName = "antivenomplus",
			name = "Anti Venom+",
			description =  "Enable to use Anti-venom+. Disable to use Antidote++",
			position = 67,
			section = Potions
	)
	default boolean antivenomplus()
	{
		return true;
	}

	@ConfigItem(
			keyName = "antipoisonamount",
			name = "Antivenom Amount",
			description =  "Amount of (4) dose Antivenom+, or Antidote++ to take",
			position = 67,
			section = Potions
	)
	default int antipoisonamount() { return 1; }

	@ConfigItem(
			keyName = "usePOHpool",
			name = "Drink POH Pool",
			description =  "Enable to drink from POH pool to restore HP / Prayer.",
			position = 68,
			section = Other
	)
	default boolean usePOHpool()
	{
		return true;
	}


	@ConfigItem(
			keyName = "useRestores",
			name = "Use Super Restores",
			description = "Disable to use Prayer Potions",
			position = 70,
			section = Potions
	)
	default boolean useRestores() { return true; }

	@ConfigItem(
			keyName = "onlytelenofood",
			name = "Only Tele With No Food",
			description =  "Enable to only teleport out when you have 0 food and / or 0 restore pots. Disable to teleport out after every kill.",
			position = 71,
			section = Other
	)
	default boolean onlytelenofood()
	{
		return false;
	}


	@ConfigItem(
			keyName = "foodID",
			name = "ID of food",
			description = "ID of food to withdraw.",
			position = 80,
			section = Potions
	)
	default int foodID() { return 385; }


	@ConfigItem(
			keyName = "healthTP",
			name = "Min Health",
			description = "Minimum health to allow before teleporting (after running out of food)",
			position = 82,
			section = Other
	)
	default int healthTP() { return 40; }

	@ConfigItem(
			keyName = "prayTP",
			name = "Min Pray",
			description = "Minimum prayer to allow before teleporting (after running out of potions)",
			position = 82,
			section = Other
	)
	default int prayTP() { return 1; }

	@ConfigItem(
			keyName = "useSpec",
			name = "Use Spec Weapon",
			description = "Enable to use a special attack.",
			position = 83,
			section = Spec
	)
	default boolean useSpec() { return false; }

	@ConfigItem(
			keyName = "specWeapon",
			name = "Spec Weapon ID",
			description = "ID of special attack weapon",
			position = 84,
			section = Spec
	)
	default int specWeapon() { return 0; }

	@ConfigItem(
			keyName = "normalWeapon",
			name = "Regular Weapon ID",
			description = "ID of regular weapon",
			position = 85,
			section = Spec
	)
	default int normalWeapon() { return 0; }

	@ConfigItem(
			keyName = "normalOffhand",
			name = "Regular Offhand ID",
			description = "ID of regular offhand (0 for none)",
			position = 85,
			section = Spec
	)
	default int normalOffhand() { return 0; }

	@ConfigItem(
			keyName = "specHP",
			name = "Spec HP",
			description = "Minimum health Vorkath must have before spec",
			position = 86,
			section = Spec
	)
	default int specHP() { return 200; }

	@ConfigItem(
			keyName = "specThreshold",
			name = "Spec Energy",
			description = "Amount of special attack energy required to spec",
			position = 86,
			section = Spec
	)
	default int specThreshold() { return 50; }

}