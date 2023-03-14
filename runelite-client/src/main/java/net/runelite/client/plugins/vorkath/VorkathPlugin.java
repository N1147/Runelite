/*
 * Copyright (c) 2018, https://openosrs.com
 * Copyright (c) 2019, Infinitay <https://github.com/Infinitay>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.vorkath;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.*;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.vorkath.VorkathPlugin.*;


@PluginDescriptor(
	name = "Vorkath (Helper)",
	enabledByDefault = false,
	description = "Vorkath helper.",
	tags = {"combat", "overlay", "pve", "pvm"}
)
@Slf4j
public class VorkathPlugin extends Plugin
{
	private static final int VORKATH_REGION = 9023;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AcidPathOverlay acidPathOverlay;

	@Getter(AccessLevel.PACKAGE)
	private Vorkath vorkath;

	@Getter(AccessLevel.PACKAGE)
	private NPC zombifiedSpawn;

	@Getter(AccessLevel.PACKAGE)
	private List<WorldPoint> acidSpots = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private List<WorldPoint> acidFreePath = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private WorldPoint[] wooxWalkPath = new WorldPoint[2];

	@Getter(AccessLevel.PACKAGE)
	private long wooxWalkTimer = -1;

	@Getter(AccessLevel.PACKAGE)
	private Rectangle wooxWalkBar;
	private int lastAcidSpotsSize = 0;

	public static final int VORKATH_WAKE_UP = 7950;
	public static final int VORKATH_DEATH = 7949;
	public static final int VORKATH_SLASH_ATTACK = 7951;
	public static final int VORKATH_ATTACK = 7952;
	public static final int VORKATH_FIRE_BOMB_OR_SPAWN_ATTACK = 7960;
	public static final int VORKATH_ACID_ATTACK = 7957;

	@Override
	protected void shutDown()
	{
		reset();
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event)
	{
		if (!isAtVorkath())
		{
			return;
		}

		final NPC npc = event.getNpc();

		if (npc.getName() == null)
		{
			return;
		}

		if (npc.getName().equals("Vorkath"))
		{
			vorkath = new Vorkath(npc);
		}
		else if (npc.getName().equals("Zombified Spawn"))
		{
			zombifiedSpawn = npc;
		}
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event)
	{
		if (!isAtVorkath())
		{
			return;
		}

		final NPC npc = event.getNpc();

		if (npc.getName() == null)
		{
			return;
		}

		if (npc.getName().equals("Vorkath"))
		{
			reset();
		}
		else if (npc.getName().equals("Zombified Spawn"))
		{
			zombifiedSpawn = null;
		}
	}

	@Subscribe
	private void onProjectileSpawned(ProjectileSpawned event)
	{
		if (!isAtVorkath() || vorkath == null)
		{
			return;
		}

		final Projectile proj = event.getProjectile();
		final VorkathAttack vorkathAttack = VorkathAttack.getVorkathAttack(proj.getId());

		if (vorkathAttack != null)
		{
			if (VorkathAttack.isBasicAttack(vorkathAttack.getProjectileID()) && vorkath.getAttacksLeft() > 0)
			{
				vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
			}
			else if (vorkathAttack == VorkathAttack.ACID)
			{
				vorkath.updatePhase(Vorkath.Phase.ACID);
				vorkath.setAttacksLeft(0);
			}
			else if (vorkathAttack == VorkathAttack.FIRE_BALL)
			{
				vorkath.updatePhase(Vorkath.Phase.FIRE_BALL);
				vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
			}
			else if (vorkathAttack == VorkathAttack.FREEZE_BREATH || vorkathAttack == VorkathAttack.ZOMBIFIED_SPAWN)
			{
				vorkath.updatePhase(Vorkath.Phase.SPAWN);
				vorkath.setAttacksLeft(0);
			}
			else
			{
				vorkath.updatePhase(vorkath.getNextPhase());
				vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
			}

			log.debug("[Vorkath ({})] {}", vorkathAttack, vorkath);
			vorkath.setLastAttack(vorkathAttack);
		}
	}

	@Subscribe
	private void onProjectileMoved(ProjectileMoved event)
	{
		if (!isAtVorkath())
		{
			return;
		}

		final Projectile proj = event.getProjectile();
		final LocalPoint loc = event.getPosition();

		if (proj.getId() == ProjectileID.VORKATH_POISON_POOL_AOE)
		{
			addAcidSpot(WorldPoint.fromLocal(client, loc));
		}
	}

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!isAtVorkath())
		{
			return;
		}

		final GameObject obj = event.getGameObject();

		if (obj.getId() == ObjectID.ACID_POOL || obj.getId() == ObjectID.ACID_POOL_32000)
		{
			addAcidSpot(obj.getWorldLocation());
		}
	}

	@Subscribe
	private void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (!isAtVorkath())
		{
			return;
		}

		final GameObject obj = event.getGameObject();

		if (obj.getId() == ObjectID.ACID_POOL || obj.getId() == ObjectID.ACID_POOL_32000)
		{
			acidSpots.remove(obj.getWorldLocation());
		}
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged event)
	{
		if (!isAtVorkath())
		{
			return;
		}

		final Actor actor = event.getActor();

		if (isAtVorkath() && vorkath != null && actor.equals(vorkath.getVorkath())
			&& actor.getAnimation() == VorkathAttack.SLASH_ATTACK.getVorkathAnimationID())
		{
			if (vorkath.getAttacksLeft() > 0)
			{
				vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
			}
			else
			{
				vorkath.updatePhase(vorkath.getNextPhase());
				vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
			}
			log.debug("[Vorkath (SLASH_ATTACK)] {}", vorkath);
		}
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		if (!isAtVorkath())
		{
			return;
		}

		// Update the acid free path every tick to account for player movement
		if (!acidSpots.isEmpty())
		{
			calculateAcidFreePath();
		}

		// Start the timer when the player walks into the WooxWalk zone
		if (wooxWalkPath[0] != null && wooxWalkPath[1] != null)
		{
			final WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();

			if (playerLoc.getX() == wooxWalkPath[0].getX() && playerLoc.getY() == wooxWalkPath[0].getY()
				&& playerLoc.getPlane() == wooxWalkPath[0].getPlane())
			{
				if (wooxWalkTimer == -1)
				{
					wooxWalkTimer = System.currentTimeMillis() - 400;
				}
			}
			else if (playerLoc.getX() == wooxWalkPath[1].getX() && playerLoc.getY() == wooxWalkPath[1].getY()
				&& playerLoc.getPlane() == wooxWalkPath[1].getPlane())
			{
				if (wooxWalkTimer == -1)
				{
					wooxWalkTimer = System.currentTimeMillis() - 1000;
				}
			}
			else if (wooxWalkTimer != -1)
			{
				wooxWalkTimer = -1;
			}
		}
	}

	@Subscribe
	private void onClientTick(ClientTick event)
	{
		if (acidSpots.size() != lastAcidSpotsSize)
		{
			if (acidSpots.size() == 0)
			{
				overlayManager.remove(acidPathOverlay);
				acidFreePath.clear();
				Arrays.fill(wooxWalkPath, null);
				wooxWalkTimer = -1;
			}
			else {
				calculateAcidFreePath();
				calculateWooxWalkPath();
				overlayManager.add(acidPathOverlay);
			}

			lastAcidSpotsSize = acidSpots.size();
		}
	}

	/**
	 * @return true if the player is in the Vorkath region, false otherwise
	 */
	private boolean isAtVorkath()
	{
		return ArrayUtils.contains(client.getMapRegions(), VORKATH_REGION);
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
		final WorldPoint vorkLoc = vorkath.getVorkath().getWorldLocation();
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

					if (currentPath.size() >= 5 && currentClicksRequired < bestClicksRequired
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

	private void calculateWooxWalkPath()
	{
		wooxWalkTimer = -1;

		updateWooxWalkBar();

		if (client.getLocalPlayer() == null || vorkath.getVorkath() == null)
		{
			return;
		}

		final WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
		final WorldPoint vorkLoc = vorkath.getVorkath().getWorldLocation();

		final int maxX = vorkLoc.getX() + 14;
		final int minX = vorkLoc.getX() - 8;
		final int baseX = playerLoc.getX();
		final int baseY = vorkLoc.getY() - 5;
		final int middleX = vorkLoc.getX() + 3;

		// Loop through the arena tiles in the x-direction and
		// alternate between positive and negative x direction
		for (int i = 0; i < 50; i++)
		{
			// Make sure we always choose the spot closest to
			// the middle of the arena
			int directionRemainder = 0;
			if (playerLoc.getX() < middleX)
			{
				directionRemainder = 1;
			}

			int deviation = (int) Math.floor(i / 2.0);
			if (i % 2 == directionRemainder)
			{
				deviation = -deviation;
			}

			final WorldPoint attackLocation = new WorldPoint(baseX + deviation, baseY, playerLoc.getPlane());
			final WorldPoint outOfRangeLocation = new WorldPoint(baseX + deviation, baseY - 1, playerLoc.getPlane());

			if (acidSpots.contains(attackLocation) || acidSpots.contains(outOfRangeLocation)
				|| attackLocation.getX() < minX || attackLocation.getX() > maxX)
			{
				continue;
			}

			wooxWalkPath[0] = attackLocation;
			wooxWalkPath[1] = outOfRangeLocation;

			break;
		}
	}

	private void updateWooxWalkBar()
	{
		// Update the WooxWalk tick indicator's dimensions
		// based on the canvas dimensions
		final Widget exp = client.getWidget(WidgetInfo.EXPERIENCE_TRACKER);

		if (exp == null)
		{
			return;
		}

		final Rectangle screen = exp.getBounds();

		int width = (int) Math.floor(screen.getWidth() / 2.0);
		if (width % 2 == 1)
		{
			width++;
		}
		int height = (int) Math.floor(width / 20.0);
		if (height % 2 == 1)
		{
			height++;
		}
		final int x = (int) Math.floor(screen.getX() + width / 2.0);
		final int y = (int) Math.floor(screen.getY() + screen.getHeight() - 2 * height);
		wooxWalkBar = new Rectangle(x, y, width, height);
	}

	private void reset()
	{
		overlayManager.remove(acidPathOverlay);
		vorkath = null;
		acidSpots.clear();
		acidFreePath.clear();
		Arrays.fill(wooxWalkPath, null);
		wooxWalkTimer = -1;
		zombifiedSpawn = null;
	}
}

class AcidPathOverlay extends Overlay
{
	private static final Color ACID_SPOTS_COLOR = Color.GREEN;
	private static final Color ACID_FREE_PATH_COLOR = Color.PINK;
	private static final Color WOOXWALK_ATTACK_SPOT_COLOR = Color.YELLOW;
	private static final Color WOOXWALK_OUT_OF_REACH_SPOT_COLOR = Color.RED;
	private static final int BAR_INDICATOR_SPACER = 5;

	private final Client client;
	private final VorkathPlugin plugin;

	@javax.inject.Inject
	public AcidPathOverlay(final Client client, final VorkathPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.getVorkath() == null || plugin.getVorkath().getVorkath().getLocalLocation() == null)
		{
			return null;
		}

		if (plugin.getAcidSpots() != null
				&& !plugin.getAcidSpots().isEmpty())
		{
			for (WorldPoint acidWorldPoint : plugin.getAcidSpots())
			{
				LocalPoint acidLocalPoint = LocalPoint.fromWorld(client, acidWorldPoint);
				if (acidLocalPoint == null)
				{
					continue;
				}
				OverlayUtil.renderPolygon(graphics, Perspective.getCanvasTilePoly(client,
						acidLocalPoint), ACID_SPOTS_COLOR);
			}
		}

		if (plugin.getAcidFreePath() != null
				&& !plugin.getAcidFreePath().isEmpty())
		{
			for (WorldPoint acidFreeWorldPoint : plugin.getAcidFreePath())
			{
				LocalPoint acidFreeLocalPoint = LocalPoint.fromWorld(client, acidFreeWorldPoint);
				if (acidFreeLocalPoint == null)
				{
					continue;
				}

				OverlayUtil.renderPolygon(graphics, Perspective.getCanvasTilePoly(client,
						acidFreeLocalPoint), ACID_FREE_PATH_COLOR);
			}
		}

		if (plugin.getWooxWalkPath()[0] != null
				&& plugin.getWooxWalkPath()[1] != null)
		{
			LocalPoint attackLocalPoint = LocalPoint.fromWorld(client, plugin.getWooxWalkPath()[0]);
			LocalPoint outOfReachLocalPoint = LocalPoint.fromWorld(client, plugin.getWooxWalkPath()[1]);

			if (attackLocalPoint != null && outOfReachLocalPoint != null)
			{
				OverlayUtil.renderPolygon(graphics, Perspective.getCanvasTilePoly(client,
						attackLocalPoint), Color.YELLOW);
				OverlayUtil.renderPolygon(graphics, Perspective.getCanvasTilePoly(client,
						outOfReachLocalPoint), Color.RED);

				if (plugin.getWooxWalkBar() != null
						&& plugin.getWooxWalkTimer() != -1)
				{
					int[] xpointsAttack = {
							(int) (plugin.getWooxWalkBar().getX() + plugin.getWooxWalkBar().getWidth() / 2.0 + 1),
							(int) (plugin.getWooxWalkBar().getX() + plugin.getWooxWalkBar().getWidth()),
							(int) (plugin.getWooxWalkBar().getX() + plugin.getWooxWalkBar().getWidth()),
							(int) (plugin.getWooxWalkBar().getX() + plugin.getWooxWalkBar().getWidth() / 2 + 1)
					};
					int[] xpointsOutOfReach = {
							(int) plugin.getWooxWalkBar().getX(),
							(int) (plugin.getWooxWalkBar().getX() + plugin.getWooxWalkBar().getWidth() / 2.0),
							(int) (plugin.getWooxWalkBar().getX() + plugin.getWooxWalkBar().getWidth() / 2.0),
							(int) plugin.getWooxWalkBar().getX()
					};
					int[] ypointsBoth = {
							(int) plugin.getWooxWalkBar().getY(),
							(int) plugin.getWooxWalkBar().getY(),
							(int) (plugin.getWooxWalkBar().getY() + plugin.getWooxWalkBar().getHeight()),
							(int) (plugin.getWooxWalkBar().getY() + plugin.getWooxWalkBar().getHeight())
					};
					Polygon wooxWalkAttack = new Polygon(xpointsAttack, ypointsBoth, 4);
					Polygon wooxWalkOutOfReach = new Polygon(xpointsOutOfReach, ypointsBoth, 4);
					OverlayUtil.renderPolygon(graphics, wooxWalkAttack, WOOXWALK_ATTACK_SPOT_COLOR);
					OverlayUtil.renderPolygon(graphics, wooxWalkOutOfReach, WOOXWALK_OUT_OF_REACH_SPOT_COLOR);

					long timeLeft = (System.currentTimeMillis() - plugin.getWooxWalkTimer()) % 1200;
					double timeScale;
					if (timeLeft <= 600)
					{
						timeScale = 1 - timeLeft / 600.0;
					}
					else
					{
						timeLeft -= 600;
						timeScale = timeLeft / 600.0;
					}
					int progress = (int) Math.round(plugin.getWooxWalkBar().getWidth() * timeScale);

					int[] xpointsIndicator = {
							(int) (plugin.getWooxWalkBar().getX() - plugin.getWooxWalkBar().getHeight() / 2 + progress),
							(int) (plugin.getWooxWalkBar().getX() + plugin.getWooxWalkBar().getHeight() / 2 + progress),
							(int) plugin.getWooxWalkBar().getX() + progress
					};
					int[] ypointsIndicator = {
							(int) (plugin.getWooxWalkBar().getY() - plugin.getWooxWalkBar().getHeight() - BAR_INDICATOR_SPACER),
							(int) (plugin.getWooxWalkBar().getY() - plugin.getWooxWalkBar().getHeight() - BAR_INDICATOR_SPACER),
							(int) (plugin.getWooxWalkBar().getY() - BAR_INDICATOR_SPACER)
					};
					Polygon indicator = new Polygon(xpointsIndicator, ypointsIndicator, 3);
					OverlayUtil.renderPolygon(graphics, indicator, Color.WHITE);
				}
			}
		}
		return null;
	}
}

@Data
class Vorkath
{
	static final int ATTACKS_PER_SWITCH = 6;
	static final int FIRE_BALL_ATTACKS = 25;

	private NPC vorkath;
	private VorkathAttack lastAttack;
	private Phase currentPhase;
	private Phase nextPhase;
	private Phase lastPhase;
	private int attacksLeft;

	public Vorkath(NPC vorkath)
	{
		this.vorkath = vorkath;
		this.attacksLeft = ATTACKS_PER_SWITCH;
		this.currentPhase = Phase.UNKNOWN;
		this.nextPhase = Phase.UNKNOWN;
		this.lastPhase = Phase.UNKNOWN;
	//	log.debug("[Vorkath] Created Vorkath: {}", this);
	}

	/**
	 * Updates the existing Vorkath object depending on the new phase it is currently on
	 *
	 * @param newPhase the new phase Vorkath is current on
	 */
	void updatePhase(Phase newPhase)
	{
		Phase oldLastPhase = this.lastPhase;
		Phase oldCurrentPhase = this.currentPhase;
		Phase oldNextPhase = this.currentPhase;
		int oldAttacksLeft = this.attacksLeft;

		this.lastPhase = this.currentPhase;
		this.currentPhase = newPhase;
		switch (newPhase)
		{
			case ACID:
				this.nextPhase = Phase.FIRE_BALL;
				break;
			case FIRE_BALL:
				this.nextPhase = Phase.SPAWN;
				break;
			case SPAWN:
				this.nextPhase = Phase.ACID;
				break;
			default:
				this.nextPhase = Phase.UNKNOWN;
				break;
		}

		if (this.currentPhase == Phase.FIRE_BALL)
		{
			this.attacksLeft = FIRE_BALL_ATTACKS;
		}
		else
		{
			this.attacksLeft = ATTACKS_PER_SWITCH;
		}

		//log.debug("[Vorkath] Update! Last Phase: {}->{}, Current Phase: {}->{}, Next Phase: {}->{}, Attacks: {}->{}",
		//		oldLastPhase, this.lastPhase, oldCurrentPhase, this.currentPhase, oldNextPhase, this.nextPhase, oldAttacksLeft, this.attacksLeft);
	}

	enum Phase
	{
		UNKNOWN,
		ACID,
		FIRE_BALL,
		SPAWN
	}
}
@AllArgsConstructor
@Getter(AccessLevel.PACKAGE)
enum VorkathAttack
{
	/**
	 * Vorkath's melee attack (see VorkathPlugin#onAnimationChanged)
	 */
	SLASH_ATTACK(VORKATH_SLASH_ATTACK, -1),
	/**
	 * Vorkath's dragon breath attack
	 */
	FIRE_BREATH(VORKATH_ATTACK, ProjectileID.VORKATH_DRAGONBREATH),
	/**
	 * Vorkath's dragon breath attack causing the player's active prayers to be deactivated
	 */
	PRAYER_BREATH(VORKATH_ATTACK, ProjectileID.VORKATH_PRAYER_DISABLE),
	/**
	 * Vorkath's dragon breath attack causing the player to become poisoned with venom
	 */
	VENOM_BREATH(VORKATH_ATTACK, ProjectileID.VORKATH_VENOM),
	/**
	 * Vorkath's ranged attack
	 */
	SPIKE(VORKATH_ATTACK, ProjectileID.VORKATH_RANGED),
	/**
	 * Vorkath's magic attack
	 */
	ICE(VORKATH_ATTACK, ProjectileID.VORKATH_MAGIC),
	/**
	 * Vorkath's aoe fire bomb attack (3x3 from where player was originally standing)
	 */
	FIRE_BOMB(VORKATH_FIRE_BOMB_OR_SPAWN_ATTACK, ProjectileID.VORKATH_BOMB_AOE),
	/**
	 * Vorkath's aoe acid attacking, spewing acid across the instance
	 */
	ACID(VORKATH_ACID_ATTACK, ProjectileID.VORKATH_POISON_POOL_AOE),
	/**
	 * Vorkath's fire ball attack that is fired during the acid phase, almost every tick for 25(?) attacks total
	 */
	FIRE_BALL(VORKATH_ACID_ATTACK, ProjectileID.VORKATH_TICK_FIRE_AOE),
	/**
	 * Vorkath's dragon breath attack causing the player to be frozen during Zombified Spawn phase
	 */
	FREEZE_BREATH(VORKATH_ATTACK, ProjectileID.VORKATH_ICE),
	/**
	 * Vorkath's spawning of a Zombified Spawn
	 */
	ZOMBIFIED_SPAWN(VORKATH_FIRE_BOMB_OR_SPAWN_ATTACK, ProjectileID.VORKATH_SPAWN_AOE);

	private static final Map<Integer, VorkathAttack> VORKATH_ATTACKS;
	private static final Map<Integer, VorkathAttack> VORKATH_BASIC_ATTACKS;

	static
	{
		ImmutableMap.Builder<Integer, VorkathAttack> builder = new ImmutableMap.Builder<>();
		for (VorkathAttack vorkathAttack : values())
		{
			builder.put(vorkathAttack.getProjectileID(), vorkathAttack);
		}
		VORKATH_ATTACKS = builder.build();
	}

	static
	{
		ImmutableMap.Builder<Integer, VorkathAttack> builder = new ImmutableMap.Builder<>();
		builder.put(FIRE_BREATH.getProjectileID(), FIRE_BREATH)
				.put(PRAYER_BREATH.getProjectileID(), PRAYER_BREATH)
				.put(VENOM_BREATH.getProjectileID(), VENOM_BREATH)
				.put(SPIKE.getProjectileID(), SPIKE)
				.put(ICE.getProjectileID(), ICE)
				.put(FIRE_BOMB.getProjectileID(), FIRE_BOMB)
				.put(FIRE_BALL.getProjectileID(), FIRE_BALL);
		// FIRE_BOMB and FIRE_BALL are also basic attacks
		// Although SLASH_ATTACK is a basic attack, we're going to handle it differently
		VORKATH_BASIC_ATTACKS = builder.build();
	}

	private final int vorkathAnimationID;
	private final int projectileID;

	/**
	 * @param projectileID id of projectile
	 * @return {@link VorkathAttack} associated with the specified projectile
	 */
	public static VorkathAttack getVorkathAttack(int projectileID)
	{
		return VORKATH_ATTACKS.get(projectileID);
	}

	/**
	 * @param projectileID id of projectile
	 * @return true if the projectile id matches a {@link VorkathAttack#getProjectileID()} within {@link VorkathAttack#VORKATH_BASIC_ATTACKS},
	 * false otherwise
	 */
	public static boolean isBasicAttack(int projectileID)
	{
		return VORKATH_BASIC_ATTACKS.get(projectileID) != null;
	}
}

