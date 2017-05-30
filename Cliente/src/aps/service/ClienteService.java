/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aps.service;

import aps.bean.ChatMessage;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 *
 * @author rodrigo_cichetto
 */
public class ClienteService {
    // Atributos
    private Socket socket;
    private ObjectOutputStream output;
    
    public Socket connect() throws IOException {
        // Conecta no ip e porta padrão
        return this.connect("localhost", 5555);
    }
    
    public Socket connect(String ip, int porta) throws IOException {
        // Conecta no ip e porta informados
        this.socket = new Socket(ip, porta);
        this.output = new ObjectOutputStream(socket.getOutputStream());
        return socket;
    }
    
    public void send(ChatMessage message) throws IOException {
        // Envia o objeto mensagem para o servidor
        output.writeObject(message);
    }
}
