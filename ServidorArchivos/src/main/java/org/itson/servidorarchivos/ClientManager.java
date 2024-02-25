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
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    public synchronized void run() {
        try {
            String archivoSolicitado = this.obtenerArchivoSolicitado();

            Path rutaArchivo = Paths.get(archivoSolicitado);
            byte[] bytesArchivo = Files.readAllBytes(rutaArchivo);
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(DELAY);

            int totalChunks = (int) Math.ceil((double) bytesArchivo.length / TAMANIO_PAQUETE);

            for (int numeroChunk = 0; numeroChunk < totalChunks; numeroChunk++) {
                enviarPaquete(numeroChunk, bytesArchivo, totalChunks, datagramSocket);
            }

            List<Integer> paquetesRecibidosCliente = new ArrayList<>();
            try {
                do {
                    byte[] confirmacionPaquetesRecibidos = new byte[totalChunks * 4];
                    paqueteRecibido = new DatagramPacket(confirmacionPaquetesRecibidos, confirmacionPaquetesRecibidos.length);
                    paqueteRecibido.getLength();
                    datagramSocket.receive(paqueteRecibido);

                    int tamanioLista = paqueteRecibido.getLength();
                    byte[] enterosRecibidos = new byte[tamanioLista];

                    System.arraycopy(confirmacionPaquetesRecibidos, 0, enterosRecibidos, 0, tamanioLista);

                    paquetesRecibidosCliente = convertirArregloEnteros(enterosRecibidos);
                    System.out.println("2Paquetes recibidos " + paquetesRecibidosCliente.size() + " Totales que deben ser: " + totalChunks);
                    if (paquetesRecibidosCliente.size() != totalChunks) {
                        for (int numeroChunk = 0; numeroChunk < totalChunks; numeroChunk++) {
                            if (!paquetesRecibidosCliente.contains(numeroChunk)) {
                                enviarPaquete(numeroChunk, bytesArchivo, totalChunks, datagramSocket);
                            }
                        }
                    }
                } while (paquetesRecibidosCliente.size() != totalChunks);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("3Paquetes recibidos " + paquetesRecibidosCliente.size() + " Totales que deben ser: " + totalChunks);
            }
            datagramSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error al inicializar valores");
        }
    }

    private void enviarPaquete(int numeroChunk, byte[] bytesArchivo, int totalChunks, DatagramSocket datagramSocket) {
        int offset = numeroChunk * TAMANIO_PAQUETE;
        int tamanio = Math.min(TAMANIO_PAQUETE, bytesArchivo.length - offset);
        byte[] chunk = new byte[tamanio + Integer.BYTES * 2];

        System.arraycopy(bytesArchivo, offset, chunk, 0, tamanio);
        ByteBuffer.wrap(chunk, tamanio, Integer.BYTES).putInt(numeroChunk);
        ByteBuffer.wrap(chunk, tamanio + 4, Integer.BYTES).putInt(totalChunks);
        DatagramPacket paqueteEnvio = new DatagramPacket(chunk, tamanio + Integer.BYTES * 2, direccionCliente, paqueteRecibido.getPort());

        try {
            datagramSocket.send(paqueteEnvio);
        } catch (IOException ex) {

        }
    }

    private static List<Integer> convertirArregloEnteros(byte[] arregloBytes) {
        IntBuffer intBuffer = ByteBuffer.wrap(arregloBytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        int[] array = new int[intBuffer.remaining()];
        intBuffer.get(array);
        return Arrays.stream(array).boxed().collect(Collectors.toList());
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
