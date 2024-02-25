package org.itson.clientearchivos;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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
    private static final int DELAY = 1000;

    private static final String DIRECCION_SERVIDOR = "localhost";

    private static final String cadenaDirectorioSalida = "C:\\Users\\mario\\3D Objects\\ArchivosRecibidos\\Archivos";
    private static final String cadenaRutaSalida = "ArchivoRecibido.rar";

    private static Integer numeroPaquetesRecibidos = 0;

    private static InetAddress direccionRemitente;
    private static int puertoRemitente = 0;

    public static void main(String[] args) {
        String archivoSeleccionado = seleccionarArchivo();
        try {
            solicitarArchivo(archivoSeleccionado);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    private static synchronized void solicitarArchivo(String archivoSolicitado) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(cadenaDirectorioSalida + "\\" + cadenaRutaSalida, "rw");
        ArrayList<Integer> paquetesRecibidos = new ArrayList<>();
        DatagramSocket datagramSocket = new DatagramSocket();
        try {
            InetAddress direccionServidor = InetAddress.getByName(DIRECCION_SERVIDOR);
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

            boolean CONTINUA_RECIBIENDO = true;
            while (CONTINUA_RECIBIENDO) {
                recibirArchivo(datagramSocket, raf, paquetesRecibidos);
            }
            raf.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("Recibio " + paquetesRecibidos.size());

        do {
            byte[] paquetesRecibidosEnviar = convertirArregloBytes(paquetesRecibidos);
            DatagramPacket acknowledgmentPacket = new DatagramPacket(paquetesRecibidosEnviar, paquetesRecibidosEnviar.length, direccionRemitente, puertoRemitente);
            System.out.println("Paquetes recibidos " + paquetesRecibidos.size());
            datagramSocket.send(acknowledgmentPacket);
            if (paquetesRecibidos.size() != numeroPaquetesRecibidos) {
                boolean CONTINUA_RECIBIENDO = true;
                try {
                    while (CONTINUA_RECIBIENDO) {
                        recibirArchivo(datagramSocket, raf, paquetesRecibidos);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } while (paquetesRecibidos.size() != numeroPaquetesRecibidos);
    }

    private static void recibirArchivo(DatagramSocket datagramSocket, RandomAccessFile raf, ArrayList<Integer> paquetesRecibidos) throws Exception {
        byte[] datosRecibidos = new byte[TAMANIO_PAQUETE + Integer.BYTES * 2];

        DatagramPacket paquete = new DatagramPacket(datosRecibidos, datosRecibidos.length);
        datagramSocket.receive(paquete);
        direccionRemitente = paquete.getAddress();
        puertoRemitente = paquete.getPort();

        int tamanioPaquete = paquete.getLength();

        byte[] chunk = new byte[tamanioPaquete - Integer.BYTES * 2];

        System.arraycopy(datosRecibidos, 0, chunk, 0, tamanioPaquete - Integer.BYTES * 2);

        byte[] ultimosOchoBytes = Arrays.copyOfRange(datosRecibidos, tamanioPaquete - Integer.BYTES * 2, tamanioPaquete);

        int numeroPaquete = ByteBuffer.wrap(ultimosOchoBytes, 0, Integer.BYTES).getInt();

        int totalPaquetes = ByteBuffer.wrap(ultimosOchoBytes, 4, Integer.BYTES).getInt();

        int offset = numeroPaquete * TAMANIO_PAQUETE;

        //Files.write(rutaSalida, chunk, StandardOpenOption.APPEND);
        System.out.println("Paquete " + numeroPaquete + " - Offset " + offset + " - Total " + totalPaquetes);
        raf.seek(offset);
        raf.write(chunk);
        paquetesRecibidos.add(numeroPaquete);
        numeroPaquetesRecibidos = totalPaquetes;
        System.out.println("Se ha recibido un chunk del cliente en " + paquete.getAddress().getHostName() + " en el puerto " + paquete.getPort());
    }

    private static byte[] convertirArregloBytes(ArrayList<Integer> paquetesRecibidos) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(paquetesRecibidos.size() * Integer.BYTES);
        for (int num : paquetesRecibidos) {
            byteBuffer.putInt(num);
        }
        return byteBuffer.array();
    }

}
