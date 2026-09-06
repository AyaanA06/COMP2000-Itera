import java.awt.*;

public abstract class Resource {

    protected int quantity;

    protected int x;
    protected int y;

    protected int size = 14;

    protected boolean collected = false;

    public Resource(
        int quantity
    ) {

        this.quantity =
            quantity;

        this.x = -100;
        this.y = -100;
    }

    public Resource(
        int quantity,
        int x,
        int y
    ) {

        this.quantity =
            quantity;

        this.x = x;
        this.y = y;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isCollected() {
        return collected;
    }

    public boolean isNear(
        Character character
    ) {

        double differenceX =
            character.getX() - x;

        double differenceY =
            character.getY() - y;

        double distance =
            Math.sqrt(
                differenceX * differenceX
                +
                differenceY * differenceY
            );

        return distance <= 25;
    }

    public void collect() {
        collected = true;
    }

    public abstract void use(
        Character target
    );

    public abstract void draw(
        Graphics g
    );
}


/*
 * MEDICINE
 */
class Medicine extends Resource {

    private int healAmount;

    public Medicine(
        int quantity,
        int healAmount
    ) {

        super(quantity);

        this.healAmount =
            healAmount;
    }

    public Medicine(
        int quantity,
        int healAmount,
        int x,
        int y
    ) {

        super(
            quantity,
            x,
            y
        );

        this.healAmount =
            healAmount;
    }

    @Override
    public void use(
        Character target
    ) {

        if (quantity <= 0) {
            return;
        }

        target.health +=
            healAmount;

        if (
            target.health > 100
        ) {

            target.health = 100;
        }

        quantity--;
    }

    @Override
    public void draw(
        Graphics g
    ) {

        if (collected) {
            return;
        }

        /*
         * Medicine box
         */
        g.setColor(Color.WHITE);

        g.fillRect(
            x,
            y,
            size,
            size
        );

        g.setColor(Color.RED);

        g.fillRect(
            x + 5,
            y + 2,
            4,
            10
        );

        g.fillRect(
            x + 2,
            y + 5,
            10,
            4
        );

        g.setColor(Color.BLACK);

        g.drawRect(
            x,
            y,
            size,
            size
        );
    }
}


/*
 * FOOD
 */
class Food extends Resource {

    private int nutrition;

    public Food(
        int quantity,
        int nutrition
    ) {

        super(quantity);

        this.nutrition =
            nutrition;
    }

    public Food(
        int quantity,
        int nutrition,
        int x,
        int y
    ) {

        super(
            quantity,
            x,
            y
        );

        this.nutrition =
            nutrition;
    }

    @Override
    public void use(
        Character target
    ) {

        if (quantity <= 0) {
            return;
        }

        if (
            target instanceof Human human
        ) {

            human.stamina +=
                nutrition;

            if (
                human.stamina > 100
            ) {

                human.stamina = 100;
            }

            quantity--;
        }
    }

    @Override
    public void draw(
        Graphics g
    ) {

        if (collected) {
            return;
        }

        g.setColor(
            new Color(
                230,
                170,
                60
            )
        );

        g.fillRect(
            x,
            y,
            size,
            size
        );

        g.setColor(Color.BLACK);

        g.drawRect(
            x,
            y,
            size,
            size
        );
    }
}


/*
 * WEAPON
 */
class Weapon extends Resource {

    private int damage;
    private int durability;

    public Weapon(
        int quantity,
        int damage,
        int durability
    ) {

        super(quantity);

        this.damage =
            damage;

        this.durability =
            durability;
    }

    public Weapon(
        int quantity,
        int damage,
        int durability,
        int x,
        int y
    ) {

        super(
            quantity,
            x,
            y
        );

        this.damage =
            damage;

        this.durability =
            durability;
    }

    /*
     * Checks whether the weapon
     * currently has ammunition and
     * enough durability to fire.
     */
    public boolean canFire() {

        return quantity > 0
            &&
            durability > 0;
    }

    /*
     * Fire the weapon at a target.
     *
     * Returns true if a shot was
     * successfully fired.
     */
    public boolean fire(
        Character target
    ) {

        if (
            target == null
            ||
            !canFire()
        ) {

            return false;
        }

        target.takeDamage(
            damage
        );

        durability--;

        quantity--;

        return true;
    }

    public int getDamage() {
        return damage;
    }

    public int getAmmo() {
        return quantity;
    }

    public int getDurability() {
        return durability;
    }

    /*
     * Allows ammunition to be
     * replenished later.
     */
    public void addAmmo(
        int amount
    ) {

        if (amount > 0) {

            quantity +=
                amount;
        }
    }

    @Override
    public void use(
        Character target
    ) {

        fire(target);
    }

    @Override
    public void draw(
        Graphics g
    ) {

        if (collected) {
            return;
        }

        g.setColor(
            Color.DARK_GRAY
        );

        g.fillRect(
            x,
            y,
            18,
            6
        );

        g.fillRect(
            x + 10,
            y + 5,
            5,
            8
        );
    }
}
