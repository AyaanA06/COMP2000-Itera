package itera.model.zombie.targeting;

import itera.model.human.Human;
import itera.model.zombie.Zombie;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Strategy for choosing which human a zombie goes after. */
@FunctionalInterface
public interface TargetingStrategy {
    Optional<Human> chooseTarget(Zombie zombie, List<Human> humans);

    /** Humans a zombie is allowed to go after: alive and not sheltered. */
    static Stream<Human> eligible(List<Human> humans) {
        return humans.stream()
            .filter(Human::isAlive)
            .filter(human -> !human.isSheltered());
    }
}
