package net.runelite.client.plugins.ARevenants;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.api.queries.InventoryItemQuery;
import net.runelite.api.queries.NPCQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.Utils.Core;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import okhttp3.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.awt.event.KeyEvent.VK_F5;

@PluginDescriptor(
	name = "ARevenants",
	description = "Kills Revenants.",
	tags = {"spec","numb","anarchise","ztd"},
	enabledByDefault = false
)
public class ARevenants extends Plugin
{
	@Provides
	ARevenantsConfig getConfig(final ConfigManager configManager)
	{
		return configManager.getConfig(ARevenantsConfig.class);
	}
	private String currentEntry = null;

	@Inject
	private ARevenantsConfig config;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ItemManager itemManager;
	@Inject
	private Client client;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Core utils;

	@Inject
	private OverlayManager overlayManager;
	@Inject ARevenantsOverlay overlay;

	Instant botTimer;

	private void reset() {
		loot.clear();
		banked = false;
		hasBanked = false;
		hasEscaped = false;
		lootValue = 0;
		botTimer = null;
	}

	@Override
	protected void startUp()
	{
		reset();
		botTimer = Instant.now();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		reset();
		overlayManager.remove(overlay);
	}

	public boolean inWilderness() {
		return client.getVarbitValue(Varbits.IN_WILDERNESS) == 1;
	}

	private boolean isPlayerSkulled(Player player) {
		if (player == null) {
			return false;
		}
		return player.getSkullIcon() == SkullIcon.SKULL;
	}

	private boolean passedWildernessChecks() {
		return inWilderness();
	}

	public boolean isAttackable(Client client, Player player) {
		if (player.getCombatLevel() > client.getLocalPlayer().getCombatLevel() + 30)
			return false;
		else if (player.getCombatLevel() < client.getLocalPlayer().getCombatLevel() - 30)
			return false;
		else return true;
	}

	private boolean isPlayerBad(Player player) {
		if (player == client.getLocalPlayer()) {
			return false;
		}
		if (!inWilderness()) {
			return false;
		}
		if (player.getInteracting() == client.getLocalPlayer()) {
			return true;
		}
		if (config.attackingOnly() && player.getInteracting() != client.getLocalPlayer()) {
			return false;
		}
		if (config.weaponsOnly() && player.getPlayerComposition().getEquipmentId(KitType.WEAPON) == ItemID.MAGIC_SHORTBOW) {
			return false;
		}
		if (config.weaponsOnly() && player.getPlayerComposition().getEquipmentId(KitType.WEAPON) == ItemID.CRAWS_BOW) {
			return false;
		}
		if (config.weaponsOnly() && player.getPlayerComposition().getEquipmentId(KitType.WEAPON) == ItemID.WEBWEAVER_BOW) {
			return false;
		}
		if (config.weaponsOnly() && player.getPlayerComposition().getEquipmentId(KitType.WEAPON) == ItemID.VIGGORAS_CHAINMACE) {
			return false;
		}
		if (config.weaponsOnly() && player.getPlayerComposition().getEquipmentId(KitType.WEAPON) == ItemID.URSINE_CHAINMACE) {
			return false;
		}
		if (!isAttackable(client, player)) {
			return false;
		}
		/*if (config.skulledOnly() && !isPlayerSkulled(player))
			return false;*/
		return true;
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event) {
		if (isPlayerBad(event.getPlayer()))
			utils.useItem(ItemID.ROYAL_SEED_POD, MenuAction.ITEM_USE);
	}

	private boolean nearPlayer() {
		List<Player> players = client.getPlayers();
		for (Player p : players) {
			if (!isPlayerBad(p)) {
				continue;
			}
			if (isPlayerBad(p)) {
				return true;
			}
		}
		return false;
	}

	List<WorldPoint> loot = new ArrayList<>();
	private void lootItem(List<WorldPoint> itemList) {
		if (itemList.get(0) != null) {
			utils.walk(itemList.get(0));
		}
	}
	public int lootValue = 0;
	public int currentInvValue = 0;
	//insideWalk3

	@Subscribe
	private void onItemSpawned(ItemSpawned event) {
		TileItem item = event.getItem();
		ItemComposition itemDef = client.getItemDefinition(item.getId()); //ItemID.BLIGHTED_KARAMBWAN, ItemID.BLIGHTED_MANTA_RAY,  ItemID.BLIGHTED_ANGLERFISH

		if (client.getLocalPlayer().getWorldArea().distanceTo(event.getTile().getWorldLocation()) <= 10) {
			if (event.getTile().getWorldLocation().isInArea(insideCave3, new WorldArea(new WorldPoint(3223, 10135, 0), new WorldPoint(3260, 10188, 0)))) {
				if (!event.getTile().getWorldLocation().isInArea(new WorldArea(new WorldPoint(3220, 10190, 0), new WorldPoint(3255, 10226, 0))) && !event.getTile().getWorldLocation().isInArea(new WorldArea(new WorldPoint(3205, 10110, 0), new WorldPoint(3238, 10150, 0)))) {
					if (!config.pickupFood()) {
						if (item.getId() == ItemID.BLIGHTED_KARAMBWAN || item.getId() == ItemID.BLIGHTED_MANTA_RAY || item.getId() == ItemID.BLIGHTED_ANGLERFISH || event.getItem().getId() == ItemID.RUNE_ARROW || event.getItem().getId() == ItemID.AMETHYST_ARROW || event.getItem().getId() == ItemID.COINS_995 || event.getItem().getId() == ItemID.BLIGHTED_ANCIENT_ICE_SACK || event.getItem().getId() == ItemID.BLIGHTED_ENTANGLE_SACK || event.getItem().getId() == ItemID.BLIGHTED_TELEPORT_SPELL_SACK || event.getItem().getId() == ItemID.BLIGHTED_VENGEANCE_SACK) {
							return;
						}
					}
					int valueAmount = itemDef.getPrice() * event.getItem().getQuantity();
					lootValue = Integer.valueOf(lootValue + valueAmount);
					currentInvValue = lootValue;
					loot.add(event.getTile().getWorldLocation());
				}
			}
		}
	}

	@Subscribe
	private void onItemDespawned(ItemDespawned event) {
		TileItem item = event.getItem();
		if (!config.pickupFood()) {
			if (item.getId() == ItemID.BLIGHTED_KARAMBWAN || item.getId() == ItemID.BLIGHTED_MANTA_RAY || item.getId() == ItemID.BLIGHTED_ANGLERFISH || event.getItem().getId() == ItemID.RUNE_ARROW || event.getItem().getId() == ItemID.AMETHYST_ARROW || event.getItem().getId() == ItemID.COINS_995 || event.getItem().getId() == ItemID.BLIGHTED_ANCIENT_ICE_SACK || event.getItem().getId() == ItemID.BLIGHTED_ENTANGLE_SACK || event.getItem().getId() == ItemID.BLIGHTED_TELEPORT_SPELL_SACK || event.getItem().getId() == ItemID.BLIGHTED_VENGEANCE_SACK) {
				return;
			}
		}
		loot.remove(event.getTile().getWorldLocation());

	}

	private WorldArea deathSpot = new WorldArea(new WorldPoint(3195, 3199, 0), new WorldPoint(3234, 3237, 0));

	private WorldArea firstFloor = new WorldArea(new WorldPoint(3195, 3199, 1),  new WorldPoint(3234, 3237, 1));
	private WorldArea secondFloor = new WorldArea(new WorldPoint(3195, 3199, 2),  new WorldPoint(3234, 3237, 2));

	private WorldArea outsideCaves = new WorldArea(new WorldPoint(3118, 3822, 0), new WorldPoint(3137, 3844, 0));
	private WorldArea insideCave1 = new WorldArea(new WorldPoint(3229, 10209, 0), new WorldPoint(3252, 10238, 0));
	private WorldPoint insideWalk1 = new WorldPoint(3250, 10203, 0);
	private WorldArea insideCave2 = new WorldArea(new WorldPoint(3224, 10189, 0), new WorldPoint(3261, 10219, 0));
	private WorldPoint insideWalk2 = new WorldPoint(3252, 10179, 0);
	private WorldArea insideCave3 = new WorldArea(new WorldPoint(3226, 10146, 0), new WorldPoint(3265, 10207, 0));
	private WorldPoint insideWalk3 = new WorldPoint(3248, 10151, 0);

	private WorldArea grandTreeBottom = new WorldArea(new WorldPoint(2460, 3490, 0), new WorldPoint(2471, 3502, 0));
	private WorldArea grandTreeTop = new WorldArea(new WorldPoint(2447, 3486, 1), new WorldPoint(2480, 3511, 1));

	private WorldArea GEEntrance = new WorldArea(new WorldPoint(3149, 3456, 0), new WorldPoint(3175, 3484, 0));
	private WorldPoint GE = new WorldPoint(3162, 3487, 0);

	private WorldArea insideGE = new WorldArea(new WorldPoint(3158, 3482, 0), new WorldPoint(3171, 3496, 0));

	private int revIDs;
	private boolean depositedBag = false;
	NPC Rev;
	private void escape(){
		if (client.getVar(Varbits.TELEBLOCK) != 0) {
			return;
		}

		/*if (client.getVar(Varbits.QUICK_PRAYER) == 0 && inWilderness() && config.avarice())
		{
			currentEntry = "Activate";
			utils.sendGameMessage("Activate Prayers");
			utils.clickWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
			return;
		}*/
		if (config.avoidRevs() && Rev != null && Rev.getInteracting() == client.getLocalPlayer()) {
			if (Rev.getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3243, 10137, 0), new WorldPoint(3256, 10148, 0)))) {
				if (client.getLocalPlayer().getWorldLocation().distanceTo(new WorldPoint(3250, 10149, 0)) >= 1) {
					currentEntry = "Walk here";
					utils.walkToMinimapTile(new WorldPoint(3250, 10149, 0));
					return;
				}
			}
			else {
				if (client.getLocalPlayer().getWorldLocation().distanceTo(new WorldPoint(3250, 10148, 0)) >= 1) {
					currentEntry = "Walk here";
					utils.walkToMinimapTile(new WorldPoint(3250, 10148, 0));
					return;
				}
			}
		}
		if (config.seedPod()) {
			currentEntry = "Commune";
			utils.useItem(ItemID.ROYAL_SEED_POD, MenuAction.ITEM_USE);
			//currentInvValue = 0;
			banked = false;
			hasBanked = false;
			depositedBag = false;
			return;
		}
		else {
			if (client.getVarcIntValue(VarClientInt.INVENTORY_TAB) == 4) {
				currentEntry = "Grand Exchange";
				Widget RING = client.getWidget(387, 24);
				utils.clickWidget(RING);
				//currentInvValue = 0;
				banked = false;
				hasBanked = false;
				depositedBag = false;
				return;
			}
			else {
				currentEntry = "Worn Equipment";
				utils.pressKey(VK_F5);
				//currentInvValue = 0;
				banked = false;
				hasBanked = false;
				depositedBag = false;
				return;
			}
		}
	}
	int timeout = 0;
	private boolean hasEscaped = false;
	int worldsHopped = 0;
	public boolean startTeaks = false;

	@Inject
	private OkHttpClient okHttpClient;

	@Nullable
	public NPC findNearestAttackableNpc(int IDs)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}


		return new NPCQuery()
				.filter(npc -> npc.getInteracting() == null && npc.getId() == IDs)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
			}
		});
	}
	public boolean isRunEnabled()
	{
		return client.getVarpValue(173) == 1;
	}

	@Subscribe
	public void onGameTick(GameTick event) throws IOException, ClassNotFoundException {
		if (client.getGameState() != GameState.LOGGED_IN) {
			return;
		}

		if (client.getMinimapZoom() != 2.0) {
			client.setMinimapZoom(2.0);
		}
		if (timeout > 0) {
			timeout--;
			return;
		}
		if (nearPlayer() && inWilderness()) {
			if (client.getVar(Varbits.QUICK_PRAYER) == 0 && inWilderness() && utils.inventoryContains(ItemID.SUPER_RESTORE1, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE4))
			{
				currentEntry = "Activate";
				utils.sendGameMessage("Activate Prayers");
				utils.clickWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
				return;
			}

		}
		if (!nearPlayer() && inWilderness()) {
			if (client.getVar(Varbits.QUICK_PRAYER) != 0 && inWilderness())
			{
				currentEntry = "Deactivate";
				utils.sendGameMessage("Deactivate Prayers");
				utils.clickWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
				return;
			}
		}
		if (hasEscaped && !inWilderness() && config.hopWorlds()) {
			utils.sendGameMessage("Escaped PKer, hopping worlds");
			if (client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) == null) {
				client.openWorldHopper();
				return;
			} else {
				if (worldsHopped >= 5) {
					timeout = 100;
					worldsHopped = 0;
					return;
				}
				if (client.getWorld() != config.world1() &&
						client.getWorld() != config.world2() &&
						client.getWorld() != config.world3() &&
						client.getWorld() != config.world4() &&
						client.getWorld() != config.world5() &&
						client.getWorld() != config.world6() &&
						client.getWorld() != config.world7() &&
						client.getWorld() != config.world8() &&
						client.getWorld() != config.world9() &&
						client.getWorld() != config.world10()) {
					hop(config.world1());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world1()) {
					hop(config.world2());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world2()) {
					hop(config.world3());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world3()) {
					hop(config.world4());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world4()) {
					hop(config.world5());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world5()) {
					hop(config.world6());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world6()) {
					hop(config.world7());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world7()) {
					hop(config.world8());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world8()) {
					hop(config.world9());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world9()) {
					hop(config.world10());
					worldsHopped++;
					hasEscaped = false;
				}
				if (client.getWorld() == config.world10()) {
					hop(config.world1());
					worldsHopped++;
					hasEscaped = false;
				}
				timeout = config.worldDelay();
			}
		}
		/****
		 * DIED, GO TO BANK
		 */
		if (client.getLocalPlayer().getWorldArea().intersectsWith(deathSpot)) {
			GameObject bottomStairs = utils.findNearestGameObject(16671);
			if (bottomStairs.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldArea()) >= 3) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(new WorldPoint(3206, 3228, 0));
				return;
			}
			else {
				currentEntry = "Climb-up";
				utils.useGameObjectDirect(bottomStairs);
				return;
			}
		}
		if (client.getVar(Varbits.QUICK_PRAYER) != 0 && !inWilderness() && config.avarice())
		{
			currentEntry = "Deactivate";
			utils.sendGameMessage("Deactivate Prayers 2");
			utils.clickWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
			return;
		}
		if (client.getLocalPlayer().getWorldArea().intersectsWith(firstFloor)) {
			currentInvValue = 0;
			GameObject firstFloorStairs = utils.findNearestGameObject(16672);
			if (firstFloorStairs.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldArea()) >= 3) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(firstFloorStairs.getWorldLocation());
				return;
			}
			else {
				currentEntry = "Climb-up";
				utils.useGameObjectDirect(firstFloorStairs);
				return;
			}
		}
		if (client.getLocalPlayer().getWorldArea().intersectsWith(GEEntrance)) {
			currentEntry = "Walk here";
			utils.walkToMinimapTile(GE);
			return;
		}
		if (client.getLocalPlayer().getWorldArea().intersectsWith(grandTreeBottom)) {
			currentEntry = "Climb-up";
			GameObject LADDER = utils.findNearestGameObject(16683);
			utils.useGameObjectDirect(LADDER);
			return;
		}
		if (client.getLocalPlayer().getWorldArea().intersectsWith(grandTreeTop)) {
			currentEntry = "Bank";
			GameObject BANK = utils.findNearestBankNoDepositBoxes();
			utils.walkToMinimapTile(BANK.getWorldLocation());
			currentInvValue = 0;
			return;
		}
		if (config.type() == Type.PYREFIENDS) {
			revIDs = 7932; //7932, 7935
		}
		if (config.type() == Type.HELLHOUNDS) {
			revIDs = 7935;
		}
		/*if (config.type() == Type.DRAGONS) {
			revIDs = 7940;
		}*/
		if (config.type() == Type.HELLHOUNDS_AND_PYREFIENDS) {
			Rev = findNearestAttackableNpc(7932);
			if (findNearestAttackableNpc(7932) == null) {
				Rev = findNearestAttackableNpc(7935);
			}
		}
		else {
			Rev = findNearestAttackableNpc(revIDs);
		}
		if (utils.findNearestNpcTargetingLocal() != null && inWilderness()) {
			Rev = utils.findNearestNpcTargetingLocal();
		}
		if (config.staminaPots() && utils.inventoryContains(ItemID.STAMINA_POTION1, ItemID.STAMINA_POTION2, ItemID.STAMINA_POTION3, ItemID.STAMINA_POTION4) && inWilderness() && client.getEnergy() > config.energyThreshold() && client.getVarbitValue(Varbits.STAMINA_EFFECT) <= 0) {
			currentEntry = "Drink";
			WidgetItem STAM = utils.getInventoryWidgetItem(ItemID.STAMINA_POTION1, ItemID.STAMINA_POTION2, ItemID.STAMINA_POTION3, ItemID.STAMINA_POTION4);
			utils.useItem(STAM.getId(), "Drink");
			return;
		}
		if (inWilderness() && client.getEnergy() > config.energyThreshold() && !isRunEnabled()) {
			currentEntry = "Toggle Run";
			Widget RunOrb = client.getWidget(WidgetInfo.MINIMAP_RUN_ORB);
			utils.clickWidget(RunOrb);
			return;
		}
		if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave2)){// && config.type() != Type.DRAGONS) {
			currentEntry = "Walk here";
			utils.walkToMinimapTile(insideWalk2);
			return;
		}
		if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave1)) {
			currentEntry = "Walk here";
			utils.walkToMinimapTile(insideWalk1);
			return;
		}
		if (inWilderness() && nearPlayer()) {
			utils.sendGameMessage("Escaping PKer");
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3234, 10138, 0), new WorldPoint(3259, 10160, 0))) && inWilderness()) {
				escape();
				hasEscaped = true;
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3227, 10160, 0), new WorldPoint(3260, 10188, 0)))) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk3);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave2)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk2);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave1)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk1);
				return;
			}
		}
		if (currentInvValue >= config.lootThreshold() && loot.isEmpty()) {
			utils.sendGameMessage("Loot Threshold Reached");
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3234, 10138, 0), new WorldPoint(3259, 10160, 0))) && inWilderness()) {
				escape();
				hasEscaped = true;
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3227, 10160, 0), new WorldPoint(3260, 10188, 0)))) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk3);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave2)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk2);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave1)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk1);
				return;
			}
			//return;
		}
		if (inWilderness() && !isItemEquipped(ItemID.RUNE_ARROW, ItemID.AMETHYST_ARROW)) {
			utils.sendGameMessage("Ran out of arrows");
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3234, 10138, 0), new WorldPoint(3259, 10160, 0))) && inWilderness()) {
				escape();
				hasEscaped = true;
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3227, 10160, 0), new WorldPoint(3260, 10188, 0)))) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk3);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave2)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk2);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave1)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk1);
				return;
			}
		}
		if (inWilderness() && utils.inventoryContains(ItemID.AMULET_OF_AVARICE, ItemID.CRAWS_BOW_U, ItemID.VIGGORAS_CHAINMACE_U, ItemID.THAMMARONS_SCEPTRE_U, ItemID.ANCIENT_EMBLEM, ItemID.ANCIENT_TOTEM, ItemID.ANCIENT_CRYSTAL, ItemID.ANCIENT_STATUETTE, ItemID.ANCIENT_MEDALLION, ItemID.ANCIENT_EFFIGY, ItemID.ANCIENT_RELIC)) {
			utils.sendGameMessage("Expensive drop, escaping");
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3234, 10138, 0), new WorldPoint(3259, 10160, 0))) && inWilderness()) {
				escape();
				hasEscaped = true;
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3227, 10160, 0), new WorldPoint(3260, 10188, 0)))) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk3);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave2)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk2);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave1)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk1);
				return;
			}
		}
		/*if (inWilderness() && !utils.inventoryContains(ItemID.STAMINA_POTION1, ItemID.STAMINA_POTION2, ItemID.STAMINA_POTION3, ItemID.STAMINA_POTION4)) {
			utils.sendGameMessage("Ran out of Staminas");
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3234, 10138, 0), new WorldPoint(3259, 10160, 0))) && inWilderness()) {
				escape();
				hasEscaped = true;
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3227, 10160, 0), new WorldPoint(3260, 10188, 0)))) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk3);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave2)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk2);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave1)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk1);
				return;
			}
		}*/
		if (inWilderness() && getRestoreItem() == null) {
			utils.sendGameMessage("Ran out of restores");
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3234, 10138, 0), new WorldPoint(3259, 10160, 0))) && inWilderness()) {
				escape();
				hasEscaped = true;
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3227, 10160, 0), new WorldPoint(3260, 10188, 0)))) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk3);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave2)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk2);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave1)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk1);
				return;
			}
		}
		if (inWilderness() && !utils.inventoryContains(config.foodID())) {
			utils.sendGameMessage("Ran out of food");
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3234, 10138, 0), new WorldPoint(3259, 10160, 0))) && inWilderness()) {
				escape();
				hasEscaped = true;
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3227, 10160, 0), new WorldPoint(3260, 10188, 0)))) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk3);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave2)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk2);
				return;
			}
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave1)) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(insideWalk1);
				return;
			}
		}
		if (inWilderness() && client.getBoostedSkillLevel(Skill.RANGED) <= client.getRealSkillLevel(Skill.RANGED) + 4) {
			if (utils.inventoryContains(ItemID.RANGING_POTION1, ItemID.RANGING_POTION2, ItemID.RANGING_POTION3, ItemID.RANGING_POTION4)) {
				utils.sendGameMessage("Drinking pot");
				currentEntry = "Drink";
				WidgetItem food = utils.getItemFromInventory(ItemID.RANGING_POTION1, ItemID.RANGING_POTION2, ItemID.RANGING_POTION3, ItemID.RANGING_POTION4);
				utils.useItem(food.getId(), "Drink");
				return;
			}
		}
		WidgetItem Ppot = getRestoreItem();
		if (inWilderness() && Ppot != null && client.getBoostedSkillLevel(Skill.PRAYER) <= config.prayerThreshold()) {
			currentEntry = "Drink";
			utils.useItem(Ppot.getId(), "drink");
			return;
		}
		if (inWilderness() && client.getBoostedSkillLevel(Skill.HITPOINTS) <= 40 && utils.inventoryContains(config.foodID())) {
			banked = false;
			hasBanked = false;
			utils.sendGameMessage("Eating food");
			currentEntry = "Eat";
			WidgetItem food = utils.getItemFromInventory(config.foodID(),ItemID.BLIGHTED_KARAMBWAN, ItemID.BLIGHTED_MANTA_RAY,  ItemID.BLIGHTED_ANGLERFISH);
			utils.useItem(food.getId(), "Eat");
			return;
		}
		if (inWilderness() && !loot.isEmpty() && utils.inventoryContains(ItemID.LOOTING_BAG_22586)) {
			if (loot.get(0).distanceTo(client.getLocalPlayer().getWorldLocation()) >= 8) {
				utils.sendGameMessage("Walk To Loot");
				currentEntry = "Walk here";
				utils.walkToMinimapTile(loot.get(0));
				return;
			}
			utils.sendGameMessage("Looting (bag)");
			currentEntry = "Take";
			lootItem(loot);
			return;
		}
		if (inWilderness() && !loot.isEmpty() && utils.getInventorySpace() >= 1) {
			if (loot.get(0).distanceTo(client.getLocalPlayer().getWorldLocation()) >= 8) {
				utils.sendGameMessage("Walk To Loot");
				currentEntry = "Walk here";
				utils.walkToMinimapTile(loot.get(0));
				return;
			}
			utils.sendGameMessage("Looting");
			utils.sendGameMessage(utils.getInventorySpace() +" bag space");
			currentEntry = "Take";
			lootItem(loot);
			return;
		}
		if (currentInvValue < config.lootThreshold() && !nearPlayer() && loot.isEmpty() && Rev != null && client.getLocalPlayer().getInteracting() == null) {
			banked = false;
			hasBanked = false;
			utils.sendGameMessage("Attacking Rev");
			if (Rev.getWorldArea().distanceTo(client.getLocalPlayer().getWorldArea()) >= 10) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(Rev.getWorldLocation());
				return;
			}
			currentEntry = "Attack";
			utils.attackNPCDirect(Rev);
			utils.attackNPCDirect(Rev);
			//timeout = 2;
			return;
		}
		if (config.type() == Type.HELLHOUNDS && loot.isEmpty() && Rev == null && client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3230, 10159, 0), new WorldPoint(3260, 10188, 0)))) {
			currentEntry = "Walk here";
			utils.walkToMinimapTile(new WorldPoint(3245, 10168, 0));
			return;
		}
		if (config.type() == Type.HELLHOUNDS_AND_PYREFIENDS && loot.isEmpty() && Rev == null && client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3230, 10159, 0), new WorldPoint(3260, 10188, 0)))) {
			currentEntry = "Walk here";
			utils.walkToMinimapTile(insideWalk3);
			return;
		}
		if (config.type() == Type.PYREFIENDS && loot.isEmpty() && Rev == null && client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3230, 10159, 0), new WorldPoint(3260, 10188, 0)))) {
			currentEntry = "Walk here";
			utils.walkToMinimapTile(insideWalk3);
			return;
		}
		if (inWilderness() && !loot.isEmpty() && utils.inventoryFull() && !utils.inventoryContains(ItemID.LOOTING_BAG_22586) && client.getLocalPlayer().getInteracting() == null) {
			utils.sendGameMessage("Eating for Space");
			utils.sendGameMessage(utils.getInventorySpace() +" bag space");
			currentEntry = "Eat";
			WidgetItem food = utils.getItemFromInventory(config.foodID(),ItemID.BLIGHTED_KARAMBWAN, ItemID.BLIGHTED_MANTA_RAY,  ItemID.BLIGHTED_ANGLERFISH);
			utils.useItem(food.getId(), "Eat");
			return;
		}

		/*if (client.getLocalPlayer().getWorldArea().intersectsWith(insideCave3) && utils.findNearestNpcWithin(client.getLocalPlayer().getWorldLocation(), 15, Rev.getId()) == null) {
			currentEntry = "Walk here";
			utils.walkToMinimapTile(insideWalk3);
			return;
		}*/

		if (client.getWidget(219, 1) != null) {
			if (client.getWidget(219, 1).getChild(1) != null) {
				currentEntry = "Continue";
				Widget skull = client.getWidget(219, 1).getChild(1);
				utils.clickWidget(skull);
				return;
			}
		}
		if (utils.inventoryContains(ItemID.LOOTING_BAG)) {
			utils.sendGameMessage("Opening Looting Bag");
			currentEntry = "Open";
			utils.useItem(ItemID.LOOTING_BAG, "open");
			return;
		}
		if (utils.isBankOpen()) {
			if (banked && utils.inventoryContains(config.foodID())) {
				banked = false;
				hasBanked = true;
				Widget close = client.getWidget(12, 2).getChild(11);
				utils.clickWidget(close);
				return;
			}
			if (client.getWidget(15, 8) != null) {
				if (!client.getWidget(15, 8).isHidden()) {
					if (depositedBag) {
						currentEntry = "Dismiss";
						Widget CLOSEBAG = client.getWidget(15, 10);
						utils.clickWidget(CLOSEBAG);
						timeout = 3;
						return;
					}
					if (!depositedBag) {
						currentEntry = "Deposit loot";
						utils.sendGameMessage("Deposit loot (widget)");
						Widget BANKBAG = client.getWidget(15, 8);
						utils.clickWidget(BANKBAG);
						depositedBag = true;
						timeout = 3;
						return;
					}
				}
			}
			if (!banked) {
				if (!depositedBag && utils.inventoryContains(ItemID.LOOTING_BAG_22586)) {
					currentEntry = "View";
					utils.sendGameMessage("Deposit loot");
					utils.useItem(ItemID.LOOTING_BAG_22586, "View");
					return;
				}
				utils.depositAll();
				utils.depositAll();
				banked = true;
				timeout = 3;
				return;
			}

			if (!utils.inventoryContains(ItemID.BRACELET_OF_ETHEREUM) && !isItemEquipped(ItemID.BRACELET_OF_ETHEREUM)) {
				utils.sendGameMessage("Withdraw Bracelet");
				withdraw1Item(ItemID.BRACELET_OF_ETHEREUM);
				timeout = 2;
				return;
			}
			/*if (!utils.inventoryContains(ItemID.BRACELET_OF_ETHEREUM) && !isItemEquipped(ItemID.BRACELET_OF_ETHEREUM) && !utils.bankContains(ItemID.BRACELET_OF_ETHEREUM)) {
				utils.sendGameMessage("Withdraw Bracelet (uncharged)");
				withdraw1Item(ItemID.BRACELET_OF_ETHEREUM_UNCHARGED);
				return;
			}*/
			if (config.dhideBodies() && config.blackDhide() && !utils.inventoryContains(ItemID.BLACK_DHIDE_BODY) && !isItemEquipped(ItemID.BLACK_DHIDE_BODY)) {
				utils.sendGameMessage("Withdraw Body");
				withdraw1Item(ItemID.BLACK_DHIDE_BODY);
				timeout = 2;
				return;
			}
			if (config.blackDhide() && !utils.inventoryContains(ItemID.BLACK_DHIDE_CHAPS) && !isItemEquipped(ItemID.BLACK_DHIDE_CHAPS)) {
				utils.sendGameMessage("Withdraw Chaps");
				withdraw1Item(ItemID.BLACK_DHIDE_CHAPS);
				timeout = 2;
				return;
			}
			if (config.dhideBodies() && !config.blackDhide() && !utils.inventoryContains(ItemID.RED_DHIDE_BODY) && !isItemEquipped(ItemID.RED_DHIDE_BODY)) {
				utils.sendGameMessage("Withdraw Body");
				withdraw1Item(ItemID.RED_DHIDE_BODY);
				timeout = 2;
				return;
			}
			if (!config.dhideBodies() && !utils.inventoryContains(ItemID.LEATHER_BODY) && !isItemEquipped(ItemID.LEATHER_BODY)) {
				utils.sendGameMessage("Withdraw Body");
				withdraw1Item(ItemID.LEATHER_BODY);
				timeout = 2;
				return;
			}
			if (!config.blackDhide() && !utils.inventoryContains(ItemID.RED_DHIDE_CHAPS) && !isItemEquipped(ItemID.RED_DHIDE_CHAPS)) {
				utils.sendGameMessage("Withdraw Chaps");
				withdraw1Item(ItemID.RED_DHIDE_CHAPS);
				timeout = 2;
				return;
			}
			if (!utils.inventoryContains(ItemID.SNAKESKIN_BOOTS) && !isItemEquipped(ItemID.SNAKESKIN_BOOTS)) {
				utils.sendGameMessage("Withdraw Boots");
				withdraw1Item(ItemID.SNAKESKIN_BOOTS);
				timeout = 2;
				return;
			}
			if (!utils.inventoryContains(ItemID.MAGIC_SHORTBOW) && !isItemEquipped(ItemID.MAGIC_SHORTBOW)) {
				utils.sendGameMessage("Withdraw Bow");
				withdraw1Item(ItemID.MAGIC_SHORTBOW);
				timeout = 2;
				return;
			}
			if (!utils.inventoryContains(ItemID.AVAS_ACCUMULATOR) && !isItemEquipped(ItemID.AVAS_ACCUMULATOR)) {
				utils.sendGameMessage("Withdraw Avas");
				withdraw1Item(ItemID.AVAS_ACCUMULATOR);
				timeout = 2;
				return;
			}
			/*if (config.seedPod() && !utils.inventoryContains(ItemID.RING_OF_DUELING8) && !isItemEquipped(ItemID.RING_OF_DUELING8)) {
				withdraw1Item(ItemID.RING_OF_DUELING8);
				return;
			}*/
			if (!config.amethyst() && !utils.inventoryContains(ItemID.RUNE_ARROW) && !isItemEquipped(ItemID.RUNE_ARROW)) {
				utils.sendGameMessage("Withdraw Arrows");
				withdrawXItem(ItemID.RUNE_ARROW);
				timeout = 2;
				return;
			}
			if (config.amethyst() && !utils.inventoryContains(ItemID.AMETHYST_ARROW) && !isItemEquipped(ItemID.AMETHYST_ARROW)) {
				utils.sendGameMessage("Withdraw Arrows");
				withdrawXItem(ItemID.AMETHYST_ARROW);
				timeout = 2;
				return;
			}
			if (config.avarice() && !utils.inventoryContains(ItemID.AMULET_OF_AVARICE) && !isItemEquipped(ItemID.AMULET_OF_AVARICE)) {
				utils.sendGameMessage("Withdraw Avarice");
				withdraw1Item(ItemID.AMULET_OF_AVARICE);
				timeout = 2;
				return;
			}
			if (!config.seedPod() && !utils.inventoryContains(ItemID.RING_OF_WEALTH_5) && !isItemEquipped(ItemID.RING_OF_WEALTH_5, ItemID.RING_OF_WEALTH_4, ItemID.RING_OF_WEALTH_3, ItemID.RING_OF_WEALTH_2, ItemID.RING_OF_WEALTH_1)) {
				utils.sendGameMessage("Withdraw Wealth");
				withdraw1Item(ItemID.RING_OF_WEALTH_5);
				timeout = 2;
				return;
			}
			if (!config.avarice() && !utils.inventoryContains(ItemID.AMULET_OF_GLORY6) && !isItemEquipped(ItemID.AMULET_OF_GLORY6)) {
				utils.sendGameMessage("Withdraw Glory");
				withdraw1Item(ItemID.AMULET_OF_GLORY6);
				timeout = 2;
				return;
			}
			/**
			 *	WITHDRAWING
			 */


			/**
			 *	EQUIPPING
			 */
			if (utils.inventoryContains(ItemID.BRACELET_OF_ETHEREUM) && !isItemEquipped(ItemID.BRACELET_OF_ETHEREUM) && utils.bankContainsAnyOf(ItemID.LOOTING_BAG_22586)) {
				utils.sendGameMessage("Equip Bracelet");
				utils.depositOneOfItem(ItemID.BRACELET_OF_ETHEREUM);
				return;
			}
			/*if (utils.inventoryContains(ItemID.BRACELET_OF_ETHEREUM_UNCHARGED) && !utils.inventoryContains(ItemID.BRACELET_OF_ETHEREUM) && !isItemEquipped(ItemID.BRACELET_OF_ETHEREUM) && !utils.bankContains(ItemID.BRACELET_OF_ETHEREUM)) {
				utils.sendGameMessage("Equip Bracelet (uncharged)");
				utils.depositOneOfItem(ItemID.BRACELET_OF_ETHEREUM);
				return;
			}*/
			if (config.dhideBodies() && config.blackDhide() && utils.inventoryContains(ItemID.BLACK_DHIDE_BODY) && !isItemEquipped(ItemID.BLACK_DHIDE_BODY)) {
				utils.sendGameMessage("Equip Body");
				utils.depositOneOfItem(ItemID.BLACK_DHIDE_BODY);
				return;
			}
			if (!config.dhideBodies() && utils.inventoryContains(ItemID.LEATHER_BODY) && !isItemEquipped(ItemID.LEATHER_BODY)) {
				utils.sendGameMessage("Equip Body");
				utils.depositOneOfItem(ItemID.LEATHER_BODY);
				return;
			}
			if (config.blackDhide() && utils.inventoryContains(ItemID.BLACK_DHIDE_CHAPS) && !isItemEquipped(ItemID.BLACK_DHIDE_CHAPS)) {
				utils.sendGameMessage("Equip Chaps");
				utils.depositOneOfItem(ItemID.BLACK_DHIDE_CHAPS);
				return;
			}
			if (config.dhideBodies() && !config.blackDhide() && utils.inventoryContains(ItemID.RED_DHIDE_BODY) && !isItemEquipped(ItemID.RED_DHIDE_BODY)) {
				utils.sendGameMessage("Equip Body");
				utils.depositOneOfItem(ItemID.RED_DHIDE_BODY);
				return;
			}
			if (!config.blackDhide() && utils.inventoryContains(ItemID.RED_DHIDE_CHAPS) && !isItemEquipped(ItemID.RED_DHIDE_CHAPS)) {
				utils.sendGameMessage("Equip Chaps");
				utils.depositOneOfItem(ItemID.RED_DHIDE_CHAPS);
				return;
			}
			if (utils.inventoryContains(ItemID.SNAKESKIN_BOOTS) && !isItemEquipped(ItemID.SNAKESKIN_BOOTS)) {
				utils.sendGameMessage("Equip Boots");
				utils.depositOneOfItem(ItemID.SNAKESKIN_BOOTS);
				return;
			}
			if (utils.inventoryContains(ItemID.MAGIC_SHORTBOW) && !isItemEquipped(ItemID.MAGIC_SHORTBOW)) {
				utils.sendGameMessage("Equip Bow");
				utils.depositOneOfItem(ItemID.MAGIC_SHORTBOW);
				return;
			}
			if (utils.inventoryContains(ItemID.AVAS_ACCUMULATOR) && !isItemEquipped(ItemID.AVAS_ACCUMULATOR)) {
				utils.sendGameMessage("Equip Avas");
				utils.depositOneOfItem(ItemID.AVAS_ACCUMULATOR);
				return;
			}

			/*if (config.seedPod() && utils.inventoryContains(ItemID.RING_OF_DUELING8) && !isItemEquipped(ItemID.RING_OF_DUELING8)) {
				utils.depositOneOfItem(ItemID.RING_OF_DUELING8);
				return;
			}*/

			if (config.avarice() && utils.inventoryContains(ItemID.AMULET_OF_AVARICE) && !isItemEquipped(ItemID.AMULET_OF_AVARICE)) {
				utils.sendGameMessage("Equip Avas");
				utils.depositOneOfItem(ItemID.AMULET_OF_AVARICE);
				banked = false;
				return;
			}
			if (!config.avarice() && utils.inventoryContains(ItemID.AMULET_OF_GLORY6) && !isItemEquipped(ItemID.AMULET_OF_GLORY6)) {
				utils.sendGameMessage("Equip Glory");
				utils.depositOneOfItem(ItemID.AMULET_OF_GLORY6);
				banked = false;
				return;
			}
			/**
			 *	EQUIPPING
			 */

			/**
			 *	WITHDRAW CONSUMABLES
			 */
			if (!utils.inventoryContains(ItemID.RANGING_POTION4)) {
				utils.sendGameMessage("Withdraw Range");
				withdraw1Item(ItemID.RANGING_POTION4);
				return;
			}
			if (config.staminaPots() && !utils.inventoryContains(ItemID.STAMINA_POTION4)) {
				utils.sendGameMessage("Withdraw Stamina");
				withdraw1Item(ItemID.STAMINA_POTION4);
				return;
			}
			if (!config.sanfewSerums() && !utils.inventoryContains(ItemID.SUPER_RESTORE4)) {
				utils.sendGameMessage("Withdraw S. Restore");
				withdraw1Item(ItemID.SUPER_RESTORE4);
				return;
			}
			if (config.sanfewSerums() && !utils.inventoryContains(ItemID.SANFEW_SERUM4)) {
				utils.sendGameMessage("Withdraw S. Serum");
				withdraw1Item(ItemID.SANFEW_SERUM4);
				return;
			}
			if (!utils.inventoryContains(ItemID.BRACELET_OF_ETHEREUM) && !isItemEquipped(ItemID.BRACELET_OF_ETHEREUM) && !utils.bankContainsAnyOf(ItemID.BRACELET_OF_ETHEREUM)) {
				utils.sendGameMessage("Withdraw Bracelet (uncharged)");
				withdraw1Item(ItemID.BRACELET_OF_ETHEREUM_UNCHARGED);
				return;
			}
			if (!utils.inventoryItemContainsAmount(ItemID.REVENANT_ETHER, 25, true, false) && utils.inventoryContains(ItemID.BRACELET_OF_ETHEREUM_UNCHARGED) && !isItemEquipped(ItemID.BRACELET_OF_ETHEREUM, ItemID.BRACELET_OF_ETHEREUM_UNCHARGED)) {
				utils.sendGameMessage("Withdraw Ether");
				withdraw5Item(ItemID.REVENANT_ETHER);
				return;
			}
			if (!utils.inventoryContains(ItemID.LOOTING_BAG_22586) && utils.bankContainsAnyOf(ItemID.LOOTING_BAG_22586)) {
				withdraw1Item(ItemID.LOOTING_BAG_22586);
				return;
			}
			if (config.seedPod() && !utils.inventoryContains(ItemID.ROYAL_SEED_POD) && !isItemEquipped(ItemID.ROYAL_SEED_POD)) {
				utils.sendGameMessage("Withdraw Pod");
				withdraw1Item(ItemID.ROYAL_SEED_POD);
				return;
			}
			if (!utils.inventoryContains(ItemID.REVENANT_CAVE_TELEPORT)) {
				utils.sendGameMessage("Withdraw Rev Teles");
				withdraw5Item(ItemID.REVENANT_CAVE_TELEPORT);
				return;
			}
			if (!utils.inventoryContains(config.foodID())) {
				utils.sendGameMessage("Withdraw Food");
				withdrawAllItem(config.foodID());
				return;
			}
		}

		if (client.getLocalPlayer().getWorldArea().intersectsWith(outsideCaves)) {
			currentEntry = "Enter";
			GameObject CAVE = utils.findNearestGameObject(31556);
			utils.useGameObjectDirect(CAVE);
			return;
		}




		if (utils.findNearestBankNoDepositBoxes() != null && hasBanked) {
			currentInvValue = 0;
			if (utils.inventoryContains(ItemID.BRACELET_OF_ETHEREUM)) {
				currentEntry = "Wear";
				utils.useItem(ItemID.BRACELET_OF_ETHEREUM, "Wear");
				return;
			}
			if (!config.seedPod() && utils.inventoryContains(ItemID.RING_OF_WEALTH_5) && !isItemEquipped(ItemID.RING_OF_WEALTH_5, ItemID.RING_OF_WEALTH_4, ItemID.RING_OF_WEALTH_3, ItemID.RING_OF_WEALTH_2, ItemID.RING_OF_WEALTH_1)) {
				currentEntry = "Wear";
				utils.sendGameMessage("Equip Wealth");
				utils.useItem(ItemID.RING_OF_WEALTH_5, "Wear");
				return;
			}
			if (!config.amethyst() && utils.inventoryContains(ItemID.RUNE_ARROW) && !isItemEquipped(ItemID.RUNE_ARROW)) {
				currentEntry = "Wield";
				utils.sendGameMessage("Equip Arrows");
				utils.useItem(ItemID.RUNE_ARROW, "Wield");
				return;
			}
			if (config.amethyst() && utils.inventoryContains(ItemID.AMETHYST_ARROW) && !isItemEquipped(ItemID.AMETHYST_ARROW)) {
				currentEntry = "Wield";
				utils.sendGameMessage("Equip Arrows");
				utils.useItem(ItemID.AMETHYST_ARROW, "Wield");
				return;
			}
			if (utils.inventoryContains(ItemID.BRACELET_OF_ETHEREUM_UNCHARGED) && utils.inventoryContains(ItemID.REVENANT_ETHER)) {
				if (first) {
					currentEntry = "Use";
					utils.useItem(ItemID.REVENANT_ETHER, "Use");
					first = false;
					timeout = 2;
					return;
				}
				if (!first) {
					utils.useItem(ItemID.BRACELET_OF_ETHEREUM_UNCHARGED, "Use");
					first = true;
					timeout = 2;
					return;
				}
			}
			currentEntry = "Teleport";
			utils.useItem(ItemID.REVENANT_CAVE_TELEPORT, "teleport");
			banked = false;
			return;
		}
		if (utils.findNearestBankNoDepositBoxes() != null && !hasBanked) {
			currentInvValue = 0;
			if (client.getLocalPlayer().getWorldArea().intersectsWith(insideGE)) {
				currentEntry = "Bank";
				NPC banker = utils.findNearestNpc(1633);
				utils.attackNPCDirect(banker);
				return;
			}
			GameObject bankBooth = utils.findNearestBankNoDepositBoxes();
			if (bankBooth.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldArea()) >= 8) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(bankBooth.getWorldLocation());
				return;
			}
			else {
				currentEntry = "Bank";
				utils.useGameObjectDirect(bankBooth);
				return;
			}
		}
		/****
		 * DIED, GO TO BANK
		 */
	}

	private boolean banked = false;
	private boolean hasBanked = false;

	public boolean isItemEquipped(int... itemName) {
		if (client.getItemContainer(InventoryID.INVENTORY) == null) {
			return false;
		} else {
			return (new InventoryItemQuery(InventoryID.EQUIPMENT)).idEquals(itemName).result(this.client).size() > 0;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {

	}

	public WidgetItem getRestoreItem() {

		WidgetItem item;

		item = utils.getInventoryWidgetItem(ItemID.PRAYER_POTION1, ItemID.PRAYER_POTION2, ItemID.PRAYER_POTION3, ItemID.PRAYER_POTION4, ItemID.SUPER_RESTORE1, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE4,
				ItemID.BLIGHTED_SUPER_RESTORE1, ItemID.BLIGHTED_SUPER_RESTORE2, ItemID.BLIGHTED_SUPER_RESTORE3,
				ItemID.BLIGHTED_SUPER_RESTORE4, ItemID.SANFEW_SERUM1, ItemID.SANFEW_SERUM2, ItemID.SANFEW_SERUM3, ItemID.SANFEW_SERUM4);

		if (item != null) {
			return item;
		}

		return item;
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN) {
			return;
		}
		var entries = client.getMenuEntries();
		int putAtTopId = -1;

		if (!client.getLocalPlayer().getWorldArea().intersectsWith(insideCave3) && !client.getLocalPlayer().getWorldArea().intersectsWith(new WorldArea(new WorldPoint(3223, 10135, 0), new WorldPoint(3260, 10188, 0)))) {
			loot.clear();
		}

		/*for (int i = 0; i < loot.size(); i++)
		{
			if (loot.get(i).distanceTo(client.getLocalPlayer().getWorldArea()) >= 15) {
				loot.remove(i);
			}
		}*/
		for (int i = 0; i < entries.length; i++)
		{
			var entry = entries[i];
			if (entry.getOption().contains("Climb-up") ||
					entry.getOption().contains("climb-up") ||
					entry.getOption().contains("Take") ||
					entry.getOption().contains("take") ||
					entry.getOption().contains("Grand Exchange") && !entry.getOption().contains("Bank") ||
					entry.getOption().contains("Bank") ||
					entry.getOption().contains("bank") ||
					entry.getOption().contains("grand exchange") && !entry.getOption().contains("bank") ||
					entry.getOption().contains("Wear") ||
					entry.getOption().contains("wear") ||
					entry.getOption().contains("Wield") ||
					entry.getOption().contains("wield")) {
				putAtTopId = i;
				break;
			}
		}

		if (putAtTopId != -1)
		{
			var temp = entries[entries.length - 1];

			entries[entries.length - 1] = entries[putAtTopId];
			entries[putAtTopId] = temp;
			client.setMenuEntries(entries);
		}

	}

	public void withdrawXItem(int bankItemID) {
		Widget item = utils.getBankItemWidget(bankItemID);
		if (item != null) {
			if (client.getVarbitValue(6590) != 3) {
				//client.setVarbit(6590, 1);
				Widget withdraw1 = client.getWidget(12, 34);
				utils.clickWidget(withdraw1);
			}
			else {
				utils.doInvoke((MenuEntry) null, (Rectangle) item.getBounds());
			}
		}
	}
	private void hop(boolean previous)
	{
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}


		World currentWorld = worldResult.findWorld(client.getWorld());

		if (currentWorld == null)
		{
			return;
		}

		EnumSet<net.runelite.http.api.worlds.WorldType> currentWorldTypes = currentWorld.getTypes().clone();
		// Make it so you always hop out of PVP and high risk worlds

		//if (config.quickhopOutOfDanger())
		//{
		currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.PVP);
		currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.HIGH_RISK);
		//}

		// Don't regard these worlds as a type that must be hopped between
		currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.BOUNTY);
		currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.SKILL_TOTAL);
		currentWorldTypes.remove(net.runelite.http.api.worlds.WorldType.LAST_MAN_STANDING);

		List<World> worlds = worldResult.getWorlds();

		//worldIDS = stringToIntList(config.worlds());

		int worldIdx = worlds.indexOf(currentWorld);
		int totalLevel = client.getTotalLevel();

		//final Set<RegionFilterMode> regionFilter = config.regionFilter();

		World world;
		do
		{

			/*
				Get the previous or next world in the list,
				starting over at the other end of the list
				if there are no more elements in the
				current direction of iteration.
			 */
			if (previous)
			{
				worldIdx--;

				if (worldIdx < 0)
				{
					worldIdx = worlds.size() - 1;
				}
			}
			else
			{
				worldIdx++;

				if (worldIdx >= worlds.size())
				{
					worldIdx = 0;
				}
			}

			world = worlds.get(worldIdx);

			// Check world region if filter is enabled
			/*if (!regionFilter.isEmpty() && !regionFilter.contains(RegionFilterMode.of(world.getRegion())))
			{
				continue;
			}*/

			EnumSet<net.runelite.http.api.worlds.WorldType> types = world.getTypes().clone();

			types.remove(net.runelite.http.api.worlds.WorldType.BOUNTY);
			// Treat LMS world like casual world
			types.remove(net.runelite.http.api.worlds.WorldType.LAST_MAN_STANDING);

			if (types.contains(net.runelite.http.api.worlds.WorldType.SKILL_TOTAL))
			{
				try
				{
					int totalRequirement = Integer.parseInt(world.getActivity().substring(0, world.getActivity().indexOf(" ")));

					if (totalLevel >= totalRequirement)
					{
						types.remove(net.runelite.http.api.worlds.WorldType.SKILL_TOTAL);
					}
				}
				catch (NumberFormatException ex)
				{
					utils.sendGameMessage("Failed to parse total level requirement for target world" + ex);
				}
			}

			// Avoid switching to near-max population worlds, as it will refuse to allow the hop if the world is full
			if (world.getPlayers() >= 1950)
			{
				continue;
			}

			if (world.getPlayers() < 0)
			{
				// offline world
				continue;
			}

			// Break out if we've found a good world to hop to
			if (currentWorldTypes.equals(types))
			{
				break;
			}
		}
		while (world != currentWorld);

		if (world == currentWorld)
		{
			utils.sendGameMessage("Couldn't find a world to quick-hop to.");
		}
		else
		{
			hop(world.getId());
		}
	}
	private void hop(World world)
	{
		assert client.isClientThread();

		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			// on the login screen we can just change the world by ourselves
			client.changeWorld(rsWorld);
			return;
		}

		utils.sendGameMessage("Quick hopping to world " + world.getId());
		client.hopToWorld(rsWorld);
		resetQuickHopper();
		//currentWorld = rsWorld;
		//displaySwitcherAttempts = 0;
	}
	private void resetQuickHopper()
	{
		currentWorld = null;
	}
	net.runelite.api.World currentWorld;
	@Inject
	WorldService worldService;
	private void hop(int worldId)
	{
		WorldResult worldResult = worldService.getWorlds();
		// Don't try to hop if the world doesn't exist
		World world = worldResult.findWorld(worldId);
		if (world == null)
		{
			utils.sendGameMessage("World is Null");
			return;
		}

		hop(world);
	}
	public void withdraw1Item(int bankItemID) {
		Widget item = utils.getBankItemWidget(bankItemID);
		if (item != null) {
			if (client.getVarbitValue(6590) != 0) {
				//client.setVarbit(6590, 1);
				Widget withdraw1 = client.getWidget(12, 28);
				utils.clickWidget(withdraw1);
			}
			else {
				utils.doInvoke((MenuEntry) null, (Rectangle) item.getBounds());
			}
		}
	}
	public void withdraw5Item(int bankItemID) {
		Widget item = utils.getBankItemWidget(bankItemID);
		if (item != null) {
			if (client.getVarbitValue(6590) != 1) {
				//client.setVarbit(6590, 1);
				Widget withdraw5 = client.getWidget(12, 30);
				utils.clickWidget(withdraw5);
			}
			else {
				utils.doInvoke((MenuEntry) null, (Rectangle) item.getBounds());
			}
		}
	}
	public void withdrawAllItem(int bankItemID) {
		Widget item = utils.getBankItemWidget(bankItemID);
		if (item != null) {
			if (client.getVarbitValue(6590) != 4) {
				Widget withdrawall = client.getWidget(12, 36);
				utils.clickWidget(withdrawall);
			}
			else {
				utils.doInvoke((MenuEntry) null, (Rectangle) item.getBounds());
			}
		}
	}
	private boolean first;
	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() == MenuAction.CC_OP && (event.getWidgetId() == WidgetInfo.WORLD_SWITCHER_LIST.getId() ||
				event.getWidgetId() == 11927560 || event.getWidgetId() == 4522007 || event.getWidgetId() == 24772686))
		{
			//Either logging out or world-hopping which is handled by 3rd party plugins so let them have priority
			utils.targetMenu = null;
			return;
		}
		if (event.getMenuOption().contains("Walk") || event.getMenuAction() == MenuAction.WALK) {
			//utils.sendGameMessage("Consumed Incorrect Walk Click");
			event.consume();
		}
		if (!event.getMenuOption().contains("Continue") && !event.getMenuOption().contains(currentEntry) && !utils.isBankOpen() && first) {
			event.consume();
			//utils.sendGameMessage("Consumed Incorrect Object Click");
		}
		//core.sendGameMessage("O:" + event.getMenuOption() + "T:" +event.getMenuTarget() + "E:" + event.getMenuEntry());
		//core.sendGameMessage(event.toString());	//Debugging
	}

}