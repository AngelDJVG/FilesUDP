package org.itson.clientearchivos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClienteArchivos {

    private static final int TAMANIO_PAQUETE = 1024;
    private static final int PUERTO = 7;
    private static final int DELAY = 3000;

    public static void main(String[] args) {
        String archivoSeleccionado = seleccionarArchivo();
        solicitarArchivo(archivoSeleccionado);
    }

    private static String seleccionarArchivo() {
        Scanner tec = new Scanner(System.in);

        String archivoSolicitado;
        do {
            System.out.println("Ingrese el archivo que quiere pedir: ");
            System.out.println("1 Trolleada al gabriel");
            System.out.println("2 Masacre en medio de la multitud");
            System.out.println("3 Nunca confies en los demas");

            archivoSolicitado = tec.nextLine();
        } while (!(archivoSolicitado.length() == 1 && (archivoSolicitado.charAt(0) == '1' || archivoSolicitado.charAt(0) == '2' || archivoSolicitado.charAt(0) == '3')));
        return archivoSolicitado;
    }

    private static void solicitarArchivo(String archivoSolicitado) {
        try {
            InetAddress direccionServidor = InetAddress.getByName("localhost");

            DatagramSocket datagramSocket = new DatagramSocket();
            DatagramPacket paqueteEnvio = new DatagramPacket(archivoSolicitado.getBytes(), 1, direccionServidor, PUERTO);
            datagramSocket.send(paqueteEnvio);

            byte[] datosRecibidos = new byte[TAMANIO_PAQUETE * 10];

            while (true) {
                Arrays.fill(datosRecibidos, (byte) 0);

                DatagramPacket paquete = new DatagramPacket(datosRecibidos, datosRecibidos.length);
                datagramSocket.receive(paquete);

                int tamanioPaquete = paquete.getLength();

                byte[] chunk = new byte[tamanioPaquete];
                System.arraycopy(datosRecibidos, 0, chunk, 0, tamanioPaquete);

                Path directorioSalida = Paths.get("C:\\Users\\mario\\3D Objects\\ArchivosRecibidos\\Archivos");
                Path rutaSalida = directorioSalida.resolve("ArchivoRecibido3.rar");

                if (!Files.exists(directorioSalida)) {
                    Files.createDirectories(directorioSalida);
                }

                if (!Files.exists(rutaSalida)) {
                    Files.createFile(rutaSalida);
                }

                Files.write(rutaSalida, chunk, StandardOpenOption.APPEND);

                System.out.println("Se ha recibido un chunk del cliente en " + paquete.getAddress().getHostName() + " en el puerto " + paquete.getPort());

                DatagramPacket paqueteConfirmacion = new DatagramPacket("k".getBytes(), 1, paquete.getAddress(), paquete.getPort());
                datagramSocket.send(paqueteConfirmacion);

                if (tamanioPaquete != 1024) {
                    System.out.println("Fin del archivo");
                    break;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ClienteArchivos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
