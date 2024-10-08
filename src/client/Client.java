package client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Client {
    private PrintWriter out;
    private JTextArea chatArea;
    private JTextField inputField;
    private String username;
    private JLabel nameLabel, chattingWithLabel;
    private JList<String> userList;
    private DefaultListModel<String> userModel;
    private HashMap<String, String> userIPs;
    private String currentChatUser;

    private GroupChatFrame groupChatFrame;
    private Set<String> groupMembers;
    private DefaultListModel<String> groupUserModel;

    public Client() throws IOException {
        Socket socket = new Socket("localhost", 12345);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Tạo giao diện
        JFrame frame = new JFrame("Chat Client");
        chatArea = new JTextArea(20, 40);
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField(40);
        bottomPanel.add(inputField, BorderLayout.CENTER);

        JButton joinButton = new JButton("Join Group");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(joinButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        JPanel titlePanel = new JPanel(new BorderLayout());
        nameLabel = new JLabel("Client: ");
        chattingWithLabel = new JLabel("Chatting with: ");
        titlePanel.add(nameLabel, BorderLayout.WEST);
        titlePanel.add(chattingWithLabel, BorderLayout.EAST);
        frame.add(titlePanel, BorderLayout.NORTH);

        userModel = new DefaultListModel<>();
        userList = new JList<>(userModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setVisibleRowCount(10);
        frame.add(new JScrollPane(userList), BorderLayout.WEST);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Giao diện pastel
        frame.getContentPane().setBackground(new Color(255, 229, 204));
        joinButton.setBackground(new Color(204, 255, 229));

        // Đăng nhập
        username = JOptionPane.showInputDialog(frame, "Enter username:", "Login", JOptionPane.PLAIN_MESSAGE);
        out.println(username);
        nameLabel.setText("Client: " + username);

        userIPs = new HashMap<>();
        groupMembers = new HashSet<>();
        groupUserModel = new DefaultListModel<>();

        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/userlist")) {
                        String[] users = message.substring(10).split(",");
                        userModel.clear();
                        for (String user : users) {
                            if (!user.equals(username)) {
                                userModel.addElement(user);
                            }
                        }
                    } else if (message.startsWith("/groupmsg")) {
                        if (groupChatFrame != null) {
                            groupChatFrame.addGroupMessage(message.substring(10));
                        }
                    } else if (message.startsWith("/grouplist")) {
                        String[] members = message.substring(12).split(",");
                        updateGroupMembers(members); // Update group members
                    } else if (message.startsWith("/members")) {
                        String members = message.substring(8); // Cắt bỏ "/members "
                        String[] memberArray = members.split(", ");
                        updateGroupMembers(memberArray);
                    } else {
                        chatArea.append(message + "\n");
                    }
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }).start();

        inputField.addActionListener(e -> {
            if (currentChatUser != null) {
                out.println("/msg " + currentChatUser + " " + inputField.getText());
                inputField.setText("");
            } else {
                chatArea.append("Select a user to chat.\n");
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUserInfo = userList.getSelectedValue();
                if (selectedUserInfo != null) {
                    // Tách tên người dùng và IP từ chuỗi đã chọn
                    String[] userInfo = selectedUserInfo.split(" \\(");
                    currentChatUser = userInfo[0]; // Lấy tên người dùng
                    String currentChatUserIP = userIPs.get(currentChatUser); // Lấy IP từ HashMap

                    chattingWithLabel.setText("Chatting with: " + currentChatUser + " (" + currentChatUserIP + ")");
                }
            }
        });

        joinButton.addActionListener(e -> {
//            if (groupChatFrame != null) {
//                JOptionPane.showMessageDialog(frame, "You are already in a group chat.", "Error", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
            String groupIP = JOptionPane.showInputDialog(frame, "Enter Group IP:");
            if (groupIP != null && !groupIP.trim().isEmpty()) {
                out.println("/join " + groupIP); // Gửi yêu cầu tham gia nhóm tới máy chủ
                groupChatFrame = new GroupChatFrame(groupUserModel, username, groupIP, out);
                groupChatFrame.addGroupMessage("Joined group at IP: " + groupIP + "\n");
            }
        });
    }
    private void updateGroupMembers(String[] members) {
        this.groupUserModel.clear(); // Xóa các thành viên hiện tại trước khi thêm thành viên mới
        for (String member : members) {
                this.groupUserModel.addElement(member); // Thêm từng thành viên từ danh sách mới
        }
        // Cập nhật giao diện của groupChatFrame nếu nó tồn tại
        if (groupChatFrame != null) {
            groupChatFrame.updateGroupMembers(this.groupUserModel);
        }
    }

    public static void main(String[] args) throws Exception {
        new Client();
    }
}