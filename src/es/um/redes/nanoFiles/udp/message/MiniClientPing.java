package es.um.redes.nanoFiles.udp.message;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import es.um.redes.nanoFiles.application.NanoFiles;
// Importa tu DirMessage y constantes de operación del proyecto
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;

public class MiniClientPing {               //Haciendo funciones del Directory_Connector

    public static void main(String[] args) throws Exception {

        InetSocketAddress dirAddr = new InetSocketAddress("127.0.0.1", 6868);
        DatagramSocket socket = new DatagramSocket();

        // 1) Crear el mensaje del protocolo
        DirMessage dm = new DirMessage(DirMessageOps.OPERATION_PING);  // operación "ping" 
        dm.setProtocolID(NanoFiles.PROTOCOL_ID); // campo protocolId que el boletín menciona en DirMessage 

  
        // 2) DirMessage -> String (ASCII field:value)
        String msg = dm.toString();  // toString() genera líneas field:value + fin de mensaje 
        System.out.println("DirMessage a enviar: "+ msg);
        // 3) String -> byte[]
        byte[] data = msg.getBytes();

        // 4) byte[] -> DatagramPacket -> send
        DatagramPacket out = new DatagramPacket(data, data.length, dirAddr);
        socket.send(out);

        // 5) Recibir respuesta (DatagramPacket -> byte[] -> String -> DirMessage)
        byte[] buf = new byte[1280];
        DatagramPacket in = new DatagramPacket(buf, buf.length);
        socket.receive(in);

        String respStr = new String(in.getData(), 0, in.getLength());
        System.out.println("DirMessage de respuesta: "+ respStr);
        DirMessage resp = DirMessage.fromString(respStr); // fromString() reconstruye el objeto 
        
        System.out.println("Respuesta operation = " + resp.getOperation());
        socket.close();
    }
}
