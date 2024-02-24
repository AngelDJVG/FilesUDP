/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.itson.servidorarchivos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientManager implements Runnable {

    private static final int DELAY = 4000;
    private static final int TAMANIO_PAQUETE = 1016;
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
                byte[] chunk = new byte[tamanio + 8];

                System.arraycopy(bytesArchivo, offset, chunk, 0, tamanio);

                ByteBuffer.wrap(chunk, tamanio, 4).putInt(numeroChunk);

                ByteBuffer.wrap(chunk, tamanio + 4, 4).putInt(totalChunks);

                DatagramPacket paqueteEnvio = new DatagramPacket(chunk, tamanio + 8, direccionCliente, paqueteRecibido.getPort());

                boolean respuestaRecibida = false;

                datagramSocket.send(paqueteEnvio);
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
