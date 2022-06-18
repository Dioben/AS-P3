package Monitor;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class responsible for handling the graphical interface
 */
public class GUI extends Thread {

    private TServerDispatcher serverDispatcher;
    private final BlockingQueue<Object[]> updates = new LinkedBlockingQueue<>();
    private final HashMap<Integer, Object[]> requestTableModels = new HashMap<>();
    private final DefaultListModel<String[]> systemListModel;

    private static final String TITLE = "Monitor";
    private static final int WINDOW_WIDTH = 686;
    private static final int WINDOW_HEIGHT = 256;
    private static final int TABLE_WIDTH = 414;
    private static final int TABLE_HEIGHT = WINDOW_HEIGHT - 32;
    private int PICellWidth = TABLE_WIDTH / 5;

    private final JFrame frame;
    private JPanel mainPanel;
    private JTable requestTable;
    private JScrollPane requestTableScrollPane;
    private JPanel cardPanel;
    private JSpinner selfPortSpinner;
    private JButton continueButton;
    private JPanel portsPanel;
    private JPanel systemPanel;
    private JList<String[]> systemList;
    private JPanel systemCountPanel;
    private JLabel availableSystemCountLabel;
    private JLabel fullSystemCountLabel;
    private JLabel stoppedSystemCountLabel;
    private JSpinner loadBalancerPortSpinner;
    private JLabel availableSystemCountXLabel;
    private JLabel fullSystemCountXLabel;
    private JLabel stoppedSystemCountXLabel;

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

        systemPanel.setMinimumSize(new Dimension(WINDOW_WIDTH - TABLE_WIDTH - 19, TABLE_HEIGHT));
        systemPanel.setPreferredSize(new Dimension(WINDOW_WIDTH - TABLE_WIDTH - 19, TABLE_HEIGHT));

        for (JSpinner spinner : new JSpinner[]{
                selfPortSpinner,
                loadBalancerPortSpinner
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
        selfPortSpinner.setValue(8000);
        loadBalancerPortSpinner.setValue(8001);

        systemListModel = new DefaultListModel<>();
        systemList.setModel(systemListModel);
        systemList.setCellRenderer(new CustomListCellRenderer());

        systemCountPanel.setBorder(new EmptyBorder(0, 0, 9, 0));

        for (JLabel label : new JLabel[]{
                availableSystemCountXLabel,
                fullSystemCountXLabel,
                stoppedSystemCountXLabel
        }) {
            label.setBorder(new EmptyBorder(0, 4, 0, 0));
        }

        continueButton.addActionListener(e -> {
            if (selfPortSpinner.getValue().equals(loadBalancerPortSpinner.getValue())) {
                JOptionPane.showMessageDialog(null, "Ports can't be the same.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (serverDispatcher == null) {
                serverDispatcher = new TServerDispatcher((int) selfPortSpinner.getValue(), (int) loadBalancerPortSpinner.getValue(), this);
                serverDispatcher.start();
            }
        });

        systemList.addListSelectionListener(e -> {
            int index = ((JList<?>) e.getSource()).getMinSelectionIndex();
            if (index < 0)
                return;
            int serverId = Integer.parseInt(systemListModel.get(index)[2]);
            DefaultTableModel tableModel = (DefaultTableModel) requestTableModels.get(serverId)[1];
            requestTable.setModel(tableModel);

            TableColumnModel columnModel = requestTable.getColumnModel();
            for (int i = 0; i < requestTable.getColumnCount(); i++)
                columnModel.getColumn(i).setMinWidth(TABLE_WIDTH / 5);
        });
    }

    /**
     * Makes the GUI start listening for update requests and updates the UI
     */
    public void run() {
        frame.setVisible(true);
        Object[] update;
        DefaultTableModel tableModel;
        int index;
        int serverId;
        int loadBalancerId;
        int requestId;
        boolean newRequest;
        boolean newSystem;
        try {
            while (true) {
                update = updates.take();
                switch ((String) update[0]) {
                    case "ADD_SERVER":
                        serverId = (int) update[1];
                        newSystem = true;
                        for (int i = 0; i < systemListModel.getSize(); i++) {
                            if (serverId == Integer.parseInt(systemListModel.get(i)[2])) {
                                systemListModel.set(i, new String[]{"Server (" + serverId + ")", "Available", Integer.toString(serverId)});
                                newSystem = false;
                                break;
                            }
                        }
                        if (newSystem) {
                            tableModel = new DefaultTableModel(new String[]{"Request", "Client", "Iterations", "Deadline", "Status"}, 0) {
                                @Override
                                public Class getColumnClass(int column) {
                                    return switch (column) {
                                        case 4 -> String.class;
                                        default -> Integer.class;
                                    };
                                }
                            };
                            index = systemListModel.getSize();
                            systemListModel.add(index, new String[]{"Server (" + serverId + ")", "Available", Integer.toString(serverId)});
                            requestTableModels.put(serverId, new Object[]{index, tableModel});
                        }
                        sortSystemList();
                        updateStatusCount();
                        break;
                    case "UPDATE_SERVER_REQUEST":
                        serverId = (int) update[1];
                        requestId = (int) update[2];
                        tableModel = (DefaultTableModel) requestTableModels.get(serverId)[1];
                        newRequest = true;
                        for (int i = 0; i < tableModel.getRowCount(); i++) {
                            if (tableModel.getValueAt(i, 0).equals(requestId)) {
                                for (int col = 2; col < update.length; col++)
                                    if (update[col] != null)
                                        tableModel.setValueAt(update[col], i, col - 2);
                                newRequest = false;
                                break;
                            }
                        }
                        if (newRequest)
                            tableModel.insertRow(0, Arrays.copyOfRange(update, 2, update.length));
                        break;
                    case "ADD_LOAD_BALANCER":
                        loadBalancerId = (int) update[1];
                        boolean primary = (boolean) update[2];
                        newSystem = true;
                        for (int i = 0; i < systemListModel.getSize(); i++) {
                            if (loadBalancerId == Integer.parseInt(systemListModel.get(i)[2])) {
                                systemListModel.set(i, new String[]{"Load balancer (" + Math.abs(loadBalancerId) + ")" + (primary ? " (Main)" : ""), "Available", Integer.toString(loadBalancerId)});
                                newSystem = false;
                                break;
                            }
                        }
                        if (newSystem) {
                            tableModel = new DefaultTableModel(new String[]{"Request", "Client", "Iterations", "Deadline"}, 0) {
                                @Override
                                public Class getColumnClass(int column) {
                                    return Integer.class;
                                }
                            };
                            index = systemListModel.getSize();
                            systemListModel.add(index, new String[]{"Load balancer (" + Math.abs(loadBalancerId) + ")" + (primary ? " (Main)" : ""), "Available", Integer.toString(loadBalancerId)});
                            requestTableModels.put(loadBalancerId, new Object[]{index, tableModel});
                        }
                        sortSystemList();
                        updateStatusCount();
                        break;
                    case "ADD_LOAD_BALANCER_REQUEST":
                        loadBalancerId = (int) update[1];
                        tableModel = (DefaultTableModel) requestTableModels.get(loadBalancerId)[1];
                        tableModel.insertRow(0, Arrays.copyOfRange(update, 2, update.length));
                        break;
                    case "REMOVE_LOAD_BALANCER_REQUEST":
                        loadBalancerId = (int) update[1];
                        requestId = (int) update[2];
                        serverId = (int) update[3];
                        tableModel = (DefaultTableModel) requestTableModels.get(loadBalancerId)[1];
                        for (int i = 0; i < tableModel.getRowCount(); i++) {
                            if (tableModel.getValueAt(i, 0).equals(requestId)) {
                                if (serverId != -1) {
                                    DefaultTableModel serverTableModel = (DefaultTableModel) requestTableModels.get(serverId)[1];
                                    serverTableModel.insertRow(0, new Object[]{
                                            tableModel.getValueAt(i, 0),
                                            tableModel.getValueAt(i, 1),
                                            tableModel.getValueAt(i, 2),
                                            tableModel.getValueAt(i, 3),
                                            "Pending"
                                    });
                                }
                                tableModel.removeRow(i);
                                break;
                            }
                        }
                        break;
                    case "CHANGE_STATUS":
                        int id = (int) update[1];
                        int listIndex = (Integer) requestTableModels.get(id)[0];
                        String[] listElement = systemListModel.get(listIndex);
                        listElement[1] = (String) update[2];
                        systemListModel.set(listIndex, listElement);
                        sortSystemList();
                        updateStatusCount();
                        break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a server to the system list
     * @param serverId ID of the server
     */
    public void addServer(int serverId) {
        try {
            updates.put(new Object[]{
                    "ADD_SERVER",
                    serverId
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates a request on a server's request table<br>
     * Can also add a request
     * @param serverId ID of the server
     * @param requestId ID of the request
     * @param clientId ID of the client that sent the request
     * @param iterations number of iterations/precision of PI
     * @param deadline arbitrary value that defines request priority
     * @param status status of the request
     */
    public void updateServerRequest(int serverId, int requestId, Integer clientId, Integer iterations, Integer deadline, String status) {
        try {
            updates.put(new Object[]{
                    "UPDATE_SERVER_REQUEST",
                    serverId,
                    requestId,
                    clientId,
                    iterations,
                    deadline,
                    status
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a load balancer to the system list
     * @param loadBalancerId ID of the load balancer
     * @param primary if this load balancer is primary
     */
    public void addLoadBalancer(int loadBalancerId, boolean primary) {
        try {
            updates.put(new Object[]{
                    "ADD_LOAD_BALANCER",
                    loadBalancerId,
                    primary
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a request to a load balancer's request table
     * @param loadBalancerId ID of the load balancer
     * @param requestId ID of the request
     * @param clientId ID of the client that sent the request
     * @param iterations number of iterations/precision of PI
     * @param deadline arbitrary value that defines request priority
     */
    public void addLoadBalancerRequest(int loadBalancerId, int requestId, int clientId, int iterations, int deadline) {
        try {
            updates.put(new Object[]{
                    "ADD_LOAD_BALANCER_REQUEST",
                    loadBalancerId,
                    requestId,
                    clientId,
                    iterations,
                    deadline
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes a request from a load balancer's request table
     * @param loadBalancerId ID of the load balancer
     * @param requestId ID of the request
     * @param serverId ID of the server the request was sent to (-1 if rejected)
     */
    public void removeLoadBalancerRequest(int loadBalancerId, int requestId, int serverId) {
        try {
            updates.put(new Object[]{
                    "REMOVE_LOAD_BALANCER_REQUEST",
                    loadBalancerId,
                    requestId,
                    serverId
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Change the status of a system
     * @param id ID of the system (server or load balancer)
     * @param status "Available", "Full", "Stopped"
     */
    public void changeStatus(int id, String status) {
        try {
            updates.put(new Object[]{
                    "CHANGE_STATUS",
                    id,
                    status
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used to make sure the self port is valid<br>
     * If so, go to the main page
     * @param valid if the self port is valid
     */
    public void setSelfPortValidity(boolean valid) {
        if (!valid) {
            serverDispatcher.interrupt();
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
                Component component;
                try {
                    component = super.prepareRenderer(renderer, row, column);
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    return null;
                }

                // Set minimum width for the PI column
                if (column == 5) {
                    int rendererWidth = component.getPreferredSize().width;
                    TableColumn tableColumn = getColumnModel().getColumn(column);
                    PICellWidth = Math.max(rendererWidth + getIntercellSpacing().width, PICellWidth);
                    tableColumn.setPreferredWidth(PICellWidth);
                } else {
                    TableColumn tableColumn = getColumnModel().getColumn(column);
                    tableColumn.setPreferredWidth(TABLE_WIDTH / 5);
                }

                // Color rows based on a cell value
                if (getColumnCount() > 4) {
                    switch (getValueAt(row, 4).toString()) {
                        case "Finished" -> component.setBackground(new Color(205, 233, 165));
                        case "Pending" -> component.setBackground(new Color(253, 223, 155));
                        case "Rejected" -> component.setBackground(new Color(253, 186, 186));
                    }
                }

                return component;
            }
        };
        requestTable.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Updates the count of how many systems are available, full, or stopped
     */
    private void updateStatusCount() {
        int available = 0;
        int full = 0;
        int stopped = 0;
        for (int i = 0; i < systemListModel.getSize(); i++) {
            switch (systemListModel.get(i)[1]) {
                case "Available" -> available++;
                case "Full" -> full++;
                case "Stopped" -> stopped++;
            }
        }
        availableSystemCountLabel.setText(Integer.toString(available));
        fullSystemCountLabel.setText(Integer.toString(full));
        stoppedSystemCountLabel.setText(Integer.toString(stopped));
    }

    /**
     * Sorts the system list so the stopped systems go to the bottom
     */
    private void sortSystemList() {
        int index = systemList.getMinSelectionIndex();
        if (index < 0)
            return;
        ArrayList<String[]> list = Collections.list(systemListModel.elements());
        list.sort((x, y) -> {
            if (Objects.equals(x[1], "Stopped") && !Objects.equals(y[1], "Stopped")) {
                return 1;
            }
            if (!Objects.equals(x[1], "Stopped") && Objects.equals(y[1], "Stopped")) {
                return -1;
            }
            return 0;
        });
        int selectedId = Integer.parseInt(systemListModel.get(index)[2]);
        for (int i = 0; i < list.size(); i++) {
            String[] system = list.get(i);
            int currentId = Integer.parseInt(system[2]);
            systemListModel.set(i, system);
            requestTableModels.get(currentId)[0] = i;
            if (currentId == selectedId)
                systemList.setSelectedIndex(i);
        }
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
        continueButton = new JButton();
        continueButton.setText("Continue");
        portsPanel.add(continueButton, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        portsPanel.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        portsPanel.add(spacer2, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        portsPanel.add(spacer3, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        portsPanel.add(spacer4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        portsPanel.add(spacer5, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        portsPanel.add(spacer6, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Load balancer port:");
        portsPanel.add(label2, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadBalancerPortSpinner = new JSpinner();
        portsPanel.add(loadBalancerPortSpinner, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        cardPanel.add(panel2, "Card1");
        requestTableScrollPane = new JScrollPane();
        requestTableScrollPane.setHorizontalScrollBarPolicy(30);
        requestTableScrollPane.setVerticalScrollBarPolicy(22);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(requestTableScrollPane, gbc);
        requestTable.setAutoResizeMode(0);
        requestTable.setEnabled(false);
        requestTable.setFillsViewportHeight(false);
        requestTable.setRowSelectionAllowed(false);
        requestTableScrollPane.setViewportView(requestTable);
        systemPanel = new JPanel();
        systemPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(systemPanel, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        systemPanel.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        systemList = new JList();
        scrollPane1.setViewportView(systemList);
        systemCountPanel = new JPanel();
        systemCountPanel.setLayout(new GridBagLayout());
        systemPanel.add(systemCountPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setForeground(new Color(-16219811));
        label3.setText("⬤");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        systemCountPanel.add(label3, gbc);
        availableSystemCountLabel = new JLabel();
        availableSystemCountLabel.setText("0");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        systemCountPanel.add(availableSystemCountLabel, gbc);
        final JLabel label4 = new JLabel();
        label4.setForeground(new Color(-1607424));
        label4.setText("⬤");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        systemCountPanel.add(label4, gbc);
        fullSystemCountLabel = new JLabel();
        fullSystemCountLabel.setText("0");
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        systemCountPanel.add(fullSystemCountLabel, gbc);
        final JLabel label5 = new JLabel();
        label5.setForeground(new Color(-3593686));
        label5.setText("⬤");
        gbc = new GridBagConstraints();
        gbc.gridx = 9;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        systemCountPanel.add(label5, gbc);
        stoppedSystemCountLabel = new JLabel();
        stoppedSystemCountLabel.setText("0");
        gbc = new GridBagConstraints();
        gbc.gridx = 11;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        systemCountPanel.add(stoppedSystemCountLabel, gbc);
        final JPanel spacer7 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        systemCountPanel.add(spacer7, gbc);
        final JPanel spacer8 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        systemCountPanel.add(spacer8, gbc);
        final JPanel spacer9 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 12;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        systemCountPanel.add(spacer9, gbc);
        final JPanel spacer10 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        systemCountPanel.add(spacer10, gbc);
        availableSystemCountXLabel = new JLabel();
        availableSystemCountXLabel.setText("x");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        systemCountPanel.add(availableSystemCountXLabel, gbc);
        fullSystemCountXLabel = new JLabel();
        fullSystemCountXLabel.setText("x");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        systemCountPanel.add(fullSystemCountXLabel, gbc);
        stoppedSystemCountXLabel = new JLabel();
        stoppedSystemCountXLabel.setText("x");
        gbc = new GridBagConstraints();
        gbc.gridx = 10;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        systemCountPanel.add(stoppedSystemCountXLabel, gbc);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT!! !!!
     * DO NOT edit this method OR call it in your code!
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    /**
     * Custom renderer for the system list
     */
    private static class CustomListCellRenderer extends JPanel implements ListCellRenderer<String[]> {

        JLabel nameLabel = new JLabel();
        JLabel statusLabel = new JLabel();

        /**
         * Constructor of the custom renderer<br>
         * Starts defining the custom look of the system list elements
         */
        public CustomListCellRenderer() {
            setLayout(new BorderLayout());
            add(nameLabel, BorderLayout.WEST);
            add(statusLabel, BorderLayout.EAST);
            setBorder(new EmptyBorder(4, 8, 6, 8));
        }

        /**
         * Defines the custom look of the system list elements
         * @param list system list
         * @param value the values in each list element
         * @param index index of the element
         * @param isSelected if the element is selected
         * @param cellHasFocus if the element has focus
         * @return the custom renderer with the custom look
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends String[]> list, String[] value, int index, boolean isSelected, boolean cellHasFocus) {
            nameLabel.setText(value[0]);

            switch (value[1]) {
                case "Available" -> statusLabel.setForeground(new Color(8, 129, 93));
                case "Full" -> statusLabel.setForeground(new Color(231, 121, 0));
                case "Stopped" -> statusLabel.setForeground(new Color(201, 42, 42));
            }
            statusLabel.setText("⬤");
            statusLabel.setBorder(new EmptyBorder(0, 0, 2, 0));
            setToolTipText(value[1]);

            Color background;
            if (isSelected)
                background = list.getSelectionBackground();
            else
                background = list.getBackground();
            setBackground(new Color(background.getRed(), background.getGreen(), background.getBlue()));

            return this;
        }
    }
}
