import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class PoliceStation extends Building {

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
