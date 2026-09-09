package itera.model;

import java.awt.Graphics;

public abstract class Resource {

    protected int quantity;

    protected int x;
    protected int y;

    protected int size = 14;

    protected boolean collected = false;

    public Resource(int quantity) {

        this.quantity = quantity;

        this.x = -100;
        this.y = -100;
    }

    public Resource(int quantity, int x, int y) {

        this.quantity = quantity;

        this.x = x;
        this.y = y;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isCollected() {
        return collected;
    }

    public boolean isNear(Character character) {

        double differenceX = character.getX() - x;

        double differenceY = character.getY() - y;

        double distance = Math.sqrt(differenceX * differenceX + differenceY * differenceY);

        return distance <= 25;
    }

    public void collect() {
        collected = true;
    }

    public abstract void use(Character target);

    public abstract void draw(Graphics g);
}
