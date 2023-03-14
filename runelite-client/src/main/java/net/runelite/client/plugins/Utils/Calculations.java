package net.runelite.client.plugins.Utils;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;

import static net.runelite.client.plugins.Utils.Core.random;

class Calculations {
    private final Render render = new Render();
    private final RenderData renderData = new RenderData();

    public Point tileToScreen(final WorldPoint tile, final double dX, final double dY, final int height) {
        return Perspective.localToCanvas(client, new LocalPoint(tile.getX(), tile.getY()), client.getPlane(), height);
    }

    /**
     * Checks whether a given tile is on the minimap.
     *
     * @param t The Tile to check.
     * @return <code>true</code> if the RSTile is on the minimap; otherwise
     * <code>false</code>.
     */
    public boolean tileOnMap(WorldPoint t) {
        return tileToMinimap(t) != null;
    }

    /**
     * Checks whether the centroid of a given tile is on the screen.
     *
     * @param t The RSTile to check.
     * @return <code>true</code> if the RSTile is on the screen; otherwise
     * <code>false</code>.
     */
    public boolean tileOnScreen(WorldPoint t) {
        Point point = tileToScreen(t, 0.5, 0.5, 0);
        return (point != null) && pointOnScreen(point);
    }

    /**
     * Returns the Point on screen where a given tile is shown on the minimap.
     *
     * @param t The RSTile to check.
     * @return <code>Point</code> within minimap; otherwise
     * <code>new Point(-1, -1)</code>.
     */

    @SuppressWarnings("unused")
    public Point tileToMinimap(WorldPoint t) {
        return worldToMinimap(t.getX(), t.getY());
    }

    /**
     * Checks whether a point is within the rectangle that determines the bounds
     * of game screen. This will work fine when in fixed mode. In resizable mode
     * it will exclude any points that are less than 253 pixels from the right
     * of the screen or less than 169 pixels from the bottom of the screen,
     * giving a rough area.
     *
     * @param check The point to check.
     * @return <code>true</code> if the point is within the rectangle; otherwise
     * <code>false</code>.
     */
    @Inject
    Client client;

    public boolean pointOnScreen(Point check) {
        int x = check.getX(), y = check.getY();
        return x > client.getViewportXOffset() && x < client.getViewportWidth()
                && y > client.getViewportYOffset() && y < client.getViewportHeight();
    }

    /**
     * Calculates the distance between two points.
     *
     * @param curr The first point.
     * @param dest The second point.
     * @return The distance between the two points, using the distance formula.
     */
    public double distanceBetween(Point curr, Point dest) {
        return Math.sqrt(((curr.getX() - dest.getX()) * (curr.getX() - dest.getX())) + ((curr.getY() - dest.getY()) * (curr.getY() - dest.getY())));
    }

    /**
     * Returns a random double in a specified range
     *
     * @param min Minimum value (inclusive).
     * @param max Maximum value (exclusive).
     * @return The random <code>double</code> generated.
     */
    public double random(double min, double max) {
        return Math.min(min, max) + random.nextDouble()
                * Math.abs(max - min);
    }

    /**
     * Will return the closest tile that is on screen to the given tile.
     *
     * @param tile Tile you want to get to.
     * @return <code>RSTile</code> that is onScreen.
     */
    public WorldPoint getTileOnScreen(WorldPoint tile) {
        try {
            if (tileOnScreen(tile)) {
                return tile;
            } else {
                WorldPoint loc = new WorldPoint(client.getLocalPlayer().getWorldLocation().getX(),
                        client.getLocalPlayer().getWorldLocation().getY(), client.getPlane());
                WorldPoint halfWayTile = new WorldPoint((tile.getX() +
                        loc.getX()) / 2, (tile.getY() +
                        loc.getY()) / 2, client.getPlane());

                if (tileOnScreen(halfWayTile)) {
                    return halfWayTile;
                } else {
                    return getTileOnScreen(halfWayTile);
                }
            }
        } catch (StackOverflowError soe) {
            return null;
        }
    }

    /**
     * Returns the length of the path generates between two RSTiles.
     *
     * @param start    The starting tile.
     * @param dest     The destination tile.
     * @param isObject <code>true</code> if reaching any tile adjacent to the destination
     *                 should be accepted.
     * @return <code>true</code> if reaching any tile adjacent to the destination
     * should be accepted.
     */
    public int pathLengthBetween(Tile start, Tile dest, boolean isObject) {
        return dijkstraDist(start.getWorldLocation().getX() - client.getBaseX(), // startX
                start.getWorldLocation().getY() - client.getBaseY(), // startY
                dest.getWorldLocation().getX() - client.getBaseX(), // destX
                dest.getWorldLocation().getY() - client.getBaseY(), // destY
                isObject); // if it's an object, accept any adjacent tile
    }

    /**
     * Returns the screen Point of given absolute x and y values in the game's
     * 3D plane.
     *
     * @param x x value based on the game plane.
     * @param y y value based on the game plane.
     * @return <code>Point</code> within minimap; otherwise
     * <code>new Point(-1, -1)</code>.
     */
    public Point worldToMinimap(double x, double y) {
        LocalPoint test = LocalPoint.fromWorld(client, (int) x, (int) y);
        if (test != null)
            return Perspective.localToMinimap(client, test, 5000);
        return null;
    }

    /**
     * Returns the screen location of a given point on the ground. This accounts
     * for the height of the ground at the given location.
     *
     * @param x      x value based on the game plane.
     * @param y      y value based on the game plane.
     * @param height height offset (normal to the ground).
     * @return <code>Point</code> based on screen; otherwise
     * <code>new Point(-1, -1)</code>.
     */
    public Point groundToScreen(final int x, final int y, final int height) {
        return Perspective.localToCanvas(client, x, y, height);
    }

    /**
     * Returns the height of the ground at the given location in the game world.
     *
     * @param x x value based on the game plane.
     * @param y y value based on the game plane.
     * @return The ground height at the given location; otherwise <code>0</code>
     * .
     */
    public int tileHeight(final int x, final int y) {
        return Perspective.getTileHeight(client, new LocalPoint(x, y), client.getPlane());

    }

    /**
     * Returns the screen location of a given 3D point in the game world.
     *
     * @param x x value on the game plane.
     * @param y y value on the game plane.
     * @param z z value on the game plane.
     * @return <code>Point</code> based on screen; otherwise
     * <code>new Point(-1, -1)</code>.
     */
    public Point worldToScreen(int x, int y, int z) {
        LocalPoint local = LocalPoint.fromWorld(client, x, y);
        if (local == null) {
            local = new LocalPoint(x, y);
        }
        return Perspective.localToCanvas(client, local, z);
    }

    /**
     * @param startX   the startX (0 < startX < 104)
     * @param startY   the startY (0 < startY < 104)
     * @param destX    the destX (0 < destX < 104)
     * @param destY    the destY (0 < destY < 104)
     * @param isObject if it's an object, it will find path which touches it.
     * @return The distance of the shortest path to the destination; or -1 if no
     * valid path to the destination was found.
     */
    private int dijkstraDist(final int startX, final int startY, final int destX, final int destY,
                             final boolean isObject) {
        final int[][] prev = new int[104][104];
        final int[][] dist = new int[104][104];
        final int[] path_x = new int[4000];
        final int[] path_y = new int[4000];
        for (int xx = 0; xx < 104; xx++) {
            for (int yy = 0; yy < 104; yy++) {
                prev[xx][yy] = 0;
                dist[xx][yy] = 99999999;
            }
        }
        int curr_x = startX;
        int curr_y = startY;
        prev[startX][startY] = 99;
        dist[startX][startY] = 0;
        int path_ptr = 0;
        int step_ptr = 0;
        path_x[path_ptr] = startX;
        path_y[path_ptr++] = startY;
        final byte blocks[][] = client.getTileSettings()[client.getPlane()];
        final int pathLength = path_x.length;
        boolean foundPath = false;
        while (step_ptr != path_ptr) {
            curr_x = path_x[step_ptr];
            curr_y = path_y[step_ptr];
            if (Math.abs(curr_x - destX) + Math.abs(curr_y - destY) == (isObject ? 1 : 0)) {
                foundPath = true;
                break;
            }
            step_ptr = (step_ptr + 1) % pathLength;
            final int cost = dist[curr_x][curr_y] + 1;
            // south
            if ((curr_y > 0) && (prev[curr_x][curr_y - 1] == 0) && ((blocks[curr_x + 1][curr_y] & 0x1280102) == 0)) {
                path_x[path_ptr] = curr_x;
                path_y[path_ptr] = curr_y - 1;
                path_ptr = (path_ptr + 1) % pathLength;
                prev[curr_x][curr_y - 1] = 1;
                dist[curr_x][curr_y - 1] = cost;
            }
            // west
            if ((curr_x > 0) && (prev[curr_x - 1][curr_y] == 0) && ((blocks[curr_x][curr_y + 1] & 0x1280108) == 0)) {
                path_x[path_ptr] = curr_x - 1;
                path_y[path_ptr] = curr_y;
                path_ptr = (path_ptr + 1) % pathLength;
                prev[curr_x - 1][curr_y] = 2;
                dist[curr_x - 1][curr_y] = cost;
            }
            // north
            if ((curr_y < 104 - 1) && (prev[curr_x][curr_y + 1] == 0) && ((blocks[curr_x + 1][curr_y + 2] &
                    0x1280120) == 0)) {
                path_x[path_ptr] = curr_x;
                path_y[path_ptr] = curr_y + 1;
                path_ptr = (path_ptr + 1) % pathLength;
                prev[curr_x][curr_y + 1] = 4;
                dist[curr_x][curr_y + 1] = cost;
            }
            // east
            if ((curr_x < 104 - 1) && (prev[curr_x + 1][curr_y] == 0) && ((blocks[curr_x + 2][curr_y + 1] &
                    0x1280180) == 0)) {
                path_x[path_ptr] = curr_x + 1;
                path_y[path_ptr] = curr_y;
                path_ptr = (path_ptr + 1) % pathLength;
                prev[curr_x + 1][curr_y] = 8;
                dist[curr_x + 1][curr_y] = cost;
            }
            // south west
            if ((curr_x > 0) && (curr_y > 0) && (prev[curr_x - 1][curr_y - 1] == 0) && ((blocks[curr_x][curr_y] &
                    0x128010e) == 0) && ((blocks[curr_x][curr_y + 1] & 0x1280108) == 0) && ((blocks[curr_x +
                    1][curr_y] & 0x1280102) == 0)) {
                path_x[path_ptr] = curr_x - 1;
                path_y[path_ptr] = curr_y - 1;
                path_ptr = (path_ptr + 1) % pathLength;
                prev[curr_x - 1][curr_y - 1] = 3;
                dist[curr_x - 1][curr_y - 1] = cost;
            }
            // north west
            if ((curr_x > 0) && (curr_y < 104 - 1) && (prev[curr_x - 1][curr_y + 1] == 0) && (
                    (blocks[curr_x][curr_y + 2] & 0x1280138) == 0) && ((blocks[curr_x][curr_y + 1] & 0x1280108) ==
                    0) && ((blocks[curr_x + 1][curr_y + 2] & 0x1280120) == 0)) {
                path_x[path_ptr] = curr_x - 1;
                path_y[path_ptr] = curr_y + 1;
                path_ptr = (path_ptr + 1) % pathLength;
                prev[curr_x - 1][curr_y + 1] = 6;
                dist[curr_x - 1][curr_y + 1] = cost;
            }
            // south east
            if ((curr_x < 104 - 1) && (curr_y > 0) && (prev[curr_x + 1][curr_y - 1] == 0) && ((blocks[curr_x +
                    2][curr_y] & 0x1280183) == 0) && ((blocks[curr_x + 2][curr_y + 1] & 0x1280180) == 0) && (
                    (blocks[curr_x + 1][curr_y] & 0x1280102) == 0)) {
                path_x[path_ptr] = curr_x + 1;
                path_y[path_ptr] = curr_y - 1;
                path_ptr = (path_ptr + 1) % pathLength;
                prev[curr_x + 1][curr_y - 1] = 9;
                dist[curr_x + 1][curr_y - 1] = cost;
            }
            // north east
            if ((curr_x < 104 - 1) && (curr_y < 104 - 1) && (prev[curr_x + 1][curr_y + 1] == 0) && ((blocks[curr_x
                    + 2][curr_y + 2] & 0x12801e0) == 0) && ((blocks[curr_x + 2][curr_y + 1] & 0x1280180) == 0) && (
                    (blocks[curr_x + 1][curr_y + 2] & 0x1280120) == 0)) {
                path_x[path_ptr] = curr_x + 1;
                path_y[path_ptr] = curr_y + 1;
                path_ptr = (path_ptr + 1) % pathLength;
                prev[curr_x + 1][curr_y + 1] = 12;
                dist[curr_x + 1][curr_y + 1] = cost;
            }
        }
        return foundPath ? dist[curr_x][curr_y] : -1;
    }

    public static java.awt.Point convertRLPointToAWTPoint(Point point) {
        return new java.awt.Point(point.getX(), point.getY());
    }

    static class Render {
        float absoluteX1 = 0, absoluteX2 = 0;
        float absoluteY1 = 0, absoluteY2 = 0;
        int xMultiplier = 512, yMultiplier = 512;
        int zNear = 50, zFar = 3500;
    }

    static class RenderData {
        float xOff = 0, xX = 32768, xY = 0, xZ = 0;
        float yOff = 0, yX = 0, yY = 32768, yZ = 0;
        float zOff = 0, zX = 0, zY = 0, zZ = 32768;
    }

    public static final int[] SIN_TABLE = new int[16384];
    public static final int[] COS_TABLE = new int[16384];

    static {
        final double d = 0.00038349519697141029D;
        for (int i = 0; i < 16384; i++) {
            Calculations.SIN_TABLE[i] = (int) (32768D * Math.sin(i * d));
            Calculations.COS_TABLE[i] = (int) (32768D * Math.cos(i * d));
        }
    }
}
