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

    private static final int DELAY = 3000;
    private static final int TAMANIO_PAQUETE = 1024;
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
            String archivoSolicitado = this.obtenerArchivoSolicitado();

            Path rutaArchivo = Paths.get(archivoSolicitado);
            byte[] bytesArchivo = Files.readAllBytes(rutaArchivo);
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(DELAY);

            int totalChunks = (int) Math.ceil((double) bytesArchivo.length / TAMANIO_PAQUETE);

            for (int numeroChunk = 0; numeroChunk < totalChunks; numeroChunk++) {
                int offset = numeroChunk * TAMANIO_PAQUETE;
                int tamanio = Math.min(TAMANIO_PAQUETE, bytesArchivo.length - offset);
                byte[] chunk = new byte[tamanio];

                System.arraycopy(bytesArchivo, offset, chunk, 0, tamanio);

                DatagramPacket paqueteEnvio = new DatagramPacket(chunk, tamanio, direccionCliente, paqueteRecibido.getPort());

                boolean respuestaRecibida = false;

                do {
                    datagramSocket.send(paqueteEnvio);
                    try {
                        byte[] datosRecibidos = new byte[TAMANIO_PAQUETE];
                        DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
                        datagramSocket.receive(paqueteRecibido);

                        if (!paqueteRecibido.getAddress().equals(direccionCliente)) {
                            throw new IOException("Se recibio un paquete de otro lugar");
                        }

                        char caracterRecibido = new String(datosRecibidos).charAt(0);

                        if (caracterRecibido == 'k') {
                            respuestaRecibida = true;
                        } else {
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
    
    private String obtenerArchivoSolicitado() {
        switch (solicitudRecibida) {
            case '1':
                return pathArchivo1;
            case '2':
                return pathArchivo2;
            case '3':
                return pathArchivo3;
            default:
                return "";
        }
    }

}
