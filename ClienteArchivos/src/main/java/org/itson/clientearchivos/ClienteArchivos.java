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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class ClienteArchivos {

    private static final int TAMANIO_PAQUETE = 1012;
    private static final int PUERTO = 7;
    private static final int DELAY = 1000;

    private static final String DIRECCION_SERVIDOR = "localhost";

    private static final String cadenaDirectorioSalida = "C:\\Users\\mario\\3D Objects\\ArchivosRecibidos\\Archivos";
    private static final String cadenaRutaSalida = "ArchivoRecibido.rar";

    private static Integer numeroPaquetesRealArchivo = 0;
    private static Integer numeroPaquetesRealTanda = 0; 

    private static InetAddress direccionRemitente;
    private static int puertoRemitente = 0;

    public static void main(String[] args) {
        String archivoSeleccionado = seleccionarArchivo();
        try {
            solicitarArchivo(archivoSeleccionado);
        } catch (Exception ex) {
            
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
        ArrayList<Integer> paquetesRecibidosTanda = new ArrayList<>();
        ArrayList<Integer> paquetesRecibidosTotalArchivo = new ArrayList<>();
        DatagramSocket datagramSocket = new DatagramSocket();
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

        boolean SEGUIR_RECIBIENDO_TANDAS = true;
        while (SEGUIR_RECIBIENDO_TANDAS) {
            try {
                boolean CONTINUA_RECIBIENDO = true;
                while (CONTINUA_RECIBIENDO) {
                    recibirArchivo(datagramSocket, raf, paquetesRecibidosTanda, paquetesRecibidosTotalArchivo);
                }
                raf.close();
            } catch (IOException ex) {
                
            }
            boolean CONTINUA_PIDIENDO = true;
            do {
                byte[] paquetesRecibidosEnviar = convertirArregloBytes(paquetesRecibidosTanda);
                System.out.println("El numero de paquetes recibidos por esa tanda es de "+paquetesRecibidosTanda.size());
                DatagramPacket acknowledgmentPacket = new DatagramPacket(paquetesRecibidosEnviar, paquetesRecibidosEnviar.length, direccionRemitente, puertoRemitente);
                datagramSocket.send(acknowledgmentPacket);
                if (paquetesRecibidosTanda.size() != numeroPaquetesRealTanda) {
                    boolean CONTINUA_RECIBIENDO = true;
                    try {
                        while (CONTINUA_RECIBIENDO) {
                            recibirArchivo(datagramSocket, raf, paquetesRecibidosTanda, paquetesRecibidosTotalArchivo);
                        }
                    } catch (Exception ex) {
                        System.out.println("Recibidos: " + paquetesRecibidosTotalArchivo.size() + " - Real: " + numeroPaquetesRealArchivo);
                    }
                } else {
                    CONTINUA_PIDIENDO = false;
                }
            } while (CONTINUA_PIDIENDO);
            paquetesRecibidosTanda.clear();
            if (paquetesRecibidosTotalArchivo.size() == numeroPaquetesRealArchivo) {
                SEGUIR_RECIBIENDO_TANDAS = false;
            }
        }
    }

    private static void recibirArchivo(DatagramSocket datagramSocket, RandomAccessFile raf, ArrayList<Integer> paquetesRecibidosTanda, ArrayList<Integer> paquetesRecibidosTotalArchivo) throws Exception {
        byte[] datosRecibidos = new byte[TAMANIO_PAQUETE + Integer.BYTES * 3];

        DatagramPacket paquete = new DatagramPacket(datosRecibidos, datosRecibidos.length);
        datagramSocket.receive(paquete);
        direccionRemitente = paquete.getAddress();
        puertoRemitente = paquete.getPort();

        int tamanioPaquete = paquete.getLength();

        byte[] chunk = new byte[tamanioPaquete - Integer.BYTES * 3];

        System.arraycopy(datosRecibidos, 0, chunk, 0, tamanioPaquete - Integer.BYTES * 3);

        byte[] ultimosDoceBytes = Arrays.copyOfRange(datosRecibidos, tamanioPaquete - Integer.BYTES * 3, tamanioPaquete);

        int numeroPaquete = ByteBuffer.wrap(ultimosDoceBytes, 0, Integer.BYTES).getInt();

        int totalPaquetes = ByteBuffer.wrap(ultimosDoceBytes, 4, Integer.BYTES).getInt();

        int totalTamanioArchivo = ByteBuffer.wrap(ultimosDoceBytes, 8, Integer.BYTES).getInt();

        int offset = numeroPaquete * TAMANIO_PAQUETE;

        raf.seek(offset);
        raf.write(chunk);
        paquetesRecibidosTanda.add(numeroPaquete);
        paquetesRecibidosTotalArchivo.add(numeroPaquete);
        numeroPaquetesRealArchivo = totalTamanioArchivo;
        numeroPaquetesRealTanda = totalPaquetes;
    }

    private static byte[] convertirArregloBytes(ArrayList<Integer> paquetesRecibidos) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(paquetesRecibidos.size() * Integer.BYTES);
        for (int num : paquetesRecibidos) {
            byteBuffer.putInt(num);
        }
        return byteBuffer.array();
    }

}
