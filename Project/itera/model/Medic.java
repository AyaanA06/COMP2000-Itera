package itera.model;

import java.awt.Graphics;

public class Medic extends Human {
    private int medKits = 3;

    public Medic(int x, int y) {
        super(x, y);
    }

    public void heal(Character target) {
        if (medKits > 0 && target.health < 100) {
            target.health += 20;

            if (target.health > 100) {
                target.health = 100;
            }

            medKits--;
        }
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
        drawTypeLabel(g, "M");
    }
}
