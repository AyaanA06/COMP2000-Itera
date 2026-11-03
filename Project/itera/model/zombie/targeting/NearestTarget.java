package itera.model.zombie.targeting;

import itera.model.human.Human;
import itera.model.zombie.Zombie;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** The original behaviour: go after the closest eligible human. */
public final class NearestTarget implements TargetingStrategy {
    @Override
    public Optional<Human> chooseTarget(Zombie zombie, List<Human> humans) {
        return TargetingStrategy.eligible(humans)
            .min(Comparator.comparingDouble(
                human -> zombie.getPosition().distanceTo(human.getPosition())));
    }
}
