package org.itson.servidorarchivos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServidorArchivos {

    private static final int MAX_CHUNK_SIZE = 1024;
    private static final int MAXIMO_HILOS = 10;
    private static final int PUERTO = 7;
    
    public static void main(String[] args) throws IOException {
        Executor service = Executors.newFixedThreadPool(MAXIMO_HILOS);
        DatagramSocket datagramSocket = new DatagramSocket(PUERTO);
        System.out.println("Ya estoy escuchando");
        while (true) {
            byte[] datosRecibidos = new byte[MAX_CHUNK_SIZE];
            DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
            datagramSocket.receive(paqueteRecibido);
            char solicitudRecibida = new String(datosRecibidos).charAt(0);
            service.execute(new ClientManager(paqueteRecibido, solicitudRecibida));
            System.out.println("Entro un cliente");
        }
    }
}
