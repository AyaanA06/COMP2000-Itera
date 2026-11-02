package itera.checks;

import itera.model.human.Civilian;
import itera.model.zombie.MutantBoss;
import itera.model.zombie.Zombie;
import itera.simulation.World;
import itera.ui.TacticalMiniMap;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.*;

/** Headless regression checks for the live tactical radar and navigation. */
public final class TacticalMiniMapChecks {
    private static int checks;

    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }

    private static boolean hasText(Container container, String snippet) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && label.getText().contains(snippet))
                return true;
            if (component instanceof Container child && hasText(child, snippet))
                return true;
        }
        return false;
    }

    private static JComponent findRadar(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComponent jc &&
                    "Click or drag to move the battlefield view".equals(jc.getToolTipText()))
                return jc;
            if (component instanceof Container child) {
                JComponent match = findRadar(child);
                if (match != null) return match;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        check(TacticalMiniMap.clampedViewPosition(0, 0, new Dimension(900, 600),
            1200, 800).equals(new Point(0, 0)), "Top-left view is clamped");
        check(TacticalMiniMap.clampedViewPosition(600, 400, new Dimension(900, 600),
            1200, 800).equals(new Point(150, 100)), "Click centers visible world");
        check(TacticalMiniMap.clampedViewPosition(1200, 800, new Dimension(900, 600),
            1200, 800).equals(new Point(300, 200)), "Bottom-right view is clamped");
        check(TacticalMiniMap.clampedViewPosition(100, 100, new Dimension(1400, 900),
            1200, 800).equals(new Point(0, 0)), "Oversized viewport does not move");

        SwingUtilities.invokeAndWait(() -> {
            World world = new World();
            world.addCharacter(new Civilian(100, 100));
            world.addCharacter(new Zombie(200, 200));
            JPanel battlefield = new JPanel();
            battlefield.setPreferredSize(new Dimension(1200, 800));
            JScrollPane scroll = new JScrollPane(battlefield);
            scroll.getViewport().setViewSize(new Dimension(1200, 800));
            scroll.getViewport().setExtentSize(new Dimension(900, 600));

            TacticalMiniMap map = new TacticalMiniMap(world, scroll, 1200, 800,
                new Rectangle(0, 0, 180, 180), List.of(new Rectangle(1020, 0, 180, 180)));
            check(map.isExpanded(), "Radar starts expanded");
            check(map.getPreferredSize().width == 256, "Expanded radar width");
            check(hasText(map, "SURVIVORS 1"), "Initial survivor counter");
            check(hasText(map, "HOSTILES 1"), "Initial hostile counter");

            map.setExpanded(false);
            check(!map.isExpanded() && map.getPreferredSize().width == 48,
                "Collapsed radar frees battlefield space");
            map.setExpanded(true);
            check(map.isExpanded() && map.getPreferredSize().width == 256,
                "Radar re-expands");

            world.addCharacter(new MutantBoss(800, 500));
            map.refresh();
            check(hasText(map, "HOSTILES 2"), "Radar refreshes after boss spawning");

            map.setSize(256, 600);
            map.doLayout();
            JComponent radar = findRadar(map);
            check(radar != null, "Interactive radar canvas exists");
            radar.setSize(230, 176);
            BufferedImage frame = new BufferedImage(256, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = frame.createGraphics();
            map.paint(g);
            g.dispose();
            check(true, "Radar paints without a display");

            radar.dispatchEvent(new MouseEvent(radar, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 217, 160, 1, false));
            check(scroll.getViewport().getViewPosition().equals(new Point(300, 200)),
                "Click near lower right navigates and clamps viewport");
        });
        System.out.println("Passed " + checks + " tactical radar checks.");
    }
}
