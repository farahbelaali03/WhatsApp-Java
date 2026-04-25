package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private static final int PORT = 5000;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Serveur démarré sur le port " + PORT);
            System.out.println("En attente de connexions...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nouveau client : " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket, this);
                addClient(handler);
                handler.start();
            }
        } catch (IOException e) {
            System.out.println("Erreur serveur : " + e.getMessage());
        } finally {
            stop();
        }
    }

    public synchronized void addClient(ClientHandler handler) {
        clients.add(handler);
        System.out.println("Clients connectés : " + clients.size());
    }

    public synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler);
        System.out.println("Client déconnecté. Restants : " + clients.size());
    }

    public synchronized void broadcast(Object obj) {
        for (ClientHandler client : clients) {
            client.send(obj);
        }
    }

    public synchronized void broadcastExcept(Object obj, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude) {
                client.send(obj);
            }
        }
    }

    public synchronized ClientHandler getClientByUsername(String username) {
        for (ClientHandler client : clients) {
            if (client.getUser() != null && client.getUser().getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

    public synchronized List<String> getUsernameList() {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (client.getUsername() != null) {
                usernames.add(client.getUsername());
            }
        }
        return usernames;
    }

    public synchronized int getClientCount() {
        return clients.size();
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Serveur arrêté.");
            }
        } catch (IOException e) {
            System.out.println("Erreur arrêt serveur : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}