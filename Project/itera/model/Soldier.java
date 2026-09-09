package itera.model;

import java.awt.Graphics;
import java.util.ArrayList;

public class Soldier extends Human {
    private int ammo = 10;

    private static final int SOLDIER_DAMAGE = 20;
    private static final double SHOOT_RANGE = 180;
    private static final long SHOOT_COOLDOWN = 700;

    private long lastShotTime = 0;

    public Soldier(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(int worldWidth, int worldHeight, ArrayList<Zombie> zombies, SafePoint safePoint) {
        super.update(worldWidth, worldHeight, zombies, safePoint);

        Zombie target = findNearestAliveZombie(zombies);

        if (target == null) {
            return;
        }

        double distance = position.distanceTo(target.getPosition());

        if (distance > SHOOT_RANGE) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastShotTime < SHOOT_COOLDOWN) {
            return;
        }

        Weapon weapon = findUsableWeapon();
        boolean fired;

        if (weapon != null) {
            fired = weapon.fire(target);
        } else {
            fired = shoot(target);
        }

        if (fired) {
            lastShotTime = now;
        }
    }

    public boolean shoot(Character target) {
        if (target == null || ammo <= 0) {
            return false;
        }

        target.takeDamage(SOLDIER_DAMAGE);
        ammo--;

        return true;
    }

    private Weapon findUsableWeapon() {
        for (Resource resource : inventory) {
            if (resource instanceof Weapon weapon && weapon.canFire()) {
                return weapon;
            }
        }

        return null;
    }

    private Zombie findNearestAliveZombie(ArrayList<Zombie> zombies) {
        Zombie nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Zombie zombie : zombies) {
            if (!zombie.isAlive()) {
                continue;
            }

            double distance = position.distanceTo(zombie.getPosition());

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = zombie;
            }
        }

        return nearest;
    }

    public int getAmmo() {
        return ammo;
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
        drawTypeLabel(g, "S");
    }
}
