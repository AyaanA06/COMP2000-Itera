import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class Test extends JPanel {

    private static final int WORLD_WIDTH = 1200;
    private static final int WORLD_HEIGHT = 800;

    private static final int BUILDING_SIZE = 180;
    private static final int BOTTOM_BUILDING_OFFSET = 70;

    private final Random random = new Random();

    private final World world;
    private final SafePoint safePoint;
    private final FastForward fastForward;

    private final ArrayList<Building> buildings = new ArrayList<>();

    private final ArrayList<Resource> resources = new ArrayList<>();

    private Timer timer;

    /* TIMED ZOMBIE WAVES */
    private static final long WAVE_INTERVAL = 15000;

    private static final int BASE_ZOMBIES_PER_WAVE = 4;
    private static final int ZOMBIES_ADDED_PER_WAVE = 2;

    private long lastWaveTime;
    private int waveNumber = 1;

    public Test() {

        setPreferredSize(new Dimension(WORLD_WIDTH, WORLD_HEIGHT));

        /* CREATE WORLD */
        world = new World();

        /* SAFE POINT - TOP LEFT */
        safePoint = new SafePoint(0, 0, BUILDING_SIZE, BUILDING_SIZE, 10);

        world.addSafePoint(safePoint);

        /* BUILDINGS */

        /* HOSPITAL - TOP RIGHT */
        Hospital hospital = new Hospital(WORLD_WIDTH - BUILDING_SIZE, 0);

        /* POLICE STATION - BOTTOM LEFT. Moved upward so Fast Forward controls do not cover it. */
        PoliceStation policeStation = new PoliceStation(0, WORLD_HEIGHT - BUILDING_SIZE - BOTTOM_BUILDING_OFFSET);

        /* CONVENIENCE STORE - BOTTOM RIGHT. Also moved upward. */
        ConvenienceStore convenienceStore = new ConvenienceStore(WORLD_WIDTH - BUILDING_SIZE, WORLD_HEIGHT - BUILDING_SIZE - BOTTOM_BUILDING_OFFSET);

        buildings.add(hospital);
        buildings.add(policeStation);
        buildings.add(convenienceStore);

        /* Register buildings with World */
        world.addBuilding(hospital);
        world.addBuilding(policeStation);
        world.addBuilding(convenienceStore);

        /* RESOURCES */

        /* Medicine near Hospital */
        Medicine medicine1 = new Medicine(1, 20, WORLD_WIDTH - 240, 220);

        Medicine medicine2 = new Medicine(1, 20, WORLD_WIDTH - 200, 220);

        /* Weapons near Police Station */
        Weapon weapon1 = new Weapon(5, 25, 20, 200, WORLD_HEIGHT - 310);

        Weapon weapon2 = new Weapon(5, 25, 20, 240, WORLD_HEIGHT - 310);

        /* Food near Convenience Store */
        Food food1 = new Food(1, 20, WORLD_WIDTH - 240, WORLD_HEIGHT - 310);

        Food food2 = new Food(1, 20, WORLD_WIDTH - 200, WORLD_HEIGHT - 310);

        resources.add(medicine1);
        resources.add(medicine2);

        resources.add(weapon1);
        resources.add(weapon2);

        resources.add(food1);
        resources.add(food2);

        /* Register resources with World */
        for (Resource resource : resources) {

            world.addResource(resource);
        }

        /* HUMANS */

        /* CIVILIANS */
        for (int i = 0; i < 15; i++) {

            Vector2D spawn = randomHumanPosition();

            world.addCharacter(new Civilian((int) spawn.getX(), (int) spawn.getY()));
        }

        /* SOLDIERS */
        for (int i = 0; i < 3; i++) {

            Vector2D spawn = randomHumanPosition();

            world.addCharacter(new Soldier((int) spawn.getX(), (int) spawn.getY()));
        }

        /* MEDICS */
        for (int i = 0; i < 2; i++) {

            Vector2D spawn = randomHumanPosition();

            world.addCharacter(new Medic((int) spawn.getX(), (int) spawn.getY()));
        }

        /* ZOMBIES */

        /* NORMAL ZOMBIE */
        world.addCharacter(new Zombie(WORLD_WIDTH / 2, WORLD_HEIGHT / 2));

        /* RUNNER */
        world.addCharacter(new Runner(WORLD_WIDTH / 2 + 70, WORLD_HEIGHT / 2));

        /* STALKER */
        world.addCharacter(new Stalker(WORLD_WIDTH / 2, WORLD_HEIGHT / 2 + 70));

        /* BLOATER */
        world.addCharacter(new Bloater(WORLD_WIDTH / 2 + 70, WORLD_HEIGHT / 2 + 70));

        /* FAST FORWARD */
        fastForward = new FastForward();

        /* TIMER */
        timer = new Timer(30, e -> {

                    timer.setDelay(fastForward.getDelay());

                    updateSimulation();

                    repaint();
            });
    }

    /* RANDOM HUMAN SPAWN */
    private Vector2D randomHumanPosition() {

        int x;
        int y;

        boolean invalid;

        do {

            x = random.nextInt(WORLD_WIDTH - 100) + 40;

            y = random.nextInt(WORLD_HEIGHT - 120) + 40;

            /* Check Safe Point */
            invalid = safePoint.contains(x, y, 15);

            /* Check other buildings */
            for (Building building : buildings) {

                if (building.contains(x, y)) {

                    invalid = true;
                    break;
                }
            }

        } while (invalid);

        return new Vector2D(x, y);
    }

    /* TIMED ZOMBIE WAVE SPAWNING */
    private void updateTimedWaves() {

        long now = System.currentTimeMillis();

        if (now - lastWaveTime < WAVE_INTERVAL) {

            return;
        }

        waveNumber++;

        /* Each new wave contains two more * zombies than the previous wave. * * Wave 1 = 4 * Wave 2 = 6 * Wave 3 = 8 * Wave 4 = 10 */
        int zombiesThisWave = BASE_ZOMBIES_PER_WAVE +
            (waveNumber - 1) * ZOMBIES_ADDED_PER_WAVE;

        ZombieWave wave = new ZombieWave(zombiesThisWave);

        /* Spawn points are kept away from * the four corner buildings. */
        wave.addSpawnPoint(new Vector2D(300, 30));

        wave.addSpawnPoint(new Vector2D(WORLD_WIDTH - 300, 30));

        wave.addSpawnPoint(new Vector2D(220, WORLD_HEIGHT / 2));

        wave.addSpawnPoint(new Vector2D(WORLD_WIDTH - 220, WORLD_HEIGHT / 2));

        world.addWave(wave);

        world.spawnWave(wave);

        lastWaveTime = now;
    }

    /* UPDATE SIMULATION */
    private void updateSimulation() {

        /* Do not update until JPanel * has a valid size. */
        if (getWidth() <= 0 || getHeight() <= 0) {

            return;
        }

        /* Spawn a new mixed zombie wave * at fixed time intervals. */
        updateTimedWaves();

        var humans = world.getHumans();

        var zombies = world.getZombies();

        /* UPDATE HUMANS */
        for (Human human : humans) {

            human.update(getWidth(), getHeight(), zombies, safePoint);

            /* RESOURCE COLLECTION */
            for (Resource resource : resources) {

                if (!resource.isCollected() && resource.isNear(human)) {

                    human.addResource(resource);

                    resource.collect();
                }
            }

            /* BUILDING INTERACTION */
            for (Building building : buildings) {

                if (building.contains(human.getX(), human.getY())) {

                    building.interact(human);
                }
            }
        }

        /* UPDATE ZOMBIES */
        ArrayList<Human> convertedHumans = new ArrayList<>();

        for (Zombie zombie : zombies) {

            /* Bloaters explode when a human * enters their blast radius. */
            if (zombie instanceof Bloater bloater && bloater.shouldExplode(humans)) {

                ArrayList<Human> explosionDeaths = bloater.explode(humans);

                for (Human human : explosionDeaths) {

                    if (!convertedHumans.contains(human)) {

                        convertedHumans.add(human);
                    }
                }

                continue;
            }

            Human deadHuman = zombie.update(getWidth(), getHeight(), humans, safePoint);

            /* Prevent the same human from * being converted multiple times. */
            if (deadHuman != null && !convertedHumans.contains(deadHuman)) {

                convertedHumans.add(deadHuman);
            }
        }

        /* CONVERT DEAD HUMANS */
        for (Human human : convertedHumans) {

            int zombieType = random.nextInt(4);

            Zombie newZombie;

            if (zombieType == 0) {

                newZombie = new Zombie(human.getX(), human.getY());

            } else if (zombieType == 1) {

                newZombie = new Runner(human.getX(), human.getY());

            } else if (zombieType == 2) {

                newZombie = new Stalker(human.getX(), human.getY());

            } else {

                newZombie = new Bloater(human.getX(), human.getY());
            }

            world.addCharacter(newZombie);
        }

        /* Remove dead characters. */
        world.update();
    }

    /* START SIMULATION */
    public void startSimulation() {

        lastWaveTime = System.currentTimeMillis();

        timer.start();
    }

    /* FAST FORWARD GETTER */
    public FastForward getFastForward() {

        return fastForward;
    }

    /* NEXT WAVE COUNTDOWN */
    private long getNextWaveSeconds() {

        if (lastWaveTime == 0) {

            return WAVE_INTERVAL / 1000;
        }

        long elapsed = System.currentTimeMillis() - lastWaveTime;

        long remaining = Math.max(0, WAVE_INTERVAL - elapsed);

        return (remaining + 999) / 1000;
    }

    /* DRAW SIMULATION */
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        /* BACKGROUND */
        g.setColor(new Color(220, 220, 220));

        g.fillRect(0, 0, getWidth(), getHeight());

        /* DRAW BUILDINGS */
        for (Building building : buildings) {

            building.draw(g);
        }

        /* DRAW RESOURCES */
        for (Resource resource : resources) {

            resource.draw(g);
        }

        /* DRAW SAFE POINT */
        safePoint.draw(g);

        /* DRAW CHARACTERS */
        for (Character character : world.getCharacters()) {

            character.draw(g);
        }

        /* SIMULATION STATUS DISPLAY */
        int humanCount = world.getHumans().size();

        int zombieCount = world.getZombies().size();

        long nextWaveSeconds = getNextWaveSeconds();

        g.setColor(Color.BLACK);

        /* Compact status text so it fits * between Safe Point and Hospital. */
        g.setFont(new Font("Arial", Font.BOLD, 14));

        String humanText = "Humans: " + humanCount;

        String zombieText = "Zombies: " + zombieCount;

        String waveText = "Wave: " + waveNumber;

        String nextWaveText = "Next wave: " + nextWaveSeconds + "s";

        FontMetrics fm = g.getFontMetrics();

        int gap = 22;

        int totalWidth = fm.stringWidth(humanText) +
            fm.stringWidth(zombieText) +
            fm.stringWidth(waveText) +
            fm.stringWidth(nextWaveText) +
            gap * 3;

        /* Centre the complete status display. */
        int startX = (getWidth() - totalWidth) / 2;

        int textY = 28;

        int currentX = startX;

        g.drawString(humanText, currentX, textY);

        currentX += fm.stringWidth(humanText) + gap;

        g.drawString(zombieText, currentX, textY);

        currentX += fm.stringWidth(zombieText) + gap;

        g.drawString(waveText, currentX, textY);

        currentX += fm.stringWidth(waveText) + gap;

        g.drawString(nextWaveText, currentX, textY);
    }

    /* MAIN */
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

                try {

                    JFrame frame = new JFrame("Zombie Survival Simulation");

                    Test simulation = new Test();

                    /* Main JFrame layout */
                    frame.setLayout(new BorderLayout());

                    /* Simulation in centre */
                    frame.add(simulation, BorderLayout.CENTER);

                    /* Fast Forward controls * at the bottom. */
                    frame.add(simulation.getFastForward(), BorderLayout.SOUTH);

                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    /* Use preferred JPanel size. */
                    frame.pack();

                    frame.setLocationRelativeTo(null);

                    /* Window must become visible * before simulation begins. */
                    frame.setVisible(true);

                    simulation.startSimulation();

                } catch (RuntimeException exception) {

                    /* Show readable startup error. */
                    JOptionPane.showMessageDialog(null, "The simulation could not start:\n" + exception.getMessage(), "Simulation Error", JOptionPane.ERROR_MESSAGE);

                    exception.printStackTrace();
                }
        });
    }
}
