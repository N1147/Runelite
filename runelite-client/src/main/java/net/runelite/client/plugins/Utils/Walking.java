package net.runelite.client.plugins.Utils;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;

public class Walking {
    private Tile lastDestination;
    private Tile lastStep;

    /**
     * Determines whether or not a given tile is in the loaded map area.
     *
     * @param tile The tile to check.
     * @return <code>true</code> if local; otherwise <code>false</code>.
     */
    public boolean isLocal(final Tile tile) {
        int[][] flags = client.getCollisionMaps()[client.getPlane()].getFlags();
        int x = tile.getWorldLocation().getX() - client.getBaseX();
        int y = tile.getWorldLocation().getY() - client.getBaseY();
        return (flags != null && x >= 0 && y >= 0 && x < flags.length && y < flags.length);
    }

    @Inject
    Core core;
    @Inject Calculations calc;

    public boolean walkTileMM(final WorldPoint t) {
        WorldPoint dest = new WorldPoint(t.getX(), t.getY(), t.getPlane());

        if (!calc.tileOnMap(dest)) {
            dest = getClosestTileOnMap(dest);
        }
        Point p = calc.tileToMinimap(dest);
        if (p.getX() != -1 && p.getY() != -1) {
            Point p2 = calc.tileToMinimap(dest);
            if (p2 == null) { // methods.mouse takes time, if character got far enough (i.e. died) p2 will be null
                return false;
            }
            if (p2.getX() != -1 && p2.getY() != -1) {
                if (!client.getMouseCanvasPosition().equals(p2)) {//We must've moved while walking, move again!
                    //methods.mouse.move(p2);
                }
                if (!client.getMouseCanvasPosition().equals(p2)) {//Get exact since we're moving... should be removed?
                    //methods.mouse.hop(p2);
                }
                core.moveClick(p2);
                return true;
            }
        }
        return false;
    }

    public void walkToWorldPointUsingMinimap(WorldPoint tileToWalk) {
    /*    LocalPoint localPoint = LocalPoint.fromWorld(client, calc.getTileOnScreen(tileToWalk));
        core.targetMenu = new NewMenuEntry("", "", 0, MenuAction.WALK,0 ,0, false);
        core.moveClick(Perspective.localToCanvas(client, localPoint, client.getPlane()));

     */
        //Point localPoint = calc.tileToMinimap(tileToWalk);
        //
        //
        //core.targetMenu = new NewMenuEntry("", "", 0, MenuAction.WALK, 0, 0, false);
        if (calc.tileOnMap(tileToWalk)) {
            Point p = calc.tileToMinimap(tileToWalk);
            core.moveClick(p);
            core.sendGameMessage("Walking to Point");
            return;
        } else {
            WorldPoint loc = client.getLocalPlayer().getWorldLocation();
            WorldPoint walk = new WorldPoint((loc.getX() + tileToWalk.getX()) / 2,
                    (loc.getY() + tileToWalk.getY()) / 2, tileToWalk.getPlane());
            Point p2 = calc.tileToMinimap(walk);
            core.moveClick(p2);
            core.sendGameMessage("Walking halfway to Point");
            return;
        }
    }

    /**
    * Walks to a given in-game tile using the minimap
    * if not visible/available will walk half-way
     */
    public void walkTileOnScreen(WorldPoint tileToWalk) {
    /*    LocalPoint localPoint = LocalPoint.fromWorld(client, calc.getTileOnScreen(tileToWalk));
        core.targetMenu = new NewMenuEntry("", "", 0, MenuAction.WALK,0 ,0, false);
        core.moveClick(Perspective.localToCanvas(client, localPoint, client.getPlane()));

     */
        //Point localPoint = calc.tileToMinimap(tileToWalk);
        //
        //
        core.targetMenu = new NewMenuEntry("", "", 0, MenuAction.WALK, 0, 0, false);
        if (calc.tileOnMap(tileToWalk)) {
            Point p = calc.tileToMinimap(tileToWalk);
            core.moveClick(p);
            return;
        } else {
            WorldPoint loc = client.getLocalPlayer().getWorldLocation();
            WorldPoint walk = new WorldPoint((loc.getX() + tileToWalk.getX()) / 2,
                    (loc.getY() + tileToWalk.getY()) / 2, tileToWalk.getPlane());
            Point p2 = calc.tileToMinimap(walk);
            core.moveClick(p2);
            return;
        }
    }

   // public void walkTileOnScreen(Point tileToWalk) {
   //     core.moveClick(tileToWalk);
   //     return;
   // }

    /**
     * Returns the closest tile on the minimap to a given tile.
     *
     * @param tile The destination tile.
     * @return Returns the closest tile to the destination on the minimap.
     */
    public WorldPoint getClosestTileOnMap(WorldPoint tile) {
        if (!calc.tileOnMap(tile)) {
            WorldPoint loc = client.getLocalPlayer().getWorldLocation();
            WorldPoint walk = new WorldPoint((loc.getX() + tile.getX()) / 2,
                    (loc.getY() + tile.getY()) / 2, tile.getPlane());
            return calc.tileOnMap(walk) ? walk
                    : getClosestTileOnMap(walk);//
        }
        return tile;
    }

    @Inject
    Client client;

    /**
     * Gets the destination tile (where the flag is on the minimap). If there is
     * no destination currently, null will be returned.
     *
     * @return The current destination tile, or null.
     */
    public WorldPoint getDestination() {

        return (client.getLocalDestinationLocation() != null) ? new WorldPoint(client.getLocalDestinationLocation().getX(),
                client.getLocalDestinationLocation().getY(), client.getPlane()) : null;
    }


    /**
     * Reverses an array of tiles.
     *
     * @param other The <code>RSTile</code> path array to reverse.
     * @return The reverse <code>RSTile</code> path for the given <code>RSTile</code>
     * path.
     */
    @Deprecated
    public Tile[] reversePath(Tile[] other) {
        Tile[] t = new Tile[other.length];
        for (int i = 0; i < t.length; i++) {
            t[i] = other[other.length - i - 1];
        }
        return t;
    }

    /**
     * Returns the next tile to walk to on a path.
     *
     * @param path The path.
     * @return The next <code>RSTile</code> to walk to on the provided path; or
     * <code>null</code> if far from path or at destination.
     */
    @Deprecated
    public WorldPoint nextTile(WorldPoint path[]) {
        return nextTile(path, 17);
    }


    public int distanceTo(WorldPoint t) {
        return t == null ? -1 : (int) distanceBetween(client.getLocalPlayer().getWorldLocation(), t);
    }


    public double distanceBetween(WorldPoint curr, WorldPoint dest) {
        return Math.sqrt((curr.getX() - dest.getX()) *
                (curr.getX() - dest.getX()) +
                (curr.getY() - dest.getY()) *
                        (curr.getY() - dest.getY()));
    }

    @Deprecated
    public WorldPoint nextTile(WorldPoint path[], int skipDist) {
        int dist = 99;
        int closest = -1;
        for (int i = path.length - 1; i >= 0; i--) {
            WorldPoint tile = path[i];
            int d = distanceTo(tile);
            if (d < dist) {
                dist = d;
                closest = i;
            }
        }

        int feasibleTileIndex = -1;

        for (int i = closest; i < path.length; i++) {

            if (distanceTo(path[i]) <= skipDist) {
                feasibleTileIndex = i;
            } else {
                break;
            }
        }

        if (feasibleTileIndex == -1) {
            return null;
        } else {
            return path[feasibleTileIndex];
        }
    }

}
