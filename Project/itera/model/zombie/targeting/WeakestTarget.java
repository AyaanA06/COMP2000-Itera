package itera.model.zombie.targeting;

import itera.model.human.Human;
import itera.model.zombie.Zombie;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks off the human with the lowest health; distance breaks ties. */
public final class WeakestTarget implements TargetingStrategy {
    @Override
    public Optional<Human> chooseTarget(Zombie zombie, List<Human> humans) {
        return TargetingStrategy.eligible(humans)
            .min(Comparator.comparingInt(Human::getHealth)
                .thenComparingDouble(human -> zombie.getPosition().distanceTo(human.getPosition())));
    }
}
