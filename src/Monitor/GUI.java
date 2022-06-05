package Monitor;

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

    private static final String TITLE = "Monitor";
    private static final int WINDOW_WIDTH = 768;
    private static final int WINDOW_HEIGHT = 256;
    private static final int TABLE_WIDTH = 496;
    private static final int TABLE_HEIGHT = WINDOW_HEIGHT-32;
    private int PICellWidth = TABLE_WIDTH/6;

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
    private DefaultListModel<String[]> systemListModel;
    private JPanel systemCountPanel;
    private JScrollPane systemListScrollPane;
    private JLabel availableSystemCountLabel;
    private JLabel fullSystemCountLabel;
    private JLabel stoppedSystemCountLabel;
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

        systemPanel.setMinimumSize(new Dimension(WINDOW_WIDTH - TABLE_WIDTH - 19, TABLE_HEIGHT));
        systemPanel.setPreferredSize(new Dimension(WINDOW_WIDTH - TABLE_WIDTH - 19, TABLE_HEIGHT));

        selfPortSpinner.setValue(8000);
        ((DefaultFormatter) ((JFormattedTextField) selfPortSpinner.getEditor().getComponent(0)).getFormatter()).setCommitsOnValidEdit(true);
        selfPortSpinner.addChangeListener(e -> {
            int port = (int) selfPortSpinner.getValue();
            if (port > 65535) {
                selfPortSpinner.setValue(65535);
            } else if (port < 0) {
                selfPortSpinner.setValue(0);
            }
        });

        continueButton.addActionListener(e -> {
            // TODO
        });

        systemListModel = new DefaultListModel<>();
        systemList.setModel(systemListModel);
        systemList.setCellRenderer(new CustomListCellRenderer());

        systemCountPanel.setBorder(new EmptyBorder(0, 0, 9, 0));

        availableSystemCountLabel.setBorder(new EmptyBorder(0, 4, 0 ,0));
        fullSystemCountLabel.setBorder(new EmptyBorder(0, 4, 0 ,0));
        stoppedSystemCountLabel.setBorder(new EmptyBorder(0, 4, 0 ,0));
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

    public void updateRequest(int requestId, int serverId, int iterations, int deadline, String status, String PI) {
        try {
            updates.put(new Object[] {
                    requestId,
                    serverId,
                    iterations,
                    deadline,
                    status,
                    PI
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setLoadBalancerPortValidity(boolean valid) {
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
        requestTableModel = new DefaultTableModel(new String[] {"Request", "Server", "Iterations", "Deadline", "Status", "PI"}, 0) {
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
