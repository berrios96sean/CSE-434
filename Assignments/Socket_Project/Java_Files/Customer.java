import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Customer {
    public static void main(String[] args) {

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        sendMessage(ip, port);
    }

    public static void sendMessage(String ip, int port)
    {
        try 
        {
            // Set up Socket 
            DatagramSocket socket = new DatagramSocket();

            // Prepare a packet to send a Message 
            String message = "What up hoe";
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

            // Send the message via packet
            socket.send(packet);
            System.out.println("Sent Message: "+ message);

            socket.close();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
