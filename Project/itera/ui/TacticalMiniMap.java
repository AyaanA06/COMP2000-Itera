package itera.ui;

import itera.model.Character;
import itera.model.human.Human;
import itera.model.zombie.MutantBoss;
import itera.model.zombie.Zombie;
import itera.simulation.World;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Objects;

/**
 * Collapsible real-time tactical radar for the whole simulation world.
 * This is a read-only view of World; clicking or dragging pans the main viewport.
 * Swing updates and painting are performed on the event-dispatch thread.
 */
@SuppressWarnings("serial")
public final class TacticalMiniMap extends JPanel {

    private static final Color PANEL = new Color(14, 22, 35);
    private static final Color MAP = new Color(20, 32, 49);
    private static final Color BORDER = new Color(45, 67, 88);
    private static final Color MUTED = new Color(153, 171, 189);
    private static final Color CYAN = new Color(73, 217, 234);
    private static final Color HUMAN = new Color(74, 224, 168);
    private static final Color ZOMBIE = new Color(255, 93, 103);
    private static final Color BOSS = new Color(207, 139, 255);

    private static final int EXPANDED_WIDTH = 256;
    private static final int COLLAPSED_WIDTH = 48;

    private final World world;
    private final JScrollPane battlefield;
    private final int worldWidth;
    private final int worldHeight;
    private final Rectangle safeZone;
    private final List<Rectangle> buildings;
    private final RadarCanvas radar = new RadarCanvas();
    private final JPanel contents = new JPanel();
    private final JLabel heading = new JLabel("TACTICAL RADAR");
    private final JLabel live = new JLabel("● LIVE");
    private final JLabel population = new JLabel();
    private final JButton collapse = new JButton("◀");
    private boolean expanded = true;

    public TacticalMiniMap(World world, JScrollPane battlefield,
            int worldWidth, int worldHeight, Rectangle safeZone,
            List<Rectangle> buildings) {

        this.world = Objects.requireNonNull(world, "World is required");
        this.battlefield = Objects.requireNonNull(battlefield, "Battlefield is required");
        this.safeZone = new Rectangle(Objects.requireNonNull(safeZone));
        this.buildings = List.copyOf(Objects.requireNonNull(buildings));
        if (worldWidth <= 0 || worldHeight <= 0) {
            throw new IllegalArgumentException("World dimensions must be positive");
        }
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;

        setLayout(new BorderLayout());
        setBackground(PANEL);
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 500));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));

        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(15, 11, 12, 9));

        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.add(heading, BorderLayout.CENTER);

        collapse.setFont(new Font("SansSerif", Font.BOLD, 14));
        collapse.setFocusable(false);
        collapse.setForeground(CYAN);
        collapse.setBackground(new Color(31, 48, 65));
        collapse.setBorder(BorderFactory.createLineBorder(BORDER));
        collapse.setPreferredSize(new Dimension(28, 27));
        collapse.setToolTipText("Collapse or expand tactical radar");
        collapse.addActionListener(e -> setExpanded(!expanded));
        header.add(collapse, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
        contents.setOpaque(false);
        contents.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        live.setForeground(HUMAN);
        live.setFont(new Font("SansSerif", Font.BOLD, 11));
        live.setAlignmentX(Component.LEFT_ALIGNMENT);
        live.setBorder(BorderFactory.createEmptyBorder(0, 2, 7, 0));
        contents.add(live);

        radar.setAlignmentX(Component.LEFT_ALIGNMENT);
        radar.setPreferredSize(new Dimension(230, 176));
        radar.setMaximumSize(new Dimension(230, 176));
        radar.setMinimumSize(new Dimension(230, 176));
        radar.setToolTipText("Click or drag to move the battlefield view");
        contents.add(radar);

        contents.add(Box.createVerticalStrut(11));
        population.setForeground(Color.WHITE);
        population.setFont(new Font("SansSerif", Font.BOLD, 12));
        population.setAlignmentX(Component.LEFT_ALIGNMENT);
        contents.add(population);
        contents.add(Box.createVerticalStrut(13));

        JPanel legend = new JPanel(new GridLayout(0, 2, 8, 12));
        legend.setOpaque(false);
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        legend.setMaximumSize(new Dimension(230, 80));
        legend.add(legendLabel("● Humans", HUMAN));
        legend.add(legendLabel("● Zombies", ZOMBIE));
        legend.add(legendLabel("● Bosses", BOSS));
        legend.add(legendLabel("▣ Viewport", CYAN));
        contents.add(legend);
        contents.add(Box.createVerticalStrut(14));

        JLabel help = new JLabel("CLICK OR DRAG TO NAVIGATE");
        help.setFont(new Font("SansSerif", Font.BOLD, 9));
        help.setForeground(MUTED);
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        contents.add(help);
        add(contents, BorderLayout.CENTER);

        ChangeListener viewportListener = e -> radar.repaint();
        battlefield.getViewport().addChangeListener(viewportListener);
        refresh();
    }

    private static JLabel legendLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setForeground(color);
        return label;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean value) {
        expanded = value;
        contents.setVisible(value);
        heading.setVisible(value);
        collapse.setText(value ? "◀" : "▶");
        collapse.setToolTipText(value ? "Collapse tactical radar" : "Expand tactical radar");
        setPreferredSize(new Dimension(value ? EXPANDED_WIDTH : COLLAPSED_WIDTH, 500));
        revalidate();
        repaint();
    }

    /** Called after each simulation update, including spawned or eliminated units. */
    public void refresh() {
        long humans = world.getHumans().stream().filter(Human::isAlive).count();
        long zombies = world.getZombies().stream().filter(Zombie::isAlive).count();
        population.setText("SURVIVORS " + humans + "   •   HOSTILES " + zombies);
        radar.repaint();
    }

    /** Center a viewport on a point, keeping its upper-left corner in world bounds. */
    public static Point clampedViewPosition(int worldX, int worldY,
            Dimension viewport, int worldWidth, int worldHeight) {
        Objects.requireNonNull(viewport, "Viewport dimensions are required");
        int maxX = Math.max(0, worldWidth - viewport.width);
        int maxY = Math.max(0, worldHeight - viewport.height);
        int x = Math.max(0, Math.min(maxX, worldX - viewport.width / 2));
        int y = Math.max(0, Math.min(maxY, worldY - viewport.height / 2));
        return new Point(x, y);
    }

    private final class RadarCanvas extends JPanel {
        private Rectangle mapBounds = new Rectangle();

        RadarCanvas() {
            setOpaque(false);
            MouseAdapter navigator = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) { navigate(event); }
                @Override public void mouseDragged(MouseEvent event) { navigate(event); }
            };
            addMouseListener(navigator);
            addMouseMotionListener(navigator);
        }

        private void navigate(MouseEvent event) {
            if (mapBounds.width <= 0 || mapBounds.height <= 0) return;
            double x = (event.getX() - mapBounds.x) / (double) mapBounds.width;
            double y = (event.getY() - mapBounds.y) / (double) mapBounds.height;
            int worldX = (int) Math.round(Math.max(0, Math.min(1, x)) * worldWidth);
            int worldY = (int) Math.round(Math.max(0, Math.min(1, y)) * worldHeight);
            JViewport viewport = battlefield.getViewport();
            viewport.setViewPosition(clampedViewPosition(
                worldX, worldY, viewport.getExtentSize(), worldWidth, worldHeight));
            repaint();
        }

        private int px(double x) {
            return mapBounds.x + (int) Math.round(x / worldWidth * mapBounds.width);
        }

        private int py(double y) {
            return mapBounds.y + (int) Math.round(y / worldHeight * mapBounds.height);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

                int availableWidth = Math.max(1, getWidth() - 8);
                int availableHeight = Math.max(1, getHeight() - 8);
                double scale = Math.min(availableWidth / (double) worldWidth,
                    availableHeight / (double) worldHeight);
                int width = Math.max(1, (int) Math.round(worldWidth * scale));
                int height = Math.max(1, (int) Math.round(worldHeight * scale));
                mapBounds = new Rectangle((getWidth() - width) / 2,
                    (getHeight() - height) / 2, width, height);

                RoundRectangle2D outline = new RoundRectangle2D.Double(
                    mapBounds.x, mapBounds.y, width, height, 10, 10);
                g.setColor(MAP);
                g.fill(outline);
                g.setClip(outline);

                // Thin tactical grid, scaled to the entire world.
                g.setColor(new Color(37, 54, 73));
                g.setStroke(new BasicStroke(1));
                for (int x = 200; x < worldWidth; x += 200)
                    g.drawLine(px(x), mapBounds.y, px(x), mapBounds.y + height);
                for (int y = 200; y < worldHeight; y += 200)
                    g.drawLine(mapBounds.x, py(y), mapBounds.x + width, py(y));

                // Buildings and the protected Safe Zone.
                g.setColor(new Color(84, 103, 124, 155));
                for (Rectangle building : buildings) drawWorldRect(g, building);
                g.setColor(new Color(40, 155, 115, 115));
                drawWorldRect(g, safeZone);
                g.setColor(HUMAN);
                g.setFont(new Font("SansSerif", Font.BOLD, 8));
                g.drawString("SAFE", px(safeZone.x) + 3, py(safeZone.y) + 11);

                // Draw living entities only; bosses are highlighted last.
                for (Character person : world.getCharacters()) {
                    if (!person.isAlive() || person instanceof MutantBoss) continue;
                    g.setColor(person instanceof Human ? HUMAN : ZOMBIE);
                    int r = person instanceof Zombie ? 3 : 2;
                    g.fillOval(px(person.getX() + person.getSize()/2.0) - r,
                        py(person.getY() + person.getSize()/2.0) - r, r * 2, r * 2);
                }

                for (Zombie zombie : world.getZombies()) {
                    if (!zombie.isAlive() || !(zombie instanceof MutantBoss)) continue;
                    int x = px(zombie.getX() + zombie.getSize()/2.0);
                    int y = py(zombie.getY() + zombie.getSize()/2.0);
                    int pulse = 6 + (int) (world.getTime() / 180L % 3);
                    g.setColor(new Color(207, 139, 255, 90));
                    g.fillOval(x - pulse, y - pulse, pulse * 2, pulse * 2);
                    g.setColor(BOSS);
                    g.fillOval(x - 4, y - 4, 8, 8);
                    g.setColor(Color.WHITE);
                    g.drawOval(x - 4, y - 4, 8, 8);
                }

                // Cyan frame shows the region visible in the scrollable world.
                Rectangle view = battlefield.getViewport().getViewRect();
                int vx = px(Math.max(0, view.x));
                int vy = py(Math.max(0, view.y));
                int vw = Math.max(1, (int) Math.round(view.width / (double) worldWidth * width));
                int vh = Math.max(1, (int) Math.round(view.height / (double) worldHeight * height));
                g.setColor(new Color(73, 217, 234, 32));
                g.fillRect(vx, vy, vw, vh);
                g.setColor(CYAN);
                g.setStroke(new BasicStroke(1.6f));
                g.drawRect(vx, vy, vw, vh);
                g.setClip(null);
                g.setColor(BORDER);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(outline);
            } finally {
                g.dispose();
            }
        }

        private void drawWorldRect(Graphics2D g, Rectangle rectangle) {
            int x = px(rectangle.x);
            int y = py(rectangle.y);
            int width = Math.max(2, px(rectangle.x + rectangle.width) - x);
            int height = Math.max(2, py(rectangle.y + rectangle.height) - y);
            g.fillRoundRect(x, y, width, height, 3, 3);
        }
    }
}
