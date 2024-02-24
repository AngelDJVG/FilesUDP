package org.itson.clientearchivos;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClienteArchivos {

    private static final int TAMANIO_PAQUETE = 1016;
    private static final int PUERTO = 7;
    private static final int DELAY = 4000;

    private static final String cadenaDirectorioSalida = "C:\\Users\\mario\\3D Objects\\ArchivosRecibidos\\Archivos";
    private static final String cadenaRutaSalida = "ArchivoRecibido.rar";

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
        RandomAccessFile raf;
        ArrayList<Integer> paquetesRecibidos = new ArrayList<>();
        int numeroPaquetes = -1;
        try {
            InetAddress direccionServidor = InetAddress.getByName("localhost");

            raf = new RandomAccessFile(cadenaDirectorioSalida + "\\" + cadenaRutaSalida, "rw");
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(DELAY);
            DatagramPacket paqueteEnvio = new DatagramPacket(archivoSolicitado.getBytes(), 1, direccionServidor, PUERTO);
            datagramSocket.send(paqueteEnvio);

            Path directorioSalida = Paths.get(cadenaDirectorioSalida);
            Path rutaSalida = directorioSalida.resolve(cadenaRutaSalida);

            if (!Files.exists(directorioSalida)) {
                Files.createDirectories(directorioSalida);
            }

            if (!Files.exists(rutaSalida)) {
                Files.createFile(rutaSalida);
            }

            byte[] datosRecibidos;

            boolean CONTINUA_RECIBIENDO = true;
            while (CONTINUA_RECIBIENDO) {
                datosRecibidos = new byte[TAMANIO_PAQUETE + 8];

                DatagramPacket paquete = new DatagramPacket(datosRecibidos, datosRecibidos.length);
                datagramSocket.receive(paquete);

                int tamanioPaquete = paquete.getLength();

                byte[] chunk = new byte[tamanioPaquete - 8];

                System.arraycopy(datosRecibidos, 0, chunk, 0, tamanioPaquete-8);

                byte[] ultimosOchoBytes = Arrays.copyOfRange(datosRecibidos, tamanioPaquete - 8, tamanioPaquete);

                int numeroPaquete = ByteBuffer.wrap(ultimosOchoBytes, 0, 4).getInt();

                int totalPaquetes = ByteBuffer.wrap(ultimosOchoBytes, 4, 4).getInt();
                
                int offset = numeroPaquete * TAMANIO_PAQUETE;

                //Files.write(rutaSalida, chunk, StandardOpenOption.APPEND);
                System.out.println("Paquete "+numeroPaquete+" - Offset "+offset+" - Total "+totalPaquetes);
                raf.seek(offset);
                raf.write(chunk);
                paquetesRecibidos.add(numeroPaquete);
                numeroPaquetes = totalPaquetes;
                System.out.println("Se ha recibido un chunk del cliente en " + paquete.getAddress().getHostName() + " en el puerto " + paquete.getPort());
            }
            raf.close();
        } catch (IOException ex) {
            
        }
        if(numeroPaquetes == paquetesRecibidos.size()){
            System.out.println("Llego todo bien :)");
        }else{
            System.out.println("Solo se recibieron "+paquetesRecibidos.size()+" de "+numeroPaquetes);
            System.out.println("Perdida de paquetes... BV");
        }
    }

}
