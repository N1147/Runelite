package net.runelite.client.plugins.autologhop;


import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.PvPUtil;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.Utils.Core;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_F3;

@PluginDescriptor(
        name = "AutoLogHop",
        description = "Auto hops/logs out when another player is seen.",
        tags = {"logout", "hop worlds", "auto log", "auto hop"},
        enabledByDefault = false,
        hidden = false
)
@Slf4j
@SuppressWarnings("unused")
@Singleton
public class AutoLogHop extends Plugin {
    @Inject Core core;
    @Inject
    private Client client;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoLogHopConfig config;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClientThread clientThread;

    @Inject
    private WorldService worldService;


    private boolean login;

    @Provides
    AutoLogHopConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoLogHopConfig.class);
    }

    @Override
    protected void startUp() {

    }

    @Override
    protected void shutDown() {

    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (Timer2 != null) {
            core.useItem(CurrentItem, "wield");
            Timer2 = null;
        }
        if (nearPlayer()) {
            handleAction();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (!login || event.getGameState() != GameState.LOGIN_SCREEN || config.user().isBlank() || config.password().isBlank()) {
            return;
        }
        hopToWorld(getValidWorld());
       // executorService.submit(() -> {
            sleep(600);
            pressKey(VK_ENTER);
            client.setUsername(config.user());
            client.setPassword(config.password());
            sleep(600);
            pressKey(VK_ENTER);
            pressKey(VK_ENTER);
       // });
        login = false;
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) {
        if (isPlayerBad(event.getPlayer()))
            handleAction();
    }

    private boolean nearPlayer() {
        List<Player> players = client.getPlayers();
        for (Player p : players) {
            if (!isPlayerBad(p))
                continue;
            return true;
        }
        return false;
    }
    int CurrentItem;
    Instant Timer2;
    public void useItem(int ID, String option) {
        WidgetItem item = core.getWidgetItem(ID);
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        if ((client.getVarcIntValue(VarClientInt.INVENTORY_TAB) != 3 && client.getWidget(WidgetInfo.BANK_CONTAINER) == null)) {
            if (item.getWidget().isHidden() || item == null) {
                CurrentItem = ID;
                core.pressKey(VK_F3);
                Timer2 = Instant.now();
            }
            else {
                if (item != null) {
                    core.doInvoke(null, item.getCanvasBounds());
                }
            }
        }

    }
    private void handleAction() {
        switch (config.method()) {
            case HOP:
                hopToWorld(getValidWorld());
                break;
            case ROYAL_SEED_POD:
                core.useItem(ItemID.ROYAL_SEED_POD, MenuAction.ITEM_USE);
                break;
            default:
                logout();
                login = config.method() == Method.LOGOUT_HOP; //only login if we caused logout
                break;
        }
    }

    private boolean passedWildernessChecks() {
        return config.disableWildyChecks() || inWilderness();
    }

    private boolean isPlayerBad(Player player) {
        if (player == client.getLocalPlayer())
            return false;

        if (isInWhitelist(player.getName()))
            return false;

        if (config.combatRange() && !PvPUtil.isAttackable(client, player))
            return false;

        if (config.skulledOnly() && !isPlayerSkulled(player))
            return false;

        if (!passedWildernessChecks())
            return false;

        return true;
    }

    private int getValidWorld() {
        WorldResult result = worldService.getWorlds();
        if (result == null)
            return -1;
        List<World> worlds = result.getWorlds();
        //Collections.shuffle(worlds);
        for (World w : worlds) {
            if (client.getWorld() == w.getId())
                continue;

            if (w.getTypes().contains(net.runelite.http.api.worlds.WorldType.HIGH_RISK) ||
                    w.getTypes().contains(net.runelite.http.api.worlds.WorldType.DEADMAN) ||
                    w.getTypes().contains(net.runelite.http.api.worlds.WorldType.PVP) ||
                    w.getTypes().contains(net.runelite.http.api.worlds.WorldType.SKILL_TOTAL) ||
                    w.getTypes().contains(net.runelite.http.api.worlds.WorldType.BOUNTY) ||
                    w.getTypes().contains(net.runelite.http.api.worlds.WorldType.SEASONAL) ||
                    config.membersWorlds() != w.getTypes().contains(net.runelite.http.api.worlds.WorldType.MEMBERS))
                continue;
            return w.getId();
        }
        return -1;
    }

    private void hopToWorld(int worldId) {
        assert client.isClientThread();

        WorldResult worldResult = worldService.getWorlds();
        // Don't try to hop if the world doesn't exist
        World world = worldResult.findWorld(worldId);
        if (world == null) {
            return;
        }

        final net.runelite.api.World rsWorld = client.createWorld();
        rsWorld.setActivity(world.getActivity());
        rsWorld.setAddress(world.getAddress());
        rsWorld.setId(world.getId());
        rsWorld.setPlayerCount(world.getPlayers());
        rsWorld.setLocation(world.getLocation());
        rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

        if (client.getGameState() == GameState.LOGIN_SCREEN) {
            // on the login screen we can just change the world by ourselves
            client.changeWorld(rsWorld);
            return;
        }

        if (client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) == null) {
            client.openWorldHopper();

            //executorService.submit(() -> {
                try {
                    Thread.sleep(25 + ThreadLocalRandom.current().nextInt(125));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                injector.getInstance(ClientThread.class).invokeLater(() -> {
                    if (client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) != null)
                        client.hopToWorld(rsWorld);
                });
           // });
        } else {
            client.hopToWorld(rsWorld);
        }


    }

    private void logout() {
        Widget logoutButton = client.getWidget(182, 8);
        Widget logoutDoorButton = client.getWidget(69, 23);
        int param1 = -1;
        if (logoutButton != null) {
            param1 = logoutButton.getId();
            core.handleMouseClick(logoutButton.getBounds());
        } else if (logoutDoorButton != null) {
            param1 = logoutDoorButton.getId();
            core.handleMouseClick(logoutDoorButton.getBounds());
        }
        if (param1 == -1) {
            return;
        }
        //client.invokeMenuAction("Logout", "", 1, MenuAction.CC_OP.getId(), -1, param1);
    }

    public boolean inWilderness() {
        return client.getVarbitValue(Varbits.IN_WILDERNESS) == 1;
    }

    public boolean isInWhitelist(String username) {
        username = username.toLowerCase().replace(" ", "_");
        String[] names = config.whitelist().toLowerCase().replace(" ", "_").split(",");

        for (String whitelisted : names) {
            if (whitelisted.isBlank() || whitelisted.isEmpty() || whitelisted.equals("_"))
                continue;

            //remove trailing whitespace on names.
            //if (whitelisted.charAt(whitelisted.length() - 1) == ' ')
            //	whitelisted = whitelisted.substring(0, whitelisted.length() - 1);

            if (whitelisted.equals(username))
                return true;
        }
        return false;
    }

    private boolean isPlayerSkulled(Player player) {
        if (player == null) {
            return false;
        }

        /*if (config.skulledOnly() && config.deadmanSkulls())
        {
            SkullIcon[] icons =
                    {
                            SkullIcon.DEAD_MAN_ONE,
                            SkullIcon.DEAD_MAN_TWO,
                            SkullIcon.DEAD_MAN_THREE,
                            SkullIcon.DEAD_MAN_FOUR,
                            SkullIcon.DEAD_MAN_FIVE
                    };

            for (SkullIcon ic : icons)
            {
                if (player.getSkullIcon() == ic)
                    return true;
            }
        }*/

        return player.getSkullIcon() == SkullIcon.SKULL;
    }

    public void pressKey(int key) {
        keyEvent(KeyEvent.KEY_PRESSED, key);
        keyEvent(KeyEvent.KEY_RELEASED, key);
    }

    private void keyEvent(int id, int key) {
        KeyEvent e = new KeyEvent(
                client.getCanvas(), id, System.currentTimeMillis(),
                0, key, KeyEvent.CHAR_UNDEFINED
        );
        client.getCanvas().dispatchEvent(e);
    }

    public static void sleep(long time) {
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
