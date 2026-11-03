package itera.model.zombie;

import itera.model.*;
import itera.model.human.*;
import itera.model.zombie.targeting.*;
import java.awt.*;
import java.util.*;

/** Stops at range and spits acid instead of walking up to bite. */
public class Spitter extends Zombie {
    public static final double SPIT_RANGE = 140;
    public static final long SPIT_COOLDOWN = 1500;
    public static final int SPIT_DAMAGE = 15;

    private long lastSpitTime = -SPIT_COOLDOWN;

    public Spitter(int x, int y) {
        super(x, y);
        health = 80;
        speed = 1.5;
        // Spitters take out the medics so nobody gets healed.
        targetingStrategy = new PreferredRoleTarget<>(Medic.class, new NearestTarget());
    }

    @Override
    public Human update(int worldWidth, int worldHeight, ArrayList<Human> humans, SafePoint safePoint) {
        if (!isAlive()) {
            return null;
        }

        Human target = findTarget(humans);

        // Too far away to spit, so walk closer like a normal zombie.
        if (target == null || position.distanceTo(target.getPosition()) > SPIT_RANGE) {
            return super.update(worldWidth, worldHeight, humans, safePoint);
        }

        long now = now();

        if (now - lastSpitTime < SPIT_COOLDOWN) {
            return null;
        }

        lastSpitTime = now;
        boolean hit = target.receiveZombieHit(SPIT_DAMAGE);

        return hit && !target.isAlive() ? target : null;
    }

    @Override
    public int getMaxHealth() { return 80; }

    @Override
    public void draw(Graphics g) {
        g.setColor(new Color(104, 159, 56));
        g.fillOval(getX(), getY(), size, size);
        drawTypeLabel(g, "SP");
    }
}
