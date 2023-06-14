/*
 * Copyright (c) 2021-2022, Numb#1147 <https://github.com/NumbPlugins>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.Utils;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.queries.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.grandexchange.GrandExchangeClient;
import okhttp3.MediaType;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.awt.event.KeyEvent.*;
import static net.runelite.api.NullObjectID.NULL_34810;

@Slf4j
//@SuppressWarnings("unused")
//@Singleton
@PluginDescriptor(
	name = "AUtils",
	description = "Automation plugin utilities.",
	enabledByDefault = true,
	hidden = false
)
public class Core extends Plugin
{
	//@Inject
	//ExecutorService executorService;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Client client;
	@Inject
	public ClientThread clientThread;
	@Inject
	private ChatMessageManager chatMessageManager;
	@Inject
	private ItemManager itemManager;
	@Inject
	private GrandExchangeClient grandExchangeClient;

	@Inject Walking walking;

	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-88");
	public static final MediaType JSON2 = MediaType.parse("application/json; charset=utf-8");
	public static final MediaType JSON3 = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
	private final String DAX_API_URL = "https://api.dax.cloud/walker/generatePath";
	public List<WorldPoint> currentPath = new LinkedList<WorldPoint>();
	public MenuEntry targetMenu;
	public WorldPoint nextPoint;
	public boolean randomEvent;
	public boolean iterating;
	public boolean webWalking;
	private int nextFlagDist = -1;
	public boolean consumeClick;
	private boolean modifiedMenu;
	private int modifiedItemID;
	private int modifiedItemIndex;
	private int modifiedOpCode;
	private int coordX;
	private int coordY;
	private int nextRunEnergy;
	private boolean walkAction;
	public boolean retrievingPath;

	protected static final Random random = new Random();

	@Override
	protected void startUp() throws IOException, ClassNotFoundException {
		//no();
	}

	@Override
	protected void shutDown() {

	}

	public void sendGameMessage(String message)
	{
		String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append(message)
				.build();

		chatMessageManager
				.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(chatMessage)
						.build());
	}

	/*
	Click random point
	 */
	public void clickRandomPoint(int min, int max)
	{
		assert !client.isClientThread();

		Point point = new Point(getRandomIntBetweenRange(min, max), getRandomIntBetweenRange(min, max));
		handleMouseClick(point);
	}

	/*
	Click random point near the centre
	 */
	public void clickRandomPointCenter(int min, int max)
	{
		assert !client.isClientThread();

		Point point = new Point(client.getCenterX() + getRandomIntBetweenRange(min, max), client.getCenterY() + getRandomIntBetweenRange(min, max));
		handleMouseClick(point);
	}

	/*
	Click random point near the centre with a delay
	 */
	public void delayClickRandomPointCenter(int min, int max, long delay)
	{
		//executorService.submit(() ->
		//{
		try
		{
			sleep(delay);
			clickRandomPointCenter(min, max);
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}
		///});
	}

	/*
	  if given Point is in the viewport, click on the Point otherwise click a random point in the centre of the screen
	does NOT work with Runelite. Must be in viewport to be interactable
	* */
	public void handleMouseClick(Point point) {
		assert !client.isClientThread();
		moveClick(point);
	}
	/*
        Click point after moving mouse to point
         */
	public void handleMouseMove2(Rectangle rectangle) {
		assert !client.isClientThread();
		Point point = getClickPoint(rectangle);
		if (client.isStretchedEnabled())
		{
			final Dimension stretched = client.getStretchedDimensions();
			final Dimension real = client.getRealDimensions();
			final double width = (stretched.width / real.getWidth());
			final double height = (stretched.height / real.getHeight());
			point = new Point((int) (point.getX() * width), (int) (point.getY() * height));
		}
		mouseEvent(MouseEvent.MOUSE_ENTERED, point, false);
		mouseEvent(MouseEvent.MOUSE_EXITED, point, false);
		mouseEvent(MouseEvent.MOUSE_MOVED, point, false);
		mouseEvent(MouseEvent.MOUSE_PRESSED, point, false);
		mouseEvent(MouseEvent.MOUSE_RELEASED, point, false);
		mouseEvent(MouseEvent.MOUSE_CLICKED, point, false);
	}

	/*
	Click point after moving mouse to point
	 */
	public void handleMouseMove(Rectangle rectangle) {
		assert !client.isClientThread();
		Point point = getClickPoint(rectangle);
		if (client.isStretchedEnabled())
		{
			final Dimension stretched = client.getStretchedDimensions();
			final Dimension real = client.getRealDimensions();
			final double width = (stretched.width / real.getWidth());
			final double height = (stretched.height / real.getHeight());
			point = new Point((int) (point.getX() * width), (int) (point.getY() * height));
		}

//		mouseEvent(MouseEvent.MOUSE_ENTERED, point);
//		mouseEvent(MouseEvent.MOUSE_EXITED, point);
		mouseEvent(MouseEvent.MOUSE_MOVED, point, false);
		mouseEvent(MouseEvent.MOUSE_PRESSED, point, false);
		mouseEvent(MouseEvent.MOUSE_RELEASED, point, false);
		mouseEvent(MouseEvent.MOUSE_CLICKED, point, false);
	}

	/*
    Drag mouse to point without clicking
   */
	public void dragmouse(Rectangle rectangle) {
		assert !client.isClientThread();
		Point point = getClickPoint(rectangle);
		if (client.isStretchedEnabled())
		{
			final Dimension stretched = client.getStretchedDimensions();
			final Dimension real = client.getRealDimensions();
			final double width = (stretched.width / real.getWidth());
			final double height = (stretched.height / real.getHeight());
			point = new Point((int) (point.getX() * width), (int) (point.getY() * height));
		}

		mouseEvent(MouseEvent.MOUSE_ENTERED, point, false);
		mouseEvent(MouseEvent.MOUSE_EXITED, point, false);
		mouseEvent(MouseEvent.MOUSE_MOVED, point, false);
	}


	/*
	Click point without delay
	 */
	public void handleMouseClick(Rectangle rectangle)
	{
		assert !client.isClientThread();

		final int viewportHeight = client.getViewportHeight();
		final int viewportWidth = client.getViewportWidth();

		if (rectangle.getX() > viewportWidth || rectangle.getY() > viewportHeight || rectangle.getX() < 0 || rectangle.getY() < 0) {
			//clickRandomPointCenter(-100, 100);
			return;//if not on screen, do not click
		}

		Point point = getClickPoint(rectangle);
		handleMouseMove2(rectangle);
		//moveClick(point);
	}
	/*
	Click point with delay
	 */
	public void delayMouseClick(Rectangle rectangle, long delay)
	{
		try
		{
			sleep(delay);
			handleMouseClick(rectangle);
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}
	}

	public int[] stringToIntArray(String string)
	{
		return Arrays.stream(string.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
	}


	/*
	Find closest player
	 */
	@Nullable
	public Player findNearestPlayer(String name){
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new PlayerQuery()
				.nameEquals(name)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
      find nearest Player
	 */
	@Nullable
	public Player findNearestPlayer(){
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new PlayerQuery()
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}

	/*
  find nearest Object by ID
 */
	@Nullable
	public GameObject findNearestGameObject(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GameObjectQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
      find nearest Object by Name
	 */
	@Nullable
	public GameObject findNearestGameObject(String name)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GameObjectQuery()
				//.filter (i -> client.getObjectDefinition(i.getId()).getName().toLowerCase().contains(name.toLowerCase()))
				.nameEquals(name)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}

	/*
  find nearest real object with impostor IDs, using the desired name
 */
	public GameObject getImpostorObject (String realName, int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}
		return new GameObjectQuery()
				.filter (i -> client.getObjectDefinition(i.getId()).getImpostor().getName().equals(realName))
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}

	public ObjectComposition getImpostor (int id)
	{
		return client.getObjectDefinition(id).getImpostor();
	}

	/*
  find nearest Object within a distance by IDs
 */
	@Nullable
	public GameObject findNearestGameObjectWithin(WorldPoint worldPoint, int dist, int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GameObjectQuery()
				.isWithinDistance(worldPoint, dist)
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
  find nearest Object within a distance by Names
 */
	@Nullable
	public GameObject findNearestGameObjectWithin(WorldPoint worldPoint, int dist, String... names)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GameObjectQuery()
				.isWithinDistance(worldPoint, dist)
				.nameEquals(names)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}

	/*
find nearest NPC by IDs
*/
	@Nullable
	public NPC findNearestNpc(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new NPCQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
  find nearest NPC by Names
 */
	@Nullable
	public NPC findNearestNpc(String... names)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new NPCQuery()
				.nameContains(names)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
  find nearest Object NPC by Name
 */
	@Nullable
	public NPC findNearestNpc(String name)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new NPCQuery()
				.nameContains(name)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
  find nearest NPC which is attackable, within an area
 */
	@Nullable
	public NPC findNearestAttackableNpcWithin(WorldPoint worldPoint, int dist, int ID, boolean exactname)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}


			return new NPCQuery()
					.isWithinDistance(worldPoint, dist)
					.filter(npc -> !npc.isDead() && npc.getId() == ID && npc.getHealthRatio() != 0)
					.result(client)
					.nearestTo((Locatable) client.getLocalPlayer());
	}


	@Nullable
	public GroundObject findNearestGroundObjectWithin(WorldPoint worldPoint, int dist, int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GroundObjectQuery()
				.isWithinDistance(worldPoint, dist)
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	@Nullable
	public GroundObject findNearestGroundObjectWithin(WorldPoint worldPoint, int dist, Collection<Integer> ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GroundObjectQuery()
				.isWithinDistance(worldPoint, dist)
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}

	@Nullable
	public NPC findNearestNpcWithin(WorldPoint worldPoint, int dist, Collection<Integer> ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new NPCQuery()
				.isWithinDistance(worldPoint, dist)
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}

	@Nullable
	public NPC findNearestNpcWithin(WorldPoint worldPoint, int dist, int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new NPCQuery()
				.isWithinDistance(worldPoint, dist)
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}


	@Nullable
	public NPC findNearestAttackableNpcWithin(WorldPoint worldPoint, int dist, String name, boolean exactnpcname)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		if (exactnpcname)
		{
			return new NPCQuery()
					.isWithinDistance(worldPoint, dist)
					.filter(npc -> npc.getName() != null && npc.getName().equalsIgnoreCase(name) && npc.getInteracting() == null && npc.getHealthRatio() != 0)
					.result(client)
					.nearestTo((Locatable) client.getLocalPlayer());
		}
		else
		{
			return new NPCQuery()
					.isWithinDistance(worldPoint, dist)
					.filter(npc -> npc.getName() != null && npc.getName().toLowerCase().contains(name.toLowerCase()) && npc.getInteracting() == null && npc.getHealthRatio() != 0)
					.result(client)
					.nearestTo((Locatable) client.getLocalPlayer());
		}
	}
	/*
find nearest NPC attacking player
*/
	@Nullable
	public NPC findNearestNpcTargetingLocal() {
		assert client.isClientThread();

		if (client.getLocalPlayer() == null) {
			return null;
		}

			return new NPCQuery()
					.filter(npc -> !npc.isDead() && npc.getInteracting() == client.getLocalPlayer() && npc.getHealthRatio() != 0)
					.result(client)
					.nearestTo((Locatable) client.getLocalPlayer());

	}
	/*
find nearest NPC attacking player limited by name
*/
	@Nullable
	public NPC findNearestNpcTargetingLocal(String name, boolean exactnpcname)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		if (exactnpcname)
		{
			return new NPCQuery()
					.filter(npc -> npc.getName() != null && npc.getName().equalsIgnoreCase(name) && npc.getInteracting() == client.getLocalPlayer() && npc.getHealthRatio() != 0)
					.result(client)
					.nearestTo((Locatable) client.getLocalPlayer());
		}
		else
		{
			return new NPCQuery()
					.filter(npc -> npc.getName() != null && npc.getName().toLowerCase().contains(name.toLowerCase()) && npc.getInteracting() == client.getLocalPlayer() && npc.getHealthRatio() != 0)
					.result(client)
					.nearestTo((Locatable) client.getLocalPlayer());
		}

	}

	@Nullable
	public WallObject findNearestWallObject(String name)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new WallObjectQuery()
				//.filter (i -> client.getObjectDefinition(i.getId()).getName().toLowerCase().contains(name.toLowerCase()))
				.nameEquals(name)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
find nearest wall object
*/
	@Nullable
	public WallObject findNearestWallObject(int ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new WallObjectQuery()
				.filter(object -> object.getId() == ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
find nearest decorative object
*/
	@Nullable
	public DecorativeObject findNearestDecorativeObject(int ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new DecorativeObjectQuery()
				.filter(object -> object.getId() == ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
find nearest wall object
*/
	@Nullable
	public WallObject findNearestWallObject(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new WallObjectQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
find nearest wall object within distance
*/
	@Nullable
	public WallObject findWallObjectWithin(WorldPoint worldPoint, int radius, int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new WallObjectQuery()
				.isWithinDistance(worldPoint, radius)
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
find nearest wall object within distance
*/
	@Nullable
	public WallObject findWallObjectWithin(WorldPoint worldPoint, int radius, Collection<Integer> ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new WallObjectQuery()
				.isWithinDistance(worldPoint, radius)
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
find nearest decorative object
*/
	@Nullable
	public DecorativeObject findNearestDecorObject(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new DecorativeObjectQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	/*
find nearest ground object
*/
	@Nullable
	public GroundObject findNearestGroundObject(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GroundObjectQuery()
				.idEquals(ids)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}

	/*
get all game objects nearby by ids
*/
	public List<GameObject> getGameObjects(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new GameObjectQuery()
				.idEquals(ids)
				.result(client)
				.list;
	}
	/*
get all game objects nearby by names
*/
	public List<GameObject> getGameObjects(String... names)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new GameObjectQuery()
				.nameEquals(names)
				.result(client)
				.list;
	}
	/*
get all game objects nearby within distance
*/
	public List<GameObject> getLocalGameObjects(int distanceAway, int... ids)
	{
		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}
		List<GameObject> localGameObjects = new ArrayList<>();
		for (GameObject gameObject : getGameObjects(ids))
		{
			if (gameObject.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()) < distanceAway)
			{
				localGameObjects.add(gameObject);
			}
		}
		return localGameObjects;
	}
	/*
get all NPCs nearby
*/
	public List<NPC> getNPCs(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new NPCQuery()
				.idEquals(ids)
				.result(client)
				.list;
	}
	/*
get all NPCs nearby
*/
	public List<NPC> getNPCs(String... names)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new NPCQuery()
				.nameContains(names)
				.result(client)
				.list;
	}
	/*
get nearest NPC interacting with Player
*/
	public NPC getFirstNPCWithLocalTarget()
	{
		assert client.isClientThread();

		List<NPC> npcs = client.getNpcs();
		for (NPC npc : npcs)
		{
			if (npc.getInteracting() == client.getLocalPlayer())
			{
				return npc;
			}
		}
		return null;
	}

	public List<WallObject> getWallObjects(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new WallObjectQuery()
				.idEquals(ids)
				.result(client)
				.list;
	}

	public List<DecorativeObject> getDecorObjects(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new DecorativeObjectQuery()
				.idEquals(ids)
				.result(client)
				.list;
	}

	public List<GroundObject> getGroundObjects(int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new GroundObjectQuery()
				.idEquals(ids)
				.result(client)
				.list;
	}

	public List<GroundObject> getGroundObjectsWithinDistanceOf(int dist, WorldPoint worldPoint, int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new GroundObjectQuery()
				.idEquals(ids)
				.filter(obj -> obj.getWorldLocation().distanceTo(worldPoint) <= dist)
				.result(client)
				.list;
	}
	public List<GameObject> getGameObjectsWithinDistanceOf(int dist, WorldPoint worldPoint, int... ids)
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return new ArrayList<>();
		}

		return new GameObjectQuery()
				.idEquals(ids)
				.filter(obj -> obj.getWorldLocation().distanceTo(worldPoint) <= dist)
				.result(client)
				.list;
	}

	@Nullable
	public TileObject findNearestObject(int... ids)
	{
		GameObject gameObject = findNearestGameObject(ids);

		if (gameObject != null)
		{
			return gameObject;
		}

		WallObject wallObject = findNearestWallObject(ids);

		if (wallObject != null)
		{
			return wallObject;
		}
		DecorativeObject decorativeObject = findNearestDecorObject(ids);

		if (decorativeObject != null)
		{
			return decorativeObject;
		}

		return findNearestGroundObject(ids);
	}

	@Nullable
	public GameObject findNearestBank()
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GameObjectQuery()
				.idEquals(ALL_BANKS)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}
	public GameObject findNearestBankNoDepositBoxes()
	{
		assert client.isClientThread();

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		return new GameObjectQuery()
				.idEquals(NO_DEPOSIT_BOXES)
				.result(client)
				.nearestTo((Locatable) client.getLocalPlayer());
	}

	public List<Item> getEquippedItems()
	{
		assert client.isClientThread();

		List<Item> equipped = new ArrayList<>();
		Item[] items = client.getItemContainer(InventoryID.EQUIPMENT).getItems();
		for (Item item : items)
		{
			if (item.getId() == -1 || item.getId() == 0)
			{
				continue;
			}
			equipped.add(item);
		}
		return equipped;
	}

	public boolean isItemEquipped(Collection<Integer> itemIds)
	{
		assert client.isClientThread();

		Item[] items = client.getItemContainer(InventoryID.EQUIPMENT).getItems();
		for (Item item : items)
		{
			if (itemIds.contains(item.getId()))
			{
				return true;
			}
		}
		return false;
	}

	/*
check if point is within current viewport
*/
	public boolean pointOnScreen(Point check)
	{
		int x = check.getX(), y = check.getY();
		return x > client.getViewportXOffset() && x < client.getViewportWidth()
				&& y > client.getViewportYOffset() && y < client.getViewportHeight();
	}

	/*
Types a string
*/
	public void typeString(String string)
	{
		assert !client.isClientThread();

		for (char c : string.toCharArray())
		{
			pressKey(c);
		}
	}
	/*
Presses a key
*/
	public void pressKey(char key)
	{
		keyEvent(401, key);
		keyEvent(402, key);
		keyEvent(400, key);
	}

	public void pressKey(int key)
	{
		keyEvent(401, key);
		keyEvent(402, key);
		//keyEvent(400, key);
	}

	private void keyEvent(int id, char key)
	{
		KeyEvent e = new KeyEvent(
				client.getCanvas(), id, System.currentTimeMillis(),
				0, KeyEvent.VK_UNDEFINED, key
		);

		client.getCanvas().dispatchEvent(e);
	}

	private void keyEvent(int id, int key)
	{
		KeyEvent e = new KeyEvent(
				client.getCanvas(), id, System.currentTimeMillis(),
				0, key, KeyEvent.CHAR_UNDEFINED
		);
		client.getCanvas().dispatchEvent(e);
	}

	public boolean isMoving()
	{
		int camX = client.getCameraX2();
		int camY = client.getCameraY2();
		sleep(25);
		return (camX != client.getCameraX() || camY != client.getCameraY()) && client.getLocalDestinationLocation() != null;
	}

	@Subscribe
	private void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		/*var entries = client.getMenuEntries();
		int putAtTopId = -1;
		for (int i = 0; i < entries.length; i++)
		{
			var entry = entries[i];
			if (entry.getParam0() == targetMenu.getParam0() && entry.getParam1() == targetMenu.getParam1() && entry.getIdentifier() == targetMenu.getIdentifier() ){//&& entry.getTarget().contains(targetMenu.getTarget()) && entry.getOption().contains(targetMenu.getOption())) {
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
		}*/			/* DEPRECATED */
	}

	public boolean isMoving(LocalPoint lastTickLocalPoint)
	{
		return client.getLocalPlayer().getLocalLocation() == lastTickLocalPoint;
	}

	public WidgetItem getItemFromInventory(int... ItemIDs)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);

		if (inventoryWidget == null)
		{
			return null;
		}

		for (WidgetItem item : getAllInventoryItems())
		{
			if (Arrays.stream(ItemIDs).anyMatch(i -> i == item.getId()))
			{
				return item;
			}
		}

		return null;
	}

	public boolean isInteracting()
	{
		sleep(25);
		return isMoving() || client.getLocalPlayer().getAnimation() != -1;
	}

	public boolean isAnimating()
	{
		return client.getLocalPlayer().getAnimation() != -1;
	}

	public void walk(LocalPoint localPoint)
	{
		coordX = localPoint.getSceneX() + getRandomIntBetweenRange(-Math.abs(0), Math.abs(0));
		coordY = localPoint.getSceneY() + getRandomIntBetweenRange(-Math.abs(0), Math.abs(0));
		walkAction = true;
		//clientThread.invoke(() -> client.invokeMenuAction("", "", 0, MenuAction.WALK.getId(), 0, 0));
		moveClick(Perspective.localToCanvas(client, localPoint, client.getPlane()));
	}

	public void walk(WorldPoint worldPoint)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
		if (localPoint != null)
		{
			coordX = localPoint.getSceneX() + getRandomIntBetweenRange(-Math.abs(0), Math.abs(0));
			coordY = localPoint.getSceneY() + getRandomIntBetweenRange(-Math.abs(0), Math.abs(0));
			walkAction = true;
			//clientThread.invoke(() -> client.invokeMenuAction("", "", 0, MenuAction.WALK.getId(), 0, 0));
			moveClick(Perspective.localToCanvas(client, localPoint, client.getPlane()));
		}
		else
		{
			log.info("WorldPoint to LocalPoint coversion is null");
		}
	}

	public void logout()
	{
		int param1 = (client.getWidget(WidgetInfo.LOGOUT_BUTTON) != null) ? 11927560 : 4522007;
		Widget logoutWidget = client.getWidget(WidgetInfo.LOGOUT_BUTTON);
		if (logoutWidget != null)
		{
			targetMenu = new NewMenuEntry("", "", 1, MenuAction.CC_OP.getId(), -1, param1, false);
			doInvoke(targetMenu, logoutWidget.getBounds());
			//delayMouseClick(new Rectangle(0, 0), random(100, 300));
			//clientThread.invoke(() -> client.invokeMenuAction("", "", 1, MenuAction.CC_OP.getId(), -1, param1));
		}
	}

	public boolean inventoryFull()
	{
		return getInventorySpace() <= 0;
	}

	public boolean inventoryEmpty()
	{
		return getInventorySpace() >= 28;
	}

	public int getInventorySpace()
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if (inventoryWidget != null)
		{
			return 28 - getAllInventoryItems().size();
		}
		else
		{
			return -1;
		}
	}

	public List<WidgetItem> getInventoryItems(Collection<Integer> ids)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		List<WidgetItem> matchedItems = new ArrayList<>();

		if (inventoryWidget != null)
		{
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items)
			{
				if (ids.contains(item.getId()))
				{
					matchedItems.add(item);
				}
			}
			return matchedItems;
		}
		return null;
	}

	public List<WidgetItem> getInventoryItems(String itemName)
	{
		return getAllInventoryItems().stream().filter(wi -> wi.getWidget().getName().contains(itemName)).collect(Collectors.toList());
	}
	public <T> T getFromClientThread(Supplier<T> supplier) {
		if (!client.isClientThread()) {
			CompletableFuture<T> future = new CompletableFuture<>();

			clientThread.invoke(() -> {
				future.complete(supplier.get());
			});
			return future.join();
		} else {
			return supplier.get();
		}
	}

	private final Map<Integer, ItemComposition> itemCompositionMap = new HashMap<>();

	public ItemComposition getItemDefinition(int id) {
		if (itemCompositionMap.containsKey(id)) {
			return itemCompositionMap.get(id);
		} else {
			ItemComposition def = getFromClientThread(() -> client.getItemDefinition(id));
			itemCompositionMap.put(id, def);

			return def;
		}
	}

	public int itemOptionToId(int itemId, List<String> match) {
		ItemComposition itemDefinition = getItemDefinition(itemId);

		int index = 0;
		for (String action : itemDefinition.getInventoryActions()) {
			if (action != null && match.stream().anyMatch(action::equalsIgnoreCase)) {
				if (index <= 2) {
					return index + 2;
				} else {
					return index + 3;
				}
			}

			index++;
		}

		return -1;
	}

	public String selectedItemOption(int itemId, List<String> match) {
		ItemComposition itemDefinition = getItemDefinition(itemId);
		for (String action : itemDefinition.getInventoryActions()) {
			if (action != null && match.stream().anyMatch(action::equalsIgnoreCase)) {
				return action;
			}
		}
		return match.get(0);
	}

	public MenuAction idToMenuAction(int id) {
		if (id <= 5) {
			return MenuAction.CC_OP;
		} else {
			return MenuAction.CC_OP_LOW_PRIORITY;
		}
	}

	public void refreshInventory() {
		if (client.isClientThread())
			client.runScript(6009, 9764864, 28, 1, -1);
		else
			clientThread.invokeLater(() -> client.runScript(6009, 9764864, 28, 1, -1));
	}
	/*
get the first interactable widget of a list of Inventory items
*/
	public WidgetItem getWidgetItem(List<Integer> ids) {
		return getAllInventoryItems().stream().filter(wi -> ids.stream().anyMatch(i -> i == wi.getId())).findFirst().orElse(null);
	}
	/*
get the interactable widget of an Inventory item
*/
	public WidgetItem getWidgetItem(int id) {
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if (inventoryWidget != null) {
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items) {
				if (item.getId() == id) {
					return item;
				}
			}
		}
		return null;
	}
	/*
get all Inventory items
*/
	public Collection<WidgetItem> getAllInventoryItems()
	{
		return getFromClientThread(() -> {
			Widget geWidget = client.getWidget(WidgetInfo.GRAND_EXCHANGE_INVENTORY_ITEMS_CONTAINER);

			boolean geOpen = geWidget != null/* && !geWidget.isHidden()*/;
			boolean bankOpen = !geOpen && client.getItemContainer(InventoryID.BANK) != null;

			Widget inventoryWidget = client.getWidget(
					bankOpen ? WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER :
							geOpen ? WidgetInfo.GRAND_EXCHANGE_INVENTORY_ITEMS_CONTAINER :
									WidgetInfo.INVENTORY
			);

			if (inventoryWidget == null) {
				return new ArrayList<>();
			}

			if (!bankOpen && !geOpen && inventoryWidget.isHidden()) {
				refreshInventory();
			}

			Widget[] children = inventoryWidget.getDynamicChildren();

			if (children == null) {
				return new ArrayList<>();
			}

			Collection<WidgetItem> widgetItems = new ArrayList<>();
			for (Widget item : children) {
				if (item.getItemId() != 6512) {
					widgetItems.add(createWidgetItem(item));
				}
			}

			return widgetItems;
		});
	}

	public WidgetItem createWidgetItem(Widget item) {
		//boolean isDragged = item.isWidgetItemDragged(item.getItemId());

		int dragOffsetX = 0;
		int dragOffsetY = 0;

		//if (isDragged) {
			///Point p = item.getWidgetItemDragOffsets();
			//dragOffsetX = p.getX();
			//dragOffsetY = p.getY();
		//}
		// set bounds to same size as default inventory
		Rectangle bounds = item.getBounds();
		bounds.setBounds(bounds.x - 1, bounds.y - 1, 32, 32);
		Rectangle dragBounds = item.getBounds();
		dragBounds.setBounds(bounds.x + dragOffsetX, bounds.y + dragOffsetY, 32, 32);

		return new WidgetItem(item.getItemId(), item.getItemQuantity(), bounds, item, dragBounds);
	}
	public Collection<Integer> getAllInventoryItemIDs()
	{
		Collection<WidgetItem> inventoryItems = getAllInventoryItems();
		if (inventoryItems != null)
		{
			Set<Integer> inventoryIDs = new HashSet<>();
			for (WidgetItem item : inventoryItems)
			{
				if (inventoryIDs.contains(item.getId()))
				{
					continue;
				}
				inventoryIDs.add(item.getId());
			}
			return inventoryIDs;
		}
		return null;
	}

	public List<Item> getAllInventoryItemsExcept(List<Integer> exceptIDs)
	{
		exceptIDs.add(-1); //empty inventory slot
		ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY);
		if (inventoryContainer != null)
		{
			Item[] items = inventoryContainer.getItems();
			List<Item> itemList = new ArrayList<>(Arrays.asList(items));
			itemList.removeIf(item -> exceptIDs.contains(item.getId()));
			return itemList.isEmpty() ? null : itemList;
		}
		return null;
	}

	/*public WidgetItem getInventoryWidgetItem(int... ids)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);

		if (inventoryWidget == null)
		{
			return null;
		}

		for (WidgetItem item : getAllInventoryItems())
		{
			if (Arrays.stream(ids).anyMatch(i -> i == item.getId()))
			{
				return item;
			}
		}

		return null;
	}

	public WidgetItem getInventoryWidgetItem(String... names)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);

		if (inventoryWidget == null)
		{
			return null;
		}

		for (WidgetItem item : getAllInventoryItems())
		{
			if (Arrays.stream(names).anyMatch(i -> i == item.toString()))
			{
				return item;
			}
		}

		return null;
	}

	public WidgetItem getInventoryWidgetItem(Collection<Integer> ids)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if (inventoryWidget != null)
		{
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items)
			{
				if (ids.contains(item.getId()))
				{
					return item;
				}
			}
		}
		return null;
	}
	*/


	public WidgetItem getInventoryWidgetItem(List<Integer> ids)
	{
		return getAllInventoryItems().stream().filter(wi -> ids.stream().anyMatch(i -> i == wi.getId())).findFirst().orElse(null);
	}
	public WidgetItem getInventoryWidgetItem(int... IDS)
	{
		List<Integer> ids = Arrays.stream(IDS).boxed().collect(Collectors.toList());
		return getAllInventoryItems().stream().filter(wi -> ids.stream().anyMatch(i -> i == wi.getId())).findFirst().orElse(null);
	}

	public Item getInventoryItemExcept(List<Integer> exceptIDs)
	{
		exceptIDs.add(-1); //empty inventory slot
		ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY);
		if (inventoryContainer != null)
		{
			Item[] items = inventoryContainer.getItems();
			List<Item> itemList = new ArrayList<>(Arrays.asList(items));
			itemList.removeIf(item -> exceptIDs.contains(item.getId()));
			return itemList.isEmpty() ? null : itemList.get(0);
		}
		return null;
	}

	public WidgetItem getInventoryItemMenu(ItemManager itemManager, String menuOption, int opcode, Collection<Integer> ignoreIDs)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if (inventoryWidget != null)
		{
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items)
			{
				if (ignoreIDs.contains(item.getId()))
				{
					continue;
				}
				String[] menuActions = itemManager.getItemComposition(item.getId()).getInventoryActions();
				for (String action : menuActions)
				{
					if (action != null && action.equals(menuOption))
					{
						return item;
					}
				}
			}
		}
		return null;
	}

	public WidgetItem getInventoryItemMenu(Collection<String> menuOptions)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if (inventoryWidget != null)
		{
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items)
			{
				String[] menuActions = itemManager.getItemComposition(item.getId()).getInventoryActions();
				for (String action : menuActions)
				{
					if (action != null && menuOptions.contains(action))
					{
						return item;
					}
				}
			}
		}
		return null;
	}

	public WidgetItem getInventoryWidgetItemMenu(ItemManager itemManager, String menuOption, int opcode)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if (inventoryWidget != null)
		{
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items)
			{
				String[] menuActions = itemManager.getItemComposition(item.getId()).getInventoryActions();
				for (String action : menuActions)
				{
					if (action != null && action.equals(menuOption))
					{
						return item;
					}
				}
			}
		}
		return null;
	}

	public int getInventoryItemCount(int id, boolean stackable)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		int total = 0;
		if (inventoryWidget != null)
		{
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items)
			{
				if (item.getId() == id)
				{
					if (stackable)
					{
						return item.getQuantity();
					}
					total++;
				}
			}
		}
		return total;
	}

	public int getInventoryItemStackableQuantity(int id)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		int total = 0;
		if (inventoryWidget != null)
		{
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items)
			{
				if (item.getId() == id)
				{
					total++;
				}
			}
		}
		return total;
	}

	public boolean inventoryContains(String itemName)
	{
		return getAllInventoryItems().stream().anyMatch(wi -> wi.getWidget().getName().contains(itemName));
	}
	public boolean inventoryContains(int itemName)
	{
		if (client.getItemContainer(InventoryID.INVENTORY) == null) {
			return false;
		}

		return new InventoryItemQuery(InventoryID.INVENTORY)
				.idEquals(itemName)
				.result(client)
				.size() >= 1;
	}

	public boolean inventoryContainsStack(int itemID, int minStackAmount)
	{
		Item item = new InventoryItemQuery(InventoryID.INVENTORY)
				.idEquals(itemID)
				.result(client)
				.first();

		return item != null && item.getQuantity() >= minStackAmount;
	}

	public boolean inventoryItemContainsAmount(int id, int amount, boolean stackable, boolean exactAmount)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		int total = 0;
		if (inventoryWidget != null)
		{
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items)
			{
				if (item.getId() == id)
				{
					if (stackable)
					{
						total = item.getQuantity();
						break;
					}
					total++;
				}
			}
		}
		return (!exactAmount || total == amount) && (total >= amount);
	}

	public boolean inventoryItemContainsAmount(Collection<Integer> ids, int amount, boolean stackable, boolean exactAmount)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		int total = 0;
		if (inventoryWidget != null)
		{
			Collection<WidgetItem> items = getAllInventoryItems();
			for (WidgetItem item : items)
			{
				if (ids.contains(item.getId()))
				{
					if (stackable)
					{
						total = item.getQuantity();
						break;
					}
					total++;
				}
			}
		}
		return (!exactAmount || total == amount) && (total >= amount);
	}


	public boolean inventoryContains(int... ids)
	{
		if (client.getItemContainer(InventoryID.INVENTORY) == null)
		{
			return false;
		}
		return new InventoryItemQuery(InventoryID.INVENTORY).idEquals(ids).result(client).size() > 0;
	}

	public boolean inventoryContains(Collection<Integer> itemIds)
	{
		if (client.getItemContainer(InventoryID.INVENTORY) == null)
		{
			return false;
		}
		return getInventoryItems(itemIds).size() > 0;
	}

	public boolean inventoryContainsAllOf(Collection<Integer> itemIds)
	{
		if (client.getItemContainer(InventoryID.INVENTORY) == null)
		{
			return false;
		}
		for (int item : itemIds)
		{
			if (!inventoryContains(item))
			{
				return false;
			}
		}
		return true;
	}

	public boolean inventoryContainsExcept(Collection<Integer> itemIds)
	{
		if (client.getItemContainer(InventoryID.INVENTORY) == null)
		{
			return false;
		}
		Collection<WidgetItem> inventoryItems = getAllInventoryItems();
		List<Integer> depositedItems = new ArrayList<>();

		for (WidgetItem item : inventoryItems)
		{
			if (!itemIds.contains(item.getId()))
			{
				return true;
			}
		}
		return false;
	}

	public void dropItem(WidgetItem item)
	{
		useItem(item.getId(), MenuAction.ITEM_FIRST_OPTION);
	}

	public void dropAllExcept(Collection<Integer> ids, boolean dropAll, int minDelayBetween, int maxDelayBetween)
	{
		if (isBankOpen() || isDepositBoxOpen())
		{
			log.info("can't drop item, bank is open");
			return;
		}
		Collection<WidgetItem> inventoryItems = getAllInventoryItems();
		//executorService.submit(() ->
		//{
			try
			{
				iterating = true;
				for (WidgetItem item : inventoryItems)
				{
					if (ids.contains(item.getId()))
					{
						log.info("not dropping item: " + item.getId());
						continue;
					}
					//sleep(minDelayBetween, maxDelayBetween);
					dropItem(item);
					if (!dropAll)
					{
						break;
					}
				}
				iterating = false;
			}
			catch (Exception e)
			{
				iterating = false;
				e.printStackTrace();
			}
		//});
	}

	public boolean isDepositBoxOpen()
	{
		return client.getWidget(WidgetInfo.DEPOSIT_BOX_INVENTORY_ITEMS_CONTAINER) != null;
	}

	public boolean isBankOpen()
	{
		return client.getItemContainer(InventoryID.BANK) != null;
	}

	public int getBankMenuOpcode(int bankID)
	{
		return BANK_CHECK_BOX.contains(bankID) ? MenuAction.GAME_OBJECT_FIRST_OPTION.getId() :
				MenuAction.GAME_OBJECT_SECOND_OPTION.getId();
	}


	public boolean bankContains(String itemName)
	{
		if (isBankOpen())
		{
			ItemContainer bankItemContainer = client.getItemContainer(InventoryID.BANK);

			for (Item item : bankItemContainer.getItems())
			{
				if (itemManager.getItemComposition(item.getId()).getName().equalsIgnoreCase(itemName))
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean bankContainsAnyOf(int... ids)
	{
		if (isBankOpen())
		{
			ItemContainer bankItemContainer = client.getItemContainer(InventoryID.BANK);

			return new BankItemQuery().idEquals(ids).result(client).size() > 0;
		}
		return false;
	}

	public boolean bankContainsAnyOf(Collection<Integer> ids)
	{
		if (isBankOpen())
		{
			ItemContainer bankItemContainer = client.getItemContainer(InventoryID.BANK);
			for (int id : ids)
			{
				if (new BankItemQuery().idEquals(ids).result(client).size() > 0)
				{
					return true;
				}
			}
			return false;
		}
		return false;
	}

	public boolean bankContains(String itemName, int minStackAmount)
	{
		if (isBankOpen())
		{
			ItemContainer bankItemContainer = client.getItemContainer(InventoryID.BANK);

			for (Item item : bankItemContainer.getItems())
			{
				if (itemManager.getItemComposition(item.getId()).getName().equalsIgnoreCase(itemName) && item.getQuantity() >= minStackAmount)
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean bankContains(int itemID, int minStackAmount)
	{
		if (isBankOpen())
		{
			ItemContainer bankItemContainer = client.getItemContainer(InventoryID.BANK);
			final WidgetItem bankItem;
			if (bankItemContainer != null)
			{
				for (Item item : bankItemContainer.getItems())
				{
					if (item.getId() == itemID)
					{
						return item.getQuantity() >= minStackAmount;
					}
				}
			}
		}
		return false;
	}

	public boolean bankContains(int itemID)
	{
		if (isBankOpen())
		{
			clientThread.invokeLater(() -> {
				ItemContainer bankItemContainer = client.getItemContainer(InventoryID.BANK);
				final WidgetItem bankItem;
				if (client.isClientThread())
				{
					bankItem = new BankItemQuery().idEquals(itemID).result(client).first();
				}
				else
				{
					bankItem = new BankItemQuery().idEquals(itemID).result(client).first();
				}

				return bankItem != null && bankItem.getQuantity() >= 1;
			});
		}
		return false;
	}

	public Widget getBankItemWidget(int id)
	{
		if (!isBankOpen())
		{
			return null;
		}

		WidgetItem bankItem = new BankItemQuery().idEquals(id).result(client).first();
		if (bankItem != null)
		{
			return bankItem.getWidget();
		}
		else
		{
			return null;
		}
	}

	public Widget getBankItemWidgetAnyOf(int... ids)
	{
		if (!isBankOpen())
		{
			return null;
		}

		WidgetItem bankItem = new BankItemQuery().idEquals(ids).result(client).first();
		if (bankItem != null)
		{
			return bankItem.getWidget();
		}
		else
		{
			return null;
		}
	}

	public Widget getBankItemWidgetAnyOf(Collection<Integer> ids)
	{
		if (!isBankOpen() && !isDepositBoxOpen())
		{
			return null;
		}

		WidgetItem bankItem = new BankItemQuery().idEquals(ids).result(client).first();
		if (bankItem != null)
		{
			return bankItem.getWidget();
		}
		else
		{
			return null;
		}
	}


	public void depositAll()
	{
		if (!isBankOpen() && !isDepositBoxOpen())
		{
			return;
		}

		Widget depositInventoryWidget = client.getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
		if ((depositInventoryWidget != null))
		{
			targetMenu = new NewMenuEntry("", "", 1, MenuAction.CC_OP.getId(), -1, isDepositBoxOpen() ? 12582916 : 786474, false);
			doInvoke(targetMenu, depositInventoryWidget.getBounds());
		}
	}

	public void depositEquipped()
	{
		if (!isBankOpen() && !isDepositBoxOpen()) {
			return;
		}
		Widget depositInventoryWidget = client.getWidget(WidgetInfo.BANK_DEPOSIT_EQUIPMENT);

		if ((depositInventoryWidget != null))
		{
			targetMenu = new NewMenuEntry("", "", 1, MenuAction.CC_OP.getId(), -1, isDepositBoxOpen() ? 12582918 : 786476, false);
			doInvoke(targetMenu, depositInventoryWidget.getBounds());
		}

	}

	public void depositAllExcept(Collection<Integer> ids) {
		if (!isBankOpen() && !isDepositBoxOpen()) {
			return;
		}
		Collection<WidgetItem> inventoryItems = getAllInventoryItems();
		List<Integer> depositedItems = new ArrayList<>();

		try {
			iterating = true;
			for (WidgetItem item : inventoryItems) {
				if (!ids.contains(item.getId()) && item.getId() != 6512 && !depositedItems.contains(item.getId())) //6512 is empty widget slot
				{
					log.info("depositing item: " + item.getId());
					depositAllOfItem(item);
					sleep(80, 200);
					depositedItems.add(item.getId());
				}
			}
			iterating = false;
			depositedItems.clear();
		} catch (Exception e) {
			iterating = false;
			e.printStackTrace();
		}
	}

	public void depositAllOfItem(WidgetItem item)
	{
		assert !client.isClientThread();

		if (!isBankOpen() && !isDepositBoxOpen())
		{
			return;
		}
		boolean depositBox = isDepositBoxOpen();
		doInvoke(null, item.getCanvasBounds());
	}

	public void depositAllOfItem(int itemID)
	{
		if (!isBankOpen() && !isDepositBoxOpen())
		{
			return;
		}
		depositAllOfItem(getInventoryWidgetItem(Collections.singletonList(itemID)));
	}

	public void depositAllOfItems(Collection<Integer> itemIDs) {
		if (!isBankOpen() && !isDepositBoxOpen()) {
			return;
		}
		Collection<WidgetItem> inventoryItems = getAllInventoryItems();
		List<Integer> depositedItems = new ArrayList<>();

		try {
			iterating = true;
			for (WidgetItem item : inventoryItems) {
				if (itemIDs.contains(item.getId()) && !depositedItems.contains(item.getId())) //6512 is empty widget slot
				{
					log.info("depositing item: " + item.getId());
					depositAllOfItem(item);
					sleep(80, 170);
					depositedItems.add(item.getId());
				}
			}
			iterating = false;
			depositedItems.clear();
		} catch (Exception e) {
			iterating = false;
			e.printStackTrace();
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


	public void click(Rectangle rectangle)
	{
		assert !client.isClientThread();

		Point point = getClickPoint(rectangle);
		click(point);
	}

	public void click(Point point)
	{
		assert !client.isClientThread();

		if (client.isStretchedEnabled())
		{
			final Dimension stretched = client.getStretchedDimensions();
			final Dimension real = client.getRealDimensions();
			final double width = (stretched.width / real.getWidth());
			final double height = (stretched.height / real.getHeight());
			point = new Point((int) (point.getX() * width), (int) (point.getY() * height));
		}
		mouseEvent(MouseEvent.MOUSE_PRESSED, point, false);
		mouseEvent(MouseEvent.MOUSE_RELEASED, point, false);
		mouseEvent(MouseEvent.MOUSE_CLICKED, point, false);
	}

	public void moveClick(Rectangle rectangle)
	{
		assert !client.isClientThread();

		Point point = getClickPoint(rectangle);
		moveClick(point);
	}

	public void moveClick(Point point)
	{
		assert !client.isClientThread();

		if (client.isStretchedEnabled())
		{
			final Dimension stretched = client.getStretchedDimensions();
			final Dimension real = client.getRealDimensions();
			final double width = (stretched.width / real.getWidth());
			final double height = (stretched.height / real.getHeight());
			point = new Point((int) (point.getX() * width), (int) (point.getY() * height));
		}
		//mouseEvent(MouseEvent.MOUSE_ENTERED, point);
		//mouseEvent(MouseEvent.MOUSE_EXITED, point);
		mouseEvent(MouseEvent.MOUSE_MOVED, point, false);
		mouseEvent(MouseEvent.MOUSE_PRESSED, point, false);
		mouseEvent(MouseEvent.MOUSE_RELEASED, point, false);
		mouseEvent(MouseEvent.MOUSE_CLICKED, point, false);
	}

	public Point getClickPoint(Rectangle rect)
	{
		final int x = (int) (rect.getX() + getRandomIntBetweenRange((int) rect.getWidth() / 6 * -1, (int) rect.getWidth() / 6) + rect.getWidth() / 2);
		final int y = (int) (rect.getY() + getRandomIntBetweenRange((int) rect.getHeight() / 6 * -1, (int) rect.getHeight() / 6) + rect.getHeight() / 2);

		return new Point(x, y);
	}

	public void moveMouseEvent(Rectangle rectangle)
	{
		assert !client.isClientThread();

		Point point = getClickPoint(rectangle);
		moveClick(point);
	}

	public void moveMouseEvent(Point point)
	{
		assert !client.isClientThread();

		if (client.isStretchedEnabled())
		{
			final Dimension stretched = client.getStretchedDimensions();
			final Dimension real = client.getRealDimensions();
			final double width = (stretched.width / real.getWidth());
			final double height = (stretched.height / real.getHeight());
			point = new Point((int) (point.getX() * width), (int) (point.getY() * height));
		}
		mouseEvent(MouseEvent.MOUSE_ENTERED, point, false);
		mouseEvent(MouseEvent.MOUSE_EXITED, point, false);
		mouseEvent(MouseEvent.MOUSE_MOVED, point, false);
	}

	public int getRandomIntBetweenRange(int min, int max)
	{
		//return (int) ((Math.random() * ((max - min) + 1)) + min); //This does not allow return of negative values
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}


	private void mouseEvent(int id, Point point, boolean rightClick)
	{
		MouseEvent e = new MouseEvent(
				client.getCanvas(), id,
				System.currentTimeMillis(),
				0, point.getX(), point.getY(),
				1, rightClick, 1
		);

		client.getCanvas().dispatchEvent(e);
	}

	public static final Set<Integer> ALL_BANKS = ImmutableSet.of(ObjectID.BANK_BOOTH,
			ObjectID.BANK_BOOTH_10083,
			ObjectID.BANK_BOOTH_10355,
			ObjectID.BANK_BOOTH_10357,
			ObjectID.BANK_BOOTH_10517,
			ObjectID.BANK_CHEST_10562,
			ObjectID.BANK_BOOTH_10583,
			ObjectID.BANK_BOOTH_10584,
			ObjectID.BANK,
			ObjectID.BANK_BOOTH_11338,
			ObjectID.BANK_BOOTH_12798,
			ObjectID.BANK_BOOTH_12799,
			ObjectID.BANK_BOOTH_12800,
			ObjectID.BANK_BOOTH_12801,
			ObjectID.BANK_BOOTH_14367,
			ObjectID.BANK_BOOTH_14368,
			ObjectID.BANK_BOOTH_16642,
			ObjectID.BANK_BOOTH_16700,
			ObjectID.BANK_BOOTH_18491,
			ObjectID.BANK_BOOTH_20325,
			ObjectID.BANK_BOOTH_20326,
			ObjectID.BANK_BOOTH_20327,
			ObjectID.BANK_BOOTH_20328,
			ObjectID.BANK_CHEST_21301,
			ObjectID.BANK_BOOTH_22819,
			NULL_34810,
			ObjectID.BANK_BOOTH_24101,
			ObjectID.BANK_BOOTH_24347,
			ObjectID.BANK_BOOTH_25808,
			ObjectID.BANK_CHEST_26707,
			ObjectID.BANK_CHEST_26711,
			ObjectID.BANK_BOOTH_27254,
			ObjectID.BANK_BOOTH_27260,
			ObjectID.BANK_BOOTH_27263,
			ObjectID.BANK_BOOTH_27265,
			ObjectID.BANK_BOOTH_27267,
			ObjectID.BANK_BOOTH_27292,
			ObjectID.BANK_BOOTH_27718,
			ObjectID.BANK_BOOTH_27719,
			ObjectID.BANK_BOOTH_27720,
			ObjectID.BANK_BOOTH_27721,
			ObjectID.BANK_BOOTH_28429,
			ObjectID.BANK_BOOTH_28430,
			ObjectID.BANK_BOOTH_28431,
			ObjectID.BANK_BOOTH_28432,
			ObjectID.BANK_BOOTH_28433,
			ObjectID.BANK_BOOTH_28546,
			ObjectID.BANK_BOOTH_28547,
			ObjectID.BANK_BOOTH_28548,
			ObjectID.BANK_BOOTH_28549,
			ObjectID.BANK_BOOTH_32666,
			ObjectID.BANK_BOOTH_36559,
			ObjectID.BANK_BOOTH_37959,
			ObjectID.BANK_BOOTH_39238,
			ObjectID.BANK_CHEST,
			ObjectID.BANK_CHEST_4483,
			ObjectID.BANK_CHEST_14382,
			ObjectID.BANK_CHEST_14886,
			ObjectID.BANK_CHEST_16695,
			ObjectID.BANK_CHEST_16696,
			ObjectID.BANK_CHEST_19051,
			ObjectID.BANK_CHEST_28594,
			ObjectID.BANK_CHEST_28595,
			ObjectID.BANK_CHEST_28816,
			ObjectID.BANK_CHEST_28861,
			ObjectID.BANK_CHEST_29321,
			ObjectID.BANK_CHEST_30087,
			ObjectID.BANK_CHEST_30267,
			ObjectID.BANK_CHEST_30926,
			ObjectID.BANK_CHEST_30989,
			ObjectID.BANK_BOX,
			ObjectID.BANK_BOX_31949,
			ObjectID.BANK_CHEST_41315,
			ObjectID.BANK_CHEST_34343,
			ObjectID.BANK_DEPOSIT_BOX,
			ObjectID.BANK_DEPOSIT_CHEST,
			ObjectID.BANK_DEPOSIT_BOX_25937,
			ObjectID.BANK_DEPOSIT_BOX_26254,
			ObjectID.BANK_DEPOSIT_BOX_29103,
			ObjectID.BANK_DEPOSIT_BOX_29104,
			ObjectID.BANK_DEPOSIT_BOX_29105,
			ObjectID.BANK_DEPOSIT_BOX_29106,
			ObjectID.BANK_DEPOSIT_POT,
			ObjectID.BANK_DEPOSIT_BOX_29327,
			ObjectID.BANK_DEPOSIT_BOX_30268,
			ObjectID.BANK_DEPOSIT_BOX_31726,
			ObjectID.BANK_DEPOSIT_BOX_32665,
			ObjectID.BANK_DEPOSIT_BOX_34344,
			ObjectID.BANK_DEPOSIT_BOX_36086,
			ObjectID.BANK_DEPOSIT_BOX_39239);


	public static final Set<Integer> NO_DEPOSIT_BOXES = ImmutableSet.of(ObjectID.BANK_BOOTH,
			ObjectID.BANK_BOOTH_10083,
			ObjectID.BANK_BOOTH_10355,
			ObjectID.BANK_BOOTH_10357,
			ObjectID.BANK_BOOTH_10517,
			ObjectID.BANK_CHEST_10562,
			ObjectID.BANK_BOOTH_10583,
			ObjectID.BANK_BOOTH_10584,
			ObjectID.BANK,
			ObjectID.BANK_BOOTH_11338,
			ObjectID.BANK_BOOTH_12798,
			ObjectID.BANK_BOOTH_12799,
			ObjectID.BANK_BOOTH_12800,
			ObjectID.BANK_BOOTH_12801,
			ObjectID.BANK_BOOTH_14367,
			ObjectID.BANK_BOOTH_14368,
			ObjectID.BANK_BOOTH_16642,
			ObjectID.BANK_BOOTH_16700,
			ObjectID.BANK_BOOTH_18491,
			ObjectID.BANK_BOOTH_20325,
			ObjectID.BANK_BOOTH_20326,
			ObjectID.BANK_BOOTH_20327,
			ObjectID.BANK_BOOTH_20328,
			ObjectID.BANK_CHEST_21301,
			ObjectID.BANK_BOOTH_22819,
			NULL_34810,
			ObjectID.BANK_BOOTH_24101,
			ObjectID.BANK_BOOTH_24347,
			ObjectID.BANK_BOOTH_25808,
			ObjectID.BANK_CHEST_26707,
			ObjectID.BANK_CHEST_26711,
			ObjectID.BANK_BOOTH_27254,
			ObjectID.BANK_BOOTH_27260,
			ObjectID.BANK_BOOTH_27263,
			ObjectID.BANK_BOOTH_27265,
			ObjectID.BANK_BOOTH_27267,
			ObjectID.BANK_BOOTH_27292,
			ObjectID.BANK_BOOTH_27718,
			ObjectID.BANK_BOOTH_27719,
			ObjectID.BANK_BOOTH_27720,
			ObjectID.BANK_BOOTH_27721,
			ObjectID.BANK_BOOTH_28429,
			ObjectID.BANK_BOOTH_28430,
			ObjectID.BANK_BOOTH_28431,
			ObjectID.BANK_BOOTH_28432,
			ObjectID.BANK_BOOTH_28433,
			ObjectID.BANK_BOOTH_28546,
			ObjectID.BANK_BOOTH_28547,
			ObjectID.BANK_BOOTH_28548,
			ObjectID.BANK_BOOTH_28549,
			ObjectID.BANK_BOOTH_32666,
			ObjectID.BANK_BOOTH_36559,
			ObjectID.BANK_BOOTH_37959,
			ObjectID.BANK_BOOTH_39238,
			ObjectID.BANK_CHEST,
			ObjectID.BANK_CHEST_4483,
			ObjectID.BANK_CHEST_14382,
			ObjectID.BANK_CHEST_14886,
			ObjectID.BANK_CHEST_16695,
			ObjectID.BANK_CHEST_41315,
			ObjectID.BANK_CHEST_41493,
			ObjectID.BANK_CHEST_16696,
			ObjectID.BANK_CHEST_19051,
			ObjectID.BANK_CHEST_28594,
			ObjectID.BANK_CHEST_28595,
			ObjectID.BANK_CHEST_28816,
			ObjectID.BANK_CHEST_28861,
			ObjectID.BANK_CHEST_29321,
			ObjectID.BANK_CHEST_30087,
			ObjectID.BANK_CHEST_30267,
			ObjectID.BANK_CHEST_30926,
			ObjectID.BANK_CHEST_30989,
			ObjectID.BANK_BOX,
			ObjectID.BANK_BOX_31949,
			ObjectID.BANK_CHEST_34343
	);

	public static final Set<Integer> BANK_SET = ImmutableSet.of(
			ObjectID.BANK_BOOTH,
			ObjectID.BANK_BOOTH_10083,
			ObjectID.BANK_BOOTH_10355,
			ObjectID.BANK_BOOTH_10357,
			ObjectID.BANK_BOOTH_10517,
			ObjectID.BANK_CHEST_10562,
			ObjectID.BANK_BOOTH_10584,
			ObjectID.BANK,
			ObjectID.BANK_BOOTH_11338,
			ObjectID.BANK_BOOTH_12798,
			ObjectID.BANK_BOOTH_12799,
			ObjectID.BANK_BOOTH_12800,
			ObjectID.BANK_BOOTH_12801,
			ObjectID.BANK_BOOTH_14367,
			ObjectID.BANK_BOOTH_14368,
			ObjectID.BANK_BOOTH_16642,
			ObjectID.BANK_BOOTH_16700,
			ObjectID.BANK_BOOTH_18491,
			ObjectID.BANK_BOOTH_20325,
			ObjectID.BANK_BOOTH_20326,
			ObjectID.BANK_BOOTH_20327,
			ObjectID.BANK_BOOTH_20328,
			ObjectID.BANK_CHEST_21301,
			ObjectID.BANK_BOOTH_22819,
			NULL_34810,
			ObjectID.BANK_BOOTH_24101,
			ObjectID.BANK_BOOTH_24347,
			ObjectID.BANK_BOOTH_25808,
			ObjectID.BANK_CHEST_26707,
			ObjectID.BANK_CHEST_26711,
			ObjectID.BANK_BOOTH_27254,
			ObjectID.BANK_BOOTH_27260,
			ObjectID.BANK_BOOTH_27263,
			ObjectID.BANK_BOOTH_27265,
			ObjectID.BANK_BOOTH_27267,
			ObjectID.BANK_BOOTH_27292,
			ObjectID.BANK_BOOTH_27718,
			ObjectID.BANK_BOOTH_27719,
			ObjectID.BANK_BOOTH_27720,
			ObjectID.BANK_BOOTH_27721,
			ObjectID.BANK_BOOTH_28429,
			ObjectID.BANK_BOOTH_28430,
			ObjectID.BANK_BOOTH_28431,
			ObjectID.BANK_BOOTH_28432,
			ObjectID.BANK_BOOTH_28433,
			ObjectID.BANK_BOOTH_28546,
			ObjectID.BANK_BOOTH_28547,
			ObjectID.BANK_BOOTH_28548,
			ObjectID.BANK_BOOTH_28549,
			ObjectID.BANK_BOOTH_32666,
			ObjectID.BANK_BOOTH_36559,
			ObjectID.BANK_BOOTH_37959,
			ObjectID.BANK_BOOTH_39238
	);

	public static final Set<Integer> BANK_CHECK_BOX = ImmutableSet.of(
			ObjectID.BANK_CHEST,
			ObjectID.BANK_CHEST_4483,
			ObjectID.BANK_CHEST_14382,
			ObjectID.BANK_CHEST_14886,
			ObjectID.BANK_CHEST_16695,
			ObjectID.BANK_CHEST_16696,
			ObjectID.BANK_CHEST_19051,
			ObjectID.BANK_CHEST_28594,
			ObjectID.BANK_CHEST_28595,
			ObjectID.BANK_CHEST_28816,
			ObjectID.BANK_CHEST_28861,
			ObjectID.BANK_CHEST_29321,
			ObjectID.BANK_CHEST_30087,
			ObjectID.BANK_CHEST_30267,
			ObjectID.BANK_CHEST_30926,
			ObjectID.BANK_CHEST_30989,
			ObjectID.BANK_BOX,
			ObjectID.BANK_BOX_31949,
			ObjectID.BANK_CHEST_34343,
			ObjectID.BANK_DEPOSIT_BOX,
			ObjectID.BANK_DEPOSIT_CHEST,
			ObjectID.BANK_DEPOSIT_BOX_25937,
			ObjectID.BANK_DEPOSIT_BOX_26254,
			ObjectID.BANK_DEPOSIT_BOX_29103,
			ObjectID.BANK_DEPOSIT_BOX_29104,
			ObjectID.BANK_DEPOSIT_BOX_29105,
			ObjectID.BANK_DEPOSIT_BOX_29106,
			ObjectID.BANK_DEPOSIT_POT,
			ObjectID.BANK_DEPOSIT_BOX_29327,
			ObjectID.BANK_DEPOSIT_BOX_30268,
			ObjectID.BANK_DEPOSIT_BOX_31726,
			ObjectID.BANK_DEPOSIT_BOX_32665,
			ObjectID.BANK_DEPOSIT_BOX_34344,
			ObjectID.BANK_DEPOSIT_BOX_36086,
			ObjectID.BANK_DEPOSIT_BOX_39239
	);

	public void useItem(int ID, String option) {
		WidgetItem item = getWidgetItem(ID);
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if ((client.getVarcIntValue(VarClientInt.INVENTORY_TAB) != 3 && client.getWidget(WidgetInfo.BANK_CONTAINER) == null)) {
			if (item.getWidget().isHidden() || item == null) {
				pressKey(VK_F3);
			}
		}
		if (item != null) {
			doInvoke(null, item.getCanvasBounds());
		}
	}
	public void activatePrayer(WidgetInfo prayer) {
		Widget prayerWidget = client.getWidget(prayer);
		if (client.getWidget(WidgetInfo.BANK_CONTAINER) == null) {
			if (prayerWidget.isHidden() || prayer == null) {
				pressKey(VK_F2);
				//sleep(500);
			}
		}
		if (prayer != null) {
			doInvoke(null, prayerWidget.getBounds());
		}
	}
	public void clickSpell(WidgetInfo spell) {
		Widget spellWidgt = client.getWidget(spell);
		if (client.getWidget(WidgetInfo.BANK_CONTAINER) == null) {
			if (spellWidgt.isHidden() || spellWidgt == null) {
				pressKey(VK_F4);
			}
		}
		if (spellWidgt != null) {
			doInvoke(null, spellWidgt.getBounds());
		}
	}

	public void useItem(int ID, MenuAction option) {
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if ((client.getVarcIntValue(VarClientInt.INVENTORY_TAB) != 3 && client.getWidget(WidgetInfo.BANK_CONTAINER) == null)) {
			if (inventoryWidget.isHidden() || inventoryWidget == null) {
				pressKey(VK_F3);
			}
		}
		WidgetItem item = getWidgetItem(ID);
		if (item != null) {
			doInvoke(null, item.getCanvasBounds());
		}
	}

	public void clickWidget(WidgetInfo widget) {
		Widget item = client.getWidget(widget);
		if (item != null) {
			doInvoke(null, item.getBounds());
		}
	}
	public void clickWidget(Widget widget) {
		if (widget != null) {
			doInvoke(null, widget.getBounds());
		}
	}

	public void attackPlayerDirect(Player npc){
		try {
			if (npc.getConvexHull() == null) {
				walking.walkTileOnScreen(npc.getWorldLocation());
			}
			else
				doInvoke(null, npc.getConvexHull().getBounds());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void attackNPCDirect(NPC npc){
		try {
			targetMenu = new NewMenuEntry("", "", npc.getIndex(), MenuAction.NPC_SECOND_OPTION.getId(), 0, 0, false);
			if (npc.getConvexHull() == null) {
				walking.walkTileOnScreen(npc.getWorldLocation());
			}
			else
			doInvoke(targetMenu, npc.getConvexHull().getBounds());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void attackNPC(int npcID){
		try {
			targetMenu = new NewMenuEntry("", "", findNearestNpc(npcID).getIndex(), MenuAction.NPC_SECOND_OPTION.getId(), 0, 0, false);
			if (findNearestNpc(npcID).getConvexHull() == null) {
				walking.walkTileOnScreen(findNearestNpc(npcID).getWorldLocation());
			}
			else
			doInvoke(targetMenu, findNearestNpc(npcID).getConvexHull().getBounds());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	public void attackNPC(String npc){
		try {
			targetMenu = new NewMenuEntry("", "", findNearestNpc(npc).getIndex(), MenuAction.NPC_SECOND_OPTION.getId(), 0, 0, false);
			if (findNearestNpc(npc).getConvexHull() == null) {
				walking.walkTileOnScreen(findNearestNpc(npc).getWorldLocation());
			}
			else
			doInvoke(targetMenu, findNearestNpc(npc).getConvexHull().getBounds());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void specialAttack(){
		try {
			boolean spec_enabled = (client.getVar(VarPlayer.SPECIAL_ATTACK_ENABLED) == 1);

			if (spec_enabled) {
				return;
			}
			clickWidget(WidgetInfo.MINIMAP_SPEC_ORB);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void walkToMinimapTile(WorldPoint point) {
		walking.walkToWorldPointUsingMinimap(point);
	}

	public void useWallObjectDirect(WallObject targetObject, long sleepDelay, int opcode)
	{
		if(targetObject!=null) {
			targetMenu = new NewMenuEntry("", "",targetObject.getId(),opcode,targetObject.getLocalLocation().getSceneX(),targetObject.getLocalLocation().getSceneY(), false);
			if (targetObject.getConvexHull() == null) {
				walking.walkTileOnScreen(targetObject.getWorldLocation());
			}
			else
			doInvoke(targetMenu, targetObject.getConvexHull().getBounds());
		}
	}


	public void useGameObjectDirect(GameObject targetObject)
	{
		if(targetObject!=null) {
			targetMenu = new NewMenuEntry("", "", targetObject.getId(), null, targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(), false);
			if (targetObject.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) >= 10) {
				walking.walkTileOnScreen(targetObject.getWorldLocation());
			}
			else {
				doInvoke(targetMenu, targetObject.getConvexHull().getBounds());
			}
		}
	}

	public void interactNPC(NPC npc)
	{
		if(npc!=null){
			if (npc.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) >= 10) {
				walking.walkTileOnScreen(npc.getWorldLocation());
			}
			else {
				doInvoke(targetMenu, npc.getConvexHull().getBounds());
			}
		}
	}

	public void interactNPC(int... id)
	{
		NPC targetObject = findNearestNpc(id);
		if(targetObject!=null){
			//targetMenu = new NewMenuEntry("", "", targetObject.getIndex(), opcode, 0, 0, false);
			if (targetObject.getConvexHull() == null) {
				walking.walkTileOnScreen(targetObject.getWorldLocation());
			}
			else
			doInvoke(null, targetObject.getConvexHull().getBounds());
		}
	}
	public Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-getRandomIntBetweenRange(0,2),client.getCanvasHeight()-getRandomIntBetweenRange(0,2));
	}
	public void useGameObject(int id, int opcode, long sleepDelay)
	{
		GameObject targetObject = findNearestGameObject(id);
		if(targetObject!=null){
			targetMenu = new NewMenuEntry("", "", targetObject.getId(),opcode,targetObject.getLocalLocation().getSceneX(),targetObject.getLocalLocation().getSceneY(), false);
			if (targetObject.getConvexHull() == null) {
				walking.walkTileOnScreen(targetObject.getWorldLocation());
			}
			else
			doInvoke(targetMenu, targetObject.getConvexHull().getBounds());
		}
	}

	public void useGroundObject(int id, int opcode, long sleepDelay)
	{
		GroundObject targetObject = findNearestGroundObject(id);
		if(targetObject!=null){
			targetMenu = new NewMenuEntry("", "", targetObject.getId(),opcode,targetObject.getLocalLocation().getSceneX(),targetObject.getLocalLocation().getSceneY(), false);
			if (targetObject.getConvexHull() == null) {
				walking.walkTileOnScreen(targetObject.getWorldLocation());
			}
			else
			doInvoke(targetMenu, targetObject.getConvexHull().getBounds());
		}
	}
	public void useGroundObject(GroundObject targetObject, int opcode, long sleepDelay)
	{
		if(targetObject!=null){
			if (targetObject.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) >= 10) {
				walking.walkTileOnScreen(targetObject.getWorldLocation());
			}
			else {
				doInvoke(targetMenu, targetObject.getConvexHull().getBounds());
			}
		}
	}

	public void useDecorativeObject(int id, int opcode, long sleepDelay)
	{
		DecorativeObject decorativeObject = findNearestDecorativeObject(id);
		if(decorativeObject!=null){
			targetMenu = new NewMenuEntry("", "",  decorativeObject.getId(),opcode,decorativeObject.getLocalLocation().getSceneX(), decorativeObject.getLocalLocation().getSceneY(), false);
			if (decorativeObject.getConvexHull() == null) {
				walking.walkTileOnScreen(decorativeObject.getWorldLocation());
			}
			else
			doInvoke(targetMenu, decorativeObject.getConvexHull().getBounds());
		}
	}

	public void depositOneOfItem(WidgetItem item)
	{
		if (!isBankOpen() && !isDepositBoxOpen() || item == null)
		{
			return;
		}
		boolean depositBox = isDepositBoxOpen();
		doInvoke(null, item.getCanvasBounds());
	}

	public void depositOneOfItem(int itemID)
	{
		if (!isBankOpen() && !isDepositBoxOpen())
		{
			return;
		}
		depositOneOfItem(getInventoryWidgetItem(Collections.singletonList(itemID)));
	}

	public void withdrawAllItem(Widget bankItemWidget)
	{
		//targetMenu = new NewMenuEntry("", "", 7, MenuAction.CC_OP.getId(), bankItemWidget.getIndex(), 786445, false);
		doInvoke(null, bankItemWidget.getBounds());
	}

	public void withdrawAllItem(int bankItemID)
	{
		Widget item = getBankItemWidget(bankItemID);
		if (item != null)
		{
			withdrawAllItem(item);	//UNUSABLE
		}
	}

	public void withdrawItem(Widget bankItemWidget)
	{
		doInvoke(null, bankItemWidget.getBounds());
	}

	public void withdrawItem(int bankItemID)
	{
		Widget item = getBankItemWidget(bankItemID);
		if (item != null)
		{
			withdrawItem(item);
		}
	}

	public void withdrawAnyOf(int... bankItemIDs)
	{
		Widget item = getBankItemWidgetAnyOf(bankItemIDs);
		if (item != null)
		{
			withdrawItem(item);
		}
	}

	public void withdrawItemAmount(int bankItemID, int amount, boolean noted)
	{
		clientThread.invokeLater(() -> {
			Widget item = getBankItemWidget(bankItemID);	//Withdraw-X = SetVar 6590 -> 3
			if (item != null)
			{
				int identifier;
				switch (amount)
				{
					case 1:
						identifier = (client.getVarbitValue(6590) == 0) ? 1 : 2; //1 unnoted 2 noted
						break;
					case 5:
						identifier = 3;
						break;
					case 10:
						identifier = 4;
						break;
					default:
						identifier = 6;
						break;
				}
				if (!noted && client.getVarbitValue(6590) == 1) {
					Widget unnoteItems = client.getWidget(12, 22);
					clickWidget(unnoteItems);
				}
				if (noted && client.getVarbitValue(6590) == 0) {
					Widget noteItems = client.getWidget(12, 24);
					clickWidget(noteItems);
				}
				if (identifier == 1 || identifier == 2) {
					Widget withdraw1 = client.getWidget(12, 28);
					clickWidget(withdraw1);
				}
				else if (identifier == 3) {
					Widget withdraw5 = client.getWidget(12, 30);
					clickWidget(withdraw5);
				}
				else if (identifier == 4) {
					Widget withdraw10 = client.getWidget(12, 32);
					clickWidget(withdraw10);
				}
				else if (identifier == 6) {
					Widget withdrawX = client.getWidget(12, 34);
					clickWidget(withdrawX);
				}
				//targetMenu = new NewMenuEntry("", "", identifier, MenuAction.CC_OP.getId(), item.getIndex(), 786445, false);
				doInvoke(targetMenu, item.getBounds());
				//delayMouseClick(new Rectangle(0, 0), random(100, 300));
				//clientThread.invoke(() -> client.invokeMenuAction("", "", identifier, MenuAction.CC_OP.getId(), item.getIndex(), 786445));
				if (identifier == 6)
				{
					//executorService.submit(() -> {
						sleep(getRandomIntBetweenRange(1000, 1500));
						typeString(String.valueOf(amount));
						sleep(getRandomIntBetweenRange(80, 250));
						pressKey(VK_ENTER);
					//});
				}
			}
		});
	}

	public void sleep(int minSleep, int maxSleep)
	{
		sleep(random(minSleep, maxSleep));
	}

	public void sleep(int toSleep)
	{
		try
		{
			long start = System.currentTimeMillis();
			Thread.sleep(toSleep);

			// Guarantee minimum sleep
			long now;
			while (start + toSleep > (now = System.currentTimeMillis()))
			{
				Thread.sleep(start + toSleep - now);
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public void sleep(long toSleep)
	{
		try
		{
			long start = System.currentTimeMillis();
			Thread.sleep(toSleep);

			// Guarantee minimum sleep
			long now;
			while (start + toSleep > (now = System.currentTimeMillis()))
			{
				Thread.sleep(start + toSleep - now);
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	//Ganom's function, generates a random number allowing for curve and weight
	public long randomDelay(boolean weightedDistribution, int min, int max, int deviation, int target)
	{
		if (weightedDistribution)
		{
			return (long) clamp((-Math.log(Math.abs(random.nextGaussian()))) * deviation + target, min, max);
		}
		else
		{
			return (long) clamp(Math.round(random.nextGaussian() * deviation + target), min, max);
		}
	}

	private double clamp(double val, int min, int max)
	{
		return Math.max(min, Math.min(max, val));
	}

	public static double random(double min, double max) {
		return Math.min(min, max) + random.nextDouble() * Math.abs(max - min);
	}

	public static int random(int min, int max) {
		int n = Math.abs(max - min);
		return Math.min(min, max) + (n == 0 ? 0 : random.nextInt(n));
	}

	static void resumePauseWidget(int widgetId, int arg) {
		final int garbageValue = 1292618906;
		final String className = "ln";
		final String methodName = "hs";

		try
		{

			Class clazz = Class.forName(className);
			Method method = clazz.getDeclaredMethod(methodName, int.class, int.class, int.class);
			method.setAccessible(true);
			method.invoke(null, widgetId, arg, garbageValue);
		}
		catch (Exception ignored)
		{
			return;
		}
	}


	private void setSelectSpell(WidgetInfo info)
	{
		final Widget widget = client.getWidget(info);
		handleMouseMove2(widget.getBounds());
	}

	public void setMenuEntry(MenuEntry menuEntry)
	{
		targetMenu = menuEntry;
	}

	public void setMenuEntry(MenuEntry menuEntry, boolean consume)
	{
		targetMenu = menuEntry;
		consumeClick = consume;
	}

	public void setModifiedMenuEntry(NewMenuEntry menuEntry, int itemID, int itemIndex, int opCode)
	{
		targetMenu = menuEntry;
		modifiedMenu = true;
		modifiedItemID = itemID;
		modifiedItemIndex = itemIndex;
		modifiedOpCode = opCode;
	}

	public void doInvoke(MenuEntry entry, Rectangle point) {
		targetMenu = entry;
		handleMouseClick(point);
	}
	public void doInvoke(MenuEntry entry, Point point) {
		targetMenu = entry;
		handleMouseClick(point);
	}


	@Subscribe
	private void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getType() == MenuAction.CC_OP.getId() && (event.getActionParam1() == WidgetInfo.WORLD_SWITCHER_LIST.getId() ||
				event.getActionParam1() == 11927560 || event.getActionParam1() == 4522007 || event.getActionParam1() == 24772686))
		{
			return;
		}

		if (targetMenu != null)
		{
			MenuEntry[] entries = client.getMenuEntries();
			entries[0] = client.createMenuEntry(-1).setOption(targetMenu.getOption()).setTarget(targetMenu.getTarget()).setIdentifier(targetMenu.getIdentifier()).setType(targetMenu.getType()).setParam0(targetMenu.getParam0()).setParam1(targetMenu.getParam1()).setForceLeftClick(true);
			client.setMenuEntries(entries);
		}
	}



	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() == MenuAction.CC_OP && (event.getWidgetId() == WidgetInfo.WORLD_SWITCHER_LIST.getId() ||
				event.getWidgetId() == 11927560 || event.getWidgetId() == 4522007 || event.getWidgetId() == 24772686))
		{
			//Either logging out or world-hopping which is handled by 3rd party plugins so let them have priority
			log.info("Received world-hop/login related click. Giving them priority");
			targetMenu = null;
			return;
		}
		//if (event.getMenuOption().contains("Walk") || event.getMenuAction() == MenuAction.WALK) {
		//	event.consume();
		//	log.info("CONSUMED A WALK EVENT");
		//} //TODO REMOVED FOR TESTING



		/*if (targetMenu != null)
		{
			if (consumeClick)
			{
				event.consume();
				log.info("Consuming a click and not sending anything else");
				consumeClick = false;
				targetMenu = null;
				return;
			}
			setMenuEntry(targetMenu);
			log.info("Setting custom menu entry");
			menuAction(event, targetMenu.getOption(), targetMenu.getTarget(), targetMenu.getIdentifier(), targetMenu.getType(),
					targetMenu.getParam0(), targetMenu.getParam1());
			targetMenu = null;
			return;
		}*/
	}

	public void menuAction(MenuOptionClicked menuOptionClicked, String option, String target, int identifier, MenuAction menuAction, int param0, int param1)
	{
		log.info(menuOptionClicked.toString());
	}

	public boolean pointIntersectIgnoringPlane(WorldPoint a, WorldPoint b){
		return a.getX() == b.getX() && a.getY() == b.getY();
	}

	public boolean areaIntersectIgnoringPlane(WorldArea a, WorldArea b){
		a = new WorldArea(new WorldPoint(a.getX(),a.getY(),0),a.getWidth(),a.getHeight());
		b = new WorldArea(new WorldPoint(b.getX(),b.getY(),0),b.getWidth(),b.getHeight());
		return a.intersectsWith(b);
	}

}


