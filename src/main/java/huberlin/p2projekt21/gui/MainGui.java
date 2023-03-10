package huberlin.p2projekt21.gui;

import huberlin.p2projekt21.Helper;
import huberlin.p2projekt21.controller.Controller;
import huberlin.p2projekt21.kademlia.Data;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.text.DateFormat;
import java.util.Date;
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

        ownFileTextArea.setEditable(false); // deactivate until own page is loaded
        friendFileTextArea.setContentType("text/html");

        ownKeyTextField.setText("<null>");
        if (ownKey != null) {
            byte[] hash = Helper.hashForKey(ownKey);
            if (hash != null) ownKeyTextField.setText(Helper.bytesToHex(hash));
        }

        uploadButton.addActionListener(e -> {
            uploadButton.setEnabled(false);
            // load created page
            String text = ownFileTextArea.getText();
            new SwingWorker<Boolean, Object>() {
                @Override
                protected Boolean doInBackground() {
                    // use standardised encoding to prevent encoding problems with different OS
                    return controller.store(text.getBytes(StandardCharsets.UTF_8));
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
            if (stringKey.trim().length() == 0) {
                friendFileTextArea.setText(resources.getString("bad_key"));
                searchButton.setEnabled(true);
                return;
            }
            BigInteger key;
            try {
                key = new BigInteger(stringKey, 16);
            } catch (NumberFormatException ignore) {
                friendFileTextArea.setText(resources.getString("bad_key"));
                searchButton.setEnabled(true);
                return;
            }
            new SwingWorker<Data.Page, Object>() {
                @Override
                protected Data.Page doInBackground() {
                    return controller.load(key);
                }
                @Override
                protected void done() {
                    try {
                        Data.Page page = get();
                        if (page == null) {
                            friendFileTextArea.setText(resources.getString("search_failed"));
                            friendFileTextArea.setContentType("text/plain");
                            dateTextField.setText("");
                        } else {
                            // use standardised encoding to prevent encoding problems with different OS
                            String content = new String(page.getData(), StandardCharsets.UTF_8);
                            //String content = new String(page.getData());
                            if (content.toLowerCase().contains("<html>")) {
                                friendFileTextArea.setContentType("text/html");
                            } else {
                                friendFileTextArea.setContentType("text/plain");
                            }
                            friendFileTextArea.setText(content);
                            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
                            String formattedDate = df.format(new Date(page.getTimeStamp()));
                            dateTextField.setText(formattedDate);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        friendFileTextArea.setText(resources.getString("search_failed"));
                    }
                    searchButton.setEnabled(true);
                }
            }.execute();
        });

        ownFileTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreview();
            }
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

        // center divider of split pane
        splitPane.setDividerLocation(splitPane.getSize().width/2);
    }

    public void setOwnPage(Data.Page page) {
        if (page != null) {
            // use standardised encoding to prevent encoding problems with different OS
            ownFileTextArea.setText(new String(page.getData(), StandardCharsets.UTF_8));
        }
        ownFileTextArea.setEditable(true);
    }

    private void updatePreview() {
        String content = ownFileTextArea.getText();
        if (content.toLowerCase().contains("<html>")) {
            htmlPreviewTextArea.setContentType("text/html");
        } else {
            htmlPreviewTextArea.setContentType("text/plain");
        }
        htmlPreviewTextArea.setText(content);
    }


    private JPanel mainPanel;
    private JTextField friendKeyTextField;
    private JButton searchButton;
    private JTextField ownKeyTextField;
    private JButton uploadButton;
    private JTextPane ownFileTextArea;
    private JTextPane friendFileTextArea;
    private JTextPane htmlPreviewTextArea;
    private JSplitPane splitPane;
    private JTextField dateTextField;
}
