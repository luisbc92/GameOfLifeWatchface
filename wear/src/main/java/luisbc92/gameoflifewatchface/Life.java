package luisbc92.gameoflifewatchface;

// Game Of Life
public class Life {

    // Constants
    private final static byte ALIVE = 0x01;
    private final static byte LAST  = 0x02;
    private final static byte SPAWN = 0x04;
    private final static byte KILL  = 0x08;
    private final static int  COUNT = 4;

    private int[][] mWorld;
    private int mWidth;
    private int mHeight;
    private int mCellCount;
    private int mGenCount;

    public Life(int width, int height) {
        mWidth = width;
        mHeight = height;
        mWorld = new int[width][height];
        resetWorld();
    }

    public int[][] getWorld() { return mWorld; }

    public int getCellCount() {
        return mCellCount;
    }

    public int getGenCount() {
        return mGenCount;
    }

    public void addCell(int x, int y) {
        mWorld[x][y] = SPAWN;
    }

    public void resetWorld() {
        for (int w = 0; w < mWidth; w++) {
            for (int h = 0; h < mHeight; h++) {
                mWorld[w][h] = 0;
            }
        }
    }

    private void addNeighbor(int x, int y) {
        for (int ix = -1; ix <= +1; ix++) {
            for (int iy = -1; iy <= +1; iy++) {
                int tx = x + ix;
                int ty = y + iy;
                if (!((tx == x) && (ty == y))) {
                    if (tx < 0) tx = mWidth-1;
                    if (ty < 0) ty = mHeight-1;
                    if (tx >= mWidth) tx = 0;
                    if (ty >= mHeight) ty = 0;
                    mWorld[tx][ty] += 16;
                }
            }
        }
    }

    private void subNeighbor(int x, int y) {
        for (int ix = -1; ix <= +1; ix++) {
            for (int iy = -1; iy <= +1; iy++) {
                int tx = x + ix;
                int ty = y + iy;
                if (!((tx == x) && (ty == y))) {
                    if (tx < 0) tx = mWidth-1;
                    if (ty < 0) ty = mHeight-1;
                    if (tx >= mWidth) tx = 0;
                    if (ty >= mHeight) ty = 0;
                    mWorld[tx][ty] -= 16;
                }
            }
        }
    }

    public void updateWorld() {
        int cell;
        int count, x, y;

        // Perform deferred state changes
        for (x = 0; x < mWidth; x++) {
            for (y = 0; y < mHeight; y++) {
                cell = mWorld[x][y];            // Get cell
                if ((cell & SPAWN) == SPAWN) {  // If marked for spawn
                    addNeighbor(x, y);          // Increase neighbors count
                    cell |= ALIVE;              // Set to alive
                    cell &= ~SPAWN;             // Clear spawn marker
                    mCellCount++;               // Increase cell count
                    mWorld[x][y] = cell;        // Write back cell
                } else
                if ((cell & KILL) == KILL) {    // If marked for kill
                    subNeighbor(x, y);          // Decrease neighbors count
                    cell &= ~ALIVE;             // Set cell to death
                    cell &= ~KILL;              // Clear kill marker
                    mCellCount--;               // Decrease cell count
                    mWorld[x][y] = cell;        // Write back cell
                }
            }
        }

        // Check for GoL rules and mark cells to update
        for (x = 0; x < mWidth; x++) {
            for (y = 0; y < mHeight; y++) {
                cell = mWorld[x][y];            // Get cell
                count = cell >> COUNT;          // Get neighbors count
                if (cell > 0) {
                    if ((cell & ALIVE) == ALIVE) {  // If cell is alive
                        if ((count != 2) && (count != 3)) {
                            mWorld[x][y] = (cell | KILL); // Kill cell
                        }
                    } else {
                        if (count == 3) {
                            mWorld[x][y] = (cell | SPAWN);// Spawn cell
                        }
                    }
                }
            }
        }
        mGenCount++;
    }
}






















