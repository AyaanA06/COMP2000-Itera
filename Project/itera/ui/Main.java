
package itera.ui;

import itera.model.*;
import itera.model.Character;
import itera.model.human.*;
import itera.model.zombie.*;
import itera.simulation.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

@SuppressWarnings({"serial", "this-escape"})
public class Main extends JPanel {

    private static final int WORLD_WIDTH = 1200;
    private static final int WORLD_HEIGHT = 800;

    private static final int BUILDING_SIZE = 180;
    private static final int BOTTOM_BUILDING_OFFSET = 70;

    private static final long WAVE_INTERVAL = 15000;
    private static final int BASE_ZOMBIES_PER_WAVE = 9;
    private static final int ZOMBIES_ADDED_PER_WAVE = 3;

    private final Random random = new Random();

    private final World world;
    private final long survivalStartTime;
    private final SafePoint safePoint;
    private final FastForward fastForward;
    private final JButton pauseButton;
    private final SimulationSettings settings;

    // Live tactical radar beside the scrollable battlefield.
    private TacticalMiniMap tacticalMiniMap;

    private final ArrayList<Building> buildings =
        new ArrayList<>();

    private final ArrayList<Resource> resources =
        new ArrayList<>();

    private Timer timer;
    private boolean paused;

    private long lastWaveTime;
    private int waveNumber = 1;

    // Zombie type leading the current wave, or null for the first and boss waves.
    private ZombieType featuredType;

    private final JLabel populationSummary =
        new JLabel(" ", SwingConstants.CENTER);

    private final JLabel safeZoneThreat =
        new JLabel("Safe Zone: SAFE (0 nearby)", SwingConstants.CENTER);

    private final JLabel zombieTypeSummary =
        new JLabel("Zombie Types: Loading...", SwingConstants.CENTER);

    private void refreshPopulationSummary() {

        String summary = world.populationSnapshot()
            .getRoles()
            .entrySet()
            .stream()
            .map(entry -> String.format(
                Locale.ROOT,
                "%s: %d alive, %d wounded, %.0f%% health",
                entry.getKey(),
                entry.getValue().count(),
                entry.getValue().wounded(),
                entry.getValue().averageHealth()
            ))
            .collect(
                java.util.stream.Collectors.joining("   |   ")
            );

        populationSummary.setText(
            summary.isEmpty()
                ? "No humans remaining"
                : summary
        );

        // Live Safe Zone Threat Alert.
        long nearby = world.countZombiesNearSafePoint(safePoint, 150);

        if (nearby == 0) {
            safeZoneThreat.setText("Safe Zone: SAFE (0 nearby)");
            safeZoneThreat.setForeground(new Color(0, 110, 0));

        } else if (nearby < 3) {
            safeZoneThreat.setText(
                "Safe Zone: WARNING (" + nearby + " nearby)"
            );
            safeZoneThreat.setForeground(new Color(196, 108, 0));

        } else {
            safeZoneThreat.setText(
                "Safe Zone: HIGH THREAT (" + nearby + " nearby)"
            );
            safeZoneThreat.setForeground(new Color(183, 28, 28));
        }

        // Show the current number of living zombies by type.
        Map<String, Long> typeCounts = world.getLivingZombieTypeCounts();

        String breakdown = typeCounts.entrySet()
            .stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(java.util.stream.Collectors.joining("   |   "));

        zombieTypeSummary.setText("Zombie Types  |  " + breakdown);
    }

    public Main() {
        this(createDefaultSettings());
    }

    public Main(SimulationSettings settings) {

        this.settings = settings;

        setPreferredSize(
            new Dimension(WORLD_WIDTH, WORLD_HEIGHT)
        );

        world = new World();
        survivalStartTime = world.getTime();

        // SAFE POINT
        safePoint = new SafePoint(
            0,
            0,
            BUILDING_SIZE,
            BUILDING_SIZE,
            10
        );

        world.addSafePoint(safePoint);

        // BUILDINGS
        Hospital hospital = new Hospital(
            WORLD_WIDTH - BUILDING_SIZE,
            0
        );

        PoliceStation policeStation = new PoliceStation(
            0,
            WORLD_HEIGHT
                - BUILDING_SIZE
                - BOTTOM_BUILDING_OFFSET
        );

        ConvenienceStore convenienceStore =
            new ConvenienceStore(
                WORLD_WIDTH - BUILDING_SIZE,
                WORLD_HEIGHT
                    - BUILDING_SIZE
                    - BOTTOM_BUILDING_OFFSET
            );

        buildings.add(hospital);
        buildings.add(policeStation);
        buildings.add(convenienceStore);

        world.addBuilding(hospital);
        world.addBuilding(policeStation);
        world.addBuilding(convenienceStore);

        // RESOURCES
        for (Building building : buildings) {
            resources.addAll(building.getResources());
        }

        for (Resource resource : resources) {
            world.addResource(resource);
        }

        // STARTING HUMANS
        addStartingHumans();

        // INITIAL ZOMBIES
        int zombieColumns = Math.min(
            10,
            Math.max(
                5,
                (int) Math.ceil(
                    Math.sqrt(settings.getStartingZombies())
                )
            )
        );

        int zombieRows = (int) Math.ceil(
            settings.getStartingZombies()
                / (double) zombieColumns
        );

        for (
            int i = 0;
            i < settings.getStartingZombies();
            i++
        ) {

            int x = WORLD_WIDTH / 2
                - (zombieColumns - 1) * 35
                + (i % zombieColumns) * 70;

            int y = WORLD_HEIGHT / 2
                - (zombieRows - 1) * 35
                + (i / zombieColumns) * 70;

            Zombie zombie = ZombieSpawns.atType(
                i % ZombieSpawns.typeCount(),
                x,
                y
            );

            world.addCharacter(zombie);
        }

        // FAST FORWARD
        fastForward = new FastForward();

        // PAUSE BUTTON
        pauseButton = new JButton("Pause");

        pauseButton.addActionListener(event -> {
            paused = !paused;

            pauseButton.setText(
                paused ? "Resume" : "Pause"
            );

            repaint();
        });

        // SIMULATION TIMER
        timer = new Timer(
            30,
            e -> {

                if (paused) {
                    return;
                }

                try {

                    timer.setDelay(
                        fastForward.getDelay()
                    );

                    updateSimulation();
                    repaint();

                } catch (RuntimeException exception) {

                    timer.stop();

                    JOptionPane.showMessageDialog(
                        this,
                        "The simulation stopped because of an unexpected error:\n"
                            + exception.getMessage(),
                        "Simulation Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        );
    }

    /**
     * Starting population:
     * 30% civilians
     * 40% soldiers
     * 30% medics
     */
    private void addStartingHumans() {

        int civilianCount =
            settings.getStartingHumans() * 3 / 10;

        int medicCount =
            settings.getStartingHumans() * 3 / 10;

        int soldierCount =
            settings.getStartingHumans()
                - civilianCount
                - medicCount;

        for (
            int i = 0;
            i < settings.getStartingHumans();
            i++
        ) {

            Vector2D spawn = randomHumanPosition();

            int x = (int) spawn.getX();
            int y = (int) spawn.getY();

            Human human;

            if (i < civilianCount) {

                human = new Civilian(x, y);

            } else if (
                i < civilianCount + soldierCount
            ) {

                human = new Soldier(x, y);

            } else {

                human = new Medic(x, y);
            }

            world.addCharacter(human);
        }
    }

    private static SimulationSettings createDefaultSettings() {

        return new SimulationSettings(
            SimulationSettings.DEFAULT_HUMANS,
            SimulationSettings.DEFAULT_ZOMBIES
        );
    }

    /**
     * Find a valid position for a human.
     */
    private Vector2D randomHumanPosition() {

        int x;
        int y;
        boolean invalid;

        do {

            x = random.nextInt(
                WORLD_WIDTH - 100
            ) + 40;

            y = random.nextInt(
                WORLD_HEIGHT - 120
            ) + 40;

            invalid = safePoint.contains(
                x,
                y,
                15
            );

            for (Building building : buildings) {

                if (building.overlaps(x, y, 15)) {

                    invalid = true;
                    break;
                }
            }

        } while (invalid);

        return new Vector2D(x, y);
    }

    /**
     * Create new zombie waves.
     * Every fifth wave contains a Mutant Boss.
     */
    private void updateTimedWaves() {

        long now = world.getTime();

        if (
            now - lastWaveTime < WAVE_INTERVAL
        ) {
            return;
        }

        waveNumber++;

        boolean bossWave =
            WaveRotation.isBossWave(waveNumber);

        ZombieWave wave;

        if (bossWave) {

            wave = new ZombieWave(
                1,
                true
            );

        } else {

            int zombiesThisWave =
                BASE_ZOMBIES_PER_WAVE
                    + (waveNumber - 1)
                    * ZOMBIES_ADDED_PER_WAVE;

            // Each normal wave is led by a different zombie type.
            wave = ZombieWave.featuring(
                zombiesThisWave,
                WaveRotation.featuredFor(waveNumber).orElseThrow(),
                random
            );
        }

        featuredType = wave.getFeaturedType().orElse(null);

        // SPAWN POINTS
        wave.addSpawnPoint(
            new Vector2D(300, 30)
        );

        wave.addSpawnPoint(
            new Vector2D(
                WORLD_WIDTH - 300,
                30
            )
        );

        wave.addSpawnPoint(
            new Vector2D(
                220,
                WORLD_HEIGHT / 2
            )
        );

        wave.addSpawnPoint(
            new Vector2D(
                WORLD_WIDTH - 220,
                WORLD_HEIGHT / 2
            )
        );

        world.addWave(wave);
        world.spawnWave(wave);

        addHumanReinforcements();

        lastWaveTime = now;
    }

    /**
     * Add two medics, two civilians,
     * and two soldiers each wave.
     */
    private void addHumanReinforcements() {

        for (int role = 0; role < 6; role++) {

            Vector2D spawn = randomHumanPosition();

            int x = (int) spawn.getX();
            int y = (int) spawn.getY();

            Human human = switch (role % 3) {

                case 0 -> new Medic(x, y);
                case 1 -> new Civilian(x, y);
                default -> new Soldier(x, y);
            };

            world.addCharacter(human);
        }
    }

    /**
     * Update the entire simulation.
     */
    private void updateSimulation() {

        if (
            getWidth() <= 0
                || getHeight() <= 0
        ) {
            return;
        }

        // ADVANCE SIMULATION TIME
        world.advanceTime(30);

        // SPAWN TIMED WAVES
        updateTimedWaves();

        // MUTANT BOSS SPECIAL ABILITIES
        // Ground Smash and Zombie Summoning
        world.updateBossAbilities();

        long now = world.getTime();

        // RESOURCE RESPAWNING
        for (Resource resource : resources) {
            resource.updateRespawn(now);
        }

        var humans = world.getHumans();
        var zombies = world.getZombies();

        // UPDATE HUMANS
        for (Human human : humans) {

            human.setEnvironment(
                buildings,
                humans
            );

            human.update(
                getWidth(),
                getHeight(),
                zombies,
                safePoint
            );

            // BUILDING INTERACTION
            for (Building building : buildings) {

                if (
                    building.contains(
                        human.getX(),
                        human.getY()
                    )
                ) {

                    building.interact(human);
                }
            }
        }

        // COLLECT DEAD HUMANS
        ArrayList<Human> convertedHumans =
            new ArrayList<>();

        // UPDATE ZOMBIES
        for (Zombie zombie : zombies) {

            zombie.setBuildings(buildings);

            // BLOATER EXPLOSION
            if (
                zombie instanceof Bloater bloater
                    && bloater.shouldExplode(humans)
            ) {

                ArrayList<Human> explosionDeaths =
                    bloater.explode(humans);

                for (Human human : explosionDeaths) {

                    if (
                        !convertedHumans.contains(human)
                    ) {

                        convertedHumans.add(human);
                    }
                }

                continue;
            }

            // NORMAL ZOMBIE AND BOSS BEHAVIOUR
            Human deadHuman = zombie.update(
                getWidth(),
                getHeight(),
                humans,
                safePoint
            );

            if (
                deadHuman != null
                    && !convertedHumans.contains(deadHuman)
            ) {

                convertedHumans.add(deadHuman);
            }
        }

        // CONVERT DEAD HUMANS TO ZOMBIES
        for (Human human : convertedHumans) {

            Zombie newZombie = ZombieSpawns.random(
                random,
                human.getX(),
                human.getY()
            );

            world.addCharacter(newZombie);
        }

        // REMOVE DEAD CHARACTERS
        world.update();

        // REFRESH POPULATION INFORMATION
        refreshPopulationSummary();

        // Refresh the tactical radar after each simulation update.
        if (tacticalMiniMap != null) {
            tacticalMiniMap.refresh();
        }

        // END CONDITION
        if (world.getHumans().isEmpty()) {

            repaint();

            SwingUtilities.invokeLater(
                () -> endSimulation()
            );

            return;
        }
    }

    /**
     * End simulation and offer restart.
     */
    private void endSimulation() {

        timer.stop();

        int choice =
            JOptionPane.showConfirmDialog(
                this,
                "Simulation Complete: "
                    + "No Humans Remaining.\n"
                    + "Wave reached: "
                    + waveNumber
                    + "\nTime survived: "
                    + getSurvivalTime()
                    + "\nZombies eliminated: "
                    + world.getTotalZombieEliminations()
                    + "\n\n"
                    + "Would you like to restart "
                    + "the simulation?",
                "Simulation Complete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            );

        if (
            choice == JOptionPane.YES_OPTION
        ) {

            java.awt.Window currentWindow =
                SwingUtilities.getWindowAncestor(this);

            if (currentWindow != null) {
                currentWindow.dispose();
            }

            try {

                createAndStartSimulation();

            } catch (RuntimeException exception) {

                JOptionPane.showMessageDialog(
                    null,
                    "The replacement simulation could not start:\n"
                        + exception.getMessage(),
                    "Simulation Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }

        } else {

            System.exit(0);
        }
    }

    /**
     * Start simulation timer.
     */
    public void startSimulation() {

        lastWaveTime = world.getTime();

        timer.start();
    }

    public FastForward getFastForward() {
        return fastForward;
    }

    /**
     * Controls and legend below the game.
     */
    public JPanel getSimulationControls() {

        JPanel controls =
            new JPanel(new BorderLayout());

        JPanel legend =
            new JPanel(
                new FlowLayout(
                    FlowLayout.CENTER,
                    10,
                    3
                )
            );

        legend.add(new JLabel("Legend:"));

        addLegendItem(
            legend,
            "C Civilian",
            new Color(0, 137, 123)
        );

        addLegendItem(
            legend,
            "S Soldier",
            new Color(0, 137, 123)
        );

        addLegendItem(
            legend,
            "M Medic",
            new Color(0, 137, 123)
        );

        addLegendItem(
            legend,
            "Z Standard zombie",
            new Color(211, 47, 47)
        );

        addLegendItem(
            legend,
            "R Runner",
            new Color(245, 124, 0)
        );

        addLegendItem(
            legend,
            "S Stalker",
            new Color(123, 31, 162)
        );

        addLegendItem(
            legend,
            "B Bloater",
            new Color(84, 110, 122)
        );

        addLegendItem(
            legend,
            "BR Brute",
            new Color(78, 52, 46)
        );

        addLegendItem(
            legend,
            "SP Spitter",
            new Color(104, 159, 56)
        );

        addLegendItem(
            legend,
            "BOSS Mutant boss",
            new Color(74, 20, 140)
        );

        addLegendItem(
            legend,
            "Safe Zone",
            new Color(0, 110, 0)
        );

        JPanel buttons =
            new JPanel(
                new FlowLayout(
                    FlowLayout.CENTER
                )
            );

        buttons.add(fastForward);
        buttons.add(pauseButton);

        JComboBox<String> treatment =
            new JComboBox<>(
                new String[] {
                    "Soldiers first",
                    "Most injured first"
                }
            );

        treatment.setToolTipText(
            "Choose who medics help first; "
                + "critical self-healing still takes priority."
        );

        treatment.addActionListener(event -> {

            world.setTreatmentStrategy(
                treatment.getSelectedIndex() == 0
                    ? new itera.model.human.treatment.SoldiersFirstStrategy()
                    : new itera.model.human.treatment.MostInjuredStrategy()
            );
        });

        buttons.add(
            new JLabel("Medic priority:")
        );

        buttons.add(treatment);

        refreshPopulationSummary();

        controls.add(
            legend,
            BorderLayout.NORTH
        );

        // Display population, Safe Zone threat, and zombie types.
        JPanel liveStatus = new JPanel(new GridLayout(3, 1));

        liveStatus.add(populationSummary);
        liveStatus.add(safeZoneThreat);
        liveStatus.add(zombieTypeSummary);

        controls.add(
            liveStatus,
            BorderLayout.CENTER
        );

        controls.add(
            buttons,
            BorderLayout.SOUTH
        );

        return controls;
    }

    private void addLegendItem(
            JPanel legend,
            String text,
            Color color) {

        JLabel item = new JLabel(text);

        item.setForeground(color);

        legend.add(item);
    }

    /** Elapsed simulation time, including fast-forward but excluding pauses. */
    private String getSurvivalTime() {
        long elapsedSeconds = Math.max(
            0L,
            (world.getTime() - survivalStartTime) / 1000
        );

        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;

        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    /**
     * Warns when the next wave is a boss wave and is at most
     * five simulation seconds away.
     */
    private static boolean isBossWaveIncoming(
            int currentWave, long secondsUntilNextWave) {
        return currentWave >= 1
            && (currentWave + 1) % 5 == 0
            && secondsUntilNextWave > 0
            && secondsUntilNextWave <= 5;
    }

    /** HUD wave text, for example "Wave: 3 (Stalker wave)". */
    private static String waveLabel(int waveNumber, ZombieType featured) {
        return featured == null
            ? "Wave: " + waveNumber
            : "Wave: " + waveNumber + " (" + featured.getLabel() + " wave)";
    }

    /** Time remaining until the next wave. */
    private long getNextWaveSeconds() {

        if (lastWaveTime == 0) {
            return WAVE_INTERVAL / 1000;
        }

        long elapsed =
            world.getTime() - lastWaveTime;

        long remaining =
            Math.max(
                0,
                WAVE_INTERVAL - elapsed
            );

        return (remaining + 999) / 1000;
    }

    /**
     * Draw the entire simulation.
     */
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        // BACKGROUND
        g.setColor(
            new Color(220, 220, 220)
        );

        g.fillRect(
            0,
            0,
            getWidth(),
            getHeight()
        );

        // BUILDINGS
        for (Building building : buildings) {
            building.draw(g);
        }

        // RESOURCES
        for (Resource resource : resources) {
            resource.draw(g);
        }

        // SAFE POINT
        safePoint.draw(g);

        // CHARACTERS
        for (Character character : world.getCharacters()) {
            character.draw(g);
        }

        // STATUS INFORMATION
        int humanCount =
            world.getHumans().size();

        int zombieCount =
            world.getZombies().size();

        long nextWaveSeconds =
            getNextWaveSeconds();

        g.setColor(Color.BLACK);

        g.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                14
            )
        );

        String humanText =
            "Humans: " + humanCount;

        String zombieText =
            "Zombies: " + zombieCount;

        String killsText =
            "Eliminated: "
                + world.getTotalZombieEliminations();

        String survivalText =
            "Survived: " + getSurvivalTime();

        String waveText =
            waveLabel(waveNumber, featuredType);

        String nextWaveText =
            "Next wave: "
                + nextWaveSeconds
                + "s";

        FontMetrics fm =
            g.getFontMetrics();

        int gap = 22;

        int totalWidth =
            fm.stringWidth(humanText)
                + fm.stringWidth(zombieText)
                + fm.stringWidth(killsText)
                + fm.stringWidth(survivalText)
                + fm.stringWidth(waveText)
                + fm.stringWidth(nextWaveText)
                + gap * 5;

        int currentX =
            (getWidth() - totalWidth) / 2;

        int textY = 28;

        g.drawString(
            humanText,
            currentX,
            textY
        );

        currentX +=
            fm.stringWidth(humanText) + gap;

        g.drawString(
            zombieText,
            currentX,
            textY
        );

        currentX +=
            fm.stringWidth(zombieText) + gap;

        g.drawString(
            killsText,
            currentX,
            textY
        );

        currentX +=
            fm.stringWidth(killsText) + gap;

        g.drawString(
            survivalText,
            currentX,
            textY
        );

        currentX +=
            fm.stringWidth(survivalText) + gap;

        g.drawString(
            waveText,
            currentX,
            textY
        );

        currentX +=
            fm.stringWidth(waveText) + gap;

        g.drawString(
            nextWaveText,
            currentX,
            textY
        );

        // PAUSE OVERLAY
        if (paused) {

            g.setColor(
                new Color(0, 0, 0, 150)
            );

            g.fillRect(
                0,
                0,
                getWidth(),
                getHeight()
            );

            g.setColor(Color.WHITE);

            g.setFont(
                new Font(
                    "Arial",
                    Font.BOLD,
                    30
                )
            );

            String pausedText = "PAUSED";

            int pausedX =
                (
                    getWidth()
                        - g.getFontMetrics()
                        .stringWidth(pausedText)
                ) / 2;

            g.drawString(
                pausedText,
                pausedX,
                getHeight() / 2
            );
        }

        // Early warning before the next boss wave.
        if (isBossWaveIncoming(waveNumber, nextWaveSeconds)) {
            g.setColor(new Color(183, 28, 28));
            g.setFont(new Font("Arial", Font.BOLD, 24));

            String warningText =
                "BOSS INCOMING - WAVE " + (waveNumber + 1)
                    + " IN " + nextWaveSeconds + "s";

            FontMetrics warningMetrics = g.getFontMetrics();
            int warningX = (getWidth()
                - warningMetrics.stringWidth(warningText)) / 2;
            g.drawString(warningText, warningX, 65);
        }

        // BOSS WAVE WARNING
        if (waveNumber % 5 == 0) {

            g.setColor(
                new Color(120, 0, 120)
            );

            g.setFont(
                new Font(
                    "Arial",
                    Font.BOLD,
                    26
                )
            );

            String bossText =
                "BOSS WAVE";

            FontMetrics bossFm =
                g.getFontMetrics();

            int bossX =
                (
                    getWidth()
                        - bossFm.stringWidth(bossText)
                ) / 2;

            g.drawString(
                bossText,
                bossX,
                65
            );
        }
    }

    /**
     * Create a new simulation window.
     */
    private static void createAndStartSimulation() {

        SimulationSettings settings =
            SimulationSettingsDialog.showDialog(null);

        if (settings == null) {
            return;
        }

        JFrame frame =
            new JFrame(
                "Zombie Survival Simulation"
            );

        Main simulation =
            new Main(settings);

        frame.setLayout(
            new BorderLayout()
        );

        // SCROLLABLE GAME AREA
        // This keeps the Police Station fully accessible.
        JScrollPane scrollPane =
            new JScrollPane(simulation);

        // Leave room on the right for the tactical mini-map.
        scrollPane.setPreferredSize(
            new Dimension(900, 600)
        );

        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        scrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        frame.add(
            scrollPane,
            BorderLayout.CENTER
        );

        // Interactive tactical radar:
        // stays visible while scrolling the battlefield.
        simulation.tacticalMiniMap = new TacticalMiniMap(
            simulation.world,
            scrollPane,
            WORLD_WIDTH,
            WORLD_HEIGHT,
            new Rectangle(
                0,
                0,
                BUILDING_SIZE,
                BUILDING_SIZE
            ),
            java.util.List.of(
                new Rectangle(
                    WORLD_WIDTH - BUILDING_SIZE,
                    0,
                    BUILDING_SIZE,
                    BUILDING_SIZE
                ),
                new Rectangle(
                    0,
                    WORLD_HEIGHT
                        - BUILDING_SIZE
                        - BOTTOM_BUILDING_OFFSET,
                    BUILDING_SIZE,
                    BUILDING_SIZE
                ),
                new Rectangle(
                    WORLD_WIDTH - BUILDING_SIZE,
                    WORLD_HEIGHT
                        - BUILDING_SIZE
                        - BOTTOM_BUILDING_OFFSET,
                    BUILDING_SIZE,
                    BUILDING_SIZE
                )
            )
        );

        frame.add(
            simulation.tacticalMiniMap,
            BorderLayout.EAST
        );

        // CONTROLS BELOW GAME
        frame.add(
            simulation.getSimulationControls(),
            BorderLayout.SOUTH
        );

        frame.setDefaultCloseOperation(
            JFrame.EXIT_ON_CLOSE
        );

        frame.pack();

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);

        simulation.startSimulation();
    }

    /**
     * Program entry point.
     */
    public static void main(String[] args) {

        SwingUtilities.invokeLater(
            () -> {

                try {

                    createAndStartSimulation();

                } catch (RuntimeException exception) {

                    JOptionPane.showMessageDialog(
                        null,
                        "The simulation could not start:\n"
                            + exception.getMessage(),
                        "Simulation Error",
                        JOptionPane.ERROR_MESSAGE
                    );

                    exception.printStackTrace();
                }
            }
        );
    }
}
