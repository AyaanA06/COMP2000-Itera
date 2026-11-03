
package itera.simulation;

import itera.model.*;
import itera.model.Character;
import itera.model.human.*;
import itera.model.zombie.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Predicate;

public class World {

    private itera.model.human.treatment.TreatmentStrategy treatmentStrategy =
        new itera.model.human.treatment.SoldiersFirstStrategy();

    /** Apply policy to current medics and future reinforcements. */
    public void setTreatmentStrategy(
            itera.model.human.treatment.TreatmentStrategy strategy) {

        treatmentStrategy = Objects.requireNonNull(
            strategy,
            "Treatment strategy is required"
        );

        getHumans().stream()
            .filter(Medic.class::isInstance)
            .map(Medic.class::cast)
            .forEach(medic -> medic.setTreatmentStrategy(strategy));
    }

    // All timers advance with simulation time.
    private long simulationTime = System.currentTimeMillis();

    public long getTime() {
        return simulationTime;
    }

    public void advanceTime(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException(
                "Time cannot go backwards"
            );
        }

        simulationTime += milliseconds;
    }

    // Track zombie eliminations.
    private long totalZombieEliminations;

    public long getTotalZombieEliminations() {
        return totalZombieEliminations;
    }

    private ArrayList<Character> characters = new ArrayList<>();
    private ArrayList<Resource> resources = new ArrayList<>();
    private ArrayList<Building> buildings = new ArrayList<>();
    private ArrayList<SafePoint> safePoints = new ArrayList<>();
    private ArrayList<ZombieWave> waves = new ArrayList<>();

    public void addCharacter(Character character) {
        character.setTimeSource(this::getTime);

        if (character instanceof Medic medic) {
            medic.setTreatmentStrategy(treatmentStrategy);
        }

        characters.add(character);
    }

    public void addResource(Resource resource) {
        resource.setTimeSource(this::getTime);
        resources.add(resource);
    }

    public void addBuilding(Building building) {
        buildings.add(building);
    }

    public void addSafePoint(SafePoint safePoint) {
        safePoints.add(safePoint);
    }

    public void addWave(ZombieWave wave) {
        waves.add(wave);
    }

    /**
     * Update Mutant Boss special abilities.
     *
     * Ground Smash:
     * The boss warns nearby humans before dealing area damage.
     *
     * Zombie Summoning:
     * At 50% health, the boss summons three zombies once.
     */
    public void updateBossAbilities() {

        ArrayList<Human> humans = getHumans();

        // Use a snapshot so spawning new zombies is safe.
        ArrayList<Zombie> zombies = getZombies();

        for (Zombie zombie : zombies) {

            if (!(zombie instanceof MutantBoss boss)) {
                continue;
            }

            if (!boss.isAlive()) {
                continue;
            }

            // Update Ground Smash and summoning state.
            boss.updateSpecialAbilities(
                humans,
                getTime()
            );

            // Spawn reinforcements once at 50% boss health.
            if (boss.consumeSummonRequest()) {

                for (int i = 0; i < 3; i++) {

                    double angle =
                        i * 2 * Math.PI / 3;

                    int spawnX = boss.getX()
                        + (int) (65 * Math.cos(angle));

                    int spawnY = boss.getY()
                        + (int) (65 * Math.sin(angle));

                    Zombie reinforcement = new Zombie(
                        spawnX,
                        spawnY
                    );

                    addCharacter(reinforcement);
                }
            }
        }
    }

    public void update() {

        // Count defeated zombies before removing them.
        totalZombieEliminations += characters.stream()
            .filter(character ->
                character instanceof Zombie
                && !character.isAlive()
            )
            .count();

        // Remove dead characters.
        characters.removeIf(character -> !character.isAlive());
    }

    public void spawnWave(ZombieWave wave) {
        wave.trigger(this);
    }

    /**
     * Capture current population statistics.
     */
    public PopulationSnapshot populationSnapshot() {
        return new PopulationSnapshot(
            findCharacters(Human.class, Human::isAlive)
        );
    }

    public boolean isCleared() {
        return characters.stream()
            .noneMatch(character ->
                character instanceof Zombie
                && character.isAlive()
            );
    }

    public ArrayList<Character> getCharacters() {
        return characters;
    }

    /**
     * Return characters matching the requested type and condition.
     */
    public <T extends Character> ArrayList<T> findCharacters(
            Class<T> type,
            Predicate<? super T> condition) {

        Objects.requireNonNull(
            type,
            "Character type is required"
        );

        Objects.requireNonNull(
            condition,
            "Filter is required"
        );

        return characters.stream()
            .filter(type::isInstance)
            .map(type::cast)
            .filter(condition)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Human> getHumans() {
        return findCharacters(
            Human.class,
            human -> true
        );
    }

    /**
     * Safe Zone Threat Detection.
     *
     * Counts living zombies inside or near the Safe Zone.
     * The radius creates a rectangular detection buffer
     * around the Safe Zone boundaries.
     *
     * Dead zombies are excluded from the count.
     */
    public long countZombiesNearSafePoint(
            SafePoint safePoint,
            int radius) {

        Objects.requireNonNull(
            safePoint,
            "Safe point is required"
        );

        if (radius < 0) {
            throw new IllegalArgumentException(
                "Threat detection radius cannot be negative"
            );
        }

        return getZombies().stream()
            .filter(Zombie::isAlive)
            .filter(zombie -> safePoint.wouldZombieEnter(
                zombie.getX() - radius,
                zombie.getY() - radius,
                zombie.getSize() + radius * 2
            ))
            .count();
    }

    public ArrayList<Zombie> getZombies() {
        return findCharacters(
            Zombie.class,
            zombie -> true
        );
    }

    /**
     * Live Zombie Type Breakdown.
     *
     * Uses Java Streams to group living zombies by type.
     * Dead zombies are excluded.
     *
     * Returns a snapshot with every supported zombie type,
     * including types that currently have zero living zombies.
     */
    public Map<String, Long> getLivingZombieTypeCounts() {

        Map<String, Long> totals = getZombies().stream()
            .filter(Zombie::isAlive)
            .collect(Collectors.groupingBy(
                World::zombieTypeLabel,
                Collectors.counting()
            ));

        // Maintain a consistent order for the interface:
        // every ZombieType in enum order, then the boss.
        Map<String, Long> counts = new LinkedHashMap<>();

        Stream.concat(
                Arrays.stream(ZombieType.values()).map(ZombieType::getLabel),
                Stream.of(BOSS_LABEL))
            .forEach(label -> counts.put(label, totals.getOrDefault(label, 0L)));

        return Collections.unmodifiableMap(counts);
    }

    private static final String BOSS_LABEL = "Boss";

    /**
     * Identify the correct zombie type.
     *
     * The boss is not a ZombieType, so it is checked first.
     */
    private static String zombieTypeLabel(Zombie zombie) {

        if (zombie instanceof MutantBoss) {
            return BOSS_LABEL;
        }

        return ZombieType.of(zombie).getLabel();
    }
}
