package es.um.redes.nanoFiles.udp.message;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;

public class MiniDirectoryServer {                     //Haciendo funciones de NFDirectoryServer

    public static void main(String[] args) throws Exception {

        DatagramSocket socket = new DatagramSocket(6868);

        byte[] buf = new byte[1280];
        DatagramPacket in = new DatagramPacket(buf, buf.length);

        while (true) {
            // 1) Recibir datagrama
        	System.out.println("MiniDirectoryServer.main(): Esperando un datagrama....");
            socket.receive(in);
            System.out.println("¡Datagrama recibido!");
            
            // 2) DatagramPacket -> String
            String reqStr = new String(in.getData(), 0, in.getLength());

            // 3) String -> DirMessage
            DirMessage req = DirMessage.fromString(reqStr);  // parseo field:value 
            System.out.println("DirMessage recibido: \n" + reqStr);
            
            // 4) Procesar operación y construir respuesta
            DirMessage resp;
            if (DirMessageOps.OPERATION_PING.equals(req.getOperation())) {
            	if (req.getProtocolId().equals(NanoFiles.PROTOCOL_ID))
            			resp = new DirMessage(DirMessageOps.OPERATION_PING_OK);
            	else {
            			resp = new DirMessage(DirMessageOps.OPERATION_PING_ERROR);
            			System.err.println("ERROR: Protocolo no compatible. Servidor: "+NanoFiles.PROTOCOL_ID+" - Recibido: "+req.getProtocolId());
            	}
            } else {
                resp = new DirMessage(DirMessageOps.OPERATION_INVALID);
            }

            // 5) DirMessage -> String -> byte[] -> DatagramPacket -> send
            String respStr = resp.toString();     // serializa respuesta 
            System.out.println("DirMessage respuesta a enviar: " + respStr);
            byte[] outData = respStr.getBytes();

            InetSocketAddress clientAddr = new InetSocketAddress(in.getAddress(), in.getPort());
            DatagramPacket out = new DatagramPacket(outData, outData.length, clientAddr);
            socket.send(out);

            // Preparar el paquete para la siguiente recepción
            in.setLength(buf.length);
        }
    }
}