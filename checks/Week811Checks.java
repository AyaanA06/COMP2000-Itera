
package itera.checks;

import itera.model.*;
import itera.model.human.*;
import itera.model.human.treatment.*;
import itera.model.zombie.*;
import itera.simulation.*;
import itera.ui.Main;
import java.util.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** Standalone deterministic checks; no external test library or display needed. */
public final class Week811Checks {
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

    private static <T extends Component> T component(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = component(container, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Medic medic = new Medic(0, 0);
        Soldier far = injured(new Soldier(100, 0), 10);
        Soldier near = injured(new Soldier(20, 0), 10);
        Civilian critical = injured(new Civilian(10, 0), 90);
        List<Human> patients = List.of(far, critical, near);
        check(medic.selectPatient(patients).orElseThrow() == near, "Default soldiers then distance");
        medic.setTreatmentStrategy(new MostInjuredStrategy());
        check(medic.selectPatient(patients).orElseThrow() == critical, "Lowest health first");
        Civilian tied = injured(new Civilian(5, 0), 90);
        check(medic.selectPatient(List.of(critical, tied)).orElseThrow() == tied, "Health tie uses distance");
        Soldier dead = injured(new Soldier(1, 0), 100);
        Civilian full = new Civilian(1, 0);
        Civilian sheltered = injured(new Civilian(50, 50), 95);
        sheltered.setEnvironment(List.of(new ConvenienceStore(0, 0)), List.of());
        check(sheltered.isSheltered(), "Fixture is inside a building");
        medic.takeDamage(10);
        check(medic.selectPatient(List.of(medic, dead, full, sheltered)).isEmpty(), "Exclude self, dead, healthy and sheltered");
        check(medic.selectPatient(List.of()).isEmpty(), "Empty candidate list");
        medic.setTreatmentStrategy(m -> Comparator.comparingInt(Human::getHealth).reversed());
        check(medic.selectPatient(List.of(near, critical)).orElseThrow() == near, "Lambda can supply ranking behaviour");
        rejects(NullPointerException.class, () -> medic.setTreatmentStrategy(null));

        World world = new World();
        world.addCharacter(medic);
        TreatmentStrategy policy = new MostInjuredStrategy();
        world.setTreatmentStrategy(policy);
        check(medic.getTreatmentStrategy() == policy, "Existing medic gets policy");
        Medic reinforcement = new Medic(0, 0);
        world.addCharacter(reinforcement);
        check(reinforcement.getTreatmentStrategy() == policy, "New medic gets policy");
        rejects(NullPointerException.class, () -> world.setTreatmentStrategy(null));
        world.addCharacter(near);
        world.addCharacter(dead);
        world.addCharacter(critical);
        world.addCharacter(new Zombie(0, 0));
        check(world.findCharacters(Human.class, Human::isAlive).size() == 4, "Typed living filter");
        check(world.findCharacters(Soldier.class, Human::isAlive).size() == 1, "Supertype method reference predicate");
        check(world.findCharacters(Human.class, h -> h.getHealth() < 20 && h.isAlive()).size() == 1, "Composed lambda filter");
        world.getHumans().clear();
        check(world.getHumans().size() == 5, "Result list mutations cannot clear world");
        rejects(NullPointerException.class, () -> world.findCharacters(Human.class, null));
        rejects(NullPointerException.class, () -> world.findCharacters(null, h -> true));

        // Verify cumulative zombie elimination tracking.
        World killWorld = new World();
        Zombie fallen = new Zombie(0, 0);
        Zombie surviving = new Zombie(1, 1);

        killWorld.addCharacter(fallen);
        killWorld.addCharacter(surviving);

        check(killWorld.getTotalZombieEliminations() == 0,
            "No eliminations initially");

        fallen.takeDamage(10000);
        killWorld.update();

        check(killWorld.getTotalZombieEliminations() == 1,
            "Counts dead zombie on removal");

        killWorld.update();

        check(killWorld.getTotalZombieEliminations() == 1,
            "Does not count removed zombie twice");

        surviving.takeDamage(10000);
        killWorld.update();

        check(killWorld.getTotalZombieEliminations() == 2,
            "Accumulates later eliminations");

        Civilian deadCivilian = new Civilian(0, 0);
        killWorld.addCharacter(deadCivilian);
        deadCivilian.takeDamage(10000);
        killWorld.update();

        check(killWorld.getTotalZombieEliminations() == 2,
            "Human deaths do not increment zombie count");

        // Safe Zone threat detection tests.
        World threatWorld = new World();
        SafePoint threatSafePoint = new SafePoint(0, 0, 180, 180, 10);

        check(
            threatWorld.countZombiesNearSafePoint(threatSafePoint, 150) == 0,
            "Safe Zone is clear without zombies"
        );

        Zombie nearbyOne = new Zombie(200, 100);
        Zombie nearbyTwo = new Zombie(250, 100);
        Zombie nearbyThree = new Zombie(300, 100);
        Zombie distantZombie = new Zombie(700, 700);

        threatWorld.addCharacter(nearbyOne);
        threatWorld.addCharacter(distantZombie);

        check(
            threatWorld.countZombiesNearSafePoint(threatSafePoint, 150) == 1,
            "One nearby zombie triggers warning count"
        );

        threatWorld.addCharacter(nearbyTwo);

        check(
            threatWorld.countZombiesNearSafePoint(threatSafePoint, 150) == 2,
            "Two nearby zombies are counted"
        );

        threatWorld.addCharacter(nearbyThree);

        check(
            threatWorld.countZombiesNearSafePoint(threatSafePoint, 150) == 3,
            "Three nearby zombies trigger high threat count"
        );

        nearbyThree.takeDamage(10000);

        check(
            threatWorld.countZombiesNearSafePoint(threatSafePoint, 150) == 2,
            "Dead zombies are excluded before cleanup"
        );

        check(
            threatWorld.countZombiesNearSafePoint(threatSafePoint, 0) == 0,
            "Radius zero only detects overlap with Safe Zone"
        );

        rejects(
            IllegalArgumentException.class,
            () -> threatWorld.countZombiesNearSafePoint(threatSafePoint, -1)
        );

        rejects(
            NullPointerException.class,
            () -> threatWorld.countZombiesNearSafePoint(null, 150)
        );

        // NEW: Live Zombie Type Breakdown regression tests.
        World typeWorld = new World();
        Map<String, Long> emptyTypeCounts = typeWorld.getLivingZombieTypeCounts();

        check(new ArrayList<>(emptyTypeCounts.keySet()).equals(
                List.of("Standard", "Runner", "Stalker", "Bloater", "Brute", "Spitter", "Boss")),
            "Zombie breakdown includes every type in display order");

        check(emptyTypeCounts.values().stream().allMatch(count -> count == 0L),
            "Empty world shows zero for all zombie types");

        rejects(UnsupportedOperationException.class,
            () -> emptyTypeCounts.put("Runner", 99L));

        Zombie typeStandard = new Zombie(10, 10);
        Runner typeRunner = new Runner(20, 20);
        Stalker typeStalker = new Stalker(30, 30);
        Bloater typeBloater = new Bloater(40, 40);

        typeWorld.addCharacter(typeStandard);
        typeWorld.addCharacter(typeRunner);
        typeWorld.addCharacter(typeStalker);
        typeWorld.addCharacter(typeBloater);
        typeWorld.addCharacter(new Civilian(50, 50));

        // Use the real boss-wave spawning path to create a Mutant Boss.
        ZombieWave typeBossWave = new ZombieWave(1, true, Runner::new);
        typeBossWave.addSpawnPoint(new Vector2D(60, 60));
        typeWorld.spawnWave(typeBossWave);

        Map<String, Long> mixedTypeCounts = typeWorld.getLivingZombieTypeCounts();

        check(mixedTypeCounts.get("Standard") == 1L,
            "Counts standard zombies");

        check(mixedTypeCounts.get("Runner") == 1L,
            "Counts runners");

        check(mixedTypeCounts.get("Stalker") == 1L,
            "Counts stalkers");

        check(mixedTypeCounts.get("Bloater") == 1L,
            "Counts bloaters");

        check(mixedTypeCounts.get("Boss") == 1L,
            "Counts mutant bosses separately");

        check(mixedTypeCounts.get("Brute") == 0L && mixedTypeCounts.get("Spitter") == 0L,
            "New types show zero until they spawn");

        check(mixedTypeCounts.values().stream().mapToLong(Long::longValue).sum() == 5L,
            "Breakdown totals only the five living zombies, not humans");

        typeStandard.takeDamage(10000);

        check(typeWorld.getLivingZombieTypeCounts().get("Standard") == 0L,
            "Dead standard zombie excluded before cleanup");

        check(mixedTypeCounts.get("Standard") == 1L,
            "Previous type-count snapshot stays unchanged");

        typeWorld.update();

        check(typeWorld.getLivingZombieTypeCounts().get("Standard") == 0L,
            "Dead standard zombie remains excluded after cleanup");

        typeWorld.addCharacter(new Runner(70, 70));

        check(typeWorld.getLivingZombieTypeCounts().get("Runner") == 2L,
            "Newly spawned runner increases the live count");

        typeRunner.takeDamage(10000);

        check(typeWorld.getLivingZombieTypeCounts().get("Runner") == 1L,
            "Eliminated runner decreases the live count");

        PopulationSnapshot snapshot = world.populationSnapshot();
        check(snapshot.getRoles().get("Soldier").count() == 1, "Dead soldier excluded");
        check(snapshot.getRoles().get("Medic").wounded() == 1, "Wounded count");
        check(snapshot.getRoles().get("Medic").averageHealth() == 95, "Mean health");
        check(new ArrayList<>(snapshot.getRoles().keySet()).equals(List.of("Civilian", "Medic", "Soldier")), "Stable alphabetical display");
        medic.takeDamage(20);
        check(snapshot.getRoles().get("Medic").averageHealth() == 95, "Snapshot detached from live health");
        rejects(UnsupportedOperationException.class, () -> snapshot.getRoles().clear());
        check(new World().populationSnapshot().getRoles().isEmpty(), "Empty snapshot");

        List<Human> largePopulation = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largePopulation.add(new Civilian(0, 0));
        }
        PopulationSnapshot largeSnapshot = new PopulationSnapshot(largePopulation);
        check(largeSnapshot.getRoles().get("Civilian").count() == 1000,
            "Parallel reduction combines all population entries");
        check(largeSnapshot.getRoles().get("Civilian").averageHealth() == 100,
            "Parallel reduction preserves average health");

        World waves = new World();
        Zombie survivor = new Zombie(2, 2);
        waves.addCharacter(survivor);
        ZombieWave runners = new ZombieWave(3, false, Runner::new);
        runners.addSpawnPoint(new Vector2D(40, 60));
        waves.spawnWave(runners);
        check(waves.getZombies().size() == 4 && waves.getZombies().contains(survivor), "Wave preserves survivors");
        check(waves.findCharacters(Runner.class, z -> z.getX() == 40 && z.getY() == 60).size() == 3, "Injected constructor gets coordinates");
        ZombieWave boss = new ZombieWave(10, true, Runner::new);
        boss.addSpawnPoint(new Vector2D(20, 30));
        waves.spawnWave(boss);
        check(waves.findCharacters(MutantBoss.class, z -> true).size() == 1 && waves.getZombies().size() == 5, "Boss remains a single boss");
        waves.spawnWave(new ZombieWave(3));
        check(waves.getZombies().size() == 5, "No spawn points means no additions");
        rejects(IllegalArgumentException.class, () -> new ZombieWave(-1));
        rejects(NullPointerException.class, () -> new ZombieWave(1, false, null));
        ZombieWave missing = new ZombieWave(1, false, (x, y) -> null);
        missing.addSpawnPoint(new Vector2D(1, 1));
        rejects(NullPointerException.class, () -> missing.trigger(waves));

        Class<?>[] types = {Zombie.class, Runner.class, Stalker.class, Bloater.class, Brute.class, Spitter.class};
        for (int i = 0; i < types.length; i++)
            check(ZombieSpawns.atType(i, 9, 8).getClass() == types[i], "Initial type rotation " + i);

        Random random = new Random(42);
        for (int i = 0; i < 30; i++) {
            Zombie zombie = ZombieSpawns.random(random, 9, 8);
            check(Arrays.asList(types).contains(zombie.getClass()) && zombie.getX() == 9 && zombie.getY() == 8, "Random spawn family and coordinates");
        }

        Medic healer = injured(new Medic(0, 0), 80);
        World healingWorld = new World();
        healingWorld.addCharacter(healer);
        healer.interact(new Medicine(3, 20, 0, 0));
        Soldier patient = injured(new Soldier(10, 0), 50);
        healer.heal(patient);
        check(patient.getHealth() == 50, "Critical medic must self-heal first");
        healer.heal(healer);
        check(healer.getHealth() == 40 && healer.getMedKits() == 2, "Critical self-healing consumes one use");
        healer.heal(patient);
        check(patient.getHealth() == 50, "Healing cooldown preserved");
        healingWorld.advanceTime(1000);
        healer.heal(patient);
        check(patient.getHealth() == 70 && healer.getMedKits() == 1, "Heal after simulation second");

        SwingUtilities.invokeAndWait(() -> {
            try {
                Main simulation = new Main(new SimulationSettings(20, 10));
                JPanel controls = simulation.getSimulationControls();
                JComboBox<?> choice = component(controls, JComboBox.class);
                check(choice != null && choice.getItemCount() == 2, "Both choices visible");

                var field = Main.class.getDeclaredField("world");
                field.setAccessible(true);
                World uiWorld = (World) field.get(simulation);

                check(uiWorld.getHumans().size() == 20 && uiWorld.getZombies().size() == 10, "Default population unchanged");

                // NEW: Live Zombie Type Breakdown UI tests.
                var typeLabelField = Main.class.getDeclaredField("zombieTypeSummary");
                typeLabelField.setAccessible(true);
                JLabel typeLabel = (JLabel) typeLabelField.get(simulation);

                check(typeLabel.getParent() != null,
                    "Zombie type summary is attached to the controls");

                check(typeLabel.getText().contains("Standard: 2")
                        && typeLabel.getText().contains("Runner: 2")
                        && typeLabel.getText().contains("Stalker: 2")
                        && typeLabel.getText().contains("Bloater: 2")
                        && typeLabel.getText().contains("Brute: 1")
                        && typeLabel.getText().contains("Spitter: 1")
                        && typeLabel.getText().contains("Boss: 0"),
                    "Initial UI breakdown matches the ten rotating zombie spawns");

                // Boss Wave Early Warning regression tests.
                var bossWarning = Main.class.getDeclaredMethod(
                    "isBossWaveIncoming", int.class, long.class
                );
                bossWarning.setAccessible(true);

                check((boolean) bossWarning.invoke(null, 4, 5L),
                    "Boss warning starts five seconds before Wave 5");

                check((boolean) bossWarning.invoke(null, 4, 1L),
                    "Boss warning remains active one second before Wave 5");

                check(!(boolean) bossWarning.invoke(null, 4, 6L),
                    "Boss warning does not appear too early");

                check(!(boolean) bossWarning.invoke(null, 4, 0L),
                    "Boss warning stops when countdown reaches zero");

                check(!(boolean) bossWarning.invoke(null, 3, 5L),
                    "Normal waves do not trigger boss warning");

                check((boolean) bossWarning.invoke(null, 9, 5L),
                    "Warning also works before Wave 10");

                check(!(boolean) bossWarning.invoke(null, 0, 5L),
                    "Invalid starting wave does not trigger warning");

                // Survival timer tests.
                Main timedSimulation = new Main(new SimulationSettings(20, 10));
                World timedWorld = (World) field.get(timedSimulation);

                var survivalTime = Main.class.getDeclaredMethod("getSurvivalTime");
                survivalTime.setAccessible(true);

                check("00:00".equals(survivalTime.invoke(timedSimulation)),
                    "Survival timer starts at zero");

                timedWorld.advanceTime(65_000);
                check("01:05".equals(survivalTime.invoke(timedSimulation)),
                    "Survival timer formats elapsed simulation time");

                timedWorld.advanceTime(3_535_000);
                check("60:00".equals(survivalTime.invoke(timedSimulation)),
                    "Survival timer supports one hour");

                Main freshSimulation = new Main(new SimulationSettings(20, 10));
                check("00:00".equals(survivalTime.invoke(freshSimulation)),
                    "New simulation resets survival timer");

                choice.setSelectedIndex(1);
                check(uiWorld.findCharacters(Medic.class, m -> m.getTreatmentStrategy() instanceof MostInjuredStrategy).size() == 6, "UI applies selected strategy");

                var spawn = Main.class.getDeclaredMethod("addHumanReinforcements");
                spawn.setAccessible(true);
                spawn.invoke(simulation);

                check(uiWorld.findCharacters(Medic.class, m -> m.getTreatmentStrategy() instanceof MostInjuredStrategy).size() == 8, "Actual reinforcement path inherits selection");
                check(uiWorld.getHumans().size() == 26, "Six human reinforcements retained");

                choice.setSelectedIndex(0);
                check(uiWorld.findCharacters(Medic.class, m -> m.getTreatmentStrategy() instanceof SoldiersFirstStrategy).size() == 8, "Switch back applies to all medics");

                simulation.setSize(1200, 800);
                var update = Main.class.getDeclaredMethod("updateSimulation");
                update.setAccessible(true);
                for (int i = 0; i < 20; i++) update.invoke(simulation);

                var label = Main.class.getDeclaredField("populationSummary");
                label.setAccessible(true);
                check(((JLabel) label.get(simulation)).getText().contains("wounded"), "Live summary refreshed");

                // NEW: Verify live UI counts after a spawn and an elimination.
                var refresh = Main.class.getDeclaredMethod("refreshPopulationSummary");
                refresh.setAccessible(true);
                long runnerCountBeforeSpawn = uiWorld.getLivingZombieTypeCounts().get("Runner");

                Runner extraRunner = new Runner(600, 400);
                uiWorld.addCharacter(extraRunner);
                refresh.invoke(simulation);

                check(typeLabel.getText().contains("Runner: " + (runnerCountBeforeSpawn + 1)),
                    "UI reflects a newly spawned runner");

                extraRunner.takeDamage(10000);
                refresh.invoke(simulation);

                check(typeLabel.getText().contains("Runner: " + runnerCountBeforeSpawn),
                    "UI excludes an eliminated runner immediately");

                var graphics = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB).createGraphics();
                simulation.paint(graphics);
                graphics.dispose();
                check(true, "Simulation renders headlessly after updates");

            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
        });

        System.out.println("Passed " + checks + " checks.");
    }
}
