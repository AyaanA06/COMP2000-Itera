package itera.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;

public abstract class Building {

    protected int capacity;

    protected int x;
    protected int y;

    protected int width;
    protected int height;

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

            human.addResource(resource);
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
