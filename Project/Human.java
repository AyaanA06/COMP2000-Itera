import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

/* HUMAN */
public class Human extends Character {

    protected int stamina = 100;

    protected static final int MAX_STAMINA = 100;
    protected static final int STAMINA_DRAIN = 1;
    protected static final int STAMINA_RECOVERY = 1;

    protected ArrayList<Resource> inventory = new ArrayList<>();

    protected static final double NORMAL_SPEED = 3.0;

    protected static final double DETECTION_RANGE = 150;

    protected boolean insideSafePoint = false;

    protected long lastDamageTime = 0;
    protected long lastHealTime = 0;

    protected static final long DAMAGE_COOLDOWN = 600;
    protected static final long HEAL_INTERVAL = 3000;

    protected Random random = new Random();

    protected double dx, dy;

    public Human(int x, int y) {

        super(100, NORMAL_SPEED, x, y, 15);

        initialize();
    }

    public void initialize() {
        chooseRandomDirection();
    }

    public void useItem(Resource item) {

        if (inventory.contains(item)) {
            item.use(this);
        }
    }

    public void interact(Object object) {

        if (object instanceof Resource resource) {

            inventory.add(resource);

        } else if (object instanceof Building building) {

            building.interact(this);
        }
    }

    public void update(int worldWidth, int worldHeight, ArrayList<Zombie> zombies, SafePoint safePoint) {

        if (insideSafePoint) {

            dx = 0;
            dy = 0;

            healInsideSafePoint();
            recoverStamina();

            if (health >= 80) {

                insideSafePoint = false;

                position.setX(safePoint.getDoorX() + 20);

                position.setY(safePoint.getDoorCentreY());

                chooseRandomDirection();
            }

            return;
        }

        if (health <= 20) {

            moveTowardsSafePoint(safePoint);

                double distance = position.distanceTo(new Vector2D(safePoint.getDoorX(), safePoint.getDoorCentreY()));

            if (distance <= 25) {

                insideSafePoint = true;

                Vector2D restPosition = safePoint.getRandomRestPosition(random, size);

                position.setX(restPosition.getX());

                position.setY(restPosition.getY());

                dx = 0;
                dy = 0;

                lastHealTime = System.currentTimeMillis();

                return;
            }

        } else {

            Zombie nearestZombie = findNearestZombie(zombies);

            if (nearestZombie != null) {

                double distance = position.distanceTo(nearestZombie.getPosition());

                if (distance <= DETECTION_RANGE) {

                    fleeFrom(nearestZombie, worldWidth, worldHeight);

                    drainStamina();

                } else {

                    roam();
                    recoverStamina();
                }

            } else {

                roam();
                recoverStamina();
            }
        }

        double nextX = position.getX() + dx;

        double nextY = position.getY() + dy;

        if (safePoint.blocksHumanMovement(position.getX(), position.getY(), nextX, nextY, size, health <= 20)) {

            chooseRandomDirection();

        } else {

            position.add(dx, dy);
        }

        keepInsideWorld(worldWidth, worldHeight);
    }

    protected Zombie findNearestZombie(ArrayList<Zombie> zombies) {

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

    protected void fleeFrom(Zombie zombie, int worldWidth, int worldHeight) {

        double directionX = position.getX() - zombie.getX();

        double directionY = position.getY() - zombie.getY();

        double distance = Math.sqrt(directionX * directionX + directionY * directionY);

        if (distance > 0) {

            directionX /= distance;
            directionY /= distance;
        }

        int margin = 60;

        if (position.getX() < margin) {
            directionX += 1.8;
        }

        if (position.getX() > worldWidth - size - margin) {
            directionX -= 1.8;
        }

        if (position.getY() < margin) {
            directionY += 1.8;
        }

        if (position.getY() > worldHeight - size - margin) {
            directionY -= 1.8;
        }

        double length = Math.sqrt(directionX * directionX + directionY * directionY);

        if (length > 0) {

            double fleeSpeed = speed;

            /* A tired human moves more slowly * until some stamina is recovered. */
            if (stamina <= 0) {
                fleeSpeed = speed * 0.5;
            }

            dx = directionX / length * fleeSpeed;

            dy = directionY / length * fleeSpeed;
        }
    }

    /* Reduce stamina while escaping zombies. */
    protected void drainStamina() {

        stamina -= STAMINA_DRAIN;

        if (stamina < 0) {
            stamina = 0;
        }
    }

    /* Recover stamina when the human * is not actively fleeing. */
    protected void recoverStamina() {

        stamina += STAMINA_RECOVERY;

        if (stamina > MAX_STAMINA) {
            stamina = MAX_STAMINA;
        }
    }

    public int getStamina() {
        return stamina;
    }

    protected void moveTowardsSafePoint(SafePoint safePoint) {

        double directionX = safePoint.getDoorX() - position.getX();

        double directionY = safePoint.getDoorCentreY() - position.getY();

        double distance = Math.sqrt(directionX * directionX + directionY * directionY);

        if (distance > 0) {

            dx = directionX / distance * speed;

            dy = directionY / distance * speed;
        }
    }

    protected void roam() {

        if (random.nextInt(100) < 2) {

            chooseRandomDirection();
        }

    }

    protected void chooseRandomDirection() {

        double angle = random.nextDouble() * Math.PI * 2;

        dx = Math.cos(angle) * speed;

        dy = Math.sin(angle) * speed;
    }

    protected void keepInsideWorld(int width, int height) {

        if (position.getX() < 0) {

            position.setX(0);
            dx = Math.abs(dx);
        }

        if (position.getX() > width - size) {

            position.setX(width - size);

            dx = -Math.abs(dx);
        }

        if (position.getY() < 0) {

            position.setY(0);
            dy = Math.abs(dy);
        }

        if (position.getY() > height - size) {

            position.setY(height - size);

            dy = -Math.abs(dy);
        }
    }

    public boolean receiveZombieHit(int damage) {

        if (insideSafePoint) {
            return false;
        }

        long now = System.currentTimeMillis();

        if (now - lastDamageTime < DAMAGE_COOLDOWN) {

            return false;
        }

        takeDamage(damage);

        lastDamageTime = now;

        return true;
    }

    protected void healInsideSafePoint() {

        long now = System.currentTimeMillis();

        if (now - lastHealTime >= HEAL_INTERVAL) {

            health += 10;

            if (health > 100) {
                health = 100;
            }

            lastHealTime = now;
        }
    }

    public boolean isInSafePoint() {
        return insideSafePoint;
    }

    @Override
    public void draw(Graphics g) {

        g.setColor(Color.BLUE);

        g.fillOval(getX(), getY(), size, size);

        drawHealthBar(g);
    }

    protected void drawHealthBar(Graphics g) {

        int width = 24;
        int height = 4;

        int x = getX() - 4;

        int y = getY() - 8;

        g.setColor(Color.RED);

        g.fillRect(x, y, width, height);

        int remaining = (int) (width * health / 100.0);

        g.setColor(Color.GREEN);

        g.fillRect(x, y, remaining, height);
    }

    protected void drawTypeLabel(Graphics g, String label) {

        g.setColor(Color.BLACK);

        g.setFont(new Font("Arial", Font.BOLD, 10));

        g.drawString(label, getX() - 5, getY() + size + 12);
    }
}


/* SOLDIER */
class Soldier extends Human {

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


/* MEDIC */
class Medic extends Human {

    private int medKits = 3;

    public Medic(int x, int y) {

        super(x, y);
    }

    public void heal(Character target) {

        if (medKits > 0 && target.health < 100) {

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

        drawTypeLabel(g, "M");
    }
}
