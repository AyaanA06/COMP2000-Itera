package itera.model.zombie.targeting;

import itera.model.human.Human;
import itera.model.zombie.Zombie;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Goes after the nearest human of one role (for example Civilian),
 * and uses the fallback strategy when none of that role are left.
 */
public final class PreferredRoleTarget<T extends Human> implements TargetingStrategy {
    private final Class<T> role;
    private final TargetingStrategy fallback;

    public PreferredRoleTarget(Class<T> role, TargetingStrategy fallback) {
        this.role = Objects.requireNonNull(role, "Role is required");
        this.fallback = Objects.requireNonNull(fallback, "Fallback strategy is required");
    }

    public Class<T> getRole() {
        return role;
    }

    @Override
    public Optional<Human> chooseTarget(Zombie zombie, List<Human> humans) {
        Optional<Human> preferred = TargetingStrategy.eligible(humans)
            .filter(role::isInstance)
            .min(Comparator.comparingDouble(
                human -> zombie.getPosition().distanceTo(human.getPosition())));

        return preferred.isPresent() ? preferred : fallback.chooseTarget(zombie, humans);
    }
}
