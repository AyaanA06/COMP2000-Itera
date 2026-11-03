
package itera.model.zombie;

import itera.model.*;
import itera.model.Character;
import itera.model.human.*;
import itera.model.zombie.targeting.*;
import java.awt.*;
import java.util.*;

public class Zombie extends Character {

    /** Standard police-station bullets deal 50 health damage. */
    public void receiveGunshot() {
        if (isAlive()) takeDamage(50);
    }

    private java.util.List<Building> buildings = java.util.List.of();

    public void setBuildings(java.util.List<Building> buildings) {
        this.buildings = buildings;
    }

    protected double detectionRange = 500;

    /** Decides which human this zombie goes after. */
    protected TargetingStrategy targetingStrategy = new NearestTarget();

    public TargetingStrategy getTargetingStrategy() {
        return targetingStrategy;
    }

    public void setTargetingStrategy(TargetingStrategy targetingStrategy) {
        this.targetingStrategy = Objects.requireNonNull(targetingStrategy, "Targeting strategy is required");
    }

    protected static final int DAMAGE = 25;
    protected static final double ATTACK_DISTANCE = 20;

    public Zombie(int x, int y) {
        super(100, 1.875, x, y, 18);
    }

    /**
     * Moves towards or attacks the human chosen by the targeting strategy.
     */
    public Human update(
            int worldWidth,
            int worldHeight,
            ArrayList<Human> humans,
            SafePoint safePoint) {

        if (!isAlive()) {
            return null;
        }

        Human target = findTarget(humans);

        if (target == null || target.isSheltered()) {
            return null;
        }

        double distance = position.distanceTo(target.getPosition());

        if (distance > detectionRange) {
            return null;
        }

        if (distance <= ATTACK_DISTANCE) {

            boolean attacked = performAttack(target);

            if (attacked && !target.isAlive()) {
                return target;
            }

            return null;
        }

        double previousX = position.getX();
        double previousY = position.getY();

        chase(target);

        double nextX = position.getX();
        double nextY = position.getY();

        if (isBlocked(nextX, nextY, safePoint)) {

            // Try moving horizontally
            position.setX(nextX);
            position.setY(previousY);

            if (isBlocked(position.getX(), position.getY(), safePoint)) {

                // Try moving vertically
                position.setX(previousX);
                position.setY(nextY);

                if (isBlocked(position.getX(), position.getY(), safePoint)) {

                    // Both directions blocked
                    position.setX(previousX);
                    position.setY(previousY);
                }
            }
        }

        keepInsideWorld(worldWidth, worldHeight);

        return null;
    }

    protected Human findTarget(ArrayList<Human> humans) {
        return targetingStrategy.chooseTarget(this, humans).orElse(null);
    }

    protected boolean performAttack(Human target) {
        return target.receiveZombieHit(DAMAGE);
    }

    public void attack(Character target) {
        target.takeDamage(DAMAGE);
    }

    public void chase(Character target) {

        double directionX = target.getX() - position.getX();
        double directionY = target.getY() - position.getY();

        double distance = Math.sqrt(
            directionX * directionX + directionY * directionY
        );

        if (distance > 0) {
            position.add(
                directionX / distance * speed,
                directionY / distance * speed
            );
        }
    }

    private boolean isBlocked(double x, double y, SafePoint safePoint) {

        if (safePoint.wouldZombieEnter(x, y, size)) {
            return true;
        }

        for (Building building : buildings) {
            if (building.overlaps(x, y, size)) {
                return true;
            }
        }

        return false;
    }

    protected void keepInsideWorld(int width, int height) {

        if (position.getX() < 0) {
            position.setX(0);
        }

        if (position.getX() > width - size) {
            position.setX(width - size);
        }

        if (position.getY() < 0) {
            position.setY(0);
        }

        if (position.getY() > height - size) {
            position.setY(height - size);
        }
    }

    @Override
    public void draw(Graphics g) {

        g.setColor(new Color(211, 47, 47));
        g.fillOval(getX(), getY(), size, size);

        drawTypeLabel(g, "Z");
    }

    public int getMaxHealth() {
        return 100;
    }

    protected void drawHealthBar(Graphics g) {

        int width = Math.max(24, size);
        int barX = getX() + (size - width) / 2;
        int barY = getY() - 9;

        int remaining = (int) Math.round(
            width * Math.max(
                0,
                Math.min(1.0, health / (double) getMaxHealth())
            )
        );

        g.setColor(Color.RED);
        g.fillRect(barX, barY, width, 4);

        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, remaining, 4);
    }

    protected void drawTypeLabel(Graphics g, String label) {

        drawHealthBar(g);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 10));

        g.drawString(label, getX() - 5, getY() + size + 12);
    }
}
