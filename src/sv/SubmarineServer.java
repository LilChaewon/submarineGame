// SubmarineServer.java

package sv;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class SubmarineServer {
	public static int inPort = 9999;
	public static Vector<Client> clients = new Vector<Client>();
	public static int maxPlayer = 2;
	public static int numPlayer = 0;
	public static int width = 10;
	public static int num_mine = 10;
	public static int turnNumber = 1;
	public static Map map;
	public JFrame frame;
	public JTextArea messageArea;
	public JButton[] buttons;
	public JLabel statusLabel;

	public SubmarineServer() throws Exception {
		map = new Map(width, num_mine);
		setupGUI();
	}

	private void setupGUI() {
		frame = new JFrame("Submarine Server");
		frame.setSize(600, 600);
		frame.setLocation(150, 150);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container cont = frame.getContentPane();
		cont.setLayout(new BorderLayout());

		// 상단 패널
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout());
		topPanel.setBackground(Color.CYAN);
		topPanel.add(new JLabel("Submarine Detection Game"));

		// 중앙 바둑판 패널
		JPanel boardPanel = new JPanel();
		boardPanel.setBackground(Color.YELLOW);
		boardPanel.setLayout(new GridLayout(width, width));

		buttons = new JButton[width * width];
		for (int i = 0; i < width * width; i++) {
			buttons[i] = new JButton();
			buttons[i].setPreferredSize(new Dimension(50, 50));
			if (map.mineMap[i / width][i % width] == 1) {
				buttons[i].setText("M");
			}
			boardPanel.add(buttons[i]);
		}

		// 하단 메시지 패널
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		statusLabel = new JLabel("Server running...");
		bottomPanel.add(statusLabel, BorderLayout.NORTH);
		messageArea = new JTextArea(5, 30);
		messageArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(messageArea);
		bottomPanel.add(scrollPane, BorderLayout.CENTER);

		cont.add(topPanel, BorderLayout.NORTH);
		cont.add(boardPanel, BorderLayout.CENTER);
		cont.add(bottomPanel, BorderLayout.SOUTH);

		frame.pack();
		frame.setVisible(true);
	}

	private void appendText(String text) {
		SwingUtilities.invokeLater(() -> {
			messageArea.append(text + "\n");
		});
	}

	public static void main(String[] args) throws Exception {
		new SubmarineServer().createServer();
	}

	public void createServer() throws Exception {
		appendText("Server start running ..");
		ServerSocket server = new ServerSocket(inPort);

		numPlayer = 0;
		while (numPlayer < maxPlayer) {
			Socket socket = server.accept();
			Client c = new Client(socket);
			clients.add(c);
			numPlayer++;
		}
		appendText("\n" + numPlayer + " players join");
		for (Client c : clients) {
			c.turn = true;
			appendText("  - " + c.userName);
		}

		sendMapToAll();
		sendtoall("Start Game");

		while (true) {
			if (allTurn()) {
				appendText("Turn " + turnNumber);
				statusLabel.setText("Turn " + turnNumber);
				turnNumber++;

				for (Client c : clients) {
					int check = map.checkMine(c.x, c.y);
					if (check >= 0) {
						appendText(c.userName + " hit at (" + c.x + " , " + c.y + ")");
						map.updateMap(c.x, c.y);
						c.score++;
					} else {
						appendText(c.userName + " miss at (" + c.x + " , " + c.y + ")");
					}

					c.send("" + check);
					c.turn = true;

					if (c.score >= 10) {
						endGame();
						return;
					}
				}
			}
		}
	}

	public void sendMapToAll() {
		for (Client c : clients) {
			c.sendMap(map);
		}
	}

	public void sendtoall(String msg) {
		for (Client c : clients)
			c.send(msg);
	}

	public boolean allTurn() {
		int i = 0;
		for (Client c : clients)
			if (!c.turn)
				i++;

		return i == clients.size();
	}

	public void endGame() {
		StringBuilder result = new StringBuilder("Game Over!\n\n");
		result.append(String.format("%-10s %-10s %-10s %-10s %-10s\n", "Player", "Attempts", "Hits", "Accuracy", "Rank"));

		clients.sort((c1, c2) -> Integer.compare(c2.score, c1.score));

		for (int i = 0; i < clients.size(); i++) {
			Client c = clients.get(i);
			int attempts = c.attempts;
			int hits = c.score;
			double accuracy = (attempts == 0) ? 0 : (double) hits / attempts * 100;
			result.append(String.format("%-10s %-10d %-10d %-10.2f %-10d\n", c.userName, attempts, hits, accuracy, i + 1));
		}

		sendtoall("GAME_OVER");
		sendtoall(result.toString());
		appendText(result.toString());

		JOptionPane.showMessageDialog(frame, result.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);

		System.exit(0);
	}

	class Client extends Thread {
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		String userName = null;
		int x, y;
		int score = 0;
		int attempts = 0;
		public boolean turn = false;

		public Client(Socket socket) throws Exception {
			initial(socket);
			start();
		}

		public void initial(Socket socket) throws IOException {
			this.socket = socket;
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			userName = in.readLine();
			System.out.println(userName + " joins from  " + socket.getInetAddress());
			send("Wait for other player..");
		}

		@Override
		public void run() {
			String msg;

			try {
				while (true) {
					msg = in.readLine();
					if (turn) {
						String[] arr = msg.split(",");
						x = Integer.parseInt(arr[0]);
						y = Integer.parseInt(arr[1]);
						attempts++;
						send("ok");
						turn = false;
					}
				}
			} catch (IOException e) {
			}
		}

		public void send(String msg) {
			out.println(msg);
		}

		public void sendMap(Map map) {
			try {
				out.println("map");
				out.println(map.width);
				out.println(map.num_mine);
				for (int i = 0; i < map.width; i++) {
					for (int j = 0; j < map.width; j++) {
						out.println(map.mineMap[i][j]);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}