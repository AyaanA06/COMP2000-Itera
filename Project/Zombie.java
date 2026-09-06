import java.awt.*;
import java.util.ArrayList;

/*
 * ZOMBIE
 */
public class Zombie extends Character {

    protected double detectionRange = 500;

    protected static final int DAMAGE = 20;

    protected static final double ATTACK_DISTANCE = 20;

    public Zombie(
        int x,
        int y
    ) {

        super(
            100,
            1.5,
            x,
            y,
            18
        );
    }

    public Human update(
        int worldWidth,
        int worldHeight,
        ArrayList<Human> humans,
        SafePoint safePoint
    ) {

        if (!isAlive()) {
            return null;
        }

        Human target =
            findClosestHuman(
                humans
            );

        if (target == null) {
            return null;
        }

        if (target.isInSafePoint()) {
            return null;
        }

        double distance =
            position.distanceTo(
                target.getPosition()
            );

        if (
            distance
            <= ATTACK_DISTANCE
        ) {

            boolean attacked =
                performAttack(
                    target
                );

            if (
                attacked
                &&
                !target.isAlive()
            ) {

                return target;
            }

            return null;
        }

        chase(target);

        double nextX =
            position.getX();

        double nextY =
            position.getY();

        if (
            safePoint.wouldZombieEnter(
                nextX,
                nextY,
                size
            )
        ) {

            return null;
        }

        keepInsideWorld(
            worldWidth,
            worldHeight
        );

        return null;
    }

    protected Human findClosestHuman(
        ArrayList<Human> humans
    ) {

        Human closest = null;

        double closestDistance =
            Double.MAX_VALUE;

        for (Human human : humans) {

            if (human.isInSafePoint()) {
                continue;
            }

            double distance =
                position.distanceTo(
                    human.getPosition()
                );

            if (
                distance
                < closestDistance
            ) {

                closestDistance =
                    distance;

                closest =
                    human;
            }
        }

        return closest;
    }

    protected boolean performAttack(
        Human target
    ) {

        return target.receiveZombieHit(
            DAMAGE
        );
    }

    public void attack(
        Character target
    ) {

        target.takeDamage(
            DAMAGE
        );
    }

    public void chase(
        Character target
    ) {

        double directionX =
            target.getX()
            - position.getX();

        double directionY =
            target.getY()
            - position.getY();

        double distance =
            Math.sqrt(
                directionX * directionX
                +
                directionY * directionY
            );

        if (distance > 0) {

            position.add(
                directionX
                / distance
                * speed,

                directionY
                / distance
                * speed
            );
        }
    }

    protected void keepInsideWorld(
        int width,
        int height
    ) {

        if (position.getX() < 0) {
            position.setX(0);
        }

        if (
            position.getX()
            > width - size
        ) {

            position.setX(
                width - size
            );
        }

        if (position.getY() < 0) {
            position.setY(0);
        }

        if (
            position.getY()
            > height - size
        ) {

            position.setY(
                height - size
            );
        }
    }

    @Override
    public void draw(
        Graphics g
    ) {

        g.setColor(Color.RED);

        g.fillOval(
            getX(),
            getY(),
            size,
            size
        );

        drawTypeLabel(
            g,
            "Z"
        );
    }

    protected void drawTypeLabel(
        Graphics g,
        String label
    ) {

        g.setColor(Color.BLACK);

        g.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                10
            )
        );

        g.drawString(
            label,
            getX() - 5,
            getY() + size + 12
        );
    }
}


/*
 * RUNNER
 */
class Runner extends Zombie {

    private static final double SPRINT_SPEED = 2.0;
    private static final double BURST_SPEED = 3.2;

    private static final double BURST_RANGE = 150;

    private static final long BURST_DURATION = 1200;
    private static final long BURST_COOLDOWN = 3000;

    private long burstEndTime = 0;
    private long lastBurstTime = 0;

    public Runner(
        int x,
        int y
    ) {

        super(x, y);

        sprint();
    }

    public void sprint() {

        speed =
            SPRINT_SPEED;
    }

    @Override
    public Human update(
        int worldWidth,
        int worldHeight,
        ArrayList<Human> humans,
        SafePoint safePoint
    ) {

        long now =
            System.currentTimeMillis();

        Human target =
            findClosestHuman(
                humans
            );

        if (target != null) {

            double distance =
                position.distanceTo(
                    target.getPosition()
                );

            if (
                distance <= BURST_RANGE
                &&
                now - lastBurstTime
                    >= BURST_COOLDOWN
            ) {

                burstEndTime =
                    now + BURST_DURATION;

                lastBurstTime =
                    now;
            }
        }

        if (now < burstEndTime) {

            speed =
                BURST_SPEED;

        } else {

            speed =
                SPRINT_SPEED;
        }

        return super.update(
            worldWidth,
            worldHeight,
            humans,
            safePoint
        );
    }

    @Override
    public void draw(
        Graphics g
    ) {

        g.setColor(
            Color.ORANGE
        );

        g.fillOval(
            getX(),
            getY(),
            size,
            size
        );

        drawTypeLabel(
            g,
            "R"
        );
    }
}


/*
 * STALKER
 */
class Stalker extends Zombie {

    private int stealth = 100;

    private static final int AMBUSH_DAMAGE = 30;

    public Stalker(
        int x,
        int y
    ) {

        super(x, y);

        speed = 1.2;
    }

    public void ambush(
        Character target
    ) {

        if (target instanceof Human human) {

            human.receiveZombieHit(
                AMBUSH_DAMAGE
            );

        } else {

            target.takeDamage(
                AMBUSH_DAMAGE
            );
        }
    }

    @Override
    protected boolean performAttack(
        Human target
    ) {

        int healthBefore =
            target.getHealth();

        ambush(target);

        return target.getHealth()
            < healthBefore;
    }

    @Override
    public void draw(
        Graphics g
    ) {

        g.setColor(
            Color.MAGENTA
        );

        g.fillOval(
            getX(),
            getY(),
            size,
            size
        );

        drawTypeLabel(
            g,
            "S"
        );
    }
}


/*
 * BLOATER
 */
class Bloater extends Zombie {

    private double blastRadius = 60;
    private int blastDamage = 60;

    public Bloater(
        int x,
        int y
    ) {

        super(x, y);

        health = 150;
        speed = 0.8;
        size = 22;
    }

    public boolean shouldExplode(
        ArrayList<Human> humans
    ) {

        if (!isAlive()) {
            return false;
        }

        for (Human human : humans) {

            if (human.isInSafePoint()) {
                continue;
            }

            double distance =
                position.distanceTo(
                    human.getPosition()
                );

            if (distance <= blastRadius) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<Human> explode(
        ArrayList<Human> humans
    ) {

        ArrayList<Human> killedHumans =
            new ArrayList<>();

        for (Human human : humans) {

            if (human.isInSafePoint()) {
                continue;
            }

            double distance =
                position.distanceTo(
                    human.getPosition()
                );

            if (distance <= blastRadius) {

                human.takeDamage(
                    blastDamage
                );

                if (!human.isAlive()) {
                    killedHumans.add(human);
                }
            }
        }

        health = 0;

        return killedHumans;
    }

    public double getBlastRadius() {
        return blastRadius;
    }

    @Override
    public void draw(
        Graphics g
    ) {

        g.setColor(
            Color.DARK_GRAY
        );

        g.fillOval(
            getX(),
            getY(),
            size,
            size
        );

        drawTypeLabel(
            g,
            "B"
        );
    }
}
