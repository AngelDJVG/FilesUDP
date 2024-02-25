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

    private static final int DELAY = 2000;
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

            int numeroTandas = 64;
            if(totalChunks < 64){
                numeroTandas = totalChunks;
            }
            int chunksPorTanda = (int) Math.ceil((double) totalChunks / numeroTandas);
            
            int tandaAuxiliar = -1;
            int ultimosChunks = -1;
            for (int tanda = 0; tanda < numeroTandas && tandaAuxiliar == -1; tanda++) {
                int limiteChunksPorTanda = (tanda+1)* chunksPorTanda; 
                ultimosChunks = chunksPorTanda; 
                if(totalChunks-(tanda*chunksPorTanda)<=chunksPorTanda){       
                    tandaAuxiliar = 0;
                    limiteChunksPorTanda = totalChunks;         
                    ultimosChunks = totalChunks-(tanda*chunksPorTanda); 
                }               
                for (int numeroChunk = tanda * chunksPorTanda; numeroChunk < limiteChunksPorTanda; numeroChunk++) {
                    enviarPaquete(numeroChunk, bytesArchivo, ultimosChunks, datagramSocket);
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
                        if (paquetesRecibidosCliente.size() != ultimosChunks) {
                            for (int numeroChunk = tanda * chunksPorTanda; numeroChunk < limiteChunksPorTanda; numeroChunk++) {
                                if (!paquetesRecibidosCliente.contains(numeroChunk)) {
                                    enviarPaquete(numeroChunk, bytesArchivo, ultimosChunks, datagramSocket);
                                }
                            }
                        }
                    } while (paquetesRecibidosCliente.size() != ultimosChunks);
                } catch (IOException e) {
                    
                }
            }
            datagramSocket.close();
        } catch (IOException e) {
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
            System.out.println("Se envio el paquete "+numeroChunk);
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
