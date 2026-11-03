package itera.model.zombie;

import itera.model.human.*;
import itera.model.zombie.targeting.*;
import java.awt.*;

/** A slow, heavily armoured zombie that only takes half damage. */
public class Brute extends Zombie {
    private static final int BRUTE_HEALTH = 250;

    public Brute(int x, int y) {
        super(x, y);
        health = BRUTE_HEALTH;
        speed = 1.2;
        size = 24;
        // Brutes walk straight at the soldiers to soak up their bullets.
        targetingStrategy = new PreferredRoleTarget<>(Soldier.class, new NearestTarget());
    }

    /** Armour halves every hit, so a 50 damage gunshot only does 25. */
    @Override
    public void takeDamage(int amount) {
        super.takeDamage((amount + 1) / 2);
    }

    @Override
    public int getMaxHealth() { return BRUTE_HEALTH; }

    @Override
    public void draw(Graphics g) {
        g.setColor(new Color(78, 52, 46));
        g.fillOval(getX(), getY(), size, size);
        drawTypeLabel(g, "BR");
    }
}
