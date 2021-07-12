package huberlin.p2projekt21.controller;

import huberlin.p2projekt21.crypto.Crypto;
import huberlin.p2projekt21.gui.MainGui;
import huberlin.p2projekt21.gui.StartDialog;
import huberlin.p2projekt21.kademlia.Data;
import huberlin.p2projekt21.kademlia.KademliaInstance;
import huberlin.p2projekt21.networking.Receiver;
import huberlin.p2projekt21.networking.Sender;
import huberlin.p2projekt21.storage.Storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class Controller {

    public static final boolean ENABLE_GUI          =   true;
    public static final boolean ENABLE_FILE_LOGGING =   true;

    private DatagramChannel socket;
    private Sender sender;
    private Receiver receiver;
    private KademliaInstance kademlia;
    private PublicKey ownPublicKey;

    // own port
    private final SocketAddress own;
    // bootstrapping
    private final InetAddress ip;
    private final int port;

    /**
     * start program
     *
     * @param args args[0]==ownPort, -1 or empty for arbitrary port
     *             args[1]==bootstrapping ip, empty for skip
     *             args[2]==bootstrapping port, empty for skip
     * @throws IOException .
     */
    public static void main(String[] args) throws Exception {
        // read own port and bootstrapping address from parameters
        int ownPort = -1;
        if (args.length >= 1) {
            ownPort = Integer.parseInt(args[0]);
            if (ownPort < 0 || ownPort > 65535) {
                ownPort = -1;
            }
        }
        InetAddress btAddress = null;
        int btPort = -1;
        if (args.length >= 3) {
            btAddress = InetAddress.getByName(args[1]);
            btPort = Integer.parseInt(args[2]);
        }

        // GUI ask for port and bootstrapping
        if (ENABLE_GUI) {
            StartDialog startDialog = new StartDialog(ownPort, btAddress, btPort);
            var res = startDialog.display();
            if (res == null) {
                return;
            } else {
                ownPort = res.ownPort;
                btAddress = res.bootstrappingAddress;
                btPort = res.bootstrappingPort;
            }
        }

        // create controller
        Controller controller = new Controller(ownPort, btAddress, btPort);

        // start gui or manual controller
        if (ENABLE_GUI) controller.guiController();
        else            controller.manualController();
    }

    /**
     * Initialize controller and start manual (console-based) controller
     *
     * @throws Exception .
     */
    private void manualController() throws Exception {
        // initialize
        init();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        boolean running = true;
        while (running) {
            // read command
            String input = reader.readLine();

            // parse command
            String[] words = input.split(" ");
            if (words.length == 0) continue;
            //if (words.length == 1 || words[0].trim().length() == 0) continue;
            String cmd = words[0].trim();

            // execute command
            switch (cmd) {
                case "store" -> {
                    if (words.length != 3) {
                        System.out.println("Usage: store <key> <value>");
                        System.out.println("Key needs to be an positive integer");
                        System.out.println("Value needs to be a string without whitespaces");
                        continue;
                    }
                    BigInteger key = BigInteger.valueOf(Long.parseLong(words[1]));
                    byte[] value = words[2].getBytes();
                    System.out.println("#Store");
                    if (kademlia.store(key, value)) {
                        System.out.println("#\tStored in the network");
                    } else {
                        System.out.println("#\tStored locally");
                    }
                }
                case "load" -> {
                    if (words.length != 2) {
                        System.out.println("Usage: load <key>");
                        System.out.println("Key needs to be an positive integer");
                        continue;
                    }
                    BigInteger key = BigInteger.valueOf(Long.parseLong(words[1]));
                    System.out.println("#Load");
                    byte[] value = kademlia.getValue(key);
                    if (value == null) {
                        System.out.println("#\tnot found");
                    } else {
                        String stringValue = new String(value);
                        System.out.println("#\t" + stringValue);
                    }
                }
                case "stop" -> {
                    System.out.println("#Stop");
                    running = false;
                }
                default -> {
                    System.out.println("Command unknown");
                    System.out.println("Supported: store, load, stop");
                }
            }
        }

        terminate();
    }

    /**
     * Initializes the controller and starts the GUI
     *
     * @throws Exception .
     */
    private void guiController() throws Exception {
        init();
        MainGui gui = new MainGui(this, ownPublicKey);
        byte[][] stored = Storage.read(ownPublicKey.getEncoded());
        if (stored == null) {
            gui.setOwnPage(null);
        } else {
            Data data = new Data(stored);
            gui.setOwnPage(data.getPage());
            // TODO search in network and compare date
        }
    }

    /**
     * Creates signature for the data (with own privateKey)
     * Stores it locally and in the network using the own publicKey
     *
     * !!!WARNING: Will block for possibly a really long time!!!
     * !!!only call from asynchronous context!!!
     *
     * @param data byte representation of the data to be stored
     * @return true, if stored 'globally', false if only stored locally
     */
    public boolean store(byte[] data) {
        byte[] signature;
        try {
            // sign data
            signature = Crypto.signWithStoredKey(data);
            // store locally
            Storage.storeOwn(data);
            // kademlia store
            Data tmp = new Data(data, signature, ownPublicKey.getEncoded(), System.currentTimeMillis());
            return kademlia.store(tmp);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads the page for the specified key
     * Loads it from local storage if possible or the network
     *
     * !!!WARNING: Will block for possibly a really long time!!!
     * !!!only call from asynchronous context!!!
     *
     * @param key for the requested data
     * @return requested page (or null)
     */
    public Data.Page load(BigInteger key) {
        try {
            // kademlia load
            Data data = kademlia.getValueData(key);
            // verify signature
            if (data != null) {
                Crypto.verify(data.getPage().toBytes(), data.getSignature(), KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(data.getPublicKey())));
                return data.getPage();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create Controller
     *
     * @param ownPort   desired own port, -1 for arbitrary port
     * @param btAddress bootstrapping address, empty for new network
     * @param btPort    bootstrapping port, empty for new network
     */
    private Controller(int ownPort, InetAddress btAddress, int btPort) {
        if (ownPort >= 0 && ownPort <= 65535) {
            own = new InetSocketAddress(ownPort);
        } else {
            own = null;
        }
        this.ip = btAddress;
        if (btPort >= 0 && btPort <= 65535)
            port = btPort;
        else
            port = -1;
    }

    /**
     * Shut down program
     *
     * @throws IOException .
     */
    public void terminate() throws IOException {
        this.sender.terminate();
        this.receiver.terminate();
        this.kademlia.stop();
        this.socket.close();
        closeHandler();
    }

    /**
     * Initialize controller
     * Open UDP socket
     * Start kademlia
     * Create transfer channel
     *
     * @throws Exception .
     */
    private void init() throws Exception {
        ownPublicKey = Crypto.getStoredPublicKey();

        if (ENABLE_FILE_LOGGING) addHandler();  // Log to file

        this.socket = DatagramChannel.open().bind(own);
        printOwnLocalIP(socket);
        printGlobalIO();

        ConcurrentLinkedDeque<DatagramPacket> senderChannel = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<DatagramPacket> receiverChannel = new ConcurrentLinkedDeque<>();

        this.sender = new Sender(socket, senderChannel);
        this.receiver = new Receiver(socket, receiverChannel);

        this.sender.start();
        this.receiver.start();

        this.kademlia = new KademliaInstance(receiverChannel, senderChannel);
        this.kademlia.start(ip, port);

        // initial publish
        new Thread(() -> {
            try {
                byte[][] tmp = Storage.read(ownPublicKey.getEncoded());
                if (tmp != null) {
                    Data data = new Data(tmp);
                    kademlia.store(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    /**
     * Tries to get the local ip and print it
     *
     * @param socket used socket to receive port
     */
    private void printOwnLocalIP(DatagramChannel socket) {
        try(final DatagramSocket tmp = new DatagramSocket()){
            tmp.connect(InetAddress.getByName("8.8.8.8"), 10002);
            InetAddress ip = tmp.getLocalAddress();
            String socketAddress = socket.getLocalAddress().toString();
            String port = socketAddress.substring(socketAddress.lastIndexOf(':')+1);
            System.out.println("internalAddress: " + ip.toString() + ":" + port);
            Logger.getGlobal().info("internalAddress " + ip.toString() + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getGlobal().warning("could not get own ip\n" + e.getMessage());
        }
    }

    /**
     * Tries to get the global ip and print it
     * (Port can't be determined as a different socket is used)
     * (Use local port, hoping the NAT is kind)
     */
    private void printGlobalIO() throws IOException {
        URL whatIsIyIP = new URL("http://checkip.amazonaws.com");
        BufferedReader in = new BufferedReader(new InputStreamReader(whatIsIyIP.openStream()));
        String address = in.readLine();
        System.out.println("externalAddress: " + address);
        Logger.getGlobal().info("externalAddress " + address);
    }

    /**
     * Add FileHandle to Logger
     *
     * @throws IOException .
     */
    private void addHandler() throws IOException {
        Path directory = Paths.get("logs");
        if (Files.notExists(directory)) Files.createDirectory(directory);
        else if (!Files.isDirectory(directory)) throw new IOException("logs already exists, but is no directory");

        String fileName = new Timestamp(new Date().getTime()).toString().replace(':', '-') + ".log";
        Path file = directory.resolve(fileName);
        Files.createFile(file);

        Logger.getGlobal().addHandler(new FileHandler(file.toString()));
    }

    /**
     * Close FileHandle of Logger (to remove lock)
     */
    private void closeHandler() {
        for (Handler handler : Logger.getGlobal().getHandlers()) {
            handler.close();
        }
    }
}
