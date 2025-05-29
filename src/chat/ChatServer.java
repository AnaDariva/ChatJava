package chat;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servidor de chat TCP que gerencia múltiplas conexões de clientes simultâneas.
 * Funcionalidades principais:
 * - Aceita conexões de vários clientes
 * - Gerencia nomes de usuários únicos
 * - Encaminha mensagens para todos (broadcast) ou para usuários específicos (privadas)
 * - Mantém lista de usuários online
 */
public class ChatServer {
    // Porta onde o servidor escuta por conexões
    private static final int PORT = 12345;

    // Pool de threads para gerenciar múltiplos clientes
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    // Mapa para armazenar clientes conectados (nome de usuário -> handler)
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    /**
     * Método principal que inicia o servidor.
     * Cria um ServerSocket e fica em loop aceitando novas conexões.
     * Cada nova conexão é tratada em uma thread separada.
     */
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor de chat iniciado na porta " + PORT);

            while (true) {// Loop infinito para aceitar conexões de clientes
                Socket clientSocket = serverSocket.accept();
                // Cria um novo handler para cada cliente e executa em thread separada
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    /**
     * Classe que gerencia a comunicação com um cliente específico.
     * Responsável por:
     * - Autenticação do usuário (verificação de nome único)
     * - Recebimento e roteamento de mensagens
     * - Controle de usuários online
     */
    static class ClientHandler implements Runnable {
        private Socket socket;          // Conexão com o cliente
        private ObjectOutputStream out; // Stream para enviar dados ao cliente
        private ObjectInputStream in;   // Stream para receber dados do cliente
        private String username;       // Nome de usuário do cliente

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Método principal que gerencia o ciclo de vida da conexão com o cliente.
         * Fluxo:
         * 1. Configura streams de entrada/saída
         * 2. Processa login do usuário
         * 3. Fica em loop tratando mensagens recebidas
         * 4. Faz limpeza ao desconectar
         */
        @Override
        public void run() {
            try {
                // Configura streams de comunicação
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Primeira mensagem deve conter o nome de usuário
                Mensagem loginMsg = (Mensagem) in.readObject();
                this.username = loginMsg.getRemetente();

                // Verifica se nome de usuário já está em uso
                if (clients.containsKey(username)) {
                    sendMessage(new Mensagem("Servidor", username, "Nome de usuário já em uso. Conexão será fechada."));
                    socket.close();
                    return;
                }

                // Registra novo cliente
                clients.put(username, this);
                // Notifica todos sobre o novo usuário
                broadcast(new Mensagem("Servidor", null, username + " entrou no chat."));
                System.out.println(username + " conectado. Clientes ativos: " + clients.size());

                // Loop principal para processar mensagens
                while (true) {
                    Mensagem msg = (Mensagem) in.readObject();

                    // Comando para sair do chat
                    if (msg.getConteudo().equals("/sair")) {
                        break;
                    }
                    // Comando para listar usuários online
                    else if (msg.getConteudo().startsWith("/usuarios")) {
                        listUsers(msg.getRemetente());
                    }
                    // Mensagem privada para outro usuário
                    else if (msg.getConteudo().startsWith("/privado:")) {
                        sendPrivateMessage(msg);
                    }
                    // Mensagem normal (broadcast para todos)
                    else {
                        broadcast(msg);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro com cliente " + username + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        /**
         * Envia uma mensagem para este cliente específico.
         * @param msg A mensagem a ser enviada
         */
        private void sendMessage(Mensagem msg) throws IOException {
            out.writeObject(msg);
            out.flush(); // Garante que a mensagem seja enviada imediatamente
        }

        /**
         * Envia uma mensagem para todos os clientes conectados.
         * @param msg A mensagem a ser transmitida
         */
        private void broadcast(Mensagem msg) {
            clients.values().forEach(client -> {
                try {
                    client.sendMessage(msg);
                } catch (IOException e) {
                    System.err.println("Erro ao enviar mensagem para " + client.username);
                }
            });
        }

        /**
         * Processa uma mensagem privada no formato /privado:destinatário:mensagem
         * @param msg A mensagem recebida contendo o comando
         */
        private void sendPrivateMessage(Mensagem msg) throws IOException {
            // Divide a mensagem em partes separadas por :
            String[] parts = msg.getConteudo().split(":", 3);

            // Verifica se o formato está correto
            if (parts.length < 3) {
                sendMessage(new Mensagem("Servidor", msg.getRemetente(), "Formato inválido. Use /privado:usuario:mensagem"));
                return;
            }

            String recipient = parts[1].trim();
            String privateMsg = parts[2];

            // Verifica se destinatário está online
            if (clients.containsKey(recipient)) {
                // Cria e envia a mensagem privada
                Mensagem privateMessage = new Mensagem(msg.getRemetente(), recipient, privateMsg);
                clients.get(recipient).sendMessage(privateMessage);
                // Envia cópia para o remetente como confirmação
                sendMessage(privateMessage);
            } else {
                // Notifica remetente que usuário não foi encontrado
                sendMessage(new Mensagem("Servidor", msg.getRemetente(), "Usuário " + recipient + " não encontrado."));
            }
        }

        /**
         * Envia a lista de usuários online para quem solicitou.
         * @param requester Nome do usuário que solicitou a lista
         */
        private void listUsers(String requester) throws IOException {
            StringBuilder userList = new StringBuilder("Usuários conectados:\n");
            clients.keySet().forEach(user -> userList.append("- ").append(user).append("\n"));
            sendMessage(new Mensagem("Servidor", requester, userList.toString()));
        }

        /**
         * Desconecta o cliente e faz limpeza:
         * - Remove da lista de usuários online
         * - Notifica outros usuários
         * - Fecha conexão e streams
         */
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