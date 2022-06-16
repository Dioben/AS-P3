package LB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GUI extends Thread{

    private TWatcherContact watcherContact;
    private final BlockingQueue<Object[]> updates = new LinkedBlockingQueue<>();

    private static final String TITLE = "Load balancer";
    private static final int WINDOW_WIDTH = 432;
    private static final int WINDOW_HEIGHT = 256;
    private static final int TABLE_WIDTH = WINDOW_WIDTH-101;
    private static final int TABLE_HEIGHT = WINDOW_HEIGHT-32;

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
        availableServerCount.setBorder(new EmptyBorder(8, 4, 0 ,0));
        fullServerCount.setBorder(new EmptyBorder(8, 4, 0 ,0));

        for (JSpinner spinner : new JSpinner[] {
                selfPortSpinner,
                monitorPortSpinner
        }) {
            spinner.setValue(8000);
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

        continueButton.addActionListener(e -> {
            if (watcherContact == null) {
                watcherContact = new TWatcherContact((int) monitorPortSpinner.getValue(), (int) selfPortSpinner.getValue(), this);
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
                requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                switch ((String) update[0]) {
                    case "ADD_REQUEST":
                        requestTableModel.addRow(Arrays.copyOfRange(update, 1, update.length));
                    case "REMOVE_REQUEST":
                        for (int i = 0; i < requestTableModel.getRowCount(); i++) {
                            if (requestTableModel.getValueAt(i, 0).equals(update[1])) {
                                requestTableModel.removeRow(i);
                                break;
                            }
                        }
                        break;
                    case "SERVER_COUNTS":
                        availableServerCount.setText("x"+((int) update[1]));
                        fullServerCount.setText("x"+((int) update[2]));
                        break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addRequest(int requestId, int clientId, int iterations, int deadline) {
        try {
            updates.put(new Object[] {
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
            updates.put(new Object[] {
                    "REMOVE_REQUEST",
                    requestId
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setServerCounts(int available, int full) {
        try {
            updates.put(new Object[] {
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
            ((CardLayout) cardPanel.getLayout()).next(cardPanel);
        }
    }

    public void setSelfMain() {
        frame.setTitle(TITLE + " (Main)");
    }

    private void createUIComponents() {
        requestTable = new JTable() {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);

                TableColumn tableColumn = getColumnModel().getColumn(column);
                tableColumn.setPreferredWidth(TABLE_WIDTH/4);

                return component;
            }
        };
        requestTableModel = new DefaultTableModel(new String[] {"Request", "Client", "Iterations", "Deadline"}, 0) {
            @Override
            public Class getColumnClass(int column) {
                return Integer.class;
            }
        };
        requestTable.setModel(requestTableModel);
        requestTable.getTableHeader().setReorderingAllowed(false);
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
}
