import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public abstract class Character {

    protected int health;
    protected double speed;
    protected Vector2D position;
    protected int size;

    public Character(
        int health,
        double speed,
        double x,
        double y,
        int size
    ) {

        this.health = health;
        this.speed = speed;
        this.position = new Vector2D(x, y);
        this.size = size;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void move(Vector2D direction) {

        position.add(
            direction.getX() * speed,
            direction.getY() * speed
        );
    }

    public void takeDamage(int amount) {

        health -= amount;

        if (health < 0) {
            health = 0;
        }
    }

    public int getHealth() {
        return health;
    }

    public int getX() {
        return (int) position.getX();
    }

    public int getY() {
        return (int) position.getY();
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getSpeed() {
        return speed;
    }

    public int getSize() {
        return size;
    }

    public abstract void draw(Graphics g);
}


/*
 * CIVILIAN
 */
class Civilian extends Human {

    private int fearLevel = 0;

    public Civilian(
        int x,
        int y
    ) {

        super(x, y);
    }

    public void flee() {
        fearLevel++;
    }

    @Override
    public void draw(Graphics g) {

        super.draw(g);

        drawTypeLabel(
            g,
            "C"
        );
    }
}


/*
 * SOLDIER
 */
class Soldier extends Human {

    private int ammo = 10;

    private static final int SOLDIER_DAMAGE = 20;
    private static final double SHOOT_RANGE = 180;
    private static final long SHOOT_COOLDOWN = 700;

    private long lastShotTime = 0;

    public Soldier(
        int x,
        int y
    ) {

        super(x, y);
    }

    @Override
    public void update(
        int worldWidth,
        int worldHeight,
        ArrayList<Zombie> zombies,
        SafePoint safePoint
    ) {

        super.update(
            worldWidth,
            worldHeight,
            zombies,
            safePoint
        );

        Zombie target =
            findNearestAliveZombie(
                zombies
            );

        if (target == null) {
            return;
        }

        double distance =
            position.distanceTo(
                target.getPosition()
            );

        if (distance > SHOOT_RANGE) {
            return;
        }

        long now =
            System.currentTimeMillis();

        if (
            now - lastShotTime
            < SHOOT_COOLDOWN
        ) {

            return;
        }

        Weapon weapon =
            findUsableWeapon();

        boolean fired;

        if (weapon != null) {

            fired =
                weapon.fire(
                    target
                );

        } else {

            fired =
                shoot(
                    target
                );
        }

        if (fired) {

            lastShotTime =
                now;
        }
    }

    public boolean shoot(
        Character target
    ) {

        if (
            target == null
            ||
            ammo <= 0
        ) {

            return false;
        }

        target.takeDamage(
            SOLDIER_DAMAGE
        );

        ammo--;

        return true;
    }

    private Weapon findUsableWeapon() {

        for (Resource resource : inventory) {

            if (
                resource instanceof Weapon weapon
                &&
                weapon.canFire()
            ) {

                return weapon;
            }
        }

        return null;
    }

    private Zombie findNearestAliveZombie(
        ArrayList<Zombie> zombies
    ) {

        Zombie nearest = null;

        double nearestDistance =
            Double.MAX_VALUE;

        for (Zombie zombie : zombies) {

            if (!zombie.isAlive()) {
                continue;
            }

            double distance =
                position.distanceTo(
                    zombie.getPosition()
                );

            if (distance < nearestDistance) {

                nearestDistance =
                    distance;

                nearest =
                    zombie;
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

        drawTypeLabel(
            g,
            "S"
        );
    }
}


/*
 * MEDIC
 */
class Medic extends Human {

    private int medKits = 3;

    public Medic(
        int x,
        int y
    ) {

        super(x, y);
    }

    public void heal(
        Character target
    ) {

        if (
            medKits > 0
            &&
            target.health < 100
        ) {

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

        drawTypeLabel(
            g,
            "M"
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
