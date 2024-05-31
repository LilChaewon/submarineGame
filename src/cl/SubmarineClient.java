// SubmarineClient.java

package cl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

class SubmarineClient extends JFrame {
    static int inPort = 9999;
    static String address;
    static public PrintWriter out;
    static public BufferedReader in;
    static String userName;
    boolean gameStarted = false;

    static int num_mine = 10;
    static int width = 10;
    static int score = 0;
    static public Socket socket;
    public int[][] clientMap;

    public Container cont;
    public JPanel p0, p1, p2;
    public JButton[] buttons;
    public JLabel statusLabel;
    public JTextField nameField;
    public JTextField ipField;
    public JButton startButton;
    public JTextArea messageArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SubmarineClient());
    }

    public SubmarineClient() {
        setTitle("SubmarineClient");
        setSize(600, 600);
        setLocation(150, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cont = getContentPane();
        cont.setLayout(new BorderLayout());

        // 맨위 패널
        p0 = new JPanel();
        p0.setLayout(new FlowLayout());
        p0.setBackground(Color.CYAN);
        p0.add(new JLabel("Enter your name:"));
        nameField = new JTextField(10);
        p0.add(nameField);

        p0.add(new JLabel("Enter Server IP:"));
        ipField = new JTextField("localhost",10);
        p0.add(ipField);

        startButton = new JButton("Start Game");
        startButton.addActionListener(new StartButtonListener());
        p0.add(startButton);

        // 중간 바둑판 패널
        p1 = new JPanel();
        p1.setBackground(Color.YELLOW);
        p1.setLayout(new GridLayout(width, width));

        buttons = new JButton[width * width];
        for (int i = 0; i < width * width; i++) {
            buttons[i] = new JButton();
            buttons[i].setPreferredSize(new Dimension(50, 50));
            buttons[i].addActionListener(new MyActionListener());
            p1.add(buttons[i]);
        }

        // 맨밑 메시지 패널
        p2 = new JPanel();
        p2.setLayout(new BorderLayout());
        statusLabel = new JLabel("Score: " + score + " / " + num_mine);
        p2.add(statusLabel, BorderLayout.NORTH);
        messageArea = new JTextArea(5, 30);
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        p2.add(scrollPane, BorderLayout.CENTER);

        cont.add(p0, BorderLayout.NORTH);
        cont.add(p1, BorderLayout.CENTER);
        cont.add(p2, BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }

    private void setupConnection() {
        try {
            address = ipField.getText();

            socket = new Socket(address, inPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(userName);
            String msg = in.readLine(); // wait message
            messageArea.append(msg + "\n");
            receiveMap();
            msg = in.readLine(); // start message
            messageArea.append(msg + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveMap() throws IOException {
        String msg = in.readLine();
        if (msg.equals("map")) {
            width = Integer.parseInt(in.readLine());
            num_mine = Integer.parseInt(in.readLine());
            clientMap = new int[width][width];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    clientMap[i][j] = Integer.parseInt(in.readLine());
                }
            }
        }
    }

    class StartButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            userName = nameField.getText();
            if (!userName.isEmpty()) {
                setupConnection();
                startButton.setEnabled(false);
                nameField.setEnabled(false);
                gameStarted = true;
            } else {
                JOptionPane.showMessageDialog(SubmarineClient.this, "Please enter your name.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class MyActionListener implements ActionListener {
        private boolean playerTurn = true;

        public void actionPerformed(ActionEvent e) {
            if (!gameStarted || !playerTurn) {
                return;
            }

            JButton b = (JButton) e.getSource();
            int i = -1;
            for (int j = 0; j < buttons.length; j++) {
                if (buttons[j] == b) {
                    i = j;
                    break;
                }
            }

            int x = i / width;
            int y = i % width;

            try {
                out.println(x + "," + y);
                String msg = in.readLine();
                if (msg.equalsIgnoreCase("ok")) {
                    msg = in.readLine();
                    int result = Integer.parseInt(msg);
                    if (result >= 0) {
                        score++;
                        b.setText("O");
                        b.setBackground(Color.GREEN);
                    } else {
                        b.setText("X");
                        b.setBackground(Color.RED);
                    }
                    statusLabel.setText("Score: " + score + " / " + num_mine);
                    messageArea.append("Coordinate (" + x + "," + y + "): " + (result >= 0 ? "Hit" : "Miss") + "\n");

                    playerTurn = !playerTurn;
                }
                if (msg.startsWith("Game Over!")) {
                    StringBuilder statistics = new StringBuilder();
                    statistics.append(msg).append("\n");
                    for (int j = 0; j < 5; j++) {
                        statistics.append(in.readLine()).append("\n");
                    }
                    messageArea.append("\n" + statistics.toString());
                    // 게임 오버 메시지와 통계를 JOptionPane으로도 출력
                    JOptionPane.showMessageDialog(SubmarineClient.this, statistics.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            playerTurn = false; // 턴 종료
        }
    }
}
