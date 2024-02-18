/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.itson.servidorarchivos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientManager implements Runnable {

    private static final int TIMEOUT = 3000;
    private static final int MAX_CHUNK_SIZE = 1024;
    private DatagramPacket paqueteRecibido;
    private InetAddress direccionCliente;
    
    private String pathArchivo1 = "C:\\Users\\mario\\OneDrive\\Imágenes\\FNAF\\ClipsGoose\\Clip.rar";
    private String pathArchivo2 = "C:\\Users\\mario\\OneDrive\\Imágenes\\FNAF\\ClipsGoose\\Clip2.rar";
    private String pathArchivo3 = "C:\\Users\\mario\\OneDrive\\Imágenes\\FNAF\\ClipsGoose\\Clip3.rar";
    
    private char solicitudRecibida; 

    public ClientManager(DatagramPacket paqueteRecibido, char solicitudRecibida) {
        this.paqueteRecibido = paqueteRecibido;
        this.direccionCliente = this.paqueteRecibido.getAddress();
        this.solicitudRecibida = solicitudRecibida;
    }

    @Override
    public void run() {
        try {
            
            String archivoSolicitado = "";
            switch(solicitudRecibida){
                case '1' : archivoSolicitado = pathArchivo1 ;break;
                case '2' : archivoSolicitado = pathArchivo2; break;
                case '3' : archivoSolicitado = pathArchivo3; break;
                default : archivoSolicitado = ""; break;
            }
            
            Path rutaArchivo = Paths.get(archivoSolicitado);
            byte[] bytesArchivo = Files.readAllBytes(rutaArchivo);
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(TIMEOUT);
            
            int totalChunks = (int) Math.ceil((double) bytesArchivo.length / MAX_CHUNK_SIZE);

            for (int chunkNumber = 0; chunkNumber < totalChunks; chunkNumber++) {
                int offset = chunkNumber * MAX_CHUNK_SIZE;
                int length = Math.min(MAX_CHUNK_SIZE, bytesArchivo.length - offset);
                byte[] chunk = new byte[length];

                System.arraycopy(bytesArchivo, offset, chunk, 0, length);

                DatagramPacket paqueteEnvio = new DatagramPacket(chunk, length, direccionCliente, paqueteRecibido.getPort());

                boolean respuestaRecibida = false;

                do {
                    datagramSocket.send(paqueteEnvio);
                    try {
                        byte[] datosRecibidos = new byte[MAX_CHUNK_SIZE];
                        DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
                        datagramSocket.receive(paqueteRecibido);

                        if (!paqueteRecibido.getAddress().equals(direccionCliente)) {
                            throw new IOException("Se recibio un paquete de otro lugar");
                        }

                        char caracterRecibido = new String(datosRecibidos).charAt(0);

                        if (caracterRecibido == 'k') {
                            respuestaRecibida = true;
                        }else{
                            System.out.println("No confirmo de recibido");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Error al enviar un paquete, se reintentará su entrega");
                    }
                } while (!respuestaRecibida);
            }
            datagramSocket.close();
        } catch (IOException e) {
            System.out.println("Error al inicializar valores");
        }
    }

}
