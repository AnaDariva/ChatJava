package gui;

import chat.Mensagem;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

/**
 * Cliente gráfico para o chat TCP que inclui:
 * - Área de exibição de mensagens
 * - Campo para digitar mensagens
 * - Suporte a mensagens privadas
 * - Listagem de usuários online
 */
public class ClienteSwing extends JFrame {
    // Componentes da interface
    private JTextArea areaTexto;      // Área onde as mensagens são exibidas
    private JTextField campoEntrada;  // Campo para digitar mensagens

    // Conexão com o servidor
    private String username;          // Nome de usuário do cliente
    private Socket socket;            // Socket de conexão com o servidor
    private ObjectOutputStream out;   // Stream para enviar objetos ao servidor
    private ObjectInputStream in;     // Stream para receber objetos do servidor
    private boolean running;         // Flag para controle da thread de recebimento

    /**
     * Construtor que inicia a conexão com o servidor
     * @param username Nome do usuário
     * @param serverAddress Endereço do servidor
     * @param serverPort Porta do servidor
     */
    public ClienteSwing(String username, String serverAddress, int serverPort) throws IOException {
        this.username = username;
        // Estabelece conexão com o servidor
        this.socket = new Socket(serverAddress, serverPort);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.running = true;

        initUI();               // Inicializa a interface gráfica
        startMessageReceiver(); // Inicia thread para receber mensagens
        sendLoginMessage();     // Envia mensagem inicial ao servidor
    }

    /**
     * Inicializa os componentes da interface gráfica
     */
    private void initUI() {
        setTitle("Chat TCP - Cliente: " + username);
        setSize(600, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Configura área de texto para exibição das mensagens
        areaTexto = new JTextArea();
        areaTexto.setEditable(false);
        areaTexto.setFont(new Font("Arial", Font.PLAIN, 14));
        add(new JScrollPane(areaTexto), BorderLayout.CENTER);

        // Painel inferior com campo de entrada e botão - (MODIFICAÇÕES AQUI?)
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Aumentei o tamanho do campo de entrada
        campoEntrada = new JTextField();
        campoEntrada.setFont(new Font("Arial", Font.PLAIN, 14));
        campoEntrada.setPreferredSize(new Dimension(campoEntrada.getPreferredSize().width, 40)); // Altura aumentada

        // Aumenta o tamanho do botão para combinar
        JButton enviarBtn = new JButton("Enviar");
        enviarBtn.setPreferredSize(new Dimension(enviarBtn.getPreferredSize().width, 40)); // Altura aumentada
        enviarBtn.addActionListener(e -> sendMessageFromField());

        // Adiciona margem interna para melhorar a aparência
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        bottomPanel.add(campoEntrada, BorderLayout.CENTER);
        bottomPanel.add(enviarBtn, BorderLayout.EAST);

        // Define altura fixa para o painel inferior
        bottomPanel.setPreferredSize(new Dimension(getWidth(), 50));
        add(bottomPanel, BorderLayout.SOUTH);

        // Configura ação ao pressionar Enter no campo de texto
        campoEntrada.addActionListener(e -> sendMessageFromField());
        campoEntrada.setFocusable(true);
        campoEntrada.requestFocusInWindow();

        // Configura menu com ações
        setupMenuBar();

        // Listener para fechamento da janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    /**
     * Configura a barra de menu com as opções:
     * - Listar usuários
     * - Sair do chat
     */
    private void setupMenuBar() {
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
    }

    /**
     * Processa e envia a mensagem digitada no campo de texto
     */
    private void sendMessageFromField() {
        String texto = campoEntrada.getText().trim();
        if (!texto.isEmpty()) {
            handleUserInput(texto);  // Processa o texto
            campoEntrada.setText(""); // Limpa o campo
            campoEntrada.requestFocusInWindow(); // Mantém o foco no campo
        }
    }

    /**
     * Processa o texto digitado pelo usuário e determina o tipo de mensagem:
     * - Comando /sair: desconecta do chat
     * - Comando /usuarios: solicita lista de usuários
     * - Mensagem privada (/privado:usuário:mensagem)
     * - Mensagem normal (broadcast)
     */
    private void handleUserInput(String text) {
        try {
            if (text.equalsIgnoreCase("/sair")) {
                out.writeObject(new Mensagem(username, null, "/sair"));
                disconnect();
            }
            // Mensagem privada no formato /privado:destinatário:mensagem
            else if (text.startsWith("/privado:")) {
                processPrivateMessage(text);
            }
            else if (text.equalsIgnoreCase("/usuarios")) {
                out.writeObject(new Mensagem(username, null, "/usuarios"));
            }
            // Mensagem normal para todos
            else {
                out.writeObject(new Mensagem(username, null, text));
            }
            out.flush(); // Garante o envio imediato
        } catch (IOException e) {
            appendMessage("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    /**
     * Processa mensagem privada, verificando o formato correto
     */
    private void processPrivateMessage(String text) throws IOException {
        String[] parts = text.split(":", 3);
        if (parts.length == 3) {
            out.writeObject(new Mensagem(username, parts[1].trim(), parts[2].trim()));
        } else {
            appendMessage("Formato inválido. Use /privado:usuário:mensagem");
        }
    }

    /**
     * Envia mensagem inicial de login ao servidor
     */
    private void sendLoginMessage() {
        try {
            out.writeObject(new Mensagem(username, null, "Conectando..."));
            out.flush();
        } catch (IOException e) {
            appendMessage("Erro ao conectar: " + e.getMessage());
            disconnect();
        }
    }

    /**
     * Inicia thread para receber mensagens do servidor em segundo plano
     */
    private void startMessageReceiver() {
        new Thread(() -> {
            try {
                while (running) {
                    Mensagem msg = (Mensagem) in.readObject();
                    // Atualiza a interface na thread de eventos
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

    /**
     * Adiciona uma mensagem à área de texto e rola para a última mensagem
     */
    private void appendMessage(String message) {
        areaTexto.append(message + "\n");
        areaTexto.setCaretPosition(areaTexto.getDocument().getLength());
    }

    /**
     * Envia um comando específico (simula digitação no campo de texto)
     */
    private void sendCommand(String command) {
        campoEntrada.setText(command);
        campoEntrada.postActionEvent(); // Dispara o evento de ação
    }

    /**
     * Desconecta do servidor e fecha todos os recursos
     */
    private void disconnect() {
        running = false; // Para a thread de recebimento
        try {
            if (out != null) {
                out.writeObject(new Mensagem(username, null, "/sair"));
                out.flush();
            }
            // Fecha todos os recursos de conexão
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar recursos: " + e.getMessage());
        }
        dispose(); // Fecha a janela
    }

    /**
     * Ponto de entrada do cliente - solicita nome de usuário e inicia a interface
     */
    public static void main(String[] args) {
        // Solicita nome de usuário
        String username = JOptionPane.showInputDialog(
                null,
                "Digite seu nome de usuário:",
                "Conectar ao Chat",
                JOptionPane.PLAIN_MESSAGE);

        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }

        // Inicia a interface na thread de eventos
        SwingUtilities.invokeLater(() -> {
            try {
                ClienteSwing cliente = new ClienteSwing(username.trim(), "localhost", 12345);
                cliente.setVisible(true);
                // Exibe mensagem de boas-vindas com instruções
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