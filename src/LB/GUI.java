package LB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GUI extends Thread {

    private TWatcherContact watcherContact;
    private final BlockingQueue<Object[]> updates = new LinkedBlockingQueue<>();

    private static String TITLE = "Load balancer";
    private static final int WINDOW_WIDTH = 432;
    private static final int WINDOW_HEIGHT = 256;
    private static final int TABLE_WIDTH = WINDOW_WIDTH - 101;
    private static final int TABLE_HEIGHT = WINDOW_HEIGHT - 32;

    private final JFrame frame;
    private JPanel mainPanel;
    private JTable requestTable;
    private JScrollPane requestTableScrollPane;
    private JPanel cardPanel;
    private JSpinner selfPortSpinner;
    private JSpinner monitorPortSpinner;
    private JButton continueButton;
    private JPanel portsPanel;
    private JPanel serverCountPanel;
    private JLabel availableServerCount;
    private JLabel fullServerCount;
    private DefaultTableModel requestTableModel;

    public GUI() {
        $$$setupUI$$$();
        frame = new JFrame(TITLE);
        frame.setContentPane(this.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        frame.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        frame.pack();

        portsPanel.setPreferredSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));
        portsPanel.setMaximumSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));

        requestTableScrollPane.setMinimumSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));
        requestTableScrollPane.setPreferredSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));

        serverCountPanel.setMinimumSize(new Dimension(WINDOW_WIDTH - TABLE_WIDTH - 30, TABLE_HEIGHT));
        serverCountPanel.setBorder(new EmptyBorder(0, 8, 4, 0));
        availableServerCount.setBorder(new EmptyBorder(8, 4, 0, 0));
        fullServerCount.setBorder(new EmptyBorder(8, 4, 0, 0));

        for (JSpinner spinner : new JSpinner[]{
                selfPortSpinner,
                monitorPortSpinner
        }) {
            ((DefaultFormatter) ((JFormattedTextField) spinner.getEditor().getComponent(0)).getFormatter()).setCommitsOnValidEdit(true);
            spinner.addChangeListener(e -> {
                int port = (int) spinner.getValue();
                if (port > 65535) {
                    spinner.setValue(65535);
                } else if (port < 1) {
                    spinner.setValue(1);
                }
            });
        }
        selfPortSpinner.setValue(1);
        monitorPortSpinner.setValue(8000);

        continueButton.addActionListener(e -> {
            if (watcherContact == null) {
                // Self port is actually just an ID, made it negative so it never conflicts with ports
                watcherContact = new TWatcherContact((int) monitorPortSpinner.getValue(), ((int) selfPortSpinner.getValue()) * -1, this);
                watcherContact.start();
            }
        });
    }

    public void run() {
        frame.setVisible(true);
        Object[] update;
        try {
            while (true) {
                update = updates.take();
                switch ((String) update[0]) {
                    case "ADD_REQUEST":
                        requestTableModel.insertRow(0, Arrays.copyOfRange(update, 1, update.length));
                        break;
                    case "REMOVE_REQUEST":
                        for (int i = 0; i < requestTableModel.getRowCount(); i++) {
                            if (requestTableModel.getValueAt(i, 0).equals(update[1])) {
                                requestTableModel.removeRow(i);
                                break;
                            }
                        }
                        break;
                    case "SERVER_COUNTS":
                        availableServerCount.setText("x" + ((int) update[1]));
                        fullServerCount.setText("x" + ((int) update[2]));
                        break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addRequest(int requestId, int clientId, int iterations, int deadline) {
        try {
            updates.put(new Object[]{
                    "ADD_REQUEST",
                    requestId,
                    clientId,
                    iterations,
                    deadline
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void removeRequest(int requestId) {
        try {
            updates.put(new Object[]{
                    "REMOVE_REQUEST",
                    requestId
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setServerCounts(int available, int full) {
        try {
            updates.put(new Object[]{
                    "SERVER_COUNTS",
                    available,
                    full
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setMonitorPortValidity(boolean valid) {
        if (!valid) {
            watcherContact = null;
            JOptionPane.showMessageDialog(null, "Connection to monitor port failed.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            TITLE += " (" + selfPortSpinner.getValue() + ")";
            frame.setTitle(TITLE);
            ((CardLayout) cardPanel.getLayout()).next(cardPanel);
        }
    }

    public void setSelfMain() {
        frame.setTitle(TITLE + " (Main)");
    }

    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void createUIComponents() {
        requestTable = new JTable();
        requestTableModel = new DefaultTableModel(new String[]{"Request", "Client", "Iterations", "Deadline"}, 0) {
            @Override
            public Class getColumnClass(int column) {
                return Integer.class;
            }
        };
        requestTable.setModel(requestTableModel);
        requestTable.getTableHeader().setReorderingAllowed(false);

        TableColumnModel columnModel = requestTable.getColumnModel();
        for (int i = 0; i < requestTable.getColumnCount(); i++)
            columnModel.getColumn(i).setMinWidth(TABLE_WIDTH / 4);
    }

    public static void setGUILook(String[] wantedLooks) {
        UIManager.LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels();
        String chosenLook = null;
        for (String wantedLook : wantedLooks) {
            if (chosenLook == null)
                for (UIManager.LookAndFeelInfo look : looks)
                    if (wantedLook.equals(look.getName())) {
                        chosenLook = look.getClassName();
                        break;
                    }
        }
        if (chosenLook == null)
            chosenLook = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(chosenLook);
            JFrame.setDefaultLookAndFeelDecorated(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout(0, 0));
        mainPanel.add(cardPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        cardPanel.add(panel1, "Card2");
        portsPanel = new JPanel();
        portsPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(9, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(portsPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Internal ID:");
        portsPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selfPortSpinner = new JSpinner();
        selfPortSpinner.setAlignmentX(0.5f);
        portsPanel.add(selfPortSpinner, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Monitor port:");
        portsPanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monitorPortSpinner = new JSpinner();
        portsPanel.add(monitorPortSpinner, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        continueButton = new JButton();
        continueButton.setText("Continue");
        portsPanel.add(continueButton, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        portsPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        portsPanel.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        portsPanel.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer4 = new com.intellij.uiDesigner.core.Spacer();
        portsPanel.add(spacer4, new com.intellij.uiDesigner.core.GridConstraints(8, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer5 = new com.intellij.uiDesigner.core.Spacer();
        portsPanel.add(spacer5, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer6 = new com.intellij.uiDesigner.core.Spacer();
        portsPanel.add(spacer6, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        cardPanel.add(panel2, "Card1");
        requestTableScrollPane = new JScrollPane();
        requestTableScrollPane.setHorizontalScrollBarPolicy(30);
        requestTableScrollPane.setVerticalScrollBarPolicy(22);
        panel2.add(requestTableScrollPane, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        requestTable.setAutoResizeMode(0);
        requestTable.setEnabled(false);
        requestTable.setFillsViewportHeight(false);
        requestTable.setRowSelectionAllowed(false);
        requestTableScrollPane.setViewportView(requestTable);
        serverCountPanel = new JPanel();
        serverCountPanel.setLayout(new GridBagLayout());
        panel2.add(serverCountPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Servers:");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.WEST;
        serverCountPanel.add(label3, gbc);
        final JLabel label4 = new JLabel();
        label4.setForeground(new Color(-16219811));
        label4.setText("⬤");
        label4.setToolTipText("Available");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        serverCountPanel.add(label4, gbc);
        final JLabel label5 = new JLabel();
        label5.setForeground(new Color(-1607424));
        label5.setText("⬤");
        label5.setToolTipText("Full");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        serverCountPanel.add(label5, gbc);
        fullServerCount = new JLabel();
        fullServerCount.setText("x0");
        fullServerCount.setToolTipText("Full");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        serverCountPanel.add(fullServerCount, gbc);
        availableServerCount = new JLabel();
        availableServerCount.setText("x0");
        availableServerCount.setToolTipText("Available");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        serverCountPanel.add(availableServerCount, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
