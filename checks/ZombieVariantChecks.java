package itera.checks;

import itera.model.*;
import itera.model.human.*;
import itera.model.zombie.*;
import itera.model.zombie.targeting.*;
import itera.simulation.*;
import itera.ui.Main;
import java.util.*;
import javax.swing.SwingUtilities;

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

    public static void main(String[] args) throws Exception {
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

        // Bloaters head for groups
        Bloater bloater = new Bloater(0, 0);
        Civilian alone = new Civilian(20, 0);
        Civilian pairA = new Civilian(300, 0);
        Civilian pairB = new Civilian(320, 0);
        check(bloater.getTargetingStrategy().chooseTarget(bloater, List.of(alone, pairA, pairB)).orElseThrow() == pairA,
            "Bloater walks towards the group");

        // Brute: slow, big and armoured
        Brute brute = new Brute(0, 0);
        check(brute.getHealth() == 250 && brute.getMaxHealth() == 250, "Brute has 250 health");
        check(brute.getSpeed() < new Zombie(0, 0).getSpeed() && brute.getSize() == 24, "Brute is slow and big");
        brute.receiveGunshot();
        check(brute.getHealth() == 225, "Armour halves a gunshot to 25");
        brute.takeDamage(10);
        check(brute.getHealth() == 220, "Armour halves other damage too");
        brute.takeDamage(0);
        check(brute.getHealth() == 220, "Zero damage still does nothing");
        brute.takeDamage(10000);
        check(!brute.isAlive() && brute.getHealth() == 0, "Brute can still be killed");

        Brute soldierHunter = new Brute(0, 0);
        Civilian closeCivilian = new Civilian(10, 0);
        Soldier distantSoldier = new Soldier(250, 0);
        check(soldierHunter.getTargetingStrategy().chooseTarget(soldierHunter, List.of(closeCivilian, distantSoldier)).orElseThrow() == distantSoldier,
            "Brute goes after soldiers first");

        // Spitter: ranged acid with a cooldown
        long[] clock = {10000};
        SafePoint farSafePoint = new SafePoint(2000, 2000, 50, 50, 10);
        Spitter spitter = new Spitter(0, 0);
        spitter.setTimeSource(() -> clock[0]);
        Civilian victim = new Civilian(100, 0);
        victim.setTimeSource(() -> clock[0]);
        ArrayList<Human> targets = new ArrayList<>(List.of(victim));
        spitter.update(800, 600, targets, farSafePoint);
        check(victim.getHealth() == 100 - Spitter.SPIT_DAMAGE, "Spitter hits from range");
        check(spitter.getX() == 0 && spitter.getY() == 0, "Spitter stands still while spitting");
        clock[0] = 11000;
        spitter.update(800, 600, targets, farSafePoint);
        check(victim.getHealth() == 100 - Spitter.SPIT_DAMAGE, "Spitter waits for its cooldown");
        clock[0] = 11500;
        spitter.update(800, 600, targets, farSafePoint);
        check(victim.getHealth() == 100 - 2 * Spitter.SPIT_DAMAGE, "Spitter spits again after the cooldown");

        Civilian outOfRange = new Civilian(400, 0);
        Spitter walker = new Spitter(0, 0);
        walker.update(800, 600, new ArrayList<>(List.of(outOfRange)), farSafePoint);
        check(outOfRange.getHealth() == 100 && walker.getX() > 0, "Spitter walks closer when out of range");

        Civilian finished = injured(new Civilian(100, 0), 90);
        finished.setTimeSource(() -> clock[0]);
        Spitter finisher = new Spitter(0, 0);
        finisher.setTimeSource(() -> clock[0]);
        check(finisher.update(800, 600, new ArrayList<>(List.of(finished)), farSafePoint) == finished,
            "Spitter reports the human it killed");
        check(new Spitter(0, 0).getMaxHealth() == 80, "Spitter is fragile");

        Spitter medicHunter = new Spitter(0, 0);
        Medic distantMedic = new Medic(300, 0);
        check(medicHunter.getTargetingStrategy().chooseTarget(medicHunter, List.of(closeCivilian, distantMedic)).orElseThrow() == distantMedic,
            "Spitter goes after medics first");

        // ZombieType enum
        check(ZombieType.values().length == 6, "Six ordinary zombie types");
        for (ZombieType type : ZombieType.values()) {
            Zombie made = type.create(7, 9);
            check(ZombieType.of(made) == type && made.getX() == 7 && made.getY() == 9, "Factory and lookup agree for " + type);
        }
        check(ZombieType.BRUTE.getLabel().equals("Brute") && ZombieType.STANDARD.getLabel().equals("Standard"), "Display labels");
        check(ZombieType.SPITTER.create(0, 0) instanceof Spitter, "Spitter factory");
        rejects(IllegalArgumentException.class, () -> ZombieType.of(new MutantBoss(0, 0)));

        // The live breakdown knows about the new types
        World typeWorld = new World();
        typeWorld.addCharacter(new Brute(10, 10));
        typeWorld.addCharacter(new Spitter(20, 20));
        typeWorld.addCharacter(new Spitter(30, 30));
        Map<String, Long> typeCounts = typeWorld.getLivingZombieTypeCounts();
        check(typeCounts.get("Brute") == 1L && typeCounts.get("Spitter") == 2L, "Brutes and Spitters are counted");
        check(typeCounts.get("Standard") == 0L, "New types are not counted as standard zombies");

        // Random spawns can produce every type
        Random spawnRandom = new Random(7);
        Set<ZombieType> spawned = EnumSet.noneOf(ZombieType.class);
        for (int i = 0; i < 200; i++) spawned.add(ZombieType.of(ZombieSpawns.random(spawnRandom, 0, 0)));
        check(spawned.equals(EnumSet.allOf(ZombieType.class)), "Random spawns include Brutes and Spitters");
        check(ZombieSpawns.typeCount() == ZombieType.values().length, "Spawn rotation covers every type");

        // Featured type for each wave
        check(WaveRotation.featuredFor(1).orElseThrow() == ZombieType.RUNNER, "Wave 1 features Runners");
        check(WaveRotation.featuredFor(2).orElseThrow() == ZombieType.STALKER, "Wave 2 features Stalkers");
        check(WaveRotation.featuredFor(4).orElseThrow() == ZombieType.BRUTE, "Wave 4 features Brutes");
        check(WaveRotation.isBossWave(5) && WaveRotation.featuredFor(5).isEmpty(), "Boss waves have no featured type");
        check(WaveRotation.featuredFor(6).orElseThrow() == ZombieType.SPITTER, "Spitters still get a turn after the boss");
        check(WaveRotation.featuredFor(7).orElseThrow() == ZombieType.RUNNER, "Rotation starts again");
        Set<ZombieType> featured = EnumSet.noneOf(ZombieType.class);
        for (int wave = 1; wave <= 30; wave++) WaveRotation.featuredFor(wave).ifPresent(featured::add);
        check(featured.equals(EnumSet.complementOf(EnumSet.of(ZombieType.STANDARD))), "Every special type is featured, standard never is");
        rejects(IllegalArgumentException.class, () -> WaveRotation.featuredFor(0));
        rejects(IllegalArgumentException.class, () -> WaveRotation.isBossWave(-3));

        // Featured waves
        ZombieWave stalkerWave = ZombieWave.featuring(100, ZombieType.STALKER, new Random(11));
        check(stalkerWave.getFeaturedType().orElseThrow() == ZombieType.STALKER && !stalkerWave.isBossWave(), "Wave remembers its featured type");
        World waveWorld = new World();
        stalkerWave.addSpawnPoint(new Vector2D(100, 100));
        waveWorld.spawnWave(stalkerWave);
        long stalkers = waveWorld.getZombies().stream().filter(z -> ZombieType.of(z) == ZombieType.STALKER).count();
        check(waveWorld.getZombies().size() == 100, "Featured wave spawns the full count");
        check(stalkers >= 55 && stalkers <= 80, "Most of the wave is the featured type (" + stalkers + ")");
        check(waveWorld.getZombies().stream().anyMatch(z -> ZombieType.of(z) != ZombieType.STALKER), "The rest of the wave is mixed");
        check(new ZombieWave(3).getFeaturedType().isEmpty() && new ZombieWave(1, true).getFeaturedType().isEmpty(),
            "Plain and boss waves have no featured type");
        rejects(NullPointerException.class, () -> ZombieWave.featuring(3, null, new Random()));
        rejects(IllegalArgumentException.class, () -> ZombieWave.featuring(-1, ZombieType.BRUTE, new Random()));

        // The simulation uses the rotation for its timed waves
        SwingUtilities.invokeAndWait(() -> {
            try {
                Main simulation = new Main(new SimulationSettings(20, 10));
                var worldField = Main.class.getDeclaredField("world");
                var featuredField = Main.class.getDeclaredField("featuredType");
                var nextWave = Main.class.getDeclaredMethod("updateTimedWaves");
                worldField.setAccessible(true);
                featuredField.setAccessible(true);
                nextWave.setAccessible(true);
                World simWorld = (World) worldField.get(simulation);

                check(featuredField.get(simulation) == null, "The opening wave has no featured type");
                for (int wave = 2; wave <= 5; wave++) {
                    simWorld.advanceTime(15_000);
                    nextWave.invoke(simulation);
                    check(featuredField.get(simulation) == WaveRotation.featuredFor(wave).orElse(null),
                        "Wave " + wave + " uses the rotation");
                }

                var waveLabel = Main.class.getDeclaredMethod("waveLabel", int.class, ZombieType.class);
                waveLabel.setAccessible(true);
                check("Wave: 3 (Stalker wave)".equals(waveLabel.invoke(null, 3, ZombieType.STALKER)), "HUD names the featured type");
                check("Wave: 5".equals(waveLabel.invoke(null, 5, null)), "Boss waves show just the number");
            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
        });

        System.out.println("Passed " + checks + " zombie variant checks.");
    }
}
