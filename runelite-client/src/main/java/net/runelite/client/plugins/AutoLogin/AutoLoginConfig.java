package net.runelite.client.plugins.AutoLogin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("AutoLogin")
public interface AutoLoginConfig extends Config
{
    @ConfigItem(
            keyName = "username",
            name = "Username",
            description = "",
            position = 1
    )
    default String username()
    {
        return "";
    }

    @ConfigItem(
            keyName = "password",
            name = "Password",
            description = "",
            position = 2
    )
    default String password()
    {
        return "";
    }
}