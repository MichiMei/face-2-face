package huberlin.p2projekt21.gui;

import huberlin.p2projekt21.Helper;
import huberlin.p2projekt21.controller.Controller;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

public class MainGui extends JFrame {

    private final ResourceBundle resources;

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Locale.setDefault(Locale.US);
        new MainGui(null, null);
    }

    public MainGui(Controller controller, PublicKey ownKey) throws NoSuchAlgorithmException {
        super();

        this.resources = ResourceBundle.getBundle("StringLiterals");

        ownKeyTextField.setText("<null>");
        if (ownKey != null) {
            byte[] hash = Helper.hashForKey(ownKey);
            if (hash != null) ownKeyTextField.setText(bytesToHex(hash));
        }

        uploadButton.addActionListener(e -> {
            uploadButton.setEnabled(false);
            // load created page
            String text = ownFileTextArea.getText();
            new SwingWorker<Boolean, Object>() {
                @Override
                protected Boolean doInBackground() {
                    return controller.store(text.getBytes());
                }
                @Override
                protected void done() {
                    try {
                        Boolean result = get();
                        if (result == null) throw new Exception("Store returned with null value");
                        if (result) {
                            // TODO display success (network)
                        } else {
                            // TODO display fail (only local)
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // TODO handle
                    }
                    uploadButton.setEnabled(true);
                }
            }.execute();
        });

        searchButton.addActionListener(e -> {
            searchButton.setEnabled(false);
            // load inserted stringKey, interpret as hex-integer
            String stringKey = friendKeyTextField.getText();
            BigInteger key = new BigInteger(stringKey, 16);
            new SwingWorker<byte[], Object>() {
                @Override
                protected byte[] doInBackground() {
                    return controller.load(key);
                }
                @Override
                protected void done() {
                    try {
                        byte[] result = get();
                        if (result == null) {
                            friendFileTextArea.setText(resources.getString("search_failed"));
                        } else {
                            friendFileTextArea.setText(new String(result));
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        friendFileTextArea.setText(resources.getString("search_failed"));
                    }
                    searchButton.setEnabled(true);
                }
            }.execute();
        });

        // WINDOW //
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) { }
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (controller != null) controller.terminate();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    System.exit(-1);
                }
            }
            @Override
            public void windowClosed(WindowEvent e) { }
            @Override
            public void windowIconified(WindowEvent e) { }
            @Override
            public void windowDeiconified(WindowEvent e) { }
            @Override
            public void windowActivated(WindowEvent e) { }
            @Override
            public void windowDeactivated(WindowEvent e) { }
        });
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }
        setMinimumSize(new Dimension(900,600));

        this.setContentPane(mainPanel);
        this.setVisible(true);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private JPanel mainPanel;
    private JTextField friendKeyTextField;
    private JButton searchButton;
    private JTextField ownKeyTextField;
    private JButton uploadButton;
    private JTextArea ownFileTextArea;
    private JTextArea friendFileTextArea;
}
