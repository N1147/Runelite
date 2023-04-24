package net.runelite.client.plugins.experiencedrop;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;

@ConfigGroup("xpdrop")
public interface XpDropConfig extends Config
{
    @ConfigItem(
            keyName = "hideSkillIcons",
            name = "Hide skill icons",
            description = "Configure if XP drops will show their respective skill icons",
            position = 0
    )
    default boolean hideSkillIcons()
    {
        return false;
    }

    @ConfigItem(
            keyName = "standardColor",
            name = "Standard Color",
            description = "XP drop color when no prayer is active",
            position = 1
    )
    Color standardColor();

    @ConfigItem(
            keyName = "meleePrayerColor",
            name = "Melee Prayer Color",
            description = "XP drop color when a melee prayer is active",
            position = 2
    )
    default Color getMeleePrayerColor()
    {
        return new Color(0x15, 0x80, 0xAD);
    }

    @ConfigItem(
            keyName = "rangePrayerColor",
            name = "Range Prayer Color",
            description = "XP drop color when a range prayer is active",
            position = 3
    )
    default Color getRangePrayerColor()
    {
        return new Color(0x15, 0x80, 0xAD);
    }

    @ConfigItem(
            keyName = "magePrayerColor",
            name = "Mage Prayer Color",
            description = "XP drop color when a mage prayer is active",
            position = 4
    )
    default Color getMagePrayerColor()
    {
        return new Color(0x15, 0x80, 0xAD);
    }

    @ConfigItem(
            keyName = "fakeXpDropDelay",
            name = "Fake Xp Drop delay",
            description = "Configures how many ticks should pass between fake XP drops, 0 to disable",
            position = 5
    )
    @Units(Units.TICKS)
    default int fakeXpDropDelay()
    {
        return 0;
    }
}