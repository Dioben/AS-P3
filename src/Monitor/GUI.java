package Monitor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GUI extends Thread{

    private TServerDispatcher serverDispatcher;
    private final BlockingQueue<Object[]> updates = new LinkedBlockingQueue<>();
    private final HashMap<Integer, Object[]> requestTableModels = new HashMap<>();
    private final DefaultListModel<String[]> systemListModel;

    private static final String TITLE = "Monitor";
    private static final int WINDOW_WIDTH = 686;
    private static final int WINDOW_HEIGHT = 256;
    private static final int TABLE_WIDTH = 414;
    private static final int TABLE_HEIGHT = WINDOW_HEIGHT-32;
    private int PICellWidth = TABLE_WIDTH/5;

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

    public GUI() {
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

        for (JSpinner spinner : new JSpinner[] {
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

        for (JLabel label : new JLabel[] {
                availableSystemCountXLabel,
                fullSystemCountXLabel,
                stoppedSystemCountXLabel
        }) {
            label.setBorder(new EmptyBorder(0, 4, 0 ,0));
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
            int serverId = Integer.parseInt(systemListModel.get(((JList<?>) e.getSource()).getMinSelectionIndex())[2]);
            DefaultTableModel tableModel = (DefaultTableModel) requestTableModels.get(serverId)[1];
            requestTable.setModel(tableModel);

            TableColumnModel columnModel = requestTable.getColumnModel();
            for (int i = 0; i < requestTable.getColumnCount(); i++)
                columnModel.getColumn(i).setMinWidth(TABLE_WIDTH/5);
        });
    }

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
                                systemListModel.set(i, new String[] {"Server ("+serverId+")", "Available", Integer.toString(serverId)});
                                newSystem = false;
                                break;
                            }
                        }
                        if (newSystem) {
                            tableModel = new DefaultTableModel(new String[] {"Request", "Client", "Iterations", "Deadline", "Status"}, 0) {
                                @Override
                                public Class getColumnClass(int column) {
                                    return switch (column) {
                                        case 4 -> String.class;
                                        default -> Integer.class;
                                    };
                                }
                            };
                            index = systemListModel.getSize();
                            systemListModel.add(index, new String[] {"Server ("+serverId+")", "Available", Integer.toString(serverId)});
                            requestTableModels.put(serverId, new Object[] {index, tableModel});
                            updateStatusCount("", "Available");
                        }
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
                                        tableModel.setValueAt(update[col], i, col-2);
                                newRequest = false;
                                break;
                            }
                        }
                        if (newRequest)
                            tableModel.addRow(Arrays.copyOfRange(update, 2, update.length));
                        break;
                    case "ADD_LOAD_BALANCER":
                        loadBalancerId = (int) update[1];
                        boolean primary = (boolean) update[2];
                        newSystem = true;
                        for (int i = 0; i < systemListModel.getSize(); i++) {
                            if (loadBalancerId == Integer.parseInt(systemListModel.get(i)[2])) {
                                systemListModel.set(i, new String[] {"Load balancer ("+Math.abs(loadBalancerId)+")"+(primary ? " (Main)" : ""), "Available", Integer.toString(loadBalancerId)});
                                newSystem = false;
                                break;
                            }
                        }
                        if (newSystem) {
                            tableModel = new DefaultTableModel(new String[] {"Request", "Client", "Iterations", "Deadline"}, 0) {
                                @Override
                                public Class getColumnClass(int column) {
                                    return Integer.class;
                                }
                            };
                            index = systemListModel.getSize();
                            systemListModel.add(index, new String[] {"Load balancer ("+Math.abs(loadBalancerId)+")"+(primary ? " (Main)" : ""), "Available", Integer.toString(loadBalancerId)});
                            requestTableModels.put(loadBalancerId, new Object[] {index, tableModel});
                            updateStatusCount("", "Available");
                        }
                        break;
                    case "ADD_LOAD_BALANCER_REQUEST":
                        loadBalancerId = (int) update[1];
                        tableModel = (DefaultTableModel) requestTableModels.get(loadBalancerId)[1];
                        tableModel.addRow(Arrays.copyOfRange(update, 2, update.length));
                        break;
                    case "REMOVE_LOAD_BALANCER_REQUEST":
                        loadBalancerId = (int) update[1];
                        requestId = (int) update[2];
                        serverId = (int) update[3];
                        tableModel = (DefaultTableModel) requestTableModels.get(loadBalancerId)[1];
                        DefaultTableModel serverTableModel = (DefaultTableModel) requestTableModels.get(serverId)[1];
                        for (int i = 0; i < tableModel.getRowCount(); i++) {
                            if (tableModel.getValueAt(i, 0).equals(requestId)) {
                                serverTableModel.addRow(new Object[] {
                                        tableModel.getValueAt(i, 0),
                                        tableModel.getValueAt(i, 1),
                                        tableModel.getValueAt(i, 2),
                                        tableModel.getValueAt(i, 3),
                                        "Pending"
                                });
                                tableModel.removeRow(i);
                                break;
                            }
                        }
                        break;
                    case "CHANGE_STATUS":
                        int id = (int) update[1];
                        String newStatus = (String) update[2];
                        int listIndex = (Integer) requestTableModels.get(id)[0];
                        String[] listElement = systemListModel.get(listIndex);
                        String oldStatus = listElement[1];
                        listElement[1] = newStatus;
                        systemListModel.set(listIndex, listElement);
                        updateStatusCount(oldStatus, newStatus);
                        break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addServer(int serverId) {
        try {
            updates.put(new Object[] {
                    "ADD_SERVER",
                    serverId
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void updateServerRequest(int serverId, int requestId, Integer clientId, Integer iterations, Integer deadline, String status) {
        try {
            updates.put(new Object[] {
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

    public void addLoadBalancer(int loadBalancerId, boolean primary) {
        try {
            updates.put(new Object[] {
                    "ADD_LOAD_BALANCER",
                    loadBalancerId,
                    primary
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addLoadBalancerRequest(int loadBalancerId, int requestId, int clientId, int iterations, int deadline) {
        try {
            updates.put(new Object[] {
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

    public void removeLoadBalancerRequest(int loadBalancerId, int requestId, int serverId) {
        try {
            updates.put(new Object[] {
                    "REMOVE_LOAD_BALANCER_REQUEST",
                    loadBalancerId,
                    requestId,
                    serverId
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void changeStatus(int id, String status) {
        try {
            updates.put(new Object[] {
                    "CHANGE_STATUS",
                    id,
                    status
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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
                } else {
                    TableColumn tableColumn = getColumnModel().getColumn(column);
                    tableColumn.setPreferredWidth(TABLE_WIDTH/5);
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

    private void updateStatusCount(String removed, String added) {
        switch (removed) {
            case "Available":
                availableSystemCountLabel.setText(Integer.toString(Integer.parseInt(availableSystemCountLabel.getText())-1));
                break;
            case "Full":
                fullSystemCountLabel.setText(Integer.toString(Integer.parseInt(fullSystemCountLabel.getText())-1));
                break;
            case "Stopped":
                stoppedSystemCountLabel.setText(Integer.toString(Integer.parseInt(stoppedSystemCountLabel.getText())-1));
                break;
        }
        switch (added) {
            case "Available":
                availableSystemCountLabel.setText(Integer.toString(Integer.parseInt(availableSystemCountLabel.getText())+1));
                break;
            case "Full":
                fullSystemCountLabel.setText(Integer.toString(Integer.parseInt(fullSystemCountLabel.getText())+1));
                break;
            case "Stopped":
                stoppedSystemCountLabel.setText(Integer.toString(Integer.parseInt(stoppedSystemCountLabel.getText())+1));
                break;
        }
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

    private static class CustomListCellRenderer extends JPanel implements ListCellRenderer<String[]> {

        JLabel nameLabel = new JLabel();
        JLabel statusLabel = new JLabel();

        public CustomListCellRenderer() {
            setLayout(new BorderLayout());
            add(nameLabel, BorderLayout.WEST);
            add(statusLabel, BorderLayout.EAST);
            setBorder(new EmptyBorder(4, 8, 6, 8));
        }

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
