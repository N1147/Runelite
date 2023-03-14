![](https://runelite.net/img/logo.png)
# runelite [![CI](https://github.com/runelite/runelite/workflows/CI/badge.svg)](https://github.com/runelite/runelite/actions?query=workflow%3ACI+branch%3Amaster) [![Discord](https://img.shields.io/discord/301497432909414422.svg)](https://discord.gg/ArdAhnN)

RuneLite is a free, open source OldSchool RuneScape client.

If you have any questions, please join our IRC channel on [irc.rizon.net #runelite](http://qchat.rizon.net/?channels=runelite&uio=d4) or alternatively our [Discord](https://discord.gg/ArdAhnN) server.

## Project Layout

- [cache](cache/src/main/java/net/runelite/cache) - Libraries used for reading/writing cache files, as well as the data in it
- [runelite-api](runelite-api/src/main/java/net/runelite/api) - RuneLite API, interfaces for accessing the client
- [runelite-client](runelite-client/src/main/java/net/runelite/client) - Game client with plugins

## Usage

Open the project in your IDE as a Maven project, build the root module and then run the RuneLite class in runelite-client.  
For more information visit the [RuneLite Wiki](https://github.com/runelite/runelite/wiki).

### License

RuneLite is licensed under the BSD 2-clause license. See the license header in the respective file to be sure.

## Bot / Automation Features

For bot/automation usage, we have included our own "Utils" plugin, in which you will find all of the supporting code for your bot plugins. This will make creating your bot plugins VERY simple, with functions such as: useItem(ID), activatePrayer(Prayer), clickSpell(Spell), findNearestBank() and many, many more. Some of the previously removed methods by RuneLite to retrieve the locations and click points of objects/items have been added back into the client, also known as Queries. 

We have also enabled sideloading plugins for all users (I.E Developer mode) to allow for easier access to the tools required to use, and create your plugins. 
