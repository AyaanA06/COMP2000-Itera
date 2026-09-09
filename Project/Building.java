import java.awt.*;
import java.util.ArrayList;

public abstract class Building {

    protected int capacity;

    protected int x, y;

    protected int width, height;

    protected String name;

    protected ArrayList<Resource> stock = new ArrayList<>();

    public Building(int capacity, int x, int y, int width, int height, String name) {

        this.capacity = capacity;

        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.name = name;
    }

    public boolean isSecure() {
        return true;
    }

    public boolean contains(int objectX, int objectY) {

        return objectX >= x && objectX <= x + width && objectY >= y && objectY <= y + height;
    }

    public Resource loot() {

        if (stock.isEmpty()) {
            return null;
        }

        return stock.remove(0);
    }

    public void interact(Human human) {

        Resource resource = loot();

        if (resource != null) {

            human.inventory.add(resource);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getName() {
        return name;
    }

    public void draw(Graphics g) {

        /* Building body */
        drawBuildingBody(g);

        /* Building border */
        g.setColor(Color.BLACK);

        g.drawRect(x, y, width, height);

        /* Building name */
        g.setColor(Color.BLACK);

        g.setFont(new Font("Arial", Font.BOLD, 14));

        g.drawString(name, x + 10, y + 22);

        /* Display available items */
        g.setFont(new Font("Arial", Font.PLAIN, 11));

        g.drawString("Stock: " + stock.size(), x + 10, y + 42);
    }

    protected abstract void drawBuildingBody(Graphics g);
}
/* POLICE STATION */
class PoliceStation extends Building {

    private ArrayList<Weapon> armory = new ArrayList<>();

        public PoliceStation(int x, int y) {

            super(10, x, y, 180, 180, "POLICE STATION");
        
            Weapon weapon = new Weapon(10, 25, 20);
        
            armory.add(weapon);
            stock.add(weapon);
        }
        
    public Weapon getWeapon() {

        if (armory.isEmpty()) {
            return null;
        }

        return armory.remove(0);
    }

    @Override
    protected void drawBuildingBody(Graphics g) {

        g.setColor(new Color(210, 225, 255));

        g.fillRect(x, y, width, height);
    }
}


/* CONVENIENCE STORE */
class ConvenienceStore extends Building {

    private ArrayList<Food> shelves = new ArrayList<>();

        public ConvenienceStore(int x, int y) {

            super(15, x, y, 180, 180, "CONVENIENCE STORE");
        
            Food food = new Food(10, 20);
        
            shelves.add(food);
            stock.add(food);
        }

    public Food getFood() {

        if (shelves.isEmpty()) {
            return null;
        }

        return shelves.remove(0);
    }

    @Override
    protected void drawBuildingBody(Graphics g) {

        g.setColor(new Color(255, 245, 200));

        g.fillRect(x, y, width, height);
    }
}
