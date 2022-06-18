package Server;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class responsible for handling the graphical interface
 */
public class GUI extends Thread {

    private TServerDispatcher serverDispatcher;
    private final BlockingQueue<Object[]> updates = new LinkedBlockingQueue<>();

    private static final String TITLE = "Server";
    private static final int WINDOW_WIDTH = 512;
    private static final int WINDOW_HEIGHT = 256;
    private static final int TABLE_WIDTH = WINDOW_WIDTH - 16;
    private static final int TABLE_HEIGHT = WINDOW_HEIGHT - 32;
    private int PICellWidth = (TABLE_WIDTH / 6) - 3;

    private final JFrame frame;
    private JPanel mainPanel;
    private JTable requestTable;
    private JScrollPane requestTableScrollPane;
    private JPanel cardPanel;
    private JSpinner selfPortSpinner;
    private JSpinner monitorPortSpinner;
    private JButton continueButton;
    private JPanel portsPanel;
    private DefaultTableModel requestTableModel;

    /**
     * Constructor for GUI class<br>
     * Initializes the frame and variables<br>
     * Also modifies swing components
     */
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
        selfPortSpinner.setValue(8100);
        monitorPortSpinner.setValue(8000);

        continueButton.addActionListener(e -> {
            if (selfPortSpinner.getValue().equals(monitorPortSpinner.getValue())) {
                JOptionPane.showMessageDialog(null, "Ports can't be the same.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (serverDispatcher == null)
                serverDispatcher = new TServerDispatcher((int) selfPortSpinner.getValue(), (int) monitorPortSpinner.getValue(), this);
        });
    }

    /**
     * Makes the GUI start listening for update requests and updates the UI
     */
    public void run() {
        frame.setVisible(true);
        Object[] update;
        try {
            while (true) {
                update = updates.take();
                boolean newRequest = true;
                for (int i = 0; i < requestTableModel.getRowCount(); i++) {
                    if (requestTableModel.getValueAt(i, 0).equals(update[0])) {
                        for (int col = 0; col < 6; col++)
                            requestTableModel.setValueAt(update[col], i, col);
                        newRequest = false;
                        break;
                    }
                }
                if (newRequest)
                    requestTableModel.insertRow(0, update);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates a request on the request table<br>
     * Can also add a request
     * @param requestId ID of the request
     * @param clientId ID of the client that sent the request
     * @param iterations number of iterations/precision of PI
     * @param deadline arbitrary value that defines request priority
     * @param status status of the request
     * @param PI result of the request
     */
    public void updateRequest(int requestId, int clientId, int iterations, int deadline, String status, String PI) {
        try {
            updates.put(new Object[]{
                    requestId,
                    clientId,
                    iterations,
                    deadline,
                    status,
                    PI
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used to make sure the monitor port is valid<br>
     * If so, start the TServerDispatcher thread
     * @param valid if the monitor port is valid
     */
    public void setMonitorPortValidity(boolean valid) {
        if (!valid) {
            serverDispatcher = null;
            JOptionPane.showMessageDialog(null, "Connection to monitor port failed.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            serverDispatcher.start();
        }
    }

    /**
     * Used to make sure the self port is valid<br>
     * If so, go to the main page
     * @param valid if the self port is valid
     */
    public void setSelfPortValidity(boolean valid) {
        if (!valid) {
            serverDispatcher = null;
            JOptionPane.showMessageDialog(null, "Invalid self port.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            frame.setTitle(TITLE + " (" + selfPortSpinner.getValue() + ")");
            ((CardLayout) cardPanel.getLayout()).next(cardPanel);
        }
    }

    /**
     * Used to create a more customizable request table
     */
    private void createUIComponents() {
        requestTable = new JTable() {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);

                // Set minimum width for the PI column
                if (column == 5) {
                    int rendererWidth = component.getPreferredSize().width;
                    TableColumn tableColumn = getColumnModel().getColumn(column);
                    PICellWidth = Math.max(rendererWidth + getIntercellSpacing().width, PICellWidth);
                    tableColumn.setPreferredWidth(PICellWidth);
                }

                // Color rows based on a cell value
                switch (getValueAt(row, 4).toString()) {
                    case "Finished" -> component.setBackground(new Color(205, 233, 165));
                    case "Pending" -> component.setBackground(new Color(253, 223, 155));
                    case "Rejected" -> component.setBackground(new Color(253, 186, 186));
                }

                return component;
            }
        };
        requestTableModel = new DefaultTableModel(new String[]{"Request", "Client", "Iterations", "Deadline", "Status", "PI"}, 0) {
            @Override
            public Class getColumnClass(int column) {
                return switch (column) {
                    case 4, 5 -> String.class;
                    default -> Integer.class;
                };
            }
        };
        requestTable.setModel(requestTableModel);
        requestTable.getTableHeader().setReorderingAllowed(false);

        TableColumnModel columnModel = requestTable.getColumnModel();
        for (int i = 0; i < requestTable.getColumnCount() - 1; i++)
            columnModel.getColumn(i).setMinWidth(TABLE_WIDTH / 6);
        columnModel.getColumn(5).setMinWidth((TABLE_WIDTH / 6) - 3);
    }

    /**
     * Changes the theme of the UI window<br>
     * If computer doesn't have any of the themes provided, the computer's default
     * one will be used
     * @param wantedLooks list of theme names
     */
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
     * !!! IMPORTANT!! !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout(0, 0));
        mainPanel.add(cardPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        cardPanel.add(panel1, "Card2");
        portsPanel = new JPanel();
        portsPanel.setLayout(new GridLayoutManager(9, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(portsPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Self port:");
        portsPanel.add(label1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selfPortSpinner = new JSpinner();
        selfPortSpinner.setAlignmentX(0.5f);
        portsPanel.add(selfPortSpinner, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Monitor port:");
        portsPanel.add(label2, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monitorPortSpinner = new JSpinner();
        portsPanel.add(monitorPortSpinner, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        continueButton = new JButton();
        continueButton.setText("Continue");
        portsPanel.add(continueButton, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        portsPanel.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        portsPanel.add(spacer2, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        portsPanel.add(spacer3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        portsPanel.add(spacer4, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        portsPanel.add(spacer5, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        portsPanel.add(spacer6, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        cardPanel.add(panel2, "Card1");
        requestTableScrollPane = new JScrollPane();
        requestTableScrollPane.setHorizontalScrollBarPolicy(30);
        requestTableScrollPane.setVerticalScrollBarPolicy(22);
        panel2.add(requestTableScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        requestTable.setAutoResizeMode(0);
        requestTable.setEnabled(false);
        requestTable.setFillsViewportHeight(false);
        requestTable.setRowSelectionAllowed(false);
        requestTableScrollPane.setViewportView(requestTable);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT!! !!!
     * DO NOT edit this method OR call it in your code!
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
