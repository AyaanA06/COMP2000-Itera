package itera.model.zombie;

import java.util.Arrays;
import java.util.function.BiFunction;

/**
 * Every ordinary zombie type, with its display label and constructor.
 * The Mutant Boss is not listed because it only appears in boss waves.
 *
 * <pre>
 * Type      Feature                                  Targets first
 * Standard  basic melee zombie                       nearest human
 * Runner    sprints, bursts of speed when close      civilians
 * Stalker   ambush hits for extra damage             the weakest (lowest health) human
 * Bloater   explodes and hurts everyone nearby       the biggest group of humans
 * Brute     250 health, armour halves all damage     soldiers
 * Spitter   stops at range and spits acid            medics
 * </pre>
 *
 * Targeting is done with a TargetingStrategy (see the targeting package).
 * Every normal wave features one type (except Standard) in rotation,
 * see WaveRotation and ZombieWave.featuring.
 */
public enum ZombieType {
    STANDARD("Standard", Zombie.class, Zombie::new),
    RUNNER("Runner", Runner.class, Runner::new),
    STALKER("Stalker", Stalker.class, Stalker::new),
    BLOATER("Bloater", Bloater.class, Bloater::new),
    BRUTE("Brute", Brute.class, Brute::new),
    SPITTER("Spitter", Spitter.class, Spitter::new);

    private final String label;
    private final Class<? extends Zombie> zombieClass;
    private final BiFunction<Integer, Integer, Zombie> factory;

    ZombieType(String label, Class<? extends Zombie> zombieClass,
            BiFunction<Integer, Integer, Zombie> factory) {
        this.label = label;
        this.zombieClass = zombieClass;
        this.factory = factory;
    }

    public String getLabel() {
        return label;
    }

    public Zombie create(int x, int y) {
        return factory.apply(x, y);
    }

    /** Finds the type of an existing zombie; bosses are rejected. */
    public static ZombieType of(Zombie zombie) {
        return Arrays.stream(values())
            .filter(type -> type.zombieClass == zombie.getClass())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Not an ordinary zombie type: " + zombie.getClass().getSimpleName()));
    }
}
