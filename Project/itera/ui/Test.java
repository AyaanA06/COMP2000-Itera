package itera.ui;

import itera.model.Bloater;
import itera.model.Building;
import itera.model.Character;
import itera.model.Civilian;
import itera.model.ConvenienceStore;
import itera.model.Food;
import itera.model.Hospital;
import itera.model.Human;
import itera.model.Medic;
import itera.model.Medicine;
import itera.model.PoliceStation;
import itera.model.Resource;
import itera.model.Runner;
import itera.model.SafePoint;
import itera.model.Soldier;
import itera.model.Stalker;
import itera.model.Vector2D;
import itera.model.Weapon;
import itera.model.Zombie;
import itera.simulation.World;
import itera.simulation.ZombieWave;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

@SuppressWarnings({"serial", "this-escape"})
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

    /* Timed zombie waves */
    private static final long WAVE_INTERVAL = 15000;

    private static final int BASE_ZOMBIES_PER_WAVE = 4;
    private static final int ZOMBIES_ADDED_PER_WAVE = 2;

    private long lastWaveTime;
    private int waveNumber = 1;

    public Test() {

        setPreferredSize(new Dimension(WORLD_WIDTH, WORLD_HEIGHT));

        world = new World();

        /* Safe point in the top-left corner */
        safePoint = new SafePoint(0, 0, BUILDING_SIZE, BUILDING_SIZE, 10);

        world.addSafePoint(safePoint);

        /* Buildings */

        /* Hospital in the top-right corner */
        Hospital hospital = new Hospital(WORLD_WIDTH - BUILDING_SIZE, 0);

        /* Police station in the bottom-left corner */
        PoliceStation policeStation = new PoliceStation(0, WORLD_HEIGHT - BUILDING_SIZE - BOTTOM_BUILDING_OFFSET);

        /* Convenience store in the bottom-right corner */
        ConvenienceStore convenienceStore = new ConvenienceStore(WORLD_WIDTH - BUILDING_SIZE, WORLD_HEIGHT - BUILDING_SIZE - BOTTOM_BUILDING_OFFSET);

        buildings.add(hospital);
        buildings.add(policeStation);
        buildings.add(convenienceStore);

        world.addBuilding(hospital);
        world.addBuilding(policeStation);
        world.addBuilding(convenienceStore);

        /* Resources */

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

        for (Resource resource : resources) {

            world.addResource(resource);
        }

        /* Humans */

        /* Civilians */
        for (int i = 0; i < 15; i++) {

            Vector2D spawn = randomHumanPosition();

            world.addCharacter(new Civilian((int) spawn.getX(), (int) spawn.getY()));
        }

        /* Soldiers */
        for (int i = 0; i < 3; i++) {

            Vector2D spawn = randomHumanPosition();

            world.addCharacter(new Soldier((int) spawn.getX(), (int) spawn.getY()));
        }

        /* Medics */
        for (int i = 0; i < 2; i++) {

            Vector2D spawn = randomHumanPosition();

            world.addCharacter(new Medic((int) spawn.getX(), (int) spawn.getY()));
        }

        /* Zombies */

        world.addCharacter(new Zombie(WORLD_WIDTH / 2, WORLD_HEIGHT / 2));

        world.addCharacter(new Runner(WORLD_WIDTH / 2 + 70, WORLD_HEIGHT / 2));

        world.addCharacter(new Stalker(WORLD_WIDTH / 2, WORLD_HEIGHT / 2 + 70));

        world.addCharacter(new Bloater(WORLD_WIDTH / 2 + 70, WORLD_HEIGHT / 2 + 70));

        fastForward = new FastForward();

        timer = new Timer(30, e -> {

            timer.setDelay(fastForward.getDelay());

            updateSimulation();

            repaint();
        });
    }

    private Vector2D randomHumanPosition() {

        int x;
        int y;

        boolean invalid;

        do {

            x = random.nextInt(WORLD_WIDTH - 100) + 40;

            y = random.nextInt(WORLD_HEIGHT - 120) + 40;

            /* Check the safe point. */
            invalid = safePoint.contains(x, y, 15);

            /* Check the other buildings. */
            for (Building building : buildings) {

                if (building.contains(x, y)) {

                    invalid = true;
                    break;
                }
            }

        } while (invalid);

        return new Vector2D(x, y);
    }

    private void updateTimedWaves() {

        long now = System.currentTimeMillis();

        if (now - lastWaveTime < WAVE_INTERVAL) {

            return;
        }

        waveNumber++;

        /* Each new wave contains two more zombies than the previous wave. */
        int zombiesThisWave = BASE_ZOMBIES_PER_WAVE +
            (waveNumber - 1) * ZOMBIES_ADDED_PER_WAVE;

        ZombieWave wave = new ZombieWave(zombiesThisWave);

        /* Spawn points are kept away from the corner buildings. */
        wave.addSpawnPoint(new Vector2D(300, 30));

        wave.addSpawnPoint(new Vector2D(WORLD_WIDTH - 300, 30));

        wave.addSpawnPoint(new Vector2D(220, WORLD_HEIGHT / 2));

        wave.addSpawnPoint(new Vector2D(WORLD_WIDTH - 220, WORLD_HEIGHT / 2));

        world.addWave(wave);

        world.spawnWave(wave);

        lastWaveTime = now;
    }

    private void updateSimulation() {

        /* Do not update until the panel has a valid size. */
        if (getWidth() <= 0 || getHeight() <= 0) {

            return;
        }

        /* Spawn a mixed zombie wave at fixed time intervals. */
        updateTimedWaves();

        var humans = world.getHumans();

        var zombies = world.getZombies();

        for (Human human : humans) {

            human.update(getWidth(), getHeight(), zombies, safePoint);

            /* Collect nearby resources. */
            for (Resource resource : resources) {

                if (!resource.isCollected() && resource.isNear(human)) {

                    human.addResource(resource);

                    resource.collect();
                }
            }

            /* Interact with occupied buildings. */
            for (Building building : buildings) {

                if (building.contains(human.getX(), human.getY())) {

                    building.interact(human);
                }
            }
        }

        ArrayList<Human> convertedHumans = new ArrayList<>();

        for (Zombie zombie : zombies) {

            /* Bloaters explode when a human enters their blast radius. */
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

            /* Prevent the same human from being converted multiple times. */
            if (deadHuman != null && !convertedHumans.contains(deadHuman)) {

                convertedHumans.add(deadHuman);
            }
        }

        /* Convert dead humans into random zombie types. */
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

    public void startSimulation() {

        lastWaveTime = System.currentTimeMillis();

        timer.start();
    }

    public FastForward getFastForward() {

        return fastForward;
    }

    private long getNextWaveSeconds() {

        if (lastWaveTime == 0) {

            return WAVE_INTERVAL / 1000;
        }

        long elapsed = System.currentTimeMillis() - lastWaveTime;

        long remaining = Math.max(0, WAVE_INTERVAL - elapsed);

        return (remaining + 999) / 1000;
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        g.setColor(new Color(220, 220, 220));

        g.fillRect(0, 0, getWidth(), getHeight());

        for (Building building : buildings) {

            building.draw(g);
        }

        for (Resource resource : resources) {

            resource.draw(g);
        }

        safePoint.draw(g);

        for (Character character : world.getCharacters()) {

            character.draw(g);
        }

        /* Draw the simulation status. */
        int humanCount = world.getHumans().size();

        int zombieCount = world.getZombies().size();

        long nextWaveSeconds = getNextWaveSeconds();

        g.setColor(Color.BLACK);

        /* Keep the status text between the safe point and hospital. */
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

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                JFrame frame = new JFrame("Zombie Survival Simulation");

                Test simulation = new Test();

                frame.setLayout(new BorderLayout());

                frame.add(simulation, BorderLayout.CENTER);

                /* Fast-forward controls at the bottom. */
                frame.add(simulation.getFastForward(), BorderLayout.SOUTH);

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                frame.pack();

                frame.setLocationRelativeTo(null);

                /* Show the window before starting the simulation timer. */
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
