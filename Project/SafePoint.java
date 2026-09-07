import java.awt.*;
import java.util.Random;

public class SafePoint {

    private int defenseLevel, capacity;

    private int x, y;

    private int width, height;

    private int doorY, doorHeight;

    public SafePoint(int x, int y, int width, int height, int capacity) {

        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.capacity = capacity;

        defenseLevel = 100;

        doorHeight = 50;

        doorY = y + height / 2 - doorHeight / 2;
    }

    public void restock() {

        defenseLevel = 100;
    }

    public int getDoorX() {
        return x + width;
    }

    public int getDoorCentreY() {

        return doorY + doorHeight / 2;
    }

    public boolean contains(double objectX, double objectY, int objectSize) {

        double centreX = objectX + objectSize / 2.0;

        double centreY = objectY + objectSize / 2.0;

        return centreX > x && centreX < x + width && centreY > y && centreY < y + height;
    }

    public boolean wouldZombieEnter(double zombieX, double zombieY, int zombieSize) {

        return zombieX + zombieSize > x && zombieX < x + width && zombieY + zombieSize > y && zombieY < y + height;
    }

    public boolean blocksHumanMovement(double currentX, double currentY, double nextX, double nextY, int humanSize, boolean allowedToEnter) {

        boolean intersects = nextX + humanSize > x && nextX < x + width && nextY + humanSize > y && nextY < y + height;

        if (!intersects) {
            return false;
        }

        double centreY = nextY + humanSize / 2.0;

        boolean atDoor = centreY >= doorY && centreY <= doorY + doorHeight;

        if (allowedToEnter && atDoor) {

            return false;
        }

        return true;
    }

    public Vector2D getRandomRestPosition(Random random, int size) {

        int restX = x + 25 + random.nextInt(width - size - 50);

        int restY = y + 45 + random.nextInt(height - size - 65);

        return new Vector2D(restX, restY);
    }

    public void draw(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        g.setColor(new Color(180, 255, 180));

        g.fillRect(x, y, width, height);

        g2.setColor(new Color(0, 110, 0));

        g2.setStroke(new BasicStroke(4));

        /* Top */
        g2.drawLine(x, y, x + width, y);

        /* Left */
        g2.drawLine(x, y, x, y + height);

        /* Bottom */
        g2.drawLine(x, y + height, x + width, y + height);

        /* Right wall above door */
        g2.drawLine(x + width, y, x + width, doorY);

        /* Right wall below door */
        g2.drawLine(x + width, doorY + doorHeight, x + width, y + height);

        g2.setStroke(new BasicStroke(1));

        g.setColor(Color.BLACK);

        g.setFont(new Font("Arial", Font.BOLD, 18));

        g.drawString("SAFE ZONE", x + 30, y + 30);
    }
}