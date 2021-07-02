package huberlin.p2projekt21.controller;

import huberlin.p2projekt21.kademlia.KademliaInstance;
import huberlin.p2projekt21.networking.Receiver;
import huberlin.p2projekt21.networking.Sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class Controller {

    private DatagramChannel socket;
    private Sender sender;
    private Receiver receiver;
    private KademliaInstance kademlia;

    // own port
    private final SocketAddress own;
    // bootstrapping
    private InetAddress ip = null;
    private int port = -1;

    /**
     * start program
     *
     * @param args args[0] == own port, empty for arbitrary port
     * @throws IOException .
     */
    public static void main(String[] args) throws IOException {
        int ownPort = -1;
        if (args.length >= 1) {
            ownPort = Integer.parseInt(args[0]);
            if (ownPort < 0 || ownPort > 65535) {
                ownPort = -1;
            }
        }

        new Controller(ownPort).init();
    }

    public Controller(int ownPort) {
        if (ownPort >= 0 && ownPort <= 65535) {
            own = new InetSocketAddress(port);
        } else {
            own = null;
        }
    }

    public void init() throws IOException {
        addHandler();
        this.socket = DatagramChannel.open().bind(own);
        printOwnIP(socket);
        readBootstrappingAddress();

        ConcurrentLinkedDeque<DatagramPacket> senderChannel = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<DatagramPacket> receiverChannel = new ConcurrentLinkedDeque<>();

        this.sender = new Sender(socket, senderChannel);
        this.receiver = new Receiver(socket, receiverChannel);

        this.sender.start();
        this.receiver.start();

        this.kademlia = new KademliaInstance(receiverChannel, senderChannel);
        this.kademlia.start(ip, port);
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
            System.out.println("own address " + ip.toString() + ":" + port);
            Logger.getGlobal().info("own address " + ip.toString() + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getGlobal().warning("could not get own ip\n" + e.getMessage());
        }
    }

    private void readBootstrappingAddress() throws IOException {
        System.out.println("Please insert bootstrapping IP: e.g. 'xxx.xxx.xxx.xxx:port'");
        System.out.println("Empty skips bootstrapping");

        String input = new BufferedReader(new InputStreamReader(System.in)).readLine();
        System.out.println("input: " + input);

        if (input.trim().length() == 0) return;

        int separator = input.lastIndexOf(':');
        port = Integer.parseInt(input.substring(separator+1));
        System.out.println("port: " + port);

        String address = input.substring(0, separator);
        System.out.println("address: " + address);

        if (address.charAt(0) == '/') address = address.substring(1);
        ip = InetAddress.getByName(address);
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
