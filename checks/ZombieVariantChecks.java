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

        // Runners go after civilians
        Runner runner = new Runner(0, 0);
        Soldier closeSoldier = new Soldier(10, 0);
        Civilian farCivilian = new Civilian(150, 0);
        check(runner.getTargetingStrategy().chooseTarget(runner, List.of(closeSoldier, farCivilian)).orElseThrow() == farCivilian,
            "Runner skips a closer soldier for a civilian");
        check(runner.getTargetingStrategy().chooseTarget(runner, List.of(closeSoldier)).orElseThrow() == closeSoldier,
            "Runner falls back to the nearest human");

        // Weakest human first
        WeakestTarget weakest = new WeakestTarget();
        Civilian healthy = new Civilian(10, 0);
        Soldier wounded = injured(new Soldier(300, 0), 60);
        check(weakest.chooseTarget(hunter, List.of(healthy, wounded)).orElseThrow() == wounded, "Lowest health beats distance");
        Soldier woundedNear = injured(new Soldier(100, 0), 60);
        check(weakest.chooseTarget(hunter, List.of(wounded, woundedNear)).orElseThrow() == woundedNear, "Health tie uses distance");
        Civilian dying = injured(new Civilian(1, 0), 100);
        check(weakest.chooseTarget(hunter, List.of(dying, healthy)).orElseThrow() == healthy, "Dead humans are not the weakest");

        // Stalkers go after the injured
        Stalker stalker = new Stalker(0, 0);
        check(stalker.getTargetingStrategy().chooseTarget(stalker, List.of(healthy, wounded)).orElseThrow() == wounded,
            "Stalker picks off the injured human");

        // Biggest crowd first
        CrowdTarget crowd = new CrowdTarget();
        Civilian loner = new Civilian(20, 0);
        Civilian groupA = new Civilian(400, 0);
        Civilian groupB = new Civilian(420, 0);
        Civilian groupC = new Civilian(400, 30);
        List<Human> crowdScene = List.of(loner, groupA, groupB, groupC);
        check(crowd.getRadius() == CrowdTarget.DEFAULT_RADIUS, "Default crowd radius");
        check(crowd.chooseTarget(hunter, crowdScene).orElseThrow() == groupA, "Picks the human in the middle of the group");
        check(new CrowdTarget(0).chooseTarget(hunter, crowdScene).orElseThrow() == loner, "No crowds means the nearest human");
        injured(groupA, 100);
        injured(groupB, 100);
        check(crowd.chooseTarget(hunter, crowdScene).orElseThrow() == loner, "Dead humans do not make a crowd");
        check(crowd.chooseTarget(hunter, List.of()).isEmpty(), "No target in an empty list");
        rejects(IllegalArgumentException.class, () -> new CrowdTarget(-1));

        System.out.println("Passed " + checks + " zombie variant checks.");
    }
}
