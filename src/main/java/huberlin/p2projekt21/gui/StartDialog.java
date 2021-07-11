package huberlin.p2projekt21.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;

public class StartDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField ownPortTextField;
    private JTextField bootstrappingAddressTextField;
    private JTextField bootstrappingPortTextField;

    StartParameters result = null;
    Color def;

    public StartDialog(int defOwnPort, InetAddress defBootstrappingAddress, int defBootstrappingPort) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        def = ownPortTextField.getBackground();
        this.setMaximumSize(new Dimension(327, 160));

        if (defOwnPort >= 0 && defOwnPort <= 65535)
            ownPortTextField.setText("" + defOwnPort);
        if (defBootstrappingAddress != null)
            bootstrappingAddressTextField.setText(defBootstrappingAddress.toString());
        if (defBootstrappingPort >= 0 && defBootstrappingPort <= 65535)
            bootstrappingPortTextField.setText("" + defBootstrappingPort);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    /**
     * Display dialog and await result
     * @return selected save name (null if canceled)
     */
    public StartParameters display() {
        pack();
        setVisible(true);
        return result;
    }

    private void onOK() {
        ownPortTextField.setBackground(def);
        bootstrappingAddressTextField.setBackground(def);
        bootstrappingPortTextField.setBackground(def);
        boolean success = true;
        int ownPort = -1;
        try {
            String tmp = ownPortTextField.getText();
            if (!(tmp == null || tmp.trim().length() == 0)) {
                ownPort = Integer.parseInt(tmp);
                if (ownPort < 0 || ownPort > 65535) throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            ownPortTextField.setBackground(Color.red);
            success = false;
        }
        InetAddress bootstrappingAddress = null;
        try {
            String tmp = bootstrappingAddressTextField.getText();
            if (!(tmp == null || tmp.trim().length() == 0)) {
                bootstrappingAddress = InetAddress.getByName(bootstrappingAddressTextField.getText());
            }
        } catch (UnknownHostException e) {
            bootstrappingAddressTextField.setBackground(Color.red);
            success = false;
        }
        int bootstrappingPort = -1;
        try {
            String tmp = bootstrappingPortTextField.getText();
            if (!(tmp == null || tmp.trim().length() == 0)) {
                bootstrappingPort = Integer.parseInt(tmp);
                if (bootstrappingPort < 0 || bootstrappingPort > 65535) throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            bootstrappingPortTextField.setBackground(Color.red);
            success = false;
        }
        if (success) {
            result = new StartParameters(ownPort, bootstrappingAddress, bootstrappingPort);
            setVisible(false);
            dispose();
        }
    }

    private void onCancel() {
        result = null;
        setVisible(false);
        dispose();
    }

    public static void main(String[] args) {
        StartDialog dialog = new StartDialog(-1, null, -1);
        dialog.pack();
        //dialog.setVisible(true);

        StartParameters res = dialog.display();
        if (res == null) {
            System.out.println("null");
        } else {
            System.out.println("ownPort: " + res.ownPort);
            if (res.bootstrappingAddress != null) System.out.println("btAddr: " + res.bootstrappingAddress.toString());
            else System.out.println("btAddr: null");
            System.out.println("btPort: " + res.bootstrappingPort);
        }

        System.exit(0);
    }

    public static class StartParameters {
        public int ownPort;
        public InetAddress bootstrappingAddress;
        public int bootstrappingPort;

        public StartParameters(int ownPort, InetAddress bootstrappingAddress, int bootstrappingPort) {
            this.ownPort = ownPort;
            this.bootstrappingAddress = bootstrappingAddress;
            this.bootstrappingPort = bootstrappingPort;
        }
    }
}
