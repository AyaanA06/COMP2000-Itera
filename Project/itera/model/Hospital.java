package itera.model;

import java.awt.Color;
import java.awt.Graphics;

public class Hospital extends Building {

    public Hospital(int x, int y) {

        super(10, x, y, 180, 180, "HOSPITAL");

        stock.add(new Medicine(5, 20));
    }

    public void treat(Character character) {

        character.health += 20;

        if (character.health > 100) {

            character.health = 100;
        }
    }

    @Override
    protected void drawBuildingBody(Graphics g) {

        g.setColor(new Color(220, 255, 230));

        g.fillRect(x, y, width, height);

        /* Hospital cross */
        g.setColor(Color.RED);

        g.fillRect(x + width - 42, y + 20, 12, 40);

        g.fillRect(x + width - 56, y + 34, 40, 12);
    }
}
