package itera.checks;

import itera.model.human.*;
import itera.model.zombie.*;
import itera.model.zombie.targeting.*;
import java.util.*;

/** Standalone checks for zombie types and how they choose targets. */
public final class ZombieVariantChecks {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            check(expected.isInstance(error), "Expected " + expected + ", got " + error);
            return;
        }
        throw new AssertionError("Expected " + expected);
    }

    private static <T extends Human> T injured(T human, int damage) {
        human.takeDamage(damage);
        return human;
    }

    public static void main(String[] args) {
        // Nearest targeting (the original behaviour)
        Zombie zombie = new Zombie(0, 0);
        Civilian near = new Civilian(30, 0);
        Soldier far = new Soldier(200, 0);
        List<Human> humans = List.of(far, near);
        check(zombie.getTargetingStrategy() instanceof NearestTarget, "Zombies target the nearest human by default");
        check(zombie.getTargetingStrategy().chooseTarget(zombie, humans).orElseThrow() == near, "Nearest human is chosen");

        Civilian dead = injured(new Civilian(5, 0), 100);
        check(new NearestTarget().chooseTarget(zombie, List.of(dead, far)).orElseThrow() == far, "Dead humans are ignored");
        check(new NearestTarget().chooseTarget(zombie, List.of(dead)).isEmpty(), "No target when everyone is dead");
        check(new NearestTarget().chooseTarget(zombie, List.of()).isEmpty(), "No target in an empty list");

        rejects(NullPointerException.class, () -> zombie.setTargetingStrategy(null));

        // A lambda can be used as a strategy
        zombie.setTargetingStrategy((self, list) -> TargetingStrategy.eligible(list)
            .filter(human -> human instanceof Soldier)
            .findFirst());
        check(zombie.getTargetingStrategy().chooseTarget(zombie, humans).orElseThrow() == far, "Lambda strategy picks the soldier");

        // Preferred role, with a fallback when that role is gone
        PreferredRoleTarget<Soldier> soldiersFirst = new PreferredRoleTarget<>(Soldier.class, new NearestTarget());
        Zombie hunter = new Zombie(0, 0);
        check(soldiersFirst.getRole() == Soldier.class, "Role is stored");
        check(soldiersFirst.chooseTarget(hunter, humans).orElseThrow() == far, "Preferred role beats a closer human");
        Soldier nearSoldier = new Soldier(50, 0);
        check(soldiersFirst.chooseTarget(hunter, List.of(far, near, nearSoldier)).orElseThrow() == nearSoldier, "Nearest of the preferred role");
        check(soldiersFirst.chooseTarget(hunter, List.of(near, dead)).orElseThrow() == near, "Falls back when no soldiers are left");
        Soldier deadSoldier = injured(new Soldier(1, 0), 100);
        check(soldiersFirst.chooseTarget(hunter, List.of(deadSoldier, near)).orElseThrow() == near, "Dead soldiers do not count");
        rejects(NullPointerException.class, () -> new PreferredRoleTarget<>(Soldier.class, null));
        rejects(NullPointerException.class, () -> new PreferredRoleTarget<Soldier>(null, new NearestTarget()));

        System.out.println("Passed " + checks + " zombie variant checks.");
    }
}
