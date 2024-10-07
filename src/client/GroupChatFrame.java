package client;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;

public class GroupChatFrame {
    private JFrame groupChatFrame;
    private JTextArea groupChatArea;
    private JTextField groupInputField;
    private DefaultListModel<String> groupUserModel;
    private JLabel groupMembersCountLabel;
    private JList<String> groupUserList;

    public GroupChatFrame(DefaultListModel<String> groupUserModel, String groupIP, PrintWriter out) {
        this.groupUserModel = groupUserModel;
        openGroupChatWindow(groupIP, out);
    }

    private void openGroupChatWindow(String groupIP, PrintWriter out) {
        groupChatFrame = new JFrame("Group Chat - " + groupIP);
        groupChatArea = new JTextArea(20, 40);
        groupChatArea.setEditable(false);
        groupChatFrame.add(new JScrollPane(groupChatArea), BorderLayout.CENTER);

        groupUserList = new JList<>(groupUserModel);
        groupUserList.setVisibleRowCount(10);
        groupChatFrame.add(new JScrollPane(groupUserList), BorderLayout.EAST);

        groupMembersCountLabel = new JLabel("Total members: " + groupUserModel.getSize());
        groupChatFrame.add(groupMembersCountLabel, BorderLayout.NORTH);

        // Tạo JPanel cho phần dưới với BoxLayout để sắp xếp theo chiều dọc
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        groupInputField = new JTextField(40);
        bottomPanel.add(groupInputField); // Thêm ô nhập vào

        JButton leaveButton = new JButton("Leave Group");
        leaveButton.setBackground(new Color(204, 204, 255));
        bottomPanel.add(leaveButton); // Thêm nút Leave vào

        groupChatFrame.add(bottomPanel, BorderLayout.SOUTH);

        groupChatFrame.pack();
        groupChatFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        groupChatFrame.setVisible(true);

        groupInputField.addActionListener(e -> {
            if (!groupInputField.getText().isEmpty()) {
                out.println("/groupmsg " + groupInputField.getText());
                groupInputField.setText("");
            }
        });

//        // Thêm nút Leave Group
//        leaveButton = new JButton("Leave Group");
//        leaveButton.addActionListener(e -> {
//            groupChatFrame.dispose(); // Đóng cửa sổ nhóm
//        });

        leaveButton.addActionListener(e -> {
            if (groupChatFrame != null) {
                out.println("/leave");
                groupChatFrame.dispose();
            }
            else {
               out.println("You are not in any group chat.");
            }
        });
    }

    public void updateGroupMembers(DefaultListModel<String> groupUserModel) {
        // Đảm bảo rằng model bên ngoài được cập nhật trước khi thực hiện cập nhật giao diện
        if (groupUserModel == null || groupUserModel.getSize() == 0) {
            this.groupUserModel.clear(); // Nếu không có thành viên, xóa model hiện tại
        } else {
            this.groupUserModel.clear(); // Xóa các thành viên hiện tại trước khi thêm thành viên mới
            for (int i = 0; i < groupUserModel.getSize(); i++) {
                this.groupUserModel.addElement(groupUserModel.getElementAt(i)); // Thêm từng thành viên từ model mới
            }
        }

        // Cập nhật số lượng thành viên
        groupMembersCountLabel.setText("Total members: " + this.groupUserModel.getSize());
    }


    public void addGroupMessage(String message) {
        groupChatArea.append(message + "\n");
    }
}