package org.itson.clientearchivos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClienteArchivos {

    private static final int TIMEOUT = 3000;
    private static final int MAX_CHUNK_SIZE = 1024; 
    private static final int MAXTRIES = 5;

    public static void main(String[] args) throws IOException {
        InetAddress direccionServidor = InetAddress.getByName("localhost");
        int servPort = 7;

        Path rutaArchivo = Paths.get("C:\\Users\\angel\\Videos\\Captures.rar");
        byte[] bytesArchivo = Files.readAllBytes(rutaArchivo);

        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.setSoTimeout(TIMEOUT);

        int totalChunks = (int) Math.ceil((double) bytesArchivo.length / MAX_CHUNK_SIZE);

        for (int chunkNumber = 0; chunkNumber < totalChunks; chunkNumber++) {
            int offset = chunkNumber * MAX_CHUNK_SIZE;
            int length = Math.min(MAX_CHUNK_SIZE, bytesArchivo.length - offset);
            byte[] chunk = new byte[length];
            
            // Copia de los bytes correspondientes al fragmento actual
            System.arraycopy(bytesArchivo, offset, chunk, 0, length);

            DatagramPacket paqueteEnvio = new DatagramPacket(chunk, length, direccionServidor, servPort);

            int tries = 0;
            boolean respuestaRecibida = false;

            do {
                datagramSocket.send(paqueteEnvio);
                try {
                    byte[] datosRecibidos = new byte[MAX_CHUNK_SIZE];
                    DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
                    datagramSocket.receive(paqueteRecibido);

                    if (!paqueteRecibido.getAddress().equals(direccionServidor)) {
                        throw new IOException("Received packet from an unknown source");
                    }
                    respuestaRecibida = true;
                } catch (IOException e) {
                    tries += 1;
                    System.out.println("Timed out, " + (MAXTRIES - tries) + " more tries...");
                }
            } while ((!respuestaRecibida) && (tries < MAXTRIES));
        }

        datagramSocket.close();
    }
}
