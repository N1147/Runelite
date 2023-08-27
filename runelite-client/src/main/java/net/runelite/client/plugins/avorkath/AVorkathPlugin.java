package net.runelite.client.plugins.avorkath;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.Utils.Core;
import net.runelite.client.plugins.Utils.Walking;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@PluginDescriptor(
	name = "AVorkath",
	description = "Anarchise' Auto Vorkath.",
	tags = {"vorkath","anarchise","aplugins"},
	enabledByDefault = false
)
public class AVorkathPlugin extends Plugin
{
	@Inject
	private Client client;
	@Provides
	AVorkathConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AVorkathConfig.class);
	}
	@Inject
	private AVorkathConfig configvk;
	@Inject
	private ClientThread clientThread;
	@Inject
	private Core utils;
	@Inject
	private KeyManager keyManager;
	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private OverlayManager overlayManager;
	private Rectangle bounds;
	private int timeout;
	private NPC vorkath;
	private List<WorldPoint> acidSpots = new ArrayList<>();
	boolean FirstWalk = true;
	private List<WorldPoint> acidFreePath = new ArrayList<>();
	private int lastAcidSpotsSize = 0;
	private final Set<Integer> RUBY_SET = Set.of(ItemID.RUBY_DRAGON_BOLTS_E, ItemID.RUBY_BOLTS_E);
	private final Set<Integer> DIAMOND_SET = Set.of(ItemID.DIAMOND_DRAGON_BOLTS_E, ItemID.DIAMOND_BOLTS_E);
	WorldArea EDGEVILLE_BANK = new WorldArea(new WorldPoint(3082, 3485, 0), new WorldPoint(3100, 3502, 0));
	WorldArea RELEKKA_POH = new WorldArea(new WorldPoint(2664, 3625, 0), new WorldPoint(2678, 3638, 0));
	WorldArea RELEKKA_TOWN= new WorldArea(new WorldPoint(2624, 3642, 0), new WorldPoint(2655, 3703, 0));
	WorldArea VORKATH = new WorldArea(new WorldPoint(2262, 4032, 0), new WorldPoint(2286, 4053, 0));
	WorldArea VORKATH2 = new WorldArea(new WorldPoint(2259, 4053, 0), new WorldPoint(2290, 4083, 0));
	//private List<Integer> RUBY_SET = new ArrayList<>();
	//private List<Integer> DIAMOND_SET = new ArrayList<>();
		//Set.of();
	AVorkathState state;
	LocalPoint beforeLoc;
	Player player;
	MenuEntry targetMenu;
	WorldPoint dodgeRight;
	WorldPoint dodgeLeft;
	Instant botTimer;
	private boolean inFight;
	private Prayer prayerToClick;
	private Random r = new Random();
	public AVorkathPlugin(){
		inFight = false;
	}
	List<String> lootableItems = new ArrayList<>();
	private Prayer prayer;
	boolean startTeaks = false;
	boolean killedvorkath = false;
	boolean noBomb = true;
	boolean noBomb2 = true;
	private NPC zulrahNpc = null;
	private int stage = 0;
	private int phaseTicks = -1;
	private int attackTicks = -1;
	private int acidFreePathLength = 3;
	private int totalTicks = 0;
	boolean banked = false;
	String[] values;


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

	private void reset() throws IOException, ClassNotFoundException {
		started = false;
		loot.clear();
		lootableItems.clear();
		values = configvk.lootNames().toLowerCase().split("\\s*,\\s*");
		if (!configvk.lootNames().isBlank()) {
			lootableItems.addAll(Arrays.asList(values));
		}
		//overlayManager.remove(overlayvk);
		startTeaks = false;
		zulrahNpc = null;
		Poisoned = false;
		stage = 0;
		phaseTicks = -1;
		state = null;
		killedvorkath = false;
		timeout = 0;
		attackTicks = -1;
		totalTicks = 0;
		inFight = false;
		prayerToClick = null;
		noBomb = true;
		noBomb2 = true;
		dodgeRight = null;
		dodgeLeft = null;
		banked = false;
		botTimer = null;
	}
	/*@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) throws IOException, ClassNotFoundException {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("avork")) {
			return;
		}
		if (configButtonClicked.getKey().equals("startButton")) {
			if (!startTeaks) {
				startTeaks = true;
				//state = null;
				//player = null;
				overlayManager.add(overlayvk);
				loot.clear();
				lootableItems.clear();
				values = configvk.lootNames().toLowerCase().split("\\s*,\\s*");
				if (!configvk.lootNames().isBlank()) {
					lootableItems.addAll(Arrays.asList(values));
				}
				noBomb = true;
				noBomb2 = true;
				banked = false;
				botTimer = Instant.now();
				RUBY_SET.add(ItemID.RUBY_DRAGON_BOLTS_E, ItemID.RUBY_BOLTS_E);
				DIAMOND_SET.add(ItemID.DIAMOND_DRAGON_BOLTS_E, ItemID.DIAMOND_BOLTS_E);
			} else {
				reset();
			}
		}
	}*/
	private boolean started = false;
	private long sleepDelay()
	{
		long sleepLength = utils.randomDelay(false, 100, 350, 100, 150);
		return sleepLength;
	}
	private int tickDelay()
	{
		int tickLength = (int) utils.randomDelay(false, 0, 2, 1, 1);
		return tickLength;
	}
	@Override
	protected void startUp() throws Exception
	{
		reset();
	}

	@Inject ConfigManager configManager;


	@Override
	protected void shutDown() throws Exception
	{
		reset();
	}

	private void openBank() {
		if (configvk.useScrolls()) {
			NPC bankTarget = utils.findNearestNpc(3472);
			utils.interactNPC(bankTarget);
		}
		else {
			GameObject bankTarget = utils.findNearestBankNoDepositBoxes();
			if (bankTarget != null) {
				utils.useGameObjectDirect(bankTarget);
				//clientThread.invoke(() -> client.invokeMenuAction("", "", bankTarget.getId(), utils.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(), bankTarget.getSceneMinLocation().getY()));
			}
		}
	}
	List<WorldPoint> loot = new ArrayList<>();
	private void lootItem(List<WorldPoint> itemList) {
		if (itemList.get(0) != null) {
			utils.walk(itemList.get(0));
			//clientThread.invoke(() -> client.invokeMenuAction("", "", lootItem.getId(), MenuAction.GROUND_ITEM_THIRD_OPTION.getId(), lootItem.getTile().getSceneLocation().getX(), lootItem.getTile().getSceneLocation().getY()));
		}
	}

	int[] ItemIDs;
	public WidgetItem GetFoodItem() {
		WidgetItem item;

		item = utils.getInventoryWidgetItem(Collections.singletonList(configvk.foodID()));

		if (item != null)
		{
			return item;
		}

		return item;
	}
	public WidgetItem GetRangedItem()
	{
		WidgetItem item;

		item = utils.getInventoryWidgetItem(ItemID.DIVINE_RANGING_POTION1, ItemID.DIVINE_RANGING_POTION2, ItemID.DIVINE_RANGING_POTION3, ItemID.DIVINE_RANGING_POTION4,ItemID.DIVINE_BASTION_POTION1, ItemID.DIVINE_BASTION_POTION2, ItemID.DIVINE_BASTION_POTION3, ItemID.DIVINE_BASTION_POTION4, ItemID.BASTION_POTION1, ItemID.BASTION_POTION2, ItemID.BASTION_POTION3, ItemID.BASTION_POTION4,ItemID.RANGING_POTION1, ItemID.RANGING_POTION2, ItemID.RANGING_POTION3, ItemID.RANGING_POTION4);

		if (item != null)
		{
			return item;
		}

		return item;
	}
	public WidgetItem GetCombatItem()
	{
		WidgetItem item;

		item = utils.getInventoryWidgetItem(ItemID.DIVINE_SUPER_COMBAT_POTION1, ItemID.DIVINE_SUPER_COMBAT_POTION2, ItemID.DIVINE_SUPER_COMBAT_POTION3, ItemID.DIVINE_SUPER_COMBAT_POTION4, ItemID.SUPER_COMBAT_POTION1, ItemID.SUPER_COMBAT_POTION2, ItemID.SUPER_COMBAT_POTION3, ItemID.SUPER_COMBAT_POTION4, ItemID.COMBAT_POTION1, ItemID.COMBAT_POTION2, ItemID.COMBAT_POTION3, ItemID.COMBAT_POTION4);

		if (item != null)
		{
			return item;
		}

		return item;
	}
	public WidgetItem GetAntifireItem()
	{
		WidgetItem item;

		item = utils.getInventoryWidgetItem(ItemID.ANTIFIRE_POTION1, ItemID.ANTIFIRE_POTION2, ItemID.ANTIFIRE_POTION3, ItemID.ANTIFIRE_POTION4,ItemID.EXTENDED_SUPER_ANTIFIRE1,ItemID.EXTENDED_SUPER_ANTIFIRE2, ItemID.EXTENDED_SUPER_ANTIFIRE3, ItemID.EXTENDED_SUPER_ANTIFIRE4);

		if (item != null)
		{
			return item;
		}

		return item;
	}	//ItemID.ANTIDOTE4_5952, ItemID.ANTIVENOM4_12913
	public WidgetItem GetAntiVenomItem() {

		WidgetItem item;

		item = utils.getInventoryWidgetItem(ItemID.ANTIDOTE1_5958, ItemID.ANTIDOTE2_5956, ItemID.ANTIDOTE3_5954, ItemID.ANTIDOTE4_5952, ItemID.ANTIVENOM1_12919 ,ItemID.ANTIVENOM2_12917,ItemID.ANTIVENOM3_12915, ItemID.ANTIVENOM4_12913);

		if (item != null) {
			return item;
		}

		return item;
	}
	public AVorkathState getState()
	{
		if (timeout > 0)
		{
			return AVorkathState.TIMEOUT;
		}
		if(utils.isBankOpen()){
			return getBankState();
		}
		else {
			return getStates();
		}
	}

	public boolean isRunEnabled()
	{
		return client.getVarpValue(173) == 1;
	}
	private WorldArea deathSpot = new WorldArea(new WorldPoint(3195, 3199, 0), new WorldPoint(3234, 3237, 0));
	private WorldArea firstFloor = new WorldArea(new WorldPoint(3195, 3199, 1),  new WorldPoint(3234, 3237, 1));
	private WorldArea secondFloor = new WorldArea(new WorldPoint(3195, 3199, 2),  new WorldPoint(3234, 3237, 2));
	public List<Integer> stringToIntList(String string)
	{
		return (string == null || string.trim().equals("")) ? List.of(0) :
				Arrays.stream(string.split(",")).map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
	}
	private boolean reBank = false;
	@Inject
	Walking walk;
	@Nullable
	NPC vorkathAwake(){
		return utils.findNearestNpc(8059);
	}
	private boolean hasVenomed = false;
	private boolean isInVorkath()
	{
		return ArrayUtils.contains(client.getMapRegions(), 9023);
	}
	private boolean leavePortal = false;
	private AVorkathState getStates(){
		if (!isInVorkath() && client.getMinimapZoom() != 2.0) {
			client.setMinimapZoom(2.0);
			//utils.sendGameMessage(String.valueOf(client.getMinimapZoom()));
		}
		if (isInPOH(client) && utils.inventoryFull() && !utils.inventoryContains(ItemID.SUPERIOR_DRAGON_BONES) && utils.inventoryContains(configvk.foodID())) {
			currentEntry = "Enter";
			GameObject Portal = utils.findNearestGameObject(4525);
			utils.useGameObjectDirect(Portal);
			utils.sendGameMessage("Leaving House");
			return AVorkathState.TIMEOUT;
		}
		if (!noBomb2 && !isInVorkath()){
			noBomb2 = true;
			attacked = false;
		}
		if (!noBomb && !isInVorkath()){
			noBomb = true;
			attacked = false;
		}
		if (utils.findNearestNpc(8059) != null && isInVorkath()){
			acidFreePath.clear();
			acidSpots.clear();
			noBomb = true;
			noBomb2 = true;
			attacked = false;
		}
		if (reBank) {
			utils.sendGameMessage("Died, Restocking inventory");
			return AVorkathState.WALK_SECOND;
		}
		if (!isInVorkath() && utils.inventoryContains(stringToIntList(configvk.gear()))){
			WidgetItem Item = utils.getWidgetItem(stringToIntList(configvk.gear()));
			if (Item != null) {
				utils.sendGameMessage("DIED, Wearing Items from Config");
				if (Item.getId() == ItemID.OSMUMTENS_FANG || Item.getId() == ItemID.DRAGON_HUNTER_LANCE || Item.getId() == ItemID.RUNE_CROSSBOW || Item.getId() == ItemID.DRAGON_CROSSBOW || Item.getId() == ItemID.DRAGON_HUNTER_CROSSBOW || Item.getId() == ItemID.DRAGONFIRE_WARD || Item.getId() == ItemID.DRAGONFIRE_SHIELD || Item.getId() == ItemID.ANTIDRAGON_SHIELD) {
					currentEntry = "Wield";
				}
				else {
					currentEntry = "Wear";
				}
				utils.doInvoke(null, Item.getCanvasBounds());
				return AVorkathState.TIMEOUT;
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
				return AVorkathState.TIMEOUT;
			}
			else {
				currentEntry = "Climb-up";
				utils.useGameObjectDirect(bottomStairs);
				return AVorkathState.TIMEOUT;
			}
		}
		if (client.getLocalPlayer().getWorldArea().intersectsWith(firstFloor)) {
			GameObject firstFloorStairs = utils.findNearestGameObject(16672);
			if (firstFloorStairs.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldArea()) >= 3) {
				currentEntry = "Walk here";
				utils.walkToMinimapTile(firstFloorStairs.getWorldLocation());
				return AVorkathState.TIMEOUT;
			}
			else {
				currentEntry = "Climb-up";
				utils.useGameObjectDirect(firstFloorStairs);
				return AVorkathState.TIMEOUT;
			}
		}
		/*if (isInVorkath() && utils.findNearestNpc("Vorkath") != null && utils.inventoryContains(ItemID.ANTIDOTE4_5952, ItemID.ANTIVENOM4_12913)) {
			currentEntry = "Drink";
			utils.sendGameMessage("Drinking Venom");
			return AVorkathState.DRINK_ANTIVENOM;	//ItemID.ANTIDOTE4_5952, ItemID.ANTIVENOM4_12913
		}*/
		/*if (isInVorkath() && client.getVarbitValue(VarPlayer.POISON) > 0) {
			utils.clickWidget(WidgetInfo.MINIMAP_HEALTH_ORB);
			Poisoned = false;
			return AVorkathState.TIMEOUT;
		}*/
		if (client.getLocalPlayer().getInteracting() != utils.findNearestNpc("Zombified Spawn") && utils.findNearestNpc("Zombified Spawn") != null) {
			NPC npc = utils.findNearestNpc("Zombified Spawn");
			if (!utils.isItemEquipped(Collections.singleton(ItemID.SLAYERS_STAFF))) {
				currentEntry = "Wield";
				utils.useItem(ItemID.SLAYERS_STAFF, "wield");
				return AVorkathState.TIMEOUT;
			}
			currentEntry = "Attack";
			if (npc != null) {
				utils.dragmouse(npc.getConvexHull().getBounds());
			}
			utils.attackNPC("Zombified Spawn");
			return AVorkathState.TIMEOUT;
		}
		if (!loot.isEmpty() && !utils.inventoryFull() && isInVorkath()){
			currentEntry = "Take";
			utils.sendGameMessage("Looting");
			return AVorkathState.LOOT_ITEMS; //
		}
		if (!loot.isEmpty() && utils.inventoryFull() && isInVorkath() && !utils.inventoryContains(configvk.foodID())){
			utils.sendGameMessage("Inventory Full, Nothing to drop");
			return AVorkathState.WALK_SECOND; //
		}
		if (utils.inventoryFull() && !loot.isEmpty() && isInVorkath()){
			if (utils.inventoryContains(configvk.foodID())) {
				currentEntry = "Eat";
				utils.sendGameMessage("Eating Food to Loot");
				return AVorkathState.EAT_FOOD;
			}
		}
		//if (banked && client.getLocalPlayer().getWorldArea().intersectsWith(EDGEVILLE_BANK)  && !utils.isBankOpen()){
		//	banked = false;
		//}
		if (utils.isItemEquipped(Collections.singleton(ItemID.SLAYERS_STAFF)) && noBomb2 == true){
			WidgetItem WEAPON = utils.getInventoryWidgetItem(configvk.normalWeapon());
			if (WEAPON != null) {
				currentEntry = "Wield";
				utils.useItem(WEAPON.getId(), "wear");
				//clientThread.invoke(() -> client.invokeMenuAction("", "", boltz.getId(), MenuAction.CC_OP.getId(), boltz.getIndex(), WidgetInfo.INVENTORY.getId());
			}
		}
		if (utils.findNearestBankNoDepositBoxes() != null && utils.inventoryContains(configvk.foodID()) && banked) {
			//currentEntry = "Break";
			utils.sendGameMessage("TP to Vorkath");
			return AVorkathState.WALK_FIRST;
		}

		if (noBomb && noBomb2 && acidSpots.isEmpty() && utils.inventoryContains(configvk.foodID()) && !isInPOH(client) && isInVorkath() && client.getBoostedSkillLevel(Skill.HITPOINTS) <= configvk.hp()){
			currentEntry = "Eat";
			leavePortal = false;
			WidgetItem food = GetFoodItem();
			if (food != null) {
				utils.useItem(food.getId(), "eat");
				//clientThread.invoke(() -> client.invokeMenuAction("", "", food.getId(), MenuAction.CC_OP.getId(), food.getIndex(), WidgetInfo.INVENTORY.getId()));
			}
			utils.sendGameMessage("Eating Food 2");
			return AVorkathState.TIMEOUT;
		}
		if (isInPOH(client) && utils.inventoryContains(RUBY_SET) && !utils.isItemEquipped(RUBY_SET)&& configvk.useRanged() && !configvk.useBlowpipe())
		{
			currentEntry = "Wield";
			utils.sendGameMessage("Equip Rubies");
			return AVorkathState.EQUIP_RUBIES;
		}
		if (!isInVorkath() && utils.inventoryContains(RUBY_SET) && !utils.isItemEquipped(RUBY_SET) && configvk.useRanged() && !configvk.useBlowpipe())
		{
			currentEntry = "Wield";
			utils.sendGameMessage("Equip Rubies");
			return AVorkathState.EQUIP_RUBIES;
		}
		if (isInVorkath() && calculateHealth(vorkath, 750) > 265 && calculateHealth(vorkath, 750) <= 750 && utils.inventoryContains(RUBY_SET) && !utils.isItemEquipped(RUBY_SET) && acidSpots.isEmpty()&& configvk.useRanged() && !configvk.useBlowpipe())
		{
			currentEntry = "Wield";
			utils.sendGameMessage("Equip Rubies");
			return AVorkathState.EQUIP_RUBIES;
		}
		if (isInVorkath() && calculateHealth(vorkath, 750) < 265 && calculateHealth(vorkath, 750) > 1 && utils.inventoryContains(DIAMOND_SET) && !utils.isItemEquipped(DIAMOND_SET) && acidSpots.isEmpty()&& configvk.useRanged() && !configvk.useBlowpipe())
		{
			currentEntry = "Wield";
			utils.sendGameMessage("Equip Diamonds");
			return AVorkathState.EQUIP_DIAMONDS;
		}
		if (client.getVar(Varbits.QUICK_PRAYER) == 1 && !isInVorkath())
		{
			currentEntry = "Deactivate";
			utils.sendGameMessage("Deactivate Pray");
			return AVorkathState.DEACTIVATE_PRAY;
		}
		if (client.getLocalPlayer().getWorldArea().intersectsWith(secondFloor) && hasDied && banked){
			//currentEntry = "Break";
			utils.sendGameMessage("TP to Vorkath (Death)");
			return AVorkathState.WALK_FIRST;
		}
		if (utils.findNearestBankNoDepositBoxes() != null && banked && configvk.autoBank()){
			//currentEntry = "Break";
			utils.sendGameMessage("TP to Vorkath 2");
			return AVorkathState.WALK_FIRST;
		}
		if (utils.findNearestBankNoDepositBoxes() != null && !banked && configvk.autoBank()){
			if (!utils.isItemEquipped(Collections.singleton(configvk.normalWeapon()))) {
				currentEntry = "Wield";
				utils.useItem(configvk.normalWeapon(), "wield");
				utils.sendGameMessage("Re-wield weapon");
				return AVorkathState.TIMEOUT;
			}
			else {
				currentEntry = "Bank";
				utils.sendGameMessage("Finding Bank");
			}
			return AVorkathState.FIND_BANK;
		}
		if (player.getLocalLocation() == new LocalPoint(4800, 7488) && isInVorkath()) {
			currentEntry = "Walk here";
			utils.sendGameMessage("Moving from D Tile");
			utils.walk(new LocalPoint(5568, 7488));
		}
		/*if (isInPOH(client) && client.getBoostedSkillLevel(Skill.HITPOINTS) < client.getRealSkillLevel(Skill.HITPOINTS) && configvk.usePOHpool()){
			currentEntry = "Drink";
			utils.sendGameMessage("Drinking POH Pool");
			return AVorkathState.DRINK_POOL;
		}*/
		if (isInPOH(client) && client.getBoostedSkillLevel(Skill.PRAYER) < client.getRealSkillLevel(Skill.PRAYER) && configvk.usePOHpool()){
			currentEntry = "Drink";
			utils.sendGameMessage("Drinking POH Pool");
			return AVorkathState.DRINK_POOL;
		}
		if (isInVorkath() && utils.isItemEquipped(Collections.singleton(configvk.specWeapon())) && client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) < configvk.specThreshold() * 10){
			currentEntry = "Wield";
			WidgetItem weapon = utils.getInventoryWidgetItem(Collections.singletonList(configvk.normalWeapon()));
			WidgetItem offhand = utils.getInventoryWidgetItem(Collections.singletonList(configvk.normalOffhand()));
			if (weapon != null) {
				utils.sendGameMessage("Equip Weapon");
				utils.useItem(weapon.getId(), "wield");
				//clientThread.invoke(() -> client.invokeMenuAction("", "", weapon.getId(), MenuAction.CC_OP.getId(), weapon.getIndex(), WidgetInfo.INVENTORY.getId()));
			}
			if (offhand != null){
				utils.sendGameMessage("Equip Offhand");
				utils.useItem(offhand.getId(), "wield");
				//clientThread.invoke(() -> client.invokeMenuAction("", "", offhand.getId(), MenuAction.CC_OP.getId(), offhand.getIndex(), WidgetInfo.INVENTORY.getId()));
			}
		}
		/*if (utils.isItemEquipped(Collections.singleton(configvk.specWeapon())) && !isInVorkath()){
			currentEntry = "Wield";
			WidgetItem weapon = utils.getInventoryWidgetItem(Collections.singletonList(configvk.normalWeapon()));
			WidgetItem offhand = utils.getInventoryWidgetItem(Collections.singletonList(configvk.normalOffhand()));
			if (weapon != null) {
				utils.sendGameMessage("Equip Weapon 2");
				utils.useItem(weapon.getId(), "wield");
				return AVorkathState.TIMEOUT;
				//clientThread.invoke(() -> client.invokeMenuAction("", "", weapon.getId(), MenuAction.CC_OP.getId(), weapon.getIndex(), WidgetInfo.INVENTORY.getId()));
			}
			if (offhand != null){
				utils.sendGameMessage("Equip Offhand 2");
				utils.useItem(offhand.getId(), "wield");
				return AVorkathState.TIMEOUT;
				//clientThread.invoke(() -> client.invokeMenuAction("", "", offhand.getId(), MenuAction.CC_OP.getId(), offhand.getIndex(), WidgetInfo.INVENTORY.getId()));
			}
		}*/
		if (!hasDied && utils.inventoryContains(22124) && loot.isEmpty() && !isInPOH(client) && isInVorkath() && !configvk.onlytelenofood()){
			//currentEntry = "Break";
			utils.sendGameMessage("Teleporting");
			return AVorkathState.WALK_SECOND;
		}
		if (!hasDied && !utils.inventoryContains(configvk.foodID()) && client.getBoostedSkillLevel(Skill.HITPOINTS) <= configvk.healthTP() && loot.isEmpty() && !isInPOH(client) && isInVorkath()){
			//currentEntry = "Break";
			utils.sendGameMessage("Teleporting 2");
			return AVorkathState.WALK_SECOND;
		}
		if (!hasDied && !utils.inventoryContains(ItemID.PRAYER_POTION1, ItemID.PRAYER_POTION2, ItemID.PRAYER_POTION3, ItemID.PRAYER_POTION4, ItemID.SUPER_RESTORE1, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE4, ItemID.BLIGHTED_SUPER_RESTORE1, ItemID.BLIGHTED_SUPER_RESTORE2, ItemID.BLIGHTED_SUPER_RESTORE3, ItemID.BLIGHTED_SUPER_RESTORE4, ItemID.SANFEW_SERUM1, ItemID.SANFEW_SERUM2, ItemID.SANFEW_SERUM3, ItemID.SANFEW_SERUM4) && client.getBoostedSkillLevel(Skill.PRAYER) <= configvk.prayTP() && isInVorkath()){
			//currentEntry = "Break";
			utils.sendGameMessage("Teleporting 3");
			return AVorkathState.WALK_SECOND;
		}
		if (!hasDied && configvk.autoBank() && isInPOH(client)){
			utils.sendGameMessage("Edge Tele");
			return AVorkathState.TELE_EDGE;
		}

		if (player.getWorldArea().intersectsWith(RELEKKA_POH)){
			currentEntry = "Walk here";
			utils.sendGameMessage("Walk 3rd");
			return AVorkathState.WALK_THIRD;
		}
		if (!hasDied && player.getWorldArea().intersectsWith(RELEKKA_TOWN)){
			utils.sendGameMessage("Use Boat");
			return AVorkathState.USE_BOAT;
		}
		if (hasDied && player.getWorldArea().intersectsWith(RELEKKA_TOWN)){
			utils.sendGameMessage("Walk to Boat");
			return AVorkathState.USE_BOAT;
		}
		if (!hasDied && player.getWorldArea().intersectsWith(VORKATH)){
			utils.sendGameMessage("Use Obstacle");
			return AVorkathState.JUMP_OBSTACLE;
		}
		if (acidSpots.isEmpty() && noBomb && noBomb2 && client.getEnergy() > 30 && !isRunEnabled()) {
			currentEntry = "Toggle Run";
			Widget RunOrb = client.getWidget(WidgetInfo.MINIMAP_RUN_ORB);
			utils.clickWidget(RunOrb);
			return AVorkathState.TIMEOUT;
		}
		if (!acidSpots.isEmpty() && isInVorkath()){
			utils.sendGameMessage("Acid Walk");
			return AVorkathState.ACID_WALK;
		}
		if (!noBomb && isInVorkath()){
			currentEntry = "Walk here";
			utils.sendGameMessage("Bomb Walk");
			return AVorkathState.HANDLE_BOMB;
		}
		if (!noBomb2 && isInVorkath()){
			utils.sendGameMessage("Handle Ice");
			return AVorkathState.HANDLE_ICE;
		}

		if (utils.inventoryContains(ItemID.PRAYER_POTION1, ItemID.PRAYER_POTION2, ItemID.PRAYER_POTION3, ItemID.PRAYER_POTION4, ItemID.SUPER_RESTORE1, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE4) && !isInPOH(client) && isInVorkath() && client.getBoostedSkillLevel(Skill.PRAYER) <= configvk.pray()){
			currentEntry = "Drink";
			utils.sendGameMessage("Drinking Prayer");
			return AVorkathState.DRINK_PRAY;
		}
		if (client.getVarpValue(VarPlayer.POISON) > 0 && isInVorkath()) {
			currentEntry = "Drink";
			utils.sendGameMessage("Drinking AntiVenom");
			return AVorkathState.DRINK_ANTIVENOM;
		}
		if (client.getBoostedSkillLevel(Skill.RANGED) <= configvk.potThreshold() && isInVorkath() && configvk.useRanged()){
			currentEntry = "Drink";
			utils.sendGameMessage("Drinking Ranged");
			return AVorkathState.DRINK_RANGE;
		}

		if (client.getBoostedSkillLevel(Skill.STRENGTH) <= configvk.potThreshold() && isInVorkath() && !configvk.useRanged()){
			currentEntry = "Drink";
			utils.sendGameMessage("Drinking Combat");
			return AVorkathState.DRINK_COMBAT;
		}
		if (configvk.superantifire() && client.getVarbitValue(6101) == 0 && isInVorkath()){
			currentEntry = "Drink";
			utils.sendGameMessage("Drinking Antifire");
			return AVorkathState.DRINK_ANTIFIRE;
		}
		if (!configvk.superantifire() && client.getVarbitValue(3981) == 0 && isInVorkath()){
			currentEntry = "Drink";
			utils.sendGameMessage("Drinking Antifire 2");
			return AVorkathState.DRINK_ANTIFIRE;
		}
		if (client.getVar(Varbits.QUICK_PRAYER) != 0 && isInVorkath() && !acidSpots.isEmpty())
		{
			currentEntry = "Deactivate";
			utils.sendGameMessage("Deactivate Prayers 3");
			return AVorkathState.DEACTIVATE_PRAY;
		}
		if (client.getVar(Varbits.QUICK_PRAYER) != 0 && isInVorkath() && (!noBomb2 || !noBomb))
		{
			currentEntry = "Deactivate";
			utils.sendGameMessage("Deactivate Prayers 2");
			return AVorkathState.DEACTIVATE_PRAY;
		}

		if (client.getVar(Varbits.QUICK_PRAYER) == 0 && isInVorkath() && acidSpots.isEmpty() && noBomb2 && noBomb)
		{
			currentEntry = "Activate";
			utils.sendGameMessage("Activate Prayers");
			return AVorkathState.ACTIVATE_PRAY;
		}
		if (client.getVar(Varbits.QUICK_PRAYER) != 0 && isInVorkath() && !acidSpots.isEmpty() || !noBomb2 || !noBomb)
		{
			currentEntry = "Deactivate";
			utils.sendGameMessage("Deactivate Prayers 2");
			return AVorkathState.DEACTIVATE_PRAY;
		}
		if (utils.findNearestNpc(8059) != null && isInVorkath() && loot.isEmpty()  && utils.inventoryItemContainsAmount(configvk.foodID(), 3, false, false)){
			/*if (!hasVenomed) {
				currentEntry = "Drink";
				utils.sendGameMessage("Drinking Venom");
				return AVorkathState.DRINK_ANTIVENOM;
			}*/
			currentEntry = "Poke";
			client.setMinimapZoom(4.0);
			utils.sendGameMessage("Wake Vorkath");
			return AVorkathState.WAKE_VORKATH;
		}
		if (utils.findNearestNpc(8059) != null && isInVorkath() && loot.isEmpty()  && !utils.inventoryItemContainsAmount(configvk.foodID(), 3, false, false)){
			//currentEntry = "Break";
			utils.sendGameMessage("Teleporting 3");
			return AVorkathState.WALK_SECOND;
		}
		if (isInVorkath() && player.getWorldLocation().distanceTo(vorkath.getWorldArea()) <= 1 && configvk.useRanged()){
			currentEntry = "Walk here";
			utils.sendGameMessage("Moving Away");
			return AVorkathState.MOVE_AWAY;
		}
		if (!utils.isItemEquipped(Collections.singleton(configvk.specWeapon())) && utils.inventoryFull() && utils.inventoryContains(configvk.foodID()) && configvk.normalOffhand() != 0 && calculateHealth(vorkath, 750) >= configvk.specHP() && client.getVar(VarPlayer.SPECIAL_ATTACK_ENABLED) == 0 && client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) >= configvk.specThreshold() * 10 && configvk.useSpec() && noBomb && noBomb2 && utils.findNearestNpc(8061) != null && acidSpots.isEmpty() && vorkath != null && isInVorkath()){
			currentEntry = "Eat";
			utils.sendGameMessage("Eating Food 3");
			return AVorkathState.EAT_FOOD;
		}
		if (!utils.isItemEquipped(Collections.singleton(configvk.specWeapon())) &&  calculateHealth(vorkath, 750) >= configvk.specHP() && client.getVar(VarPlayer.SPECIAL_ATTACK_ENABLED) == 0 && client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) >= configvk.specThreshold() * 10 && configvk.useSpec() && noBomb && noBomb2 && utils.findNearestNpc(8061) != null && acidSpots.isEmpty() && vorkath != null && isInVorkath()){
			currentEntry = "Wield";
			utils.sendGameMessage("Equipping Spec");
			return AVorkathState.EQUIP_SPEC;
		}
		if (utils.isItemEquipped(Collections.singleton(configvk.specWeapon())) && calculateHealth(vorkath, 750) >= configvk.specHP() && client.getVar(VarPlayer.SPECIAL_ATTACK_ENABLED) == 0 && client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) >= configvk.specThreshold() * 10 && configvk.useSpec() && noBomb && noBomb2 && utils.findNearestNpc(8061) != null && acidSpots.isEmpty() && vorkath != null && isInVorkath()) {
			currentEntry = "Use";
			utils.sendGameMessage("Specing");
			return AVorkathState.SPECIAL_ATTACK;
		}

		if (noBomb && noBomb2 && utils.findNearestNpc(8061) != null && utils.findNearestNpc("Zombified Spawn") == null && noBomb2 && noBomb && acidSpots.isEmpty() && vorkath != null && client.getLocalPlayer().getInteracting() != vorkath && isInVorkath()) {
			currentEntry = "Attack";
			utils.sendGameMessage("Attack Vorkath");
			return AVorkathState.ATTACK_VORKATH;
		}
		else return AVorkathState.TIMEOUT;
	}
	private boolean hasDied = false;
	private AVorkathState getBankState()
	{
		if (configvk.autoBank()) {
			if (!banked && client.getLocalPlayer().getWorldArea().intersectsWith(secondFloor)) {
				hasDied = true;
				if (!configvk.useScrolls() && !utils.inventoryContains(ItemID.TELEPORT_TO_HOUSE)) {
					utils.sendGameMessage("Withdraw Teles (Death)");
					return AVorkathState.WITHDRAW_TELES;
				}
				if (configvk.useScrolls() && !utils.inventoryContains(ItemID.LUNAR_ISLE_TELEPORT)) {
					utils.sendGameMessage("Withdraw Teles (Death)");
					return AVorkathState.WITHDRAW_SCROLLS;
				}
				if (utils.inventoryContains(ItemID.TELEPORT_TO_HOUSE, ItemID.LUNAR_ISLE_TELEPORT)) {
					utils.sendGameMessage("Finished banking (Death)");
					return AVorkathState.WALK_FIRST;
				}
			}
			if (banked && utils.inventoryContains(ItemID.SUPERIOR_DRAGON_BONES)) {
				banked = false;
				return AVorkathState.TIMEOUT;
			}
			if (!banked) {
				utils.depositAll();
				banked = true;
				utils.sendGameMessage("Depositing");
				return AVorkathState.DEPOSIT_ITEMS;
			}
			if (!utils.inventoryContains(ItemID.SLAYERS_STAFF)) {
				withdraw1Item(ItemID.SLAYERS_STAFF);
				utils.sendGameMessage("Withdraw Staff");
				return AVorkathState.TIMEOUT;
			}
			if (configvk.useSpec() && !utils.inventoryContains(configvk.specWeapon())) {
				utils.sendGameMessage("Withdraw Spec");
				withdraw1Item(configvk.specWeapon());
			}
			if (!utils.inventoryContains(12791) && !utils.inventoryContains(27281)) {
				utils.sendGameMessage("Withdraw Pouch");
				return AVorkathState.WITHDRAW_POUCH;
			}
			if (!utils.inventoryContains(2444) && configvk.useRanged() && !configvk.supers()) {
				utils.sendGameMessage("Withdraw Ranged");
				return AVorkathState.WITHDRAW_RANGED;
			}
			if (!utils.inventoryContains(22461) && configvk.useRanged() && configvk.supers()) {
				utils.sendGameMessage("Withdraw Ranged 2");
				return AVorkathState.WITHDRAW_RANGED;
			}
			if (!utils.inventoryContains(12695) && !configvk.useRanged()) {
				utils.sendGameMessage("Withdraw Combat");
				return AVorkathState.WITHDRAW_COMBAT;
			}
			if (configvk.superantifire() && !utils.inventoryContains(22209)) {
				utils.sendGameMessage("Withdraw Antifire");
				return AVorkathState.WITHDRAW_ANTIFIRE;
			}
			if (!configvk.superantifire() && !utils.inventoryContains(2452)) {
				utils.sendGameMessage("Withdraw Antifire 2");
				return AVorkathState.WITHDRAW_ANTIFIRE;
			}
			if (configvk.antivenomplus() && !utils.inventoryItemContainsAmount(12913, configvk.antipoisonamount(), false, true)) {
				utils.sendGameMessage("Withdraw Venom");
				return AVorkathState.WITHDRAW_VENOM;
			}
			if (!configvk.antivenomplus() && !utils.inventoryItemContainsAmount(5952, configvk.antipoisonamount(), false, true)) {
				utils.sendGameMessage("Withdraw Venom 2");
				return AVorkathState.WITHDRAW_VENOM;
			}
			if (!utils.inventoryContains(3024) && !utils.inventoryContains(2434)) {
				utils.sendGameMessage("Withdraw Restores");
				return AVorkathState.WITHDRAW_RESTORES;
			}
			if (configvk.useScrolls() && !utils.inventoryContains(ItemID.LUNAR_ISLE_TELEPORT)) {
				return AVorkathState.WITHDRAW_SCROLLS;
			}
			if (!configvk.useScrolls() && !utils.inventoryContains(8013)) {
				utils.sendGameMessage("Withdraw Teles");
				return AVorkathState.WITHDRAW_TELES;
			}
			if (!utils.inventoryContains(DIAMOND_SET) && configvk.useRanged() && !configvk.useBlowpipe()) {
				utils.sendGameMessage("Withdraw Bolts");
				return AVorkathState.WITHDRAW_BOLTS;
			}
			if (!utils.inventoryContains(configvk.foodID())) {
				utils.sendGameMessage("Withdraw Food");
				return AVorkathState.WITHDRAW_FOOD1;
			}
			if (utils.inventoryContains(configvk.foodID()) && banked) {
				utils.sendGameMessage("Finished Banking");
				return AVorkathState.WALK_FIRST;
			}
		}
		return AVorkathState.TIMEOUT;
	}

	private boolean attacked = false;
	private int AcidTickCount = 0;
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
	private void onGameTick(final GameTick event) throws IOException, ClassNotFoundException {
		player = client.getLocalPlayer();

		if (client != null && player != null)
				{
					state = getState();
					beforeLoc = player.getLocalLocation();
					switch (state)
					{
						case TIMEOUT:
							//if (client.)
							timeout--;
							break;
						case SPECIAL_ATTACK:
							if (utils.isItemEquipped(Collections.singleton(configvk.specWeapon()))){
								utils.specialAttack();
								//clientThread.invoke(() -> client.invokeMenuAction("Use <col=00ff00>Special Attack</col>", "", 1, MenuAction.CC_OP.getId(), -1, 38862884));
							}
							break;
						case MOVE_AWAY:
							utils.walkToMinimapTile(new WorldPoint(player.getWorldLocation().getX(), player.getWorldLocation().getY() - 3, player.getWorldLocation().getPlane()));
							//timeout = tickDelay();
							break;
						case TELE_EDGE:
							if (configvk.useScrolls()) {
								currentEntry = "Teleport";
								utils.useItem(ItemID.LUNAR_ISLE_TELEPORT, "use");
							}
							else {
								currentEntry = "Edgeville";
								utils.useDecorativeObject(13523, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), sleepDelay());
							}
							timeout = tickDelay();
							break;
						case EQUIP_SPEC:
							WidgetItem weapon = utils.getInventoryWidgetItem(Collections.singletonList(configvk.specWeapon()));
							if (weapon != null) {
								utils.useItem(weapon.getId(), "wear");
								//clientThread.invoke(() -> client.invokeMenuAction("", "", weapon.getId(), MenuAction.CC_OP.getId(), weapon.getIndex(), WidgetInfo.INVENTORY.getId()));
							}
							break;
						case EQUIP_RUBIES:
							WidgetItem boltz = utils.getInventoryWidgetItem(ItemID.RUBY_DRAGON_BOLTS_E, ItemID.RUBY_BOLTS_E);
							if (boltz != null) {
								utils.useItem(boltz.getId(), "wear");
								//clientThread.invoke(() -> client.invokeMenuAction("", "", boltz.getId(), MenuAction.CC_OP.getId(), boltz.getIndex(), WidgetInfo.INVENTORY.getId());
							}
							break;
						case DRINK_POOL:
							GameObject Pool = utils.findNearestGameObject(29240, 29241, 29239);
							utils.useGameObjectDirect(Pool);
							timeout = tickDelay();
							break;
						case EQUIP_DIAMONDS:
							WidgetItem bolts = utils.getInventoryWidgetItem(ItemID.DIAMOND_DRAGON_BOLTS_E, ItemID.DIAMOND_BOLTS_E);
							if (bolts != null) {
								utils.useItem(bolts.getId(), "wear");
							}
							//timeout = tickDelay();
							break;
						case ACID_WALK:
							calculateAcidFreePath();
							AcidTickCount++;
							if (isRunEnabled()) {
								currentEntry = "Toggle Run";
								Widget RunOrb = client.getWidget(WidgetInfo.MINIMAP_RUN_ORB);
								utils.clickWidget(RunOrb);
							}
							else {
								currentEntry = "Walk here";
								if (acidFreePathLength >= 3) {
									if (FirstWalk) {
										utils.walkToMinimapTile(acidFreePath.get(1));
										FirstWalk = false;
									}

									if (!FirstWalk) {
										//utils.walkToMinimapTile(acidFreePath.get(3));
										utils.walkToMinimapTile(acidFreePath.get(acidFreePath.size() - 1));
										FirstWalk = true;
									}
									break;
								}
							}
							break;
						case ATTACK_VORKATH:
							if (isInVorkath()) {
								if (vorkath != null) {
									utils.dragmouse(vorkath.getConvexHull().getBounds());
									utils.sleep(25);

									utils.attackNPCDirect(vorkath);
								}
							}
							break;
						case HANDLE_ICE:
							NPC npc = utils.findNearestNpc("Zombified Spawn");
							if (!utils.isItemEquipped(Collections.singleton(ItemID.SLAYERS_STAFF))) {
								currentEntry = "Wield";
								utils.useItem(ItemID.SLAYERS_STAFF, "wield");
							}
							else {
								if (client.getLocalPlayer().getInteracting() == null && utils.findNearestNpc("Zombified Spawn") != null) {
									currentEntry = "Attack";
									if (npc != null) {
										utils.dragmouse(npc.getConvexHull().getBounds());
									}
									utils.attackNPC("Zombified Spawn");
								}
								if (utils.findNearestNpc("Zombified Spawn") == null) {
									noBomb2 = true;
								}
							}
							break;
						case HANDLE_BOMB:
							final WorldPoint loc = client.getLocalPlayer().getWorldLocation();
							final LocalPoint localLoc = LocalPoint.fromWorld(client, loc);
							dodgeRight = new WorldPoint(loc.getX() + 8, loc.getY(), client.getPlane());
							dodgeLeft = new WorldPoint(loc.getX() - 8, loc.getY(), client.getPlane());
							if (loc.distanceTo(dodgeLeft) <= 1){
								noBomb = true;
								noBomb2 = true;
							}
							if (loc.distanceTo(dodgeRight) <= 1){
								noBomb = true;
								noBomb2 = true;
							}
							if (localLoc.getX() < 6208) {
								walk.walkTileOnScreen(dodgeRight);
								timeout = tickDelay();
								noBomb = true;
								noBomb2 = true;
								break;
							} else {
								walk.walkTileOnScreen(dodgeLeft);
								timeout = tickDelay();
								noBomb = true;
								noBomb2 = true;
								break;
							}
						case DEACTIVATE_PRAY:
						case ACTIVATE_PRAY:
							utils.clickWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
							//clientThread.invoke(() -> client.invokeMenuAction("Deactivate", "Quick-prayers", 1,  MenuAction.CC_OP.getId(), -1, 10485775));
							timeout = 1;
							break;
						case WITHDRAW_COMBAT:
							if (!configvk.supers()) {
								withdraw1Item(9739); //combat
							}
							else {
								withdraw1Item(12695); //supercombat
							}
							timeout = tickDelay();
							break;
						case WITHDRAW_RANGED:
							if (!configvk.supers()) {
								withdraw1Item(2444); // range
							}
							else {
								withdraw1Item(22461); // bastion
							}
							timeout = tickDelay();
							break;
						case WAKE_VORKATH:
							NPC Vorkath = utils.findNearestNpc("Vorkath");
							utils.attackNPCDirect(Vorkath);
							//clientThread.invoke(() -> client.invokeMenuAction("", "", utils.findNearestNpc("Vorkath").getIndex(), MenuAction.NPC_FIRST_OPTION.getId(), 0, 0));
							timeout = tickDelay();
							break;
						case CLOSE_BANK:
							utils.pressKey(KeyCode.KC_ESCAPE);
							timeout = tickDelay();
							break;
						case WITHDRAW_VENOM:
							if (configvk.antivenomplus()) {
								withdraw1Item(12913); //anti venom+
							}
							if (!configvk.antivenomplus()){
								withdraw1Item(5952); // antidote++
							}
							timeout = tickDelay();
							break;
						case WITHDRAW_ANTIFIRE:
							if (configvk.superantifire()) {
								withdraw1Item(22209); //extended super antifire
							}
							if (!configvk.superantifire()){
								withdraw1Item(2452); // regular antifire
							}
							timeout = tickDelay();
							break;
						case WITHDRAW_POUCH:
							if (utils.getBankItemWidget(27281) != null) {
								withdraw1Item(27281);
								timeout = tickDelay();
								break;
							}
							else {
								withdraw1Item(12791); //rune pouch
								timeout = tickDelay();
								break;
							}
						case WITHDRAW_RESTORES:
							if (configvk.useRestores()) {
								withdraw5Item(3024); //super restore x2
							}
							else {
								withdraw5Item(2434); //prayer pot x2
							}
							timeout = 2;
							break;
						case WITHDRAW_SCROLLS:
							withdraw5Item(ItemID.LUNAR_ISLE_TELEPORT);
							timeout = 1;
							break;
						case WITHDRAW_TELES:
							withdraw5Item(8013); //house tabs
							timeout = 1;
							break;
						case WITHDRAW_BOLTS:
							if (utils.bankContains(ItemID.DIAMOND_DRAGON_BOLTS_E, 1)) {
								withdrawAllItem(ItemID.DIAMOND_DRAGON_BOLTS_E);
							}
							if (!utils.bankContains(ItemID.DIAMOND_DRAGON_BOLTS_E, 1) && utils.bankContains(ItemID.DIAMOND_BOLTS_E, 1)){
								withdrawAllItem(ItemID.DIAMOND_BOLTS_E);
							}
							timeout = tickDelay();
							break;
						case WITHDRAW_FOOD1:
							withdrawAllItem(configvk.foodID());
							timeout = tickDelay();
							break;
						case WITHDRAW_FOOD2:
							timeout = 2;
							break;
						case MOVING:
							//utils.handleRun(30, 20);
							timeout = tickDelay();
							break;
						case DRINK_ANTIVENOM:
							WidgetItem ven = GetAntiVenomItem();
							if (ven != null) {
								utils.useItem(ven.getId(), "drink");
								//clientThread.invoke(() -> client.invokeMenuAction("Drink", "<col=ff9040>Potion", ven.getId(), MenuAction.CC_OP.getId(), ven.getIndex(), WidgetInfo.INVENTORY.getId()));
							}
							hasVenomed = true;
							timeout = 3;
							break;
						case DRINK_COMBAT:
							leavePortal = false;
							WidgetItem Cpot = GetCombatItem();
							if (Cpot != null) {
								utils.useItem(Cpot.getId(), "drink");
								//clientThread.invoke(() -> client.invokeMenuAction("Drink", "<col=ff9040>Potion", Cpot.getId(), MenuAction.CC_OP.getId(), Cpot.getIndex(), WidgetInfo.INVENTORY.getId()));
							}
							break;
						case DRINK_PRAY:
							WidgetItem Ppot = getRestoreItem();
							if (Ppot != null) {
								utils.useItem(Ppot.getId(), "drink");
							}
							break;
						case EAT_FOOD:
							leavePortal = false;
							WidgetItem food = GetFoodItem();
							if (food != null) {
								utils.useItem(food.getId(), "eat");
								//clientThread.invoke(() -> client.invokeMenuAction("", "", food.getId(), MenuAction.CC_OP.getId(), food.getIndex(), WidgetInfo.INVENTORY.getId()));
							}
							break;
						case DRINK_RANGE:
							leavePortal = false;
							WidgetItem Rpot = GetRangedItem();
							if (Rpot != null) {
								utils.useItem(Rpot.getId(), "drink");
							}
							//timeout = tickDelay();
							break;
						case DRINK_ANTIFIRE:
							leavePortal = false;
							WidgetItem overload = GetAntifireItem();
							if (overload != null) {
								utils.useItem(overload.getId(), "drink");
							}
							//timeout = tickDelay();
							break;
						case WALK_FIRST:
							if (utils.isBankOpen()) {
								Widget close = client.getWidget(12, 2).getChild(11);
								utils.clickWidget(close);
								utils.sendGameMessage("Closing Bank");
								timeout = tickDelay();
								break;
							}
							if (configvk.useScrolls()) {
								if (client.getWidget(217, 5) != null){// && client.getWidget(217, 5).getText().contains("continue")) {
									currentEntry = "Continue";
									Widget Continue = client.getWidget(217, 5);
									utils.clickWidget(Continue);
									//utils.pressKey(KeyCode.KC_SPACE);
									utils.sendGameMessage("Continuing");
									break;
								}
								if (client.getWidget(231, 5) != null){// && client.getWidget(217, 5).getText().contains("continue")) {
									currentEntry = "Continue";
									Widget Continue = client.getWidget(231, 5);
									utils.clickWidget(Continue);
									//utils.pressKey(KeyCode.KC_SPACE);
									utils.sendGameMessage("Continuing");
									break;
								}
								else {
									NPC banker = utils.findNearestNpc(3843);
									currentEntry = "Bank";
									utils.interactNPC(banker);
									utils.sendGameMessage("Finding Banker");
									timeout = 1;
									break;
								}
							}
							else {
								currentEntry = "Break";
								WidgetItem tab = utils.getInventoryWidgetItem(Collections.singletonList(8013));
								utils.useItem(tab.getId(), "outside");
								//clientThread.invoke(() -> client.invokeMenuAction("", "", 8013, MenuAction.ITEM_THIRD_OPTION.getId(), utils.getInventoryWidgetItem(Collections.singletonList(8013)).getIndex(), WidgetInfo.INVENTORY.getId()));
								banked = false;
								leavePortal = true;
								utils.sendGameMessage("Using tab (bank)");
							}
							timeout = tickDelay();
							break;
						case WALK_SECOND:
							if (configvk.useScrolls()) {
								currentEntry = "Teleport";
								WidgetItem Scroll = utils.getInventoryWidgetItem(Collections.singletonList(ItemID.LUNAR_ISLE_TELEPORT));
								utils.useItem(Scroll.getId(), "teleport");
							}
							else {
								currentEntry = "Break";
								WidgetItem tab2 = utils.getInventoryWidgetItem(Collections.singletonList(8013));
								utils.useItem(tab2.getId(), "break");
							}
							if (reBank) {
								reBank = false;
								timeout = 10;
							}
							else {
								timeout = tickDelay();
							}
							break;
						case WALK_THIRD:
							utils.walkToMinimapTile(new WorldPoint(2652, 3664, 0));
							timeout = tickDelay();
							break;
						case USE_BOAT:
							if (hasDied)  {
								if (client.getWidget(602, 6) != null) {
									if (firstUnlock) {
										currentEntry = "Unlock";
										Widget UNLOCK = client.getWidget(602, 6);
										utils.clickWidget(UNLOCK);
										firstUnlock = false;
										timeout = 3;
										break;
									}
									if (!firstUnlock) {
										currentEntry = "Take-All";
										Widget UNLOCK = client.getWidget(602, 6);
										utils.clickWidget(UNLOCK);
										firstUnlock = true;
										hasDied = false;
										reBank = true;
										timeout = 6;
										break;
									}
								}
								NPC Torfinn = utils.findNearestNpc(10405);
								if (Torfinn == null) {
									currentEntry = "Walk here";
									walk.walkTileMM(new WorldPoint(2646, 3682, 0));
								}
								if (Torfinn.getWorldArea().distanceTo(client.getLocalPlayer().getWorldArea()) < 5) {
									currentEntry = "Collect";
									utils.interactNPC(Torfinn);
								}
								break;
							}

							leavePortal = false;
							GameObject boat = utils.findNearestGameObject(29917);
							if (boat != null && boat.getConvexHull() != null) {
								currentEntry = "Travel";
								utils.useGameObjectDirect(boat);
							}
							else {
								currentEntry = "Walk here";
								walk.walkTileMM(new WorldPoint(2646, 3682, 0));
							}
							timeout = tickDelay();
							break;
						case FIND_BANK:
							openBank();
							timeout = tickDelay();
							break;
						case DEPOSIT_ITEMS:
							timeout = tickDelay();
							break;
						case LOOT_ITEMS:
							leavePortal = false;
							lootItem(loot);
							//timeout = tickDelay();
							break;
						case JUMP_OBSTACLE:
							if (client.getLocalPlayer().getWorldArea().distanceTo(utils.findNearestGameObject(31990).getWorldLocation()) >= 3){
								currentEntry = "Walk here";
								walk.walkTileMM(utils.findNearestGameObject(31990).getWorldLocation());
							}
							else {
								currentEntry = "Climb-over";
								utils.useGameObject(31990, 3, sleepDelay());
							}
							timeout = tickDelay();
							break;
					}
				}
	}

	/*static void invokeMenuAction(String entry, String target, int param0, int actionType, int targetID, int index) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		invoke(param0, targetID, actionType, index, -1, entry, target, 0, 0);
	}

	static void invoke(int var0, int var1, int var2, int var3, int var4, String var5, String var6, int var7, int var8) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
			Class clazz = Class.forName("lk");
			Method method = clazz.getDeclaredMethod("ku", int.class, int.class, int.class, int.class, int.class,
					String.class, String.class, int.class, int.class, int.class);
			method.setAccessible(true);
			method.invoke(null, var0, var1, var2, var3, var4, var5, var6, var7, var8, 1849187210);

	}*/
	boolean firstUnlock = true;
	@Subscribe
	private void onItemSpawned(ItemSpawned event) {
		TileItem item = event.getItem();
		String itemName = client.getItemDefinition(item.getId()).getName().toLowerCase();

		if (lootableItems.stream().anyMatch(itemName.toLowerCase()::contains)) {             // || client.getItemDefinition(event.getItem().getId()).getName() == "Dragon bones" || client.getItemDefinition(event.getItem().getId()).getName() == "Draconic visage") {
			loot.add(event.getTile().getWorldLocation());
		}
	}
	@Subscribe
	private void onItemDespawned(ItemDespawned event) {
		loot.remove(event.getTile().getWorldLocation());
	}

	private int calculateHealth(NPC target, Integer maxHealth)
	{
		if (target == null || target.getName() == null)
		{
			return -1;
		}

		final int healthScale = target.getHealthScale();
		final int healthRatio = target.getHealthRatio();
		//final Integer maxHealth = 750;

		if (healthRatio < 0 || healthScale <= 0 || maxHealth == null)
		{
			return -1;
		}

		return (int)((maxHealth * healthRatio / healthScale) + 0.5f);
	}


	@Subscribe
	private void onClientTick(ClientTick event)
	{

		if (acidSpots.size() != lastAcidSpotsSize)
		{
			if (acidSpots.size() == 0)
			{
				acidFreePath.clear();
			}
			else
			{
				calculateAcidFreePath();
			}

			lastAcidSpotsSize = acidSpots.size();
		}

		if (utils.isItemEquipped(Collections.singleton(ItemID.SLAYERS_STAFF)) && client.getLocalPlayer().getInteracting() != utils.findNearestNpc("Zombified Spawn") && utils.findNearestNpc("Zombified Spawn") != null) {
			currentEntry = "Attack";
			utils.attackNPC("Zombified Spawn");
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) {
		if (vorkath != null) {
			final Actor actor = event.getActor();
			if (actor.getAnimation() == 7950 && actor.getName().contains("Vorkath")) {
				Widget widget = client.getWidget(10485775);

				if (widget != null) {
					bounds = widget.getBounds();
				}


			}
			if (actor.getAnimation() == 7949 && actor.getName().contains("Vorkath")) {
				if (client.getVar(Varbits.QUICK_PRAYER) == 1) {
					utils.clickWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
					//clientThread.invoke(() -> client.invokeMenuAction("Deactivate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
				}
			}
		}
	}

	@Subscribe
	private void onProjectileSpawned(ProjectileSpawned event) {
		if (this.client.getGameState() == GameState.LOGGED_IN) {
			final Projectile projectile = event.getProjectile();

			final WorldPoint loc = client.getLocalPlayer().getWorldLocation();

			final LocalPoint localLoc = LocalPoint.fromWorld(client, loc);

			if (projectile.getId() == ProjectileID.VORKATH_BOMB_AOE) {
				noBomb = false;
			}
			if (projectile.getId() == ProjectileID.VORKATH_ICE) {
				noBomb2 = false;
				if (client.getLocalPlayer().getInteracting() != null) {
					utils.walkToMinimapTile(loc);
				}
			}
		}
	}

	@Subscribe
	private void onProjectileMoved(ProjectileMoved event) {
		final Projectile proj = event.getProjectile();
		final LocalPoint loc = event.getPosition();
		final WorldPoint location = WorldPoint.fromLocal(client, loc);
		final LocalPoint playerLocation = client.getLocalPlayer().getLocalLocation();
		final WorldPoint loc1 = client.getLocalPlayer().getWorldLocation();
		final LocalPoint localLoc = LocalPoint.fromWorld(client, loc1);
		if (proj.getId() == ProjectileID.VORKATH_POISON_POOL_AOE) {
			addAcidSpot(WorldPoint.fromLocal(client, loc));
		}
		if (proj.getId() == ProjectileID.VORKATH_ICE) {
			noBomb2 = false;
			if (client.getLocalPlayer().getInteracting() != null) {
				utils.walkToMinimapTile(loc1);
			}
		}
		if (proj.getId() == ProjectileID.VORKATH_BOMB_AOE) {
			noBomb = false;
		}
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event) {
		final NPC npc = event.getNpc();
		if (npc.getName() == null) {
			return;
		}

		if (npc.getName().equals("Vorkath")) {
			vorkath = event.getNpc();
		}

		if (npc.getName().equals("Zombified Spawn")) {
			noBomb2 = false;
		}
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event) {
		final NPC npc = event.getNpc();
		if (npc.getName() == null) {
			return;
		}
		Widget widget = client.getWidget(10485775);
		if (widget != null) {
			bounds = widget.getBounds();
		}
		if (npc.getName().equals("Vorkath")) {
			vorkath = null;
			if (client.getVar(Varbits.QUICK_PRAYER) == 1) {
				utils.clickWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
				//clientThread.invoke(() -> client.invokeMenuAction("Deactivate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event) throws IOException, ClassNotFoundException {
		loot.clear();
		GameState gamestate = event.getGameState();

		if (gamestate == GameState.LOADING && inFight) {
			reset();
		}
	}
	private String currentEntry = null;
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
			utils.sendGameMessage("Consumed Incorrect Walk Click");
			event.consume();
		}
		if (!event.getMenuOption().contains(currentEntry) && !utils.isBankOpen()) {
			event.consume();
			utils.sendGameMessage("Consumed Incorrect Object Click");
		}
		//core.sendGameMessage("O:" + event.getMenuOption() + "T:" +event.getMenuTarget() + "E:" + event.getMenuEntry());
		//core.sendGameMessage(event.toString());	//Debugging
	}

	private static final List<Integer> regions = Arrays.asList(7513, 7514, 7769, 7770);
	public static boolean isInPOH(Client client) {
		return Arrays.stream(client.getMapRegions()).anyMatch(regions::contains);
	}
	private boolean Poisoned = false;
	@Subscribe
	private void onChatMessage(ChatMessage event) {

			if (event.getType() != ChatMessageType.GAMEMESSAGE)
			{
				return;
			}

			Widget widget = client.getWidget(10485775);
			if (widget != null)
			{
				bounds = widget.getBounds();
			}

			String prayerMessage = ("Your prayers have been disabled!");
			String poisonMessage = ("You have been poisoned by venom!");
			String poisonMessageNV = ("You have been poisoned!");
			String frozenMessage = ("You have been frozen!");
			String spawnExplode = ("The spawn violently explodes, unfreezing you as it does so.");
			String unfrozenMessage = ("You become unfrozen as you kill the spawn.");
			String deathMessage = ("Oh dear, you are dead!");
			String antiPoison = ("You drink some of your super antivenom potion");
			String antiPoison2 = ("You drink some of your antipoison potion.");
			if ((event.getMessage().equals(prayerMessage) || event.getMessage().contains(prayerMessage)))
			{
				utils.clickWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
				//clientThread.invoke(() -> client.invokeMenuAction("Activate", "Quick-prayers", 1, MenuAction.CC_OP.getId(), -1, 10485775));
			}
			if ((event.getMessage().contains(deathMessage) || event.getMessage().equals(deathMessage))){
				utils.logout();
				//clientThread.invoke(() -> client.invokeMenuAction("", "", 1, MenuAction.CC_OP.getId(), -1, 11927560));
			}
			if ((event.getMessage().equals(antiPoison)|| event.getMessage().contains(antiPoison)) || (event.getMessage().equals(antiPoison2) || event.getMessage().contains(antiPoison2))) {
				hasVenomed = true;
				Poisoned = false;
			}
			if (((event.getMessage().equals(frozenMessage))))
			{
				noBomb = false;
				noBomb2 = false;
			}
			if (((event.getMessage().equals(poisonMessage))))
			{
				hasVenomed = false;
				Poisoned = true;
			}
			if (((event.getMessage().equals(poisonMessageNV))))
			{
				hasVenomed = false;
				Poisoned = true;
			}
			if ((event.getMessage().equals(spawnExplode) || (event.getMessage().equals(unfrozenMessage))))
			{
				noBomb = true;
				noBomb2 = true;
				//if (isInVorkath()) {
				//	utils.attackNPCDirect(vorkath);
				//}
			}

	}

	private void addAcidSpot(WorldPoint acidSpotLocation)
	{
		if (!acidSpots.contains(acidSpotLocation))
		{
			acidSpots.add(acidSpotLocation);
		}
	}

	private void calculateAcidFreePath()
	{
		acidFreePath.clear();

		if (vorkath == null)
		{
			return;
		}

		final int[][][] directions = {
				{
						{0, 1}, {0, -1} // Positive and negative Y
				},
				{
						{1, 0}, {-1, 0} // Positive and negative X
				}
		};

		List<WorldPoint> bestPath = new ArrayList<>();
		double bestClicksRequired = 99;

		final WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
		final WorldPoint vorkLoc = vorkath.getWorldLocation();
		final int maxX = vorkLoc.getX() + 14;
		final int minX = vorkLoc.getX() - 8;
		final int maxY = vorkLoc.getY() - 1;
		final int minY = vorkLoc.getY() - 8;

		// Attempt to search an acid free path, beginning at a location
		// adjacent to the player's location (including diagonals)
		for (int x = -1; x < 2; x++)
		{
			for (int y = -1; y < 2; y++)
			{
				final WorldPoint baseLocation = new WorldPoint(playerLoc.getX() + x,
						playerLoc.getY() + y, playerLoc.getPlane());

				if (acidSpots.contains(baseLocation) || baseLocation.getY() < minY || baseLocation.getY() > maxY)
				{
					continue;
				}

				// Search in X and Y direction
				for (int d = 0; d < directions.length; d++)
				{
					// Calculate the clicks required to start walking on the path
					double currentClicksRequired = Math.abs(x) + Math.abs(y);
					if (currentClicksRequired < 2)
					{
						currentClicksRequired += Math.abs(y * directions[d][0][0]) + Math.abs(x * directions[d][0][1]);
					}
					if (d == 0)
					{
						// Prioritize a path in the X direction (sideways)
						currentClicksRequired += 0.5;
					}

					List<WorldPoint> currentPath = new ArrayList<>();
					currentPath.add(baseLocation);

					// Positive X (first iteration) or positive Y (second iteration)
					for (int i = 1; i < 25; i++)
					{
						final WorldPoint testingLocation = new WorldPoint(baseLocation.getX() + i * directions[d][0][0],
								baseLocation.getY() + i * directions[d][0][1], baseLocation.getPlane());

						if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY || testingLocation.getY() > maxY
								|| testingLocation.getX() < minX || testingLocation.getX() > maxX)
						{
							break;
						}

						currentPath.add(testingLocation);
					}

					// Negative X (first iteration) or positive Y (second iteration)
					for (int i = 1; i < 25; i++)
					{
						final WorldPoint testingLocation = new WorldPoint(baseLocation.getX() + i * directions[d][1][0],
								baseLocation.getY() + i * directions[d][1][1], baseLocation.getPlane());

						if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY || testingLocation.getY() > maxY
								|| testingLocation.getX() < minX || testingLocation.getX() > maxX)
						{
							break;
						}

						currentPath.add(testingLocation);
					}

					if (currentPath.size() >= this.acidFreePathLength && currentClicksRequired < bestClicksRequired
							|| (currentClicksRequired == bestClicksRequired && currentPath.size() > bestPath.size()))
					{
						bestPath = currentPath;
						bestClicksRequired = currentClicksRequired;
					}
				}
			}
		}

		if (bestClicksRequired != 99)
		{
			acidFreePath = bestPath;
		}
	}

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event) {
		final GameObject obj = event.getGameObject();

		if (obj.getId() == ObjectID.ACID_POOL || obj.getId() == ObjectID.ACID_POOL_32000) {
			addAcidSpot(obj.getWorldLocation());
		}
	}
	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		final GameObject obj = event.getGameObject();
		if (obj.getId() == ObjectID.ACID_POOL || obj.getId() == ObjectID.ACID_POOL_32000) {
			acidSpots.remove(obj.getWorldLocation());
		}
	}
}