package Server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GUI extends Thread{

    private TServerDispatcher serverDispatcher;
    private final BlockingQueue<Object[]> updates = new LinkedBlockingQueue<>();

    private static final String TITLE = "Server";
    private static final int WINDOW_WIDTH = 512;
    private static final int WINDOW_HEIGHT = 256;
    private static final int TABLE_WIDTH = WINDOW_WIDTH-16;
    private static final int TABLE_HEIGHT = WINDOW_HEIGHT-32;
    private int PICellWidth = TABLE_WIDTH/6;

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

        for (JSpinner spinner : new JSpinner[] {
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
        selfPortSpinner.setValue(8200);
        monitorPortSpinner.setValue(8000);

        continueButton.addActionListener(e -> {
            if (serverDispatcher == null)
                serverDispatcher = new TServerDispatcher((int) selfPortSpinner.getValue(), (int) monitorPortSpinner.getValue(), this);
        });
    }

    public void run() {
        frame.setVisible(true);
        Object[] update;
        try {
            while (true) {
                update = updates.take();
                requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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
                    requestTableModel.addRow(update);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void updateRequest(int requestId, int clientId, int iterations, int deadline, String status, String PI) {
        try {
            updates.put(new Object[] {
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

    public void setMonitorPortValidity(boolean valid) {
        if (!valid) {
            serverDispatcher = null;
            JOptionPane.showMessageDialog(null, "Connection to monitor port failed.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            serverDispatcher.start();
        }
    }

    public void setSelfPortValidity(boolean valid) {
        if (!valid) {
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
                    tableColumn.setPreferredWidth(TABLE_WIDTH/6);
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
        requestTableModel = new DefaultTableModel(new String[] {"Request", "Client", "Iterations", "Deadline", "Status", "PI"}, 0) {
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
