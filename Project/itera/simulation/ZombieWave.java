package itera.simulation;

import itera.model.*;
import itera.model.zombie.*;
import java.util.*;

public class ZombieWave {

    private int count;

    private boolean bossWave;

    private ArrayList<Vector2D> spawnPoints = new ArrayList<>();

    private final java.util.function.BiFunction<Integer, Integer, Zombie> spawnZombie;

    public ZombieWave(int count) {

        this(count, false);
    }

    public ZombieWave(int count, boolean bossWave) {

        this(count, bossWave, defaultSpawner());
    }

    /** Supply creation behaviour for experiments without rewriting wave timing. */
    public ZombieWave(int count, boolean bossWave,
            java.util.function.BiFunction<Integer, Integer, Zombie> spawnZombie) {
        if (count < 0) throw new IllegalArgumentException("Wave count cannot be negative");
        this.count = count;
        this.bossWave = bossWave;
        this.spawnZombie = Objects.requireNonNull(spawnZombie, "Zombie spawner is required");
    }

    private static java.util.function.BiFunction<Integer, Integer, Zombie> defaultSpawner() {
        Random random = new Random();
        return (x, y) -> ZombieSpawns.random(random, x, y);
    }

    /** Share of a featured wave that is the featured type; the rest are random. */
    public static final double FEATURED_SHARE = 0.6;

    private ZombieType featuredType;

    /**
     * A normal wave where most zombies are the featured type,
     * for example a "Stalker wave".
     */
    public static ZombieWave featuring(int count, ZombieType featured, Random random) {
        Objects.requireNonNull(featured, "Featured type is required");
        Objects.requireNonNull(random, "Random is required");

        ZombieWave wave = new ZombieWave(count, false, (x, y) ->
            random.nextDouble() < FEATURED_SHARE
                ? featured.create(x, y)
                : ZombieSpawns.random(random, x, y));
        wave.featuredType = featured;

        return wave;
    }

    /** The featured type, or empty for boss waves and plain random waves. */
    public Optional<ZombieType> getFeaturedType() {
        return Optional.ofNullable(featuredType);
    }

    public void addSpawnPoint(Vector2D point) {

        spawnPoints.add(point);
    }

    public void trigger(World world) {

        if (spawnPoints.isEmpty()) {
            return;
        }

        /*
         * Boss waves spawn one MutantBoss.
         */
        if (bossWave) {

            Vector2D point = spawnPoints.get(0);

            MutantBoss boss = new MutantBoss(
                (int) point.getX(),
                (int) point.getY()
            );

            world.addCharacter(boss);

            return;
        }

        /*
         * Normal waves spawn a random mixture
         * of the existing zombie types.
         */
        for (int i = 0; i < count; i++) {

            Vector2D point = spawnPoints.get(i % spawnPoints.size());

            Zombie zombie = Objects.requireNonNull(spawnZombie.apply(
                (int) point.getX(), (int) point.getY()), "Spawner returned no zombie");

            world.addCharacter(zombie);
        }
    }

    public boolean isBossWave() {

        return bossWave;
    }
}