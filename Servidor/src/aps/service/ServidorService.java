/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aps.service;

import aps.bean.ChatMessage;
import aps.bean.ChatMessage.Action;
import aps.frame.ServidorFrame;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rodrigo_cichetto
 */
public class ServidorService {
    // Atributos
    private ServerSocket serverSocket;
    private Socket socket;
    private Map<String, ObjectOutputStream> mapOnlines = new HashMap<String, ObjectOutputStream>();
    private Thread tServer;
    private ServidorFrame view;
    
    public ServidorService(ServidorFrame view) {
        // Inicia o servidor informando a porta padrão e a View para adicionar o log
        this(view, 5555);
    }
    
    public ServidorService(ServidorFrame view, int port) {
        // Inicia o servidor informando a porta informada e a View para adicionar o log
        this.view = view;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void subir() {
        // Inicia o servidor
        while(true) {
            try {
                this.socket = serverSocket.accept();
                this.tServer = new Thread(new ListenerSocket(this.socket));
                this.tServer.start();
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void stop() {
        // Para o servidor
        try {
            this.serverSocket.close();
            this.tServer.stop();
            this.tServer.interrupt();
        } catch (java.net.SocketException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean connect(ChatMessage message, ObjectOutputStream output) {
        // Conexão para os usuários
        // Verifica se é o primeiro a conectar
        if(mapOnlines.size() == 0) {
            message.setText("YES");
            send(message, output);
            
            return true;
        }
        
        for(Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            // Verifica se já existe alguém conectado com o mesmo nome
            if (kv.getKey().equals(message.getName())) {
                message.setText("NO");
                send(message, output);
                return false;
            } else {
                message.setText("YES");
                send(message, output);
                
                return true;
            }
        }
        
        return false;
    }
    
    private void disconnect(ChatMessage message, ObjectOutputStream output) {
        mapOnlines.remove(message.getName());
        
        message.setText("deixou o chat!");
        // message.setAction(Action.SEND_ONE);
        message.setAction(Action.DISCONNECT);
        sendAll(message);
        
        System.out.println("Usuário " + message.getName() + " saiu.");
        this.view.addLog("Usuário " + message.getName() + " saiu.");
    }
    
    private void send(ChatMessage message, ObjectOutputStream output) {
        try {
            output.writeObject(message);
        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void sendOne(ChatMessage message) {
        // Envia mensagem apenas para o usuário selecionado
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            // Verifica na lista de usuários se é o que deve receber a mensagem
            if(kv.getKey().equals(message.getNameReserved())) {
                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private void sendAll(ChatMessage message) {
        // Envia mensagem para todos os usuários
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            if (!kv.getKey().equals(message.getName())) {
                message.setAction(Action.SEND_ONE);
                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }   
        }
    }
    
    private void sendOnlines() {
        // Envia lista dos usuários online
        Set<String> setNames = new HashSet<String>();
        
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            setNames.add(kv.getKey());
        }
        
        ChatMessage message = new ChatMessage();
        message.setAction(Action.USERS_ONLINE);
        message.setSetOnlines(setNames);
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            message.setName(kv.getKey());
            try {
                kv.getValue().writeObject(message);
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private class ListenerSocket implements Runnable {
        // Classe que conversa com os usuários
        private ObjectOutputStream output;
        private ObjectInputStream input;
        
        public ListenerSocket(Socket socket) {
            try {
                this.output = new ObjectOutputStream(socket.getOutputStream());
                this.input = new ObjectInputStream(socket.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        @Override
        public void run() {
            ChatMessage message = null;
            
            try {
                while((message = (ChatMessage) input.readObject()) != null) {
                    // Pega a ação da mensagem e filtra para o metodo responsável
                    Action action = message.getAction();
                    // Informa no log
                    System.out.println(message.getName() + " " + action);
                    view.addLog(message.getName() + " " + action);
                    if(action.equals(Action.CONNECT)) {
                        boolean isConnect = connect(message, output);
                        if (isConnect) {
                            mapOnlines.put(message.getName(), output);
                            sendOnlines();
                        }
                    } else if (action.equals(Action.DISCONNECT)) {
                        disconnect(message, output);
                        sendOnlines();
                        return;
                    } else if (action.equals(Action.SEND_ONE)) {
                        sendOne(message);
                    } else if (action.equals(Action.SEND_ALL)) {
                        sendAll(message);
                    }
                }
            } catch (IOException ex) {
                disconnect(message, output);
                sendOnlines();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
}