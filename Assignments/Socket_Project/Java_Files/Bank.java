import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Bank {
    public static Map<String,Object> customers = new HashMap<>(); 
    public static void main(String[] args) 
    {
        try 
        {
            // Set up a UDP socket on a specific IP address and port
            InetAddress address = InetAddress.getByName("127.0.0.1"); // replace with desired IP address
            int port = 12345; // replace with desired port number
            DatagramSocket socket = new DatagramSocket(port, address);
            
            // Listen for incoming messages
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            boolean listening = true;
            while (listening) 
            {
                socket.receive(packet);
                
                // Process incoming message
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received message: " + message);
                String[] parsedMessage = message.split(" ");
                String command = parsedMessage[0];
                if (command.equals("open"))
                {
                    System.out.println("OPEN");
                }
                if (command.equals("new-cohort"))
                {
                    System.out.println("NEW-COHORT");
                }
                if (command.equals("delete-cohort"))
                {
                    System.out.println("DELETECOHORT");
                }
                if (command.equals("exit"))
                {
                    System.out.println("EXIT");
                }
                // Stop listening when a message is received
                listening = false;
            }
            socket.close();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
