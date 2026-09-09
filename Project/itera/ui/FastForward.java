package itera.ui;

import java.awt.*;
import javax.swing.*;

public class FastForward extends JPanel {

    private int speed = 1;

    private JButton normalButton, twoTimesButton;
    private JButton fiveTimesButton;

    public FastForward() {

        setLayout(new FlowLayout());

        JLabel speedLabel = new JLabel("Simulation Speed:");

        normalButton = new JButton("1x");
        twoTimesButton = new JButton("2x");
        fiveTimesButton = new JButton("5x");

        add(speedLabel);
        add(normalButton);
        add(twoTimesButton);
        add(fiveTimesButton);

        normalButton.addActionListener(e -> { speed = 1; });

        twoTimesButton.addActionListener(e -> { speed = 2; });

        fiveTimesButton.addActionListener(e -> { speed = 5; });
    }

    public int getSpeed() {
        return speed;
    }

    public int getDelay() {

        int normalDelay = 30;

        return normalDelay / speed;
    }
}
