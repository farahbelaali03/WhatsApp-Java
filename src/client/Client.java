package client;

import model.Command;
import model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {

    // Attributs d'instance (encapsulation )

    private String username;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connecte;

    // Liste locale des utilisateurs connectés
    private List<User> utilisateursConnectes;

    // Constructeur

    public Client() {
        this.connecte = false;
        this.utilisateursConnectes = new ArrayList<>();
    }

    // Méthode connecter()

    public void connecter(String username) {

        // Vérification : ne pas se connecter deux fois
        if (connecte) {
            System.out.println("[Client] Déjà connecté.");
            return;
        }

        this.username = username;

        try {
            // Etape 1 — Ouvrir le socket TCP vers le serveur
            // Socket = notion réseau, port 5000 défini dans l'architecture
            socket = new Socket("localhost", 5000);
            System.out.println("[Client] Socket ouvert vers le serveur.");

            // Etape 2 — Ouvrir les flux d'objets
            // ObjectOutputStream AVANT ObjectInputStream (obligatoire en Java)
            // sinon deadlock : les deux côtés attendent l'en-tête de l'autre
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // envoyer l'en-tête immédiatement
            in  = new ObjectInputStream(socket.getInputStream());
            System.out.println("[Client] Flux ouverts.");

            // Etape 3 — Envoyer la commande CONNECT
            // On utilise notre classe Command (Tâche 1) avec le pseudo
            Command cmdConnect = new Command(Command.CONNECT, username);
            out.writeObject(cmdConnect);
            out.flush();
            System.out.println("[Client] CONNECT envoyé pour : " + username);

            // Etape 4 — Attendre la réponse CONNECT_OK du serveur
            // readObject() est bloquant : on attend ici jusqu'à la réponse
            Command reponse = (Command) in.readObject();

            if (reponse.getType().equals(Command.CONNECT_OK)) {

                // Cast polymorphique : data est Object, on sait que c'est List<User

                List<User> liste = (List<User>) reponse.getData();

                // Copier dans notre liste locale
                utilisateursConnectes.clear();
                if (liste != null) {
                    utilisateursConnectes.addAll(liste);
                }

                connecte = true;
                System.out.println("[Client] Connexion acceptée !");
                System.out.println("[Client] Utilisateurs connectés : "
                        + utilisateursConnectes);

                // Etape 5 — Lancer le thread de réception (Tâche 4)
                // On prépare l'appel mais le thread sera implémenté en Tâche 4
                // demarrerReception(); // sera décommenté à la Tâche 4

            } else {
                System.out.println("[Client] Réponse inattendue : "
                        + reponse.getType());
                fermerRessources();
            }

        } catch (IOException e) {
            System.out.println("[Client] Erreur réseau : " + e.getMessage());
            fermerRessources();
        } catch (ClassNotFoundException e) {
            System.out.println("[Client] Classe inconnue reçue : "
                    + e.getMessage());
            fermerRessources();
        }
    }

    // Méthode deconnecter()

    public void deconnecter() {

        if (!connecte) {
            System.out.println("[Client] Pas connecté.");
            return;
        }

        try {
            // Prévenir le serveur avant de fermer
            Command cmdDisconnect = new Command(Command.DISCONNECT);
            out.writeObject(cmdDisconnect);
            out.flush();
            System.out.println("[Client] DISCONNECT envoyé.");

        } catch (IOException e) {
            // Si le serveur est déjà tombé, on continue quand même
            System.out.println("[Client] Impossible d'envoyer DISCONNECT : "
                    + e.getMessage());
        } finally {
            // finally : s'exécute toujours, même si exception
            fermerRessources();
            connecte = false;
            System.out.println("[Client] Déconnecté proprement.");
        }
    }

    // Méthode privée utilitaire — fermeture des ressources

    private void fermerRessources() {
        try {
            if (in != null) in.close();
            if (out!= null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("[Client] Erreur à la fermeture : "
                    + e.getMessage());
        }
    }
    // Getters

    public String getUsername() { return username; }
    public boolean isConnecte() { return connecte; }
    public List<User> getUtilisateursConnectes() { return utilisateursConnectes; }

    // Setter pour injection future
    public ObjectInputStream  getIn()  { return in; }
    public ObjectOutputStream getOut() { return out; }
}
