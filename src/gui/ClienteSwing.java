package gui;

import chat.Mensagem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class ClienteSwing extends JFrame {
    private JTextArea areaTexto;
    private JTextField campoEntrada;
    private String username;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean running;

    public ClienteSwing(String username, String serverAddress, int serverPort) throws IOException {
        this.username = username;
        this.socket = new Socket(serverAddress, serverPort);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.running = true;

        initUI();
        startMessageReceiver();
        sendLoginMessage();
    }

    private void initUI() {
        setTitle("Chat TCP - Cliente: " + username);
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());


        areaTexto = new JTextArea();
        areaTexto.setEditable(false);
        areaTexto.setFont(new Font("Arial", Font.PLAIN, 14));
        add(new JScrollPane(areaTexto), BorderLayout.CENTER);


        JPanel bottomPanel = new JPanel(new BorderLayout());
        campoEntrada = new JTextField();
        campoEntrada.setFont(new Font("Arial", Font.PLAIN, 14));

        JButton enviarBtn = new JButton("Enviar");
        enviarBtn.addActionListener(e -> sendMessageFromField());

        bottomPanel.add(campoEntrada, BorderLayout.CENTER);
        bottomPanel.add(enviarBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
        campoEntrada.addActionListener(e -> sendMessageFromField());
        campoEntrada.setFocusable(true);
        campoEntrada.requestFocusInWindow();

        // Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Ações");

        JMenuItem usuariosItem = new JMenuItem("Listar Usuários");
        usuariosItem.addActionListener(e -> sendCommand("/usuarios"));

        JMenuItem sairItem = new JMenuItem("Sair");
        sairItem.addActionListener(e -> disconnect());

        menu.add(usuariosItem);
        menu.addSeparator();
        menu.add(sairItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    private void sendMessageFromField() {
        String texto = campoEntrada.getText().trim();
        if (!texto.isEmpty()) {
            handleUserInput(texto);
            campoEntrada.setText("");
            campoEntrada.requestFocusInWindow();
        }
    }

    private void handleUserInput(String text) {
        try {
            if (text.equalsIgnoreCase("/sair")) {
                out.writeObject(new Mensagem(username, null, "/sair"));
                disconnect();
            } else if (text.startsWith("/privado:")) {
                String[] parts = text.split(":", 3);
                if (parts.length == 3) {
                    out.writeObject(new Mensagem(username, parts[1].trim(), parts[2].trim()));
                } else {
                    appendMessage("Formato inválido. Use /privado:usuário:mensagem");
                }
            } else if (text.equalsIgnoreCase("/usuarios")) {
                out.writeObject(new Mensagem(username, null, "/usuarios"));
            } else {
                out.writeObject(new Mensagem(username, null, text));
            }
            out.flush();
        } catch (IOException e) {
            appendMessage("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    private void sendLoginMessage() {
        try {
            out.writeObject(new Mensagem(username, null, "Conectando..."));
            out.flush();
        } catch (IOException e) {
            appendMessage("Erro ao conectar: " + e.getMessage());
            disconnect();
        }
    }

    private void startMessageReceiver() {
        new Thread(() -> {
            try {
                while (running) {
                    Mensagem msg = (Mensagem) in.readObject();
                    SwingUtilities.invokeLater(() -> appendMessage(msg.toString()));
                }
            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    SwingUtilities.invokeLater(() ->
                            appendMessage("Conexão com o servidor foi perdida."));
                    running = false;
                }
            }
        }).start();
    }

    private void appendMessage(String message) {
        areaTexto.append(message + "\n");
        areaTexto.setCaretPosition(areaTexto.getDocument().getLength());
    }

    private void sendCommand(String command) {
        campoEntrada.setText(command);
        campoEntrada.postActionEvent();
    }

    private void disconnect() {
        running = false;
        try {
            if (out != null) {
                out.writeObject(new Mensagem(username, null, "/sair"));
                out.flush();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar recursos: " + e.getMessage());
        }
        dispose();
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog(
                null,
                "Digite seu nome de usuário:",
                "Conectar ao Chat",
                JOptionPane.PLAIN_MESSAGE);

        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                ClienteSwing cliente = new ClienteSwing(username.trim(), "localhost", 12345);
                cliente.setVisible(true);
                JOptionPane.showMessageDialog(
                        cliente,
                        "Conectado ao servidor.\n\n" +
                                "Comandos disponíveis:\n" +
                                "- Digite mensagens normalmente para enviar a todos\n" +
                                "- /sair - Desconectar\n" +
                                "- /usuarios - Listar usuários online\n" +
                                "- /privado:usuário:mensagem - Enviar mensagem privada",
                        "Bem-vindo ao Chat",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Não foi possível conectar ao servidor: " + e.getMessage(),
                        "Erro de Conexão",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}