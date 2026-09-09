package itera.model;

import java.awt.*;
import java.util.ArrayList;

/* ZOMBIE */
public class Zombie extends Character {

    protected double detectionRange = 500;

    protected static final int DAMAGE = 20;

    protected static final double ATTACK_DISTANCE = 20;

    public Zombie(int x, int y) {

        super(100, 1.5, x, y, 18);
    }

    public Human update(int worldWidth, int worldHeight, ArrayList<Human> humans, SafePoint safePoint) {

        if (!isAlive()) {
            return null;
        }

        Human target = findClosestHuman(humans);

        if (target == null) {
            return null;
        }

        if (target.isInSafePoint()) {
            return null;
        }

        double distance = position.distanceTo(target.getPosition());

        if (distance <= ATTACK_DISTANCE) {

            boolean attacked = performAttack(target);

            if (attacked && !target.isAlive()) {

                return target;
            }

            return null;
        }

        chase(target);

        double nextX = position.getX();

        double nextY = position.getY();

        if (safePoint.wouldZombieEnter(nextX, nextY, size)) {

            return null;
        }

        keepInsideWorld(worldWidth, worldHeight);

        return null;
    }

    protected Human findClosestHuman(ArrayList<Human> humans) {

        Human closest = null;

        double closestDistance = Double.MAX_VALUE;

        for (Human human : humans) {

            if (!human.isAlive()) {
                continue;
            }

            if (human.isInSafePoint()) {
                continue;
            }

            double distance = position.distanceTo(human.getPosition());

            if (distance < closestDistance) {

                closestDistance = distance;

                closest = human;
            }
        }

        return closest;
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

        double distance = Math.sqrt(directionX * directionX + directionY * directionY);

        if (distance > 0) {

            position.add(directionX / distance * speed, directionY / distance * speed);
        }
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

        g.setColor(Color.RED);

        g.fillOval(getX(), getY(), size, size);

        drawTypeLabel(g, "Z");
    }

    protected void drawTypeLabel(Graphics g, String label) {

        g.setColor(Color.BLACK);

        g.setFont(new Font("Arial", Font.BOLD, 10));

        g.drawString(label, getX() - 5, getY() + size + 12);
    }
}
