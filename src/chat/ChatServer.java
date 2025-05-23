package chat;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor de chat iniciado na porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Primeira mensagem deve ser o nome de usuário
                Mensagem loginMsg = (Mensagem) in.readObject();
                this.username = loginMsg.getRemetente();

                // Verifica se o nome já está em uso
                if (clients.containsKey(username)) {
                    sendMessage(new Mensagem("Servidor", username, "Nome de usuário já em uso. Conexão será fechada."));
                    socket.close();
                    return;
                }

                clients.put(username, this);
                broadcast(new Mensagem("Servidor", null, username + " entrou no chat."));
                System.out.println(username + " conectado. Clientes ativos: " + clients.size());

                // Processa mensagens do cliente
                while (true) {
                    Mensagem msg = (Mensagem) in.readObject();

                    if (msg.getConteudo().equals("/sair")) {
                        break;
                    } else if (msg.getConteudo().startsWith("/usuarios")) {
                        listUsers(msg.getRemetente());
                    } else if (msg.getConteudo().startsWith("/privado:")) {
                        sendPrivateMessage(msg);
                    } else {
                        broadcast(msg);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro com cliente " + username + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void sendMessage(Mensagem msg) throws IOException {
            out.writeObject(msg);
            out.flush();
        }

        private void broadcast(Mensagem msg) {
            clients.values().forEach(client -> {
                try {
                    client.sendMessage(msg);
                } catch (IOException e) {
                    System.err.println("Erro ao enviar mensagem para " + client.username);
                }
            });
        }

        private void sendPrivateMessage(Mensagem msg) throws IOException {
            String[] parts = msg.getConteudo().split(":", 3);
            if (parts.length < 3) {
                sendMessage(new Mensagem("Servidor", msg.getRemetente(), "Formato inválido. Use /privado:usuario:mensagem"));
                return;
            }

            String recipient = parts[1].trim();
            String privateMsg = parts[2];

            if (clients.containsKey(recipient)) {
                Mensagem privateMessage = new Mensagem(msg.getRemetente(), recipient, privateMsg);
                clients.get(recipient).sendMessage(privateMessage);
                // Envia cópia para o remetente
                sendMessage(privateMessage);
            } else {
                sendMessage(new Mensagem("Servidor", msg.getRemetente(), "Usuário " + recipient + " não encontrado."));
            }
        }

        private void listUsers(String requester) throws IOException {
            StringBuilder userList = new StringBuilder("Usuários conectados:\n");
            clients.keySet().forEach(user -> userList.append("- ").append(user).append("\n"));
            sendMessage(new Mensagem("Servidor", requester, userList.toString()));
        }

        private void disconnect() {
            if (username != null) {
                clients.remove(username);
                broadcast(new Mensagem("Servidor", null, username + " saiu do chat."));
                System.out.println(username + " desconectado. Clientes ativos: " + clients.size());
            }
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket: " + e.getMessage());
            }
        }
    }
}