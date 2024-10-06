package server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashSet;
import java.util.Set;

public class Server {
    private static final int PORT = 12345;
    private static final String GROUP_ADDRESS = "230.0.0.0";
    private Set<InetAddress> clients = new HashSet<>();

    public static void main(String[] args) {
        new Server().start();
    }

    public void start() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(GROUP_ADDRESS);
            socket.joinGroup(group);

            System.out.println("Server started. Waiting for clients...");

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + message);
                // Broadcast message to all clients
                sendMessageToClients(socket, group, message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToClients(MulticastSocket socket, InetAddress group, String message) {
        for (InetAddress client : clients) {
            try {
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, client, PORT);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addClient(InetAddress client) {
        clients.add(client);
    }

    public void removeClient(InetAddress client) {
        clients.remove(client);
    }
}
