package itera.model;

import java.awt.Graphics;

public class Civilian extends Human {
    private int fearLevel = 0;

    public Civilian(int x, int y) {
        super(x, y);
    }

    public void flee() {
        fearLevel++;
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
        drawTypeLabel(g, "C");
    }
}
