package LB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GUI extends Thread{

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
        serverCountPanel.setBorder(new EmptyBorder(0, 8, 0, 0));
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
                } else if (port < 0) {
                    spinner.setValue(0);
                }
            });
        }

        continueButton.addActionListener(e -> {
            // TODO
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

    public void updateRequest(int requestId, int clientId, int iterations, int deadline) {
        try {
            updates.put(new Object[] {
                    requestId,
                    clientId,
                    iterations,
                    deadline
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setMonitorPortValidity(boolean valid) {
        if (!valid) {
            // TODO
            JOptionPane.showMessageDialog(null, "Connection to monitor port failed.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            // TODO
        }
    }

    public void setSelfPortValidity(boolean valid) {
        if (!valid) {
            // TODO
            JOptionPane.showMessageDialog(null, "Invalid self port.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            frame.setTitle(TITLE + " (" + selfPortSpinner.getValue() + ")"); // TODO
            ((CardLayout) cardPanel.getLayout()).next(cardPanel);
        }
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
