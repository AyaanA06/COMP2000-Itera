package itera.model.zombie.targeting;

import itera.model.human.Human;
import itera.model.zombie.Zombie;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Walks towards the biggest group: the human with the most other
 * eligible humans within the radius. Distance breaks ties.
 */
public final class CrowdTarget implements TargetingStrategy {
    public static final double DEFAULT_RADIUS = 80;

    private final double radius;

    public CrowdTarget() {
        this(DEFAULT_RADIUS);
    }

    public CrowdTarget(double radius) {
        if (radius < 0) throw new IllegalArgumentException("Crowd radius cannot be negative");
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public Optional<Human> chooseTarget(Zombie zombie, List<Human> humans) {
        List<Human> eligible = TargetingStrategy.eligible(humans).toList();

        return eligible.stream()
            .max(Comparator.comparingLong((Human human) -> neighboursOf(human, eligible))
                .thenComparingDouble(human -> -zombie.getPosition().distanceTo(human.getPosition())));
    }

    /** How many other eligible humans stand within the radius of this one. */
    private long neighboursOf(Human human, List<Human> eligible) {
        return eligible.stream()
            .filter(other -> other != human)
            .filter(other -> human.getPosition().distanceTo(other.getPosition()) <= radius)
            .count();
    }
}
