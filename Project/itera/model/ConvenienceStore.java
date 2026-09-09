package itera.model;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class ConvenienceStore extends Building {

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
