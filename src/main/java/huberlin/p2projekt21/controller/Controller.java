package huberlin.p2projekt21.controller;

import huberlin.p2projekt21.Helper;
import huberlin.p2projekt21.crypto.Crypto;
import huberlin.p2projekt21.gui.MainGui;
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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class Controller {

    public static final boolean ENABLE_FILE_LOGGING = true;

    private static final byte[] PREFIX = {48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2, -126, 1, 1, 0};
    private static final byte[] SUFFIX = {2, 3, 1, 0, 1};

    private DatagramChannel socket;
    private Sender sender;
    private Receiver receiver;
    private KademliaInstance kademlia;
    private PublicKey ownPublicKey;

    // own port
    private final SocketAddress own;
    // bootstrapping
    private InetAddress ip = null;
    private int port = -1;

    /**
     * start program
     *
     * @param args args[0]==ownPort, -1 or empty for arbitrary port
     *             args[1]==bootstrapping ip, empty for skip
     *             args[2]==bootstrapping port, empty for skip
     * @throws IOException .
     */
    public static void main(String[] args) throws Exception {
        int ownPort = -1;
        if (args.length >= 1) {
            ownPort = Integer.parseInt(args[0]);
            if (ownPort < 0 || ownPort > 65535) {
                ownPort = -1;
            }
        }

        Controller controller = new Controller(ownPort);
        //controller.readBootstrappingAddress();
        //controller.ip = InetAddress.getByName("192.168.178.21");
        //controller.port = 50571;
        if (args.length >= 3) {
            controller.ip = InetAddress.getByName(args[1]);
            controller.port = Integer.parseInt(args[2]);
        }

        controller.manualController();
    }

    public void manualController() throws Exception {
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
     * Creates signature for the data (with own privateKey)
     * Stores it locally and in the network using the own publicKey
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
            Data tmp = new Data(data, signature, ownPublicKey.getEncoded());
            return kademlia.store(tmp);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads the data for the specified key
     * Loads it from local storage if possible or the network
     *
     * @param key for the requested data
     * @return byte representation of the requested data or null if not found
     */
    public byte[] load(BigInteger key) {
        try {
            // kademlia load
            Data data = kademlia.getValueData(key);
            // verify signature
            Crypto.verify(data.getData(), data.getSignature(), KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(data.getPublicKey())));
            return data.getData();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Controller(int ownPort) {
        if (ownPort >= 0 && ownPort <= 65535) {
            own = new InetSocketAddress(ownPort);
        } else {
            own = null;
        }
    }

    public void init() throws Exception {
        ownPublicKey = Crypto.getStoredPublicKey();

        if (ENABLE_FILE_LOGGING) addHandler();  // Log to file

        this.socket = DatagramChannel.open().bind(own);
        printOwnIP(socket);

        ConcurrentLinkedDeque<DatagramPacket> senderChannel = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<DatagramPacket> receiverChannel = new ConcurrentLinkedDeque<>();

        this.sender = new Sender(socket, senderChannel);
        this.receiver = new Receiver(socket, receiverChannel);

        this.sender.start();
        this.receiver.start();

        this.kademlia = new KademliaInstance(receiverChannel, senderChannel);
        this.kademlia.start(ip, port);

        // initial publish
        byte[][] tmp = Storage.read(Helper.hashForKey(ownPublicKey));
        if (tmp != null) {
            Data data = new Data(tmp);
            kademlia.store(data);
        }

        new MainGui(this, ownPublicKey);
    }

    public void terminate() throws IOException {
        this.sender.terminate();
        this.receiver.terminate();
        this.kademlia.stop();
        this.socket.close();
        closeHandler();
    }

    private void printOwnIP(DatagramChannel socket) {
        try(final DatagramSocket tmp = new DatagramSocket()){
            tmp.connect(InetAddress.getByName("8.8.8.8"), 10002);
            InetAddress ip = tmp.getLocalAddress();
            String socketAddress = socket.getLocalAddress().toString();
            String port = socketAddress.substring(socketAddress.lastIndexOf(':')+1);
            System.out.println(ip.toString() + ":" + port);
            Logger.getGlobal().info("own address " + ip.toString() + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getGlobal().warning("could not get own ip\n" + e.getMessage());
        }
    }

    private void addHandler() throws IOException {
        Path directory = Paths.get("logs");
        if (Files.notExists(directory)) Files.createDirectory(directory);
        else if (!Files.isDirectory(directory)) throw new IOException("logs already exists, but is no directory");

        String fileName = new Timestamp(new Date().getTime()).toString().replace(':', '-') + ".log";
        Path file = directory.resolve(fileName);
        Files.createFile(file);

        Logger.getGlobal().addHandler(new FileHandler(file.toString()));
    }

    private void closeHandler() {
        for (Handler handler : Logger.getGlobal().getHandlers()) {
            handler.close();
        }
    }
}
