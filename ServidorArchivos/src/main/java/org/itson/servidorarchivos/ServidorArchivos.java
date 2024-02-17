package org.itson.servidorarchivos;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.*;

public class ServidorArchivos {

    private static final int MAX_CHUNK_SIZE = 1024;

    public static void main(String[] args) throws IOException {
        int servPort = 7;
        
        DatagramSocket datagramSocket = new DatagramSocket(servPort);

        byte[] datosRecibidos = new byte[MAX_CHUNK_SIZE * 10];

        while (true) {
            
            DatagramPacket paquete = new DatagramPacket(datosRecibidos, datosRecibidos.length);
            datagramSocket.receive(paquete);

            int tamanio = paquete.getLength();
            byte[] chunk = new byte[tamanio];
            System.arraycopy(datosRecibidos, 0, chunk, 0, tamanio);

            Path directorioSalida = Paths.get("C:\\Users\\angel\\Downloads\\Nueva carpeta");
            Path rutaSalida = directorioSalida.resolve("ArchivoRecibido.rar");

            if (!Files.exists(directorioSalida)) {
                Files.createDirectories(directorioSalida);
            }

            if (!Files.exists(rutaSalida)) {
                Files.createFile(rutaSalida);
            }

            Files.write(rutaSalida, chunk, StandardOpenOption.APPEND);

            System.out.println("Se ha recibido un chunk del cliente en " + paquete.getAddress().getHostName() + " en el puerto " + paquete.getPort());
            
            //el receptor del mensaje envía un "OK" (ACK) al remitente para decirle que recibió esos paquetes correctamente. 
            //Si el remitente no recibe el "OK", puede intentar enviar los paquetes nuevamente para asegurarse de que el 
            //mensaje se entregue de manera correcta.
            DatagramPacket acknowledgmentPacket = new DatagramPacket("ACK".getBytes(), 3, paquete.getAddress(), paquete.getPort());
            datagramSocket.send(acknowledgmentPacket);
        }
    }
}
