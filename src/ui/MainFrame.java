package ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    

    private JTabbedPane tabbedPane;

    public MainFrame() {
        

        setTitle("Query Optimization Visualizer");

        setSize(1100, 700);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();
    }

    private void initComponents() {

        tabbedPane = new JTabbedPane();

        SimulationPanel simulationPanel = new SimulationPanel();

        ExperimentPanel experimentPanel = new ExperimentPanel();


        tabbedPane.addTab(
                "Mo phong ly thuyet",
                simulationPanel
        );

        tabbedPane.addTab(
                "Thuc nghiem SQL Server",
                experimentPanel
        );

        add(tabbedPane);
    }
}