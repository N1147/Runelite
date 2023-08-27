package net.runelite.client.plugins.azulrah;

import com.google.common.base.Preconditions;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.queries.GroundObjectQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.Utils.Core;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;

import static java.awt.event.KeyEvent.VK_F2;

//@Extension
//@PluginDependency(PUtils.class)
@PluginDescriptor(
	name = "AZulrah",
	description = "Anarchise' Auto Zulrah",
	tags = {"anarchise","zulrah","aplugins"},
	enabledByDefault = false
)
public class AZulrahPlugin extends Plugin
{
	private int nextRestoreVal = 0;

	@Inject
	private Client client;

	@Provides
	AZulrahConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AZulrahConfig.class);
	}

	@Inject
	private AZulrahConfig configph;

	@Inject
	private ClientThread clientThread;


	private Rectangle bounds;

	private int timeout;

	private  Map<LocalPoint, Integer> projectilesMap = new HashMap<LocalPoint, Integer>();
	private  Map<GameObject, Integer> toxicCloudsMap = new HashMap<GameObject, Integer>();

	private int lastAttackTick = -1;

	LocalPoint standPos;
	private WorldPoint lastLocation = new WorldPoint(0, 0, 0);

	@Getter(AccessLevel.PACKAGE)
	private  List<WorldPoint> obstacles = new ArrayList<>();

	@Getter
	private  Map<LocalPoint, Projectile> poisonProjectiles = new HashMap<>();

	@Inject
	private ItemManager itemManager;

	@Inject
	private Core utils;

	private boolean inFight;
	private boolean cursed;
	private Prayer prayerToClick;
	private Random r = new Random();
	public AZulrahPlugin(){
		inFight = false;
	}

	//List<TileItem> loot = new ArrayList<>();
	List<String> lootableItems = new ArrayList<>();
	List<String> withdrawList = new ArrayList<>();
	String[] list;
	String[] Loot;
	private Prayer prayer;
	@Inject
	private KeyManager keyManager;
	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private OverlayManager overlayManager;

	ZulrahAttributes zulrahAttributes;
	ZulrahData zulrahData;
	ZulrahPhase zulrahPhase;

	Instant botTimer;
	boolean noBomb = true;
	boolean noBomb2 = true;
	private NPC zulrahNpc = null;
	public AZulrahState state;
	private NPC zulrah = null;
	private int stage = 0;
	private int phaseTicks = -1;
	private int attackTicks = -1;
	private int acidFreePathLength = 3;
	private int totalTicks = 0;
	private RotationType currentRotation = null;
	private List<RotationType> potentialRotations = new ArrayList<RotationType>();
	private static boolean flipStandLocation = false;
	private static boolean flipPhasePrayer = false;
	private static boolean zulrahReset = false;
	private  Collection<NPC> snakelings = new ArrayList<NPC>();
	private boolean holdingSnakelingHotkey = false;
	private Counter zulrahTotalTicksInfoBox;

	@Override
	protected void startUp()
	{
		reset();
	}

	private void reset() {
		loot.clear();
		lootableItems.clear();
		withdrawList.clear();
		Loot = configph.lootNames().toLowerCase().split("\\s*,\\s*");
		if (!configph.lootNames().isBlank()) {
			lootableItems.addAll(Arrays.asList(Loot));
		}
		banked = false;
		startTeaks = false;
		zulrahNpc = null;
		stage = 0;
		phaseTicks = -1;
		attackTicks = -1;
		totalTicks = 0;
		currentRotation = null;
		potentialRotations.clear();
		projectilesMap.clear();
		toxicCloudsMap.clear();
		flipStandLocation = false;
		flipPhasePrayer = false;
		zulrahReset = false;
		//clearSnakelingCollection();
		holdingSnakelingHotkey = false;
		lastAttackTick = -1;
		inFight = false;
		prayerToClick = null;
		alreadyBanked = false;
		zulrah = null;
		state = null;
		botTimer = null;
		/*overlayManager.remove(phaseOverlay);
		overlayManager.remove(prayerHelperOverlay);
		overlayManager.remove(prayerMarkerOverlay);
		overlayManager.remove(sceneOverlay);*/
		//overlayManager.remove(zulrahOverlay);
	}
	@Override
	protected void shutDown() {
		reset();
	}

	private void resetZul() {
		zulrahNpc = null;
		stage = 0;
		phaseTicks = -1;
		attackTicks = -1;
		totalTicks = 0;
		currentRotation = null;
		potentialRotations.clear();
		projectilesMap.clear();
		toxicCloudsMap.clear();
		flipStandLocation = false;
		flipPhasePrayer = false;
		zulrahReset = false;
		//clearSnakelingCollection();
		holdingSnakelingHotkey = false;
		lastAttackTick = -1;
		inFight = false;
		prayerToClick = null;
		zulrah = null;
		banked = false;
		//zulrahReset = true;
	}


	//private static  BufferedImage CLOCK_ICON = ImageUtil.getResourceStreamFromClass(AZulrahPlugin.class, "clock.png");
	private  BiConsumer<RotationType, RotationType> phaseTicksHandler = (current, potential) -> {
		if (zulrahReset)
		{
			phaseTicks = 38;
		}
		else
		{
			ZulrahPhase p = current != null ? getCurrentPhase((RotationType)((Object)current)) : getCurrentPhase((RotationType)((Object)potential));
			Preconditions.checkNotNull(p, "Attempted to set phase ticks but current Zulrah phase was somehow null. Stage: " + stage);
			phaseTicks = p.getAttributes().getPhaseTicks();
		}
	};

	private static  List<Integer> regions = Arrays.asList(7513, 7514, 7769, 7770);
	private static  List<Integer> regionz = Arrays.asList(9007, 9008);
	private static boolean isInPOH(Client client) {return Arrays.stream(client.getMapRegions()).anyMatch(regions::contains);}
	private static boolean isInZulrah(Client client)
	{
		return Arrays.stream(client.getMapRegions()).anyMatch(regionz::contains);
	}
	private void openBank() {
		GameObject bankTarget = utils.findNearestBankNoDepositBoxes();
		if (bankTarget != null) {
			utils.useGameObjectDirect(bankTarget);
		}
	}
	List<WorldPoint> loot = new ArrayList<>();
	private void lootItem(List<WorldPoint> itemList) {
		if (itemList.get(0) != null) {
			utils.walk(itemList.get(0));
			//clientThread.invoke(() -> client.invokeMenuAction("", "", lootItem.getId(), MenuAction.GROUND_ITEM_THIRD_OPTION.getId(), lootItem.getTile().getSceneLocation().getX(), lootItem.getTile().getSceneLocation().getY()));
		}
	}

	public AZulrahState getState()
	{
		if (timeout > 0)
		{
			return AZulrahState.TIMEOUT;
		}
		if(utils.isBankOpen()){
			return getBankState();
		}
		//else if(client.getLocalPlayer().getAnimation()!=-1){
		//	return AVorkathState.ANIMATING;
		//}
		else {
			return getStates();
		}
	}
	Player player;
	WorldArea ZULRAH_BOAT = new WorldArea(new WorldPoint(2192, 3045, 0), new WorldPoint(2221, 3068, 0));
	WorldArea ZULRAH_ISLAND = new WorldArea(new WorldPoint(2145, 3065, 0), new WorldPoint(2156, 3076, 0));
	WorldArea ZULRAH_ISLAND2 = new WorldArea(new WorldPoint(2158, 3066, 0), new WorldPoint(2193, 3086, 0));
	WorldPoint ZULRAHPOINT = new WorldPoint(2178, 3068, 0);
	WorldArea ZULRAH_ISLAND3 = new WorldArea(new WorldPoint(2172, 3063, 0), new WorldPoint(2182, 3075, 0));
	WorldPoint ZULRAHPOINT2 = new WorldPoint(2195, 3059, 0);
	WorldArea ZULRAH_ISLAND4 = new WorldArea(new WorldPoint(2157, 3068, 0), new WorldPoint(2166, 3078, 0));

	WorldArea EDGEVILLE_BANK = new WorldArea(new WorldPoint(3082, 3485, 0), new WorldPoint(3100, 3502, 0));
	LocalPoint standPos1;
	ZulrahData data;
	ZulrahAttributes attributes;

	private AZulrahState getStates(){
		NPC bs = utils.findNearestNpc(2042,2043,2044);
		if (player.getWorldArea().intersectsWith(EDGEVILLE_BANK) && utils.inventoryContains(configph.foodID()) && banked && alreadyBanked){
			utils.sendGameMessage("EXIT BANK 2");
			return AZulrahState.WALK_SECOND;
		}
		if (player.getWorldArea().intersectsWith(EDGEVILLE_BANK) && !banked){
			loot.clear();
			utils.sendGameMessage("Finding Bank");
			return AZulrahState.FIND_BANK;
		}
		if (player.getWorldArea().intersectsWith(EDGEVILLE_BANK) && banked && !utils.isBankOpen()){
			banked = false;
			loot.clear();
			utils.sendGameMessage("Finding Bank 2");
			return AZulrahState.FIND_BANK;
		}
		if (!configph.MageOnly() &&client.getBoostedSkillLevel(Skill.RANGED) <= client.getRealSkillLevel(Skill.RANGED) && utils.findNearestGameObject(10068) != null){
			utils.sendGameMessage("Drink Ranged");
			return AZulrahState.DRINK_RANGE;
		}
		if (!configph.RangedOnly() && !configph.nomagepots() && client.getBoostedSkillLevel(Skill.MAGIC) <= client.getRealSkillLevel(Skill.MAGIC) && utils.findNearestGameObject(10068) != null){
			utils.sendGameMessage("Drink Magic");
			return AZulrahState.DRINK_MAGIC;
		}
		if (configph.antivenomplus() && client.getVar(VarPlayer.POISON) > 0 && utils.findNearestGameObject(10068) != null) {
			utils.sendGameMessage("Drink Venom");
			return AZulrahState.DRINK_ANTIVENOM;
		}
		if (player.getWorldArea().intersectsWith(ZULRAH_BOAT) && utils.findNearestGameObject(10068) != null){
			utils.sendGameMessage("Use Boat");
			return AZulrahState.USE_BOAT;
		}
		if (player.getWorldArea().intersectsWith(ZULRAH_ISLAND)) {
			alreadyBanked = false;
			utils.useGroundObject(10663, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), sleepDelay());
			utils.sendGameMessage("Hopping Obstacle");
		}
		if (player.getWorldArea().intersectsWith(ZULRAH_ISLAND3)) {
			utils.sendGameMessage("Walk Point 1");
			return AZulrahState.WALK_FOURTH;
		}
		if (player.getWorldArea().intersectsWith(ZULRAH_ISLAND4)) {
			alreadyBanked = false;
			utils.sendGameMessage("Walk Point 2");
			return AZulrahState.WALK_THIRD;
		}
		if (!loot.isEmpty() && !utils.inventoryFull() && isInZulrah(client)){
			utils.sendGameMessage("Looting");
			return AZulrahState.LOOT_ITEMS;
		}
		if (utils.inventoryContains(12934) && loot.isEmpty() && !isInPOH(client) && isInZulrah(client)){
			utils.sendGameMessage("Tele Tab");
			return AZulrahState.TELE_TAB;
		}
		if (getRestoreItem() == null && isInZulrah(client)){
			utils.sendGameMessage("Tele Tab 2");
			return AZulrahState.TELE_TAB;
		}
		if (!utils.inventoryContains(configph.foodID()) && client.getBoostedSkillLevel(Skill.HITPOINTS) < 50 && isInZulrah(client)){
			utils.sendGameMessage("Tele Tab 3");
			return AZulrahState.TELE_TAB;
		}
		if (utils.inventoryContains(configph.foodID()) && utils.inventoryFull() && !loot.isEmpty() && !isInPOH(client) && isInZulrah(client)){
			utils.sendGameMessage("Eat Food");
			return AZulrahState.EAT_FOOD;
		}
		if (client.getVar(Varbits.PRAYER_PROTECT_FROM_MAGIC) != 0 && isInPOH(client)) {
			activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);
		}
		if (client.getVar(Varbits.PRAYER_PROTECT_FROM_MISSILES) != 0 && isInPOH(client)) {
			activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);
		}
		if (client.getVar(Varbits.PRAYER_EAGLE_EYE) != 0 && isInPOH(client)) {
			activatePrayer(WidgetInfo.PRAYER_EAGLE_EYE);
		}
		if (client.getVar(Varbits.PRAYER_MYSTIC_MIGHT) != 0 && isInPOH(client)) {
			activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
		}
		if (client.getVar(Varbits.PRAYER_AUGURY) != 0 && isInPOH(client)) {
			activatePrayer(WidgetInfo.PRAYER_AUGURY);
		}
		if (client.getVar(Varbits.PRAYER_RIGOUR) != 0 && isInPOH(client)) {
			activatePrayer(WidgetInfo.PRAYER_RIGOUR);
		}


		if (isInZulrah(client) && client.getBoostedSkillLevel(Skill.HITPOINTS) > configph.hpToEat() && client.getBoostedSkillLevel(Skill.PRAYER) > configph.prayToDrink()) {
			//if ((client.getVarcIntValue(VarClientInt.INVENTORY_TAB) == 3 && client.getWidget(WidgetInfo.BANK_CONTAINER) == null)) {
			//	utils.pressKey(VK_F2);
			//	utils.sendGameMessage("Opening Prayers");
			//	return AZulrahState.TIMEOUT;
			//}
			if (client.getVar(Prayer.RIGOUR.getVarbit()) == 0 && configph.Rigour() && isInZulrah(client) && configph.RangedOnly() && !configph.MageOnly()) {
				activatePrayer(WidgetInfo.PRAYER_RIGOUR);
				utils.sendGameMessage("Opening Prayers 2");
				return AZulrahState.TIMEOUT;
			}
			if (client.getVar(Prayer.EAGLE_EYE.getVarbit()) == 0 && !configph.Rigour() && isInZulrah(client) && configph.RangedOnly() && !configph.MageOnly()) {
				activatePrayer(WidgetInfo.PRAYER_EAGLE_EYE);
				utils.sendGameMessage("Opening Prayers 3");
				return AZulrahState.TIMEOUT;
			}
			if (client.getVar(Prayer.AUGURY.getVarbit()) == 0 && configph.Augury() && isInZulrah(client) && !configph.RangedOnly() && configph.MageOnly()) {
				activatePrayer(WidgetInfo.PRAYER_AUGURY);
				utils.sendGameMessage("Opening Prayers 4");
				return AZulrahState.TIMEOUT;
			}
			if (client.getVar(Prayer.MYSTIC_MIGHT.getVarbit()) == 0 && !configph.Augury() && isInZulrah(client) && !configph.RangedOnly() && configph.MageOnly()) {
				activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
				utils.sendGameMessage("Opening Prayers 5");
				return AZulrahState.TIMEOUT;
			}
			if (currentPrayer != null && client.getVar(currentPrayer.getVarbit()) == 0) {
				if ((client.getVarcIntValue(VarClientInt.INVENTORY_TAB) == 3 && client.getWidget(WidgetInfo.BANK_CONTAINER) == null)) {
					utils.pressKey(VK_F2);
					utils.sendGameMessage("Opening Prayers 6");
					return AZulrahState.TIMEOUT;
				}
				activatePrayer(currentPrayer.getWidgetInfo());
				utils.sendGameMessage("Activating Prayer");
				return AZulrahState.TIMEOUT;
			}
		}
		if (isInPOH(client) && client.getBoostedSkillLevel(Skill.PRAYER) < client.getRealSkillLevel(Skill.PRAYER) && configph.usePOHPool()){
			utils.sendGameMessage("Drink Pool");
			return AZulrahState.DRINK_POOL;
		}
		if (isInPOH(client) && !alreadyBanked && configph.fairyRings()){
			utils.sendGameMessage("Edge Tele");
			return AZulrahState.TELE_EDGE;
		}
		if (isInPOH(client) && alreadyBanked && configph.fairyRings()){
			utils.sendGameMessage("Fairy Ring");
			return AZulrahState.FAIRY_RING;
		}
		if (isInPOH(client) && !configph.fairyRings()){
			utils.sendGameMessage("Edge Tele 2");
			return AZulrahState.TELE_EDGE;
		}
		if (standPos != null && client.getLocalPlayer().getLocalLocation().distanceTo(standPos) >= 1 && isInZulrah(client) && !utils.isMoving()) {
			return AZulrahState.WALK_SAFE;
		}
		if (utils.inventoryContains(configph.foodID()) && client.getBoostedSkillLevel(Skill.HITPOINTS) <= configph.hpToEat() && !isInPOH(client) && isInZulrah(client)){
			utils.sendGameMessage("Eat Food 4");
			return AZulrahState.EAT_FOOD;
		}
		if (!configph.serphelm() && client.getVar(VarPlayer.POISON) < 1 && utils.inventoryContains(ItemID.ANTIDOTE1_5958, ItemID.ANTIDOTE2_5956, ItemID.ANTIDOTE3_5954, ItemID.ANTIDOTE4_5952, ItemID.ANTIVENOM1_12919 ,ItemID.ANTIVENOM2_12917,ItemID.ANTIVENOM3_12915, ItemID.ANTIVENOM4_12913)) {
			utils.sendGameMessage("Drink Antivenom");
			return AZulrahState.DRINK_ANTIVENOM;
		}
		if (utils.inventoryContains(ItemID.PRAYER_POTION1, ItemID.PRAYER_POTION2, ItemID.PRAYER_POTION3, ItemID.PRAYER_POTION4, ItemID.SUPER_RESTORE1, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE4,
				ItemID.BLIGHTED_SUPER_RESTORE1, ItemID.BLIGHTED_SUPER_RESTORE2, ItemID.BLIGHTED_SUPER_RESTORE3,
				ItemID.BLIGHTED_SUPER_RESTORE4, ItemID.SANFEW_SERUM1, ItemID.SANFEW_SERUM2, ItemID.SANFEW_SERUM3, ItemID.SANFEW_SERUM4)
				&& client.getBoostedSkillLevel(Skill.PRAYER) <= configph.prayToDrink() && !isInPOH(client) && isInZulrah(client)){
			utils.sendGameMessage("Drink Prayer");
			return AZulrahState.DRINK_PRAYER;
		}
		if (!configph.suffering() && utils.inventoryContains(ItemID.RING_OF_RECOIL) && !utils.isItemEquipped(Collections.singleton(ItemID.RING_OF_RECOIL))) {
			utils.useItem(ItemID.RING_OF_RECOIL, "wear");
			return AZulrahState.TIMEOUT;
		}
		if (client.getLocalPlayer().getInteracting() == null && isInZulrah(client)){
			utils.sendGameMessage("Attacking Zulrah");
			return AZulrahState.ATTACK_ZULRAH;
		}

		else return AZulrahState.TIMEOUT;
	}
	private boolean banked = false;

	private AZulrahState getBankState()
	{
		if (player.getWorldArea().intersectsWith(EDGEVILLE_BANK) && utils.inventoryContains(configph.foodID()) && banked){
			alreadyBanked = true;
			utils.sendGameMessage("EXIT BANK 1");
			return AZulrahState.WALK_SECOND;
		}
		if (!banked){
			utils.depositAll();
			banked = true;
			return AZulrahState.DEPOSIT_ITEMS;
		}
		if (configph.fairyRings() && !utils.inventoryContains(772)){
			withdraw1Item(772);
		}
		if (!configph.suffering() && !utils.inventoryContains(ItemID.RING_OF_RECOIL)) {
			return AZulrahState.WITHDRAW_RECOIL;
		}
		if (!configph.RangedOnly() && !configph.nomagepots() &&configph.imbuedheart() && !utils.inventoryContains(20724)){
			return AZulrahState.WITHDRAW_MAGIC;
		}
		if (!configph.RangedOnly() && !configph.nomagepots() && configph.supers() && !configph.imbuedheart() && !utils.inventoryContains(23745)){
			return AZulrahState.WITHDRAW_MAGIC;
		}
		if (!configph.RangedOnly() && !configph.nomagepots() && !configph.supers() && !configph.imbuedheart() && !utils.inventoryContains(3040)){
			return AZulrahState.WITHDRAW_MAGIC;
		}
		if (!configph.MageOnly() && configph.supers() && !utils.inventoryContains(24635)){
			return AZulrahState.WITHDRAW_RANGED;
		}
		if (!configph.MageOnly() && !configph.supers() && !utils.inventoryContains(2444)){
			return AZulrahState.WITHDRAW_RANGED;
		}
		if (!utils.inventoryContains(12913) && configph.antivenomplus() && !configph.serphelm() && !configph.superantipoison()){
			return AZulrahState.WITHDRAW_VENOM;	//
		}
		if (!utils.inventoryContains(5952) && !configph.antivenomplus() && !configph.serphelm() && !configph.superantipoison()){
			return AZulrahState.WITHDRAW_VENOM;	//
		}
		if (!utils.inventoryContains(2448) && !configph.antivenomplus() && !configph.serphelm() && configph.superantipoison()){
			return AZulrahState.WITHDRAW_VENOM;	//
		}
		if (!configph.fairyRings() && !utils.inventoryContains(12938)){
			return AZulrahState.WITHDRAW_TELES;
		}
		if (!utils.inventoryContains(8013)){
			return AZulrahState.WITHDRAW_HOUSE;
		}
		if (!configph.useRestores() && !utils.inventoryContains(2434)){
			return AZulrahState.WITHDRAW_RESTORES;
		}
		if (configph.useRestores() && !utils.inventoryContains(3024)){
			return AZulrahState.WITHDRAW_RESTORES;
		}
		if (!utils.inventoryContains(configph.foodID())){
			return AZulrahState.WITHDRAW_FOOD1;
		}
		else return AZulrahState.TIMEOUT;
	}
	public boolean startTeaks = false;


	@Inject ConfigManager configManager;

	public Prayer currentPrayer;

	boolean alreadyBanked = false;

	private boolean started = false;

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
	private  Set<Integer> SAFE_TILES = Set.of(14501, 14502);
	@Nullable
	public GroundObject findZulrahSafeTile(WorldArea avoidWorldArea, Collection<Integer> safeIds, GroundObject avoidObject)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GroundObjectQuery()
				.idEquals(safeIds)
				.filter(obj ->
						//!obj.getWorldLocation().toWorldArea().intersectsWith(dangerousTiles.getWorldLocation().toWorldArea())
						!obj.getWorldLocation().toWorldArea().intersectsWith(avoidObject.getWorldLocation().toWorldArea())
						&& !obj.getWorldLocation().toWorldArea().intersectsWith(avoidWorldArea))
						//&& !obj.getWorldLocation().toWorldArea().intersectsWith(client.getLocalPlayer().getWorldArea())
						//&& obj.getWorldLocation().toWorldArea().distanceTo(worldArea) >= distFrom
						//&& obj.getWorldLocation().toWorldArea().distanceTo(client.getLocalPlayer().getWorldArea()) >= distFrom)
				.result(client)
				.nearestTo(client.getLocalPlayer());//Arrays.stream(ids).anyMatch(i -> i == item.getId()
	}
	@Subscribe
	private void onGameTick( GameTick event) {

		//if (isInZulrah(client) && zulrahNpc != null) {
		//	utils.sendGameMessage(currentRotation.getRotationName());
		//}
		if (client.getWidget(219, 1) != null) {
			if (player.getWorldArea().intersectsWith(ZULRAH_BOAT) && !utils.isMoving() && client.getWidget(219, 1).getChildren()[0].getText().equals("Return to Zulrah's shrine?")) {
				utils.typeString("1");
				//clientThread.invoke(() -> client.invokeMenuAction("", "", 0, 30, 1, 14352385));
			}
			if (player.getWorldArea().intersectsWith(ZULRAH_BOAT) && !utils.isMoving() && client.getWidget(219, 1).getText().equals("The priestess rows you to Zulrah's shrine, then hurriedly paddles away.")) {
				utils.typeString(" ");
				//clientThread.invoke(() -> client.invokeMenuAction("", "", 0, MenuAction.WIDGET_CONTINUE.getId(), -1, 15007746));
			}
		}
		if (zulrahNpc == null && utils.findNearestNpc("Zulrah") != null) {
			zulrahNpc = utils.findNearestNpc("Zulrah");
		}
		if (zulrahNpc != null && utils.findNearestNpc("Zulrah").getId() != zulrahNpc.getId()) {
			zulrahNpc = utils.findNearestNpc("Zulrah");
		}
		if (zulrahNpc != null && zulrahNpc.getId() == 2042) { //2042 Ranged form, 2044 Magic Form, 2043 Melee Form
			currentPrayer = Prayer.PROTECT_FROM_MISSILES;
		}
		if (zulrahNpc != null && zulrahNpc.getId() == 2044) {
			currentPrayer = Prayer.PROTECT_FROM_MAGIC;
		}
		if (zulrahNpc != null && client.getLocalPlayer().getInteracting() == null && isInZulrah(client) && client.getLocalPlayer().getLocalLocation() == standPos){
			utils.attackNPCDirect(zulrahNpc);
			utils.attackNPCDirect(zulrahNpc); //Double click so no misclicking...
		}
		if (getZulrahNpc() != null) {
			++totalTicks;
			if (attackTicks >= 0) {
				--attackTicks;
			}
			if (phaseTicks >= 0) {
				--phaseTicks;
			}
			if (projectilesMap.size() > 0) {
				projectilesMap.values().removeIf(v -> v <= 0);
				projectilesMap.replaceAll((k, v) -> v - 1);
			}
			if (toxicCloudsMap.size() > 0) {
				toxicCloudsMap.values().removeIf(v -> v <= 0);
				toxicCloudsMap.replaceAll((k, v) -> v - 1);
			}
		}
		//if (attributes != null) {
		//	standPos = attributes.getStandLocation().toLocalPoint();
		//}
		for (ZulrahData data : getZulrahData()) {
			if (data.getCurrentDynamicStandLocation().isPresent()) {
				if (utils.findNearestGroundObject(11700) != null) {
					//if (utils.findNearestGroundObject(11700).getLocalLocation().distanceTo(data.getCurrentDynamicStandLocation().get().toLocalPoint()) < 1 && utils.findNearestGroundObject(11700).getWorldLocation().toWorldArea().intersectsWith(client.getLocalPlayer().getWorldArea())) {
					if (!utils.isMoving() && utils.findNearestGroundObject(11700).getWorldLocation().toWorldArea().intersectsWith(client.getLocalPlayer().getWorldArea())) {
						utils.sendGameMessage("Forcing Safe Tile");
						standPos = findZulrahSafeTile(utils.findNearestGroundObject(11700).getWorldLocation().toWorldArea(), SAFE_TILES, utils.findNearestGroundObject(11700)).getLocalLocation();
					}
				}
				else {
					standPos = data.getCurrentDynamicStandLocation().get().toLocalPoint();
				}
			}
		}

		player = client.getLocalPlayer();
		if (client != null && player != null) {
			state = getState();
			//beforeLoc = player.getLocalLocation();
			//utils.setMenuEntry(null);
			switch (state) {
				case TIMEOUT:
					//utils.handleRun(30, 20);
					timeout--;
					break;
				case FAIRY_RING:
					if (!utils.isItemEquipped(Collections.singleton(772))){
						WidgetItem ITEM = utils.getInventoryWidgetItem(Collections.singletonList(ItemID.DRAMEN_STAFF));
						utils.useItem(ITEM.getId(), "wield");
						//clientThread.invoke(() -> client.invokeMenuAction("Wield", "<col=ff9040>" + 772, 772, MenuAction.CC_OP.getId(), utils.getInventoryWidgetItem(Collections.singletonList(772)).getIndex(), WidgetInfo.INVENTORY.getId()));
					}
					GameObject ring = utils.findNearestGameObject(29228);
					utils.useGameObjectDirect(ring);
					break;
				case WALK_FOURTH:
					utils.walkToMinimapTile(ZULRAHPOINT2);
					timeout = tickDelay();
					break;
				case WALK_THIRD:
					//Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
					if (configph.RangedOnly()) {
						for (WidgetItem item : utils.getAllInventoryItems()) {
							if (("Group 2").equalsIgnoreCase(getTag(item.getId()))) {
								//plugin.entryList.add(new MenuEntry("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId(), false));
								utils.useItem(item.getId(), "wield");
								//clientThread.invoke(() -> client.invokeMenuAction("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId()));
							}
						}
					}
					if (configph.MageOnly()) {
						for (WidgetItem item : utils.getAllInventoryItems()) {
							if (("Group 3").equalsIgnoreCase(getTag(item.getId()))) {
								//plugin.entryList.add(new MenuEntry("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId(), false));
								//clientThread.invoke(() -> client.invokeMenuAction("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId()));
								utils.useItem(item.getId(), "wield");
							}
						}
					}
					if (!configph.RangedOnly()) {
						for (WidgetItem item : utils.getAllInventoryItems()) {
							if (("Group 3").equalsIgnoreCase(getTag(item.getId()))) {
								//plugin.entryList.add(new MenuEntry("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId(), false));
								//clientThread.invoke(() -> client.invokeMenuAction("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId()));
								utils.useItem(item.getId(), "wield");
							}
						}
					}
					utils.walkToMinimapTile(ZULRAHPOINT);
					timeout = tickDelay();
					break;
				case WALK_SAFE:
					//if (!utils.isMoving())
					if (utils.inventoryContains(configph.foodID()) && client.getBoostedSkillLevel(Skill.HITPOINTS) <= configph.hpToEat() && !isInPOH(client) && isInZulrah(client)){
						WidgetItem food = GetFoodItem();
						if (food != null) {
							utils.useItem(food.getId(), "eat");
						}
					}
					WorldPoint stand = WorldPoint.fromLocal(client, standPos);
					utils.walkToMinimapTile(stand);
					//}
					timeout = 1;
					break;
				case WITHDRAW_RECOIL:
					withdraw1Item(ItemID.RING_OF_RECOIL);
					timeout = 1;
					break;
				case WITHDRAW_VENOM:
					if (configph.antivenomplus() && !configph.superantipoison()) {
						withdraw1Item(12913); //anti venom+
					}
					if (!configph.antivenomplus() && !configph.superantipoison()){
						withdraw1Item(5952); // antidote++
					}
					if (configph.superantipoison()) {
						withdraw1Item(2448);	//superantipoison
					}
					timeout = tickDelay();
					break;
				case ATTACK_ZULRAH:
					//	NPC zulrah = utils.findNearestNpc(2042,2043,2044);
					if (utils.inventoryContains(configph.foodID()) && client.getBoostedSkillLevel(Skill.HITPOINTS) <= configph.hpToEat() && !isInPOH(client) && isInZulrah(client)){
						WidgetItem food = GetFoodItem();
						if (food != null) {
							utils.useItem(food.getId(), "eat");
						}
					}
					utils.attackNPCDirect(zulrahNpc);
					utils.attackNPCDirect(zulrahNpc);// Double click again
					//timeout = tickDelay();
					break;
				case TELE_TAB:
					WidgetItem ITEM = utils.getInventoryWidgetItem(Collections.singletonList(8013));
					utils.useItem(ITEM.getId(), "break");
					//clientThread.invoke(() -> client.invokeMenuAction("", "", 8013, MenuAction.CC_OP.getId(), utils.getInventoryWidgetItem(Collections.singletonList(8013)).getIndex(), WidgetInfo.INVENTORY.getId()));
					timeout = tickDelay();
					break;
				case WALK_SECOND:
					if (utils.isBankOpen()) {
						Widget close = client.getWidget(12, 2).getChild(11);
						utils.clickWidget(close);
						utils.sendGameMessage("Closing Bank");
						timeout = tickDelay();
						break;
					}
					if (configph.fairyRings()) {
						WidgetItem ITEM2 = utils.getInventoryWidgetItem(Collections.singletonList(8013));
						utils.useItem(ITEM2.getId(), "break");
						//clientThread.invoke(() -> client.invokeMenuAction("", "", 8013, MenuAction.CC_OP.getId(), utils.getInventoryWidgetItem(Collections.singletonList(8013)).getIndex(), WidgetInfo.INVENTORY.getId()));
					}
					if (!configph.fairyRings()){
						WidgetItem ITEM2 = utils.getInventoryWidgetItem(Collections.singletonList(12938));
						utils.useItem(ITEM2.getId(), "teleport");
						//clientThread.invoke(() -> client.invokeMenuAction("", "", 12938, MenuAction.CC_OP.getId(), utils.getInventoryWidgetItem(Collections.singletonList(12938)).getIndex(), WidgetInfo.INVENTORY.getId()));
					}
					resetZul();
					timeout = tickDelay();
					break;
				case TELE_EDGE:
					resetZul();
					utils.useDecorativeObject(13523, MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), sleepDelay());
					timeout = tickDelay();
					break;
				case DRINK_POOL:
					resetZul();
					GameObject Pool = utils.findNearestGameObject(29240, 29241);
					utils.useGameObjectDirect(Pool);
					timeout = tickDelay();
					break;
				case WITHDRAW_RANGED:
					if (!configph.supers()){
						withdraw1Item(2444);
					}
					if (configph.supers()){
						withdraw1Item(24635);
					}
					timeout = tickDelay();
					break;
				case WITHDRAW_MAGIC:
					if (!configph.supers()&& !configph.imbuedheart()){
						withdraw1Item(3040);
					}
					if (configph.supers() && !configph.imbuedheart()){
						withdraw1Item(23745);
					}
					if (configph.imbuedheart()){
						withdraw1Item(20724);
					}
					timeout = tickDelay();
					break;
				case WITHDRAW_RESTORES:
					if (!configph.useRestores()){
						withdraw5Item(2434);
					}
					if (configph.useRestores()){
						withdraw5Item(3024);
					}
					timeout = tickDelay();
					break;
				case WITHDRAW_TELES:
					//utils.withdrawItemAmount(12938, 10); //zul andra tele
					withdraw5Item(12938);
					timeout = tickDelay();
					break;
				case WITHDRAW_HOUSE:
					withdraw5Item(8013); //house tabs TODO
					//utils.withdrawItem(8013);
					timeout = tickDelay();
					break;
				case WITHDRAW_FOOD1:
					withdrawAllItem(configph.foodID());
					timeout = tickDelay();
					break;
				case WITHDRAW_FOOD2:
					//withdrawItemAmount(configph.foodID2(), configph.foodAmount2(), false);
					timeout = tickDelay();
					break;
				case MOVING:
					//utils.handleRun(30, 20);
					timeout = tickDelay();
					break;
				case DRINK_ANTIVENOM:
					WidgetItem ven = GetAntiVenomItem();
					if (ven != null) {
						utils.useItem(ven.getId(), "drink");
					}
					//timeout = tickDelay();
					break;
				case DRINK_PRAYER:
					WidgetItem Ppot = getRestoreItem();
					if (Ppot != null) {
						utils.useItem(Ppot.getId(), "drink");
					}
					break;
				case DRINK_MAGIC:
					WidgetItem Cpot = GetMagicItem();
					if (Cpot != null) {
						utils.useItem(Cpot.getId(), "drink");
					}
					break;
				case EAT_FOOD:
					WidgetItem food = GetFoodItem();
					if (food != null) {
						utils.useItem(food.getId(), "eat");
					}
					//timeout = 1;
					break;
				case DRINK_RANGE:
					WidgetItem Rpot = GetRangedItem();
					if (Rpot != null) {
						utils.useItem(Rpot.getId(), "drink");
					}
					//timeout = tickDelay();
					break;
				case USE_BOAT:
					GameObject boat = utils.findNearestGameObject(10068);
					utils.useGameObjectDirect(boat);
					timeout = tickDelay();
					break;
				case FIND_BANK:
					//resetZul();
					openBank();
					timeout = tickDelay();
					break;
				case DEPOSIT_ITEMS:
					//utils.depositAll();
					timeout = tickDelay();
					break;
				case WITHDRAW_ITEMS:
					timeout = tickDelay();
					break;
				case LOOT_ITEMS:
					lootItem(loot);
					break;

			}
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
			if (!(event.getActor() instanceof NPC))
			{
				return;
			}
			NPC npc = (NPC)((Object)event.getActor());
			if (npc.getName() != null && !npc.getName().equalsIgnoreCase("zulrah"))
			{
				return;
			}
		//if (npc.getName().equalsIgnoreCase("zulrah")){
		//	zulrahNpc = npc;
		//}
			switch (npc.getAnimation())
			{
				case 5071:
				{
					zulrahNpc = npc;
					potentialRotations = RotationType.findPotentialRotations(npc, stage);
					phaseTicksHandler.accept(currentRotation, potentialRotations.get(0));
					//log.debug("New Zulrah Encounter Started");
					break;
				}
				case 5073:
				{
					++stage;
					if (currentRotation == null)
					{
						potentialRotations = RotationType.findPotentialRotations(npc, stage);
						currentRotation = potentialRotations.size() == 1 ? potentialRotations.get(0) : null;
					}
					phaseTicksHandler.accept(currentRotation, potentialRotations.get(0));
					break;
				}
				case 5072:
				{
					if (zulrahReset)
					{
						zulrahReset = false;
					}
					if (currentRotation == null || !isLastPhase(currentRotation)) break;
					stage = -1;
					currentRotation = null;
					potentialRotations.clear();
					snakelings.clear();
					flipStandLocation = false;
					flipPhasePrayer = false;
					zulrahReset = true;
					//log.debug("Resetting Zulrah");
					break;
				}
				case 5069:
				{
					attackTicks = 4;
					if (currentRotation == null || !getCurrentPhase(currentRotation).getZulrahNpc().isJad() || zulrahNpc.getInteracting() != client.getLocalPlayer()) break;
					flipPhasePrayer = !flipPhasePrayer;
					break;
				}
				case 5806:
				case 5807:
				{
					attackTicks = 8;
					flipStandLocation = !flipStandLocation;
					break;
				}
				case 5804:
				{
					resetZul();
				}
			}
	}

	@Subscribe
	private void onItemSpawned(ItemSpawned event) {
		TileItem item = event.getItem();
		String itemName = client.getItemDefinition(item.getId()).getName().toLowerCase();

		if (lootableItems.stream().anyMatch(itemName.toLowerCase()::contains) && item.getId() != 1751) {             // || client.getItemDefinition(event.getItem().getId()).getName() == "Dragon bones" || client.getItemDefinition(event.getItem().getId()).getName() == "Draconic visage") {
			loot.add(event.getTile().getWorldLocation());
		}
	}
	@Subscribe
	private void onItemDespawned(ItemDespawned event) {
		loot.remove(event.getTile().getWorldLocation());
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

	int[] ItemIDs;


	public WidgetItem GetFoodItem() {
		WidgetItem item;

		item = utils.getInventoryWidgetItem(Collections.singletonList(configph.foodID()));

		if (item != null)
		{
			return item;
		}

		return item;
	}
	public WidgetItem GetRangedItem()
	{
		WidgetItem item;

		item = utils.getInventoryWidgetItem(ItemID.DIVINE_BASTION_POTION1, ItemID.DIVINE_BASTION_POTION2, ItemID.DIVINE_BASTION_POTION3, ItemID.DIVINE_BASTION_POTION4, ItemID.BASTION_POTION1, ItemID.BASTION_POTION2, ItemID.BASTION_POTION3, ItemID.BASTION_POTION4,ItemID.RANGING_POTION1, ItemID.RANGING_POTION2, ItemID.RANGING_POTION3, ItemID.RANGING_POTION4);

		if (item != null)
		{
			return item;
		}

		return item;
	}
	public WidgetItem GetMagicItem()
	{
		WidgetItem item;

		item = utils.getInventoryWidgetItem(ItemID.IMBUED_HEART, ItemID.DIVINE_MAGIC_POTION1, ItemID.DIVINE_MAGIC_POTION2, ItemID.DIVINE_MAGIC_POTION3, ItemID.DIVINE_MAGIC_POTION4, ItemID.MAGIC_POTION1, ItemID.MAGIC_POTION2, ItemID.MAGIC_POTION3, ItemID.MAGIC_POTION4);

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
	}
	public WidgetItem GetAntiVenomItem() {

		WidgetItem item;

		item = utils.getInventoryWidgetItem(ItemID.ANTIDOTE1_5958, ItemID.ANTIDOTE2_5956, ItemID.ANTIDOTE3_5954, ItemID.ANTIDOTE4_5952, ItemID.ANTIVENOM1_12919 ,ItemID.ANTIVENOM2_12917,ItemID.ANTIVENOM3_12915, ItemID.ANTIVENOM4_12913);

		if (item != null) {
			return item;
		}

		return item;
	}

	@Subscribe
	private void onProjectileMoved(ProjectileMoved event)
	{
			if (zulrahNpc == null)
			{
				return;
			}
			Projectile p = event.getProjectile();
			switch (p.getId())
			{
				case 1045:
				case 1047:
				{
					projectilesMap.put(event.getPosition(), p.getRemainingCycles() / 30);
				}
			}
	}

	public String getTag(int itemId)
	{
		String tag = configManager.getConfiguration("inventorytags", "item_" + itemId);
		if (tag == null || tag.isEmpty())
		{
			return "";
		}

		return tag;
	}
	@Subscribe
	private void onNpcChanged(NpcChanged event) {
		int npcId = event.getNpc().getId();
		if (event.getNpc().getName().equalsIgnoreCase("zulrah")) {
			zulrahNpc = event.getNpc();
		}
		/*if (npcId == 2043 && client.getVar(Prayer.PROTECT_FROM_MISSILES.getVarbit()) != 0) {
			if (client.getBoostedSkillLevel(Skill.HITPOINTS) > configph.hpToEat() && client.getBoostedSkillLevel(Skill.PRAYER) > configph.prayToDrink()) {
				activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);    //Melee Form Range prayer enabled
			}
		}
		if (npcId == 2043 && client.getVar(Prayer.PROTECT_FROM_MAGIC.getVarbit()) != 0) {
			if (client.getBoostedSkillLevel(Skill.HITPOINTS) > configph.hpToEat() && client.getBoostedSkillLevel(Skill.PRAYER) > configph.prayToDrink()) {
				activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);    //Melee Form Mage prayer enabled
			}
		}
		if (npcId == 2042 && client.getVar(Prayer.PROTECT_FROM_MISSILES.getVarbit()) == 0) {
			if (client.getBoostedSkillLevel(Skill.HITPOINTS) > configph.hpToEat() && client.getBoostedSkillLevel(Skill.PRAYER) > configph.prayToDrink()) {
				activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);    //2042 Ranged form, 2044 Magic Form, 2043 Melee Form
			}
		} else if (npcId == 2044 && client.getVar(Prayer.PROTECT_FROM_MAGIC.getVarbit()) == 0) {
			if (client.getBoostedSkillLevel(Skill.HITPOINTS) > configph.hpToEat() && client.getBoostedSkillLevel(Skill.PRAYER) > configph.prayToDrink()) {
				activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);    //Magic form
			}
		}

		// int npcId = event.getNpc().getId();
		if (npcId == 2042 && !configph.RangedOnly()) {        //	RANGED FORM
			Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
			if (inventory == null) {
				return;
			}
			for (WidgetItem item : utils.getAllInventoryItems()) {
				if (("Group 3").equalsIgnoreCase(getTag(item.getId()))) {
					//plugin.entryList.add(new MenuEntry("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId(), false));
					//clientThread.invoke(() -> client.invokeMenuAction("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId()));
				}
			}
			if (client.getVar(Prayer.AUGURY.getVarbit()) == 0 && configph.Augury()) {
				activatePrayer(WidgetInfo.PRAYER_AUGURY);
			}
			if (client.getVar(Prayer.MYSTIC_MIGHT.getVarbit()) == 0 && !configph.Augury()) {
				activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
			}
			//utils.attackNPCDirect(zulrahNpc);
		} else if (npcId == 2043 && !configph.RangedOnly()) {    //	MELEE FORM
			Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
			if (inventory == null) {
				return;
			}
			for (WidgetItem item : utils.getAllInventoryItems()) {
				if (("Group 3").equalsIgnoreCase(getTag(item.getId()))) {
					//plugin.entryList.add(new MenuEntry("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId(), false));
					//clientThread.invoke(() -> client.invokeMenuAction("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId()));
				}
			}
			if (client.getVar(Prayer.AUGURY.getVarbit()) == 0 && configph.Augury()) {
				activatePrayer(WidgetInfo.PRAYER_AUGURY);
			}
			if (client.getVar(Prayer.MYSTIC_MIGHT.getVarbit()) == 0 && !configph.Augury()) {
				activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
			}
			//utils.attackNPCDirect(zulrahNpc);
		} else if (npcId == 2044 && !configph.MageOnly()) {    //	MAGIC FORM
			Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
			if (inventory == null) {
				return;
			}
			for (WidgetItem item : utils.getAllInventoryItems()) {
				if (("Group 2").equalsIgnoreCase(getTag(item.getId()))) {
					//plugin.entryList.add(new MenuEntry("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId(), false));
					//clientThread.invoke(() -> client.invokeMenuAction("Wield", "<col=ff9040>" + item.getId(), item.getId(), MenuAction.CC_OP.getId(), item.getIndex(), WidgetInfo.INVENTORY.getId()));
				}
			}
			if (client.getVar(Prayer.RIGOUR.getVarbit()) == 0 && configph.Rigour()) {
				activatePrayer(WidgetInfo.PRAYER_RIGOUR);
			}
			if (client.getVar(Prayer.RIGOUR.getVarbit()) == 0 && !configph.Rigour()) {
				activatePrayer(WidgetInfo.PRAYER_EAGLE_EYE);
			}
			//utils.attackNPCDirect(zulrahNpc);
		}*/
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		/*if (event.getMenuAction() == MenuAction.CC_OP && (event.getWidgetId() == WidgetInfo.WORLD_SWITCHER_LIST.getId() ||
				event.getWidgetId() == 11927560 || event.getWidgetId() == 4522007 || event.getWidgetId() == 24772686))
		{
			//Either logging out or world-hopping which is handled by 3rd party plugins so let them have priority
			utils.targetMenu = null;
			return;
		}*/
		if (event.getMenuOption().contains("Walk") || event.getMenuAction() == MenuAction.WALK) {
			event.consume();
		}
		/*if (utils.targetMenu != null && event.getParam1() != WidgetInfo.INVENTORY.getId() && event.getParam1() != WidgetInfo.FIXED_VIEWPORT_PRAYER_TAB.getId() && event.getParam1() != WidgetInfo.RESIZABLE_VIEWPORT_PRAYER_TAB.getId()){
			if (event.getId() != utils.targetMenu.getIdentifier() ||
					event.getParam0() != utils.targetMenu.getParam0() ||
					event.getParam1() != utils.targetMenu.getParam1()) {
				event.consume();
			}
			utils.targetMenu = null;
		}*/
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event) {

		NPC npc = event.getNpc();
		//if (npc.getName().toLowerCase().contains("zulrah")) {
		//	zulrah = npc;
		//}

		 int npcId = event.getNpc().getId();

		if (event.getNpc().getName().equalsIgnoreCase("zulrah")){
			zulrahNpc = event.getNpc();
		}
		/*if (npcId == 2042 && client.getLocalPlayer().getOverheadIcon() != HeadIcon.RANGED) {
			zulrah = npc;
			zulrahNpc = npc;
			if (client.getBoostedSkillLevel(Skill.HITPOINTS) > configph.hpToEat() && client.getBoostedSkillLevel(Skill.PRAYER) > configph.prayToDrink()) {
				activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);    //Ranged form
				if (client.getVar(Prayer.AUGURY.getVarbit()) == 0 && configph.Augury() && !configph.RangedOnly()) {
					activatePrayer(WidgetInfo.PRAYER_AUGURY);
				}
				if (client.getVar(Prayer.MYSTIC_MIGHT.getVarbit()) == 0 && !configph.Augury() && !configph.RangedOnly()) {
					activatePrayer(WidgetInfo.PRAYER_MYSTIC_MIGHT);
				}
			}
		} else if (npcId == 2044 && client.getLocalPlayer().getOverheadIcon() != HeadIcon.MAGIC) {
			zulrah = npc;
			zulrahNpc = npc;
			if (client.getBoostedSkillLevel(Skill.HITPOINTS) > configph.hpToEat() && client.getBoostedSkillLevel(Skill.PRAYER) > configph.prayToDrink()) {
				activatePrayer(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);    //Magic form
			}
			//activatePrayer(WidgetInfo.PRAYER_EAGLE_EYE);
		}*/
	}


	private static void setHidden(Renderable renderable, boolean hidden)
	{
		Method setHidden = null;
		try
		{
			setHidden = renderable.getClass().getMethod("setHidden", Boolean.TYPE);
		}
		catch (NoSuchMethodException e)
		{
			return;
		}
		try
		{
			setHidden.invoke(renderable, hidden);
		}
		catch (IllegalAccessException | InvocationTargetException e)
		{

		}
	}


	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event) {
		if (zulrahNpc == null) {
			return;
		}
		GameObject obj = event.getGameObject();
		if (obj.getId() == 11700) {
			toxicCloudsMap.put(obj, 30);
		}
	}

	public Set<ZulrahData> getZulrahData()
	{
		LinkedHashSet<ZulrahData> zulrahDataSet = new LinkedHashSet<ZulrahData>();
		if (currentRotation == null)
		{
			potentialRotations.forEach(type -> zulrahDataSet.add(new ZulrahData(getCurrentPhase((RotationType)((Object)type)), getNextPhase((RotationType)((Object)type)))));
		}
		else
		{
			zulrahDataSet.add(new ZulrahData(getCurrentPhase(currentRotation), getNextPhase(currentRotation)));
		}
		return zulrahDataSet.size() > 0 ? zulrahDataSet : Collections.emptySet();
	}

	@Nullable
	private ZulrahPhase getCurrentPhase(RotationType type)
	{
		return stage >= type.getZulrahPhases().size() ? null : type.getZulrahPhases().get(stage);
	}

	@Nullable
	private ZulrahPhase getNextPhase(RotationType type)
	{
		return isLastPhase(type) ? null : type.getZulrahPhases().get(stage + 1);
	}

	private boolean isLastPhase(RotationType type)
	{
		return stage == type.getZulrahPhases().size() - 1;
	}

	public NPC getZulrahNpc()
	{
		return zulrahNpc;
	}

	public static boolean isFlipStandLocation()
	{
		return flipStandLocation;
	}

	public static boolean isFlipPhasePrayer()
	{
		return flipPhasePrayer;
	}

	public static boolean isZulrahReset()
	{
		return zulrahReset;
	}

	public void activatePrayer(WidgetInfo widgetInfo)
	{
		utils.activatePrayer(widgetInfo);
	}

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
}