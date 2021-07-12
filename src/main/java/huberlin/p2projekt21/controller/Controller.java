package huberlin.p2projekt21.controller;

import huberlin.p2projekt21.Helper;
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
     * @param args args[0]==type, 0=gui, 1=dummy, 2=publish-dummy;
     *             args[1]==ownPort, -1 or empty for arbitrary port;
     *             args[2]==bootstrapping ip, empty for skip;
     *             args[3]==bootstrapping port, empty for skip;
     * @throws IOException .
     */
    public static void main(String[] args) throws Exception {
        // read parameters
        int type = 0;                   // default type: gui
        int ownPort = -1;               // default ownPort: arbitrary
        InetAddress btAddress = null;   // default bootstrapping address: skip
        int btPort = -1;                // default bootstrapping address: skip
        if (args.length >= 1) {
            type = Integer.parseInt(args[0]);
            if (type < 0 || type > 2)   type = 0;
        }
        if (args.length >= 2) {
            ownPort = Integer.parseInt(args[1]);
            if (ownPort < 0 || ownPort > 65535) ownPort = -1;
        }
        if (args.length >= 4) {
            btAddress = InetAddress.getByName(args[2]);
            btPort = Integer.parseInt(args[3]);
        }

        //ownPort = 30006;

        // GUI ask for port and bootstrapping
        if (type == 0) {
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

        switch (type) {
            case 0 -> controller.guiController();           // start gui or manual controller
            case 1 -> controller.dummyController();         // start dummy-node
            case 2 -> controller.dummyPublishController();  // start dummy-publish-node
        }
    }

    /**
     * Initialize controller and start manual (console-based) controller
     *
     * @throws Exception .
     */
    @Deprecated
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
     * This is the main controller for users
     *
     * @throws Exception .
     */
    private void guiController() throws Exception {
        init();
        MainGui gui = new MainGui(this, ownPublicKey);

        // load own page locally and in the network and restore
        byte[][] stored = Storage.read(ownPublicKey.getEncoded());
        Data networkData = kademlia.getValueData(Helper.bigIntHashForKey(ownPublicKey));
        Data newest;
        if (stored == null) {
            // own page not stored locally
            // restore network version
            newest = networkData;
        } else {
            Data localData = new Data(stored);
            // stored locally
            if (networkData == null) {
                // not stored in network -> restore local version
                newest = localData;
            } else {
                // both found -> use newer version
                if (localData.compareDate(networkData) < 0) {
                    // network version is newer
                    newest = networkData;
                } else {
                    // local version is newer
                    newest = localData;
                }
            }
        }
        if (newest != null) {
            gui.setOwnPage(newest.getPage());
        } else {
            gui.setOwnPage(null);
        }

        // initial publish own data
        if (newest != null) {
            kademlia.store(newest);
        }
    }

    /**
     * Initializes the controller
     *
     * This controller is for testing
     * It participates in the network but can't publish
     *
     * @throws Exception .
     */
    private void dummyController() throws Exception {
        init();
    }

    /**
     * Initializes the controller
     *
     * This controller is for testing
     * It participates in the network and will publish (and republish) a number of default pages
     *
     * @throws Exception .
     */
    private void dummyPublishController() throws Exception {
        init();
        // TODO publish given data
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
    private Controller(int ownPort, InetAddress btAddress, int btPort) throws IOException, NoSuchAlgorithmException {
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
        Crypto.createPair();
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
