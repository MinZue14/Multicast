package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Map<String, PrintWriter> userMap = new HashMap<>();
    private static Map<String, String> userIPs = new HashMap<>();
    private static Map<String, Set<PrintWriter>> groupUsers = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private Set<String> userGroups = new HashSet<>(); // Giữ tên nhóm của người dùng

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Đăng nhập
                username = in.readLine();
                userMap.put(username, out);
                clientWriters.add(out);
                userIPs.put(username, socket.getInetAddress().getHostAddress());
                broadcastUserList();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/msg")) {
                        String[] parts = message.split(" ", 3);
                        sendPrivateMessage(parts[1], parts[2]);
                    } else if (message.startsWith("/groupmsg")) {
                        sendGroupMessage(message.substring(10));
                    } else if (message.startsWith("/join")) {
                        joinGroup(message.substring(6)); // Gọi hàm joinGroup
                    } else if (message.startsWith("/leave")) {
                        leaveGroup(message.substring(7)); // Gọi hàm leaveGroup với tên nhóm
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientWriters.remove(out);
                userMap.remove(username);
                userIPs.remove(username);
                broadcastUserList();
                leaveAllGroups();
            }
        }

        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder("/userlist ");
            for (String user : userMap.keySet()) {
                userList.append(user).append(",");
            }
            for (PrintWriter writer : clientWriters) {
                writer.println(userList.toString());
            }
        }

        private void sendPrivateMessage(String recipient, String message) {
            PrintWriter recipientWriter = userMap.get(recipient);
            if (recipientWriter != null) {
                recipientWriter.println(username + ": " + message);
                out.println("You to " + recipient + ": " + message);
            } else {
                out.println("User " + recipient + " is not available.");
            }
        }

        private void sendGroupMessage(String message) {
            for (String group : userGroups) {
                Set<PrintWriter> groupSet = groupUsers.get(group);
                if (groupSet != null) {
                    for (PrintWriter writer : groupSet) {
                        writer.println("/groupmsg " + username + ": " + message);
                    }
                }
            }
        }

        private void joinGroup(String groupName) {
            Set<PrintWriter> groupSet = groupUsers.computeIfAbsent(groupName, k -> new HashSet<>());
            groupSet.add(out);
            userGroups.add(groupName);

            // Thông báo đến mọi người trong nhóm về sự tham gia mới
            broadcastGroupMessage(groupName, username + " has joined the group.");

            // Gửi danh sách thành viên nhóm cho tất cả thành viên
            sendMemberListToGroup(groupName);
        }

        private void leaveGroup(String groupName) {
            Set<PrintWriter> groupSet = groupUsers.get(groupName);
            if (groupSet != null) {
                groupSet.remove(out);
                userGroups.remove(groupName);
                broadcastGroupMessage(groupName, username + " has left the group.");
                sendMemberListToGroup(groupName);
//
//                // Nếu nhóm không còn thành viên, xóa nhóm
//                if (groupSet.isEmpty()) {
//                    groupUsers.remove(groupName);
//                }
            }
        }

        private void leaveAllGroups() {
            for (String group : new HashSet<>(userGroups)) {
                leaveGroup(group);
            }
        }

        private void sendMemberListToGroup(String groupName) {
            Set<PrintWriter> groupSet = groupUsers.get(groupName);
            if (groupSet != null) {
                StringBuilder memberList = new StringBuilder("/grouplist ");
                for (PrintWriter writer : groupSet) {
                    memberList.append(getUsernameByWriter(writer)).append(",");
                }
                for (PrintWriter writer : groupSet) {
                    writer.println(memberList.toString());
                }
            }
        }

        private String getUsernameByWriter(PrintWriter writer) {
            for (Map.Entry<String, PrintWriter> entry : userMap.entrySet()) {
                if (entry.getValue() == writer) {
                    return entry.getKey();
                }
            }
            return null;
        }

        private void broadcastGroupMessage(String groupName, String message) {
            Set<PrintWriter> groupSet = groupUsers.get(groupName);
            if (groupSet != null) {
                for (PrintWriter writer : groupSet) {
                    writer.println("/groupmsg " + message);
                }
            }
        }
    }
}
