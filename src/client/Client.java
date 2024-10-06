package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Client {
    private static final String GROUP_ADDRESS = "230.0.0.0";
    private static final int PORT = 12345;
    private MulticastSocket socket;
    private InetAddress group;
    private String name;
    private boolean isJoined = false;

    public static void main(String[] args) {
        new Client().createAndShowGUI();
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Multicast Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel nameLabel = new JLabel("Client: ");
        titlePanel.add(nameLabel);
        frame.add(titlePanel, BorderLayout.NORTH);

        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        frame.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField inputField = new JTextField();
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton joinButton = new JButton("Join Group");
        JButton leaveButton = new JButton("Leave Group");

        joinButton.addActionListener(e -> {
            if (!isJoined) {
                String ip = JOptionPane.showInputDialog("Enter IP Address:");
                String portStr = JOptionPane.showInputDialog("Enter Port:");

                if (ip != null && portStr != null) {
                    try {
                        int port = Integer.parseInt(portStr);
                        socket = new MulticastSocket(port);
                        group = InetAddress.getByName(GROUP_ADDRESS);
                        socket.joinGroup(group);
                        name = JOptionPane.showInputDialog("Enter your name:");
                        isJoined = true;

                        // Cập nhật tên trong nameLabel
                        nameLabel.setText("Client: " + name);

                        sendMessage(name + " has joined the group.");

                        new Thread(() -> receiveMessages(messageArea)).start();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Failed to join group: " + ex.getMessage());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Already joined the group.");
            }
        });

        leaveButton.addActionListener(e -> {
            if (isJoined) {
                try {
                    socket.leaveGroup(group);
                    isJoined = false;
                    messageArea.append(name + " left the group.\n");
                    sendMessage(name + " left the group.");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "You are not in the group.");
            }
        });

        // Thêm KeyListener để gửi tin nhắn khi nhấn Enter
        inputField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (isJoined) {
                        String message = inputField.getText();
                        if (!message.trim().isEmpty()) {
                            sendMessage(message);
                            inputField.setText("");
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "You must join the group to send messages.");
                    }
                }
            }
        });

        buttonPanel.add(joinButton);
        buttonPanel.add(leaveButton);
        inputPanel.add(buttonPanel, BorderLayout.EAST); // Thêm nút vào bên phải
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void sendMessage(String message) {
        try {
            String fullMessage = name + ": " + message;
            byte[] buffer = fullMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages(JTextArea messageArea) {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                messageArea.append(receivedMessage + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
