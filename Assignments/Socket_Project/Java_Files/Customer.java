import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Customer {
    public static void main(String[] args) {

        String command = args[0]+" ";
        
        String ip = "127.0.0.1";
        int port = 12345;
        String message = "";
        System.out.println(command);
        if (command.equals("open "))
        {
            String customer = args[1]+" ";
            String arg2 = args[2]+" ";
            String ip4Adress = args[3]+" ";
            String porta = args[4]+" ";
            String portb = args[5]+" ";
            message = command + customer + arg2 + ip4Adress + porta + portb;
            sendMessage(ip, port,message);
        }
        if (command.equals("new-cohort "))
        {
            String customer = args[1]+" ";
            String arg2 = args[2]+" ";
            message = command + customer + arg2;
            sendMessage(ip, port,message);
        }
        if (command.equals("delete-cohort ")||command.equals("exit "))
        {
            String customer = args[1]+" ";
            message = command + customer;
            sendMessage(ip, port,message);
        }
        // This will send a message to Bank to close the bank service 
        if (command.equals("close-bank "))
        {
            message = command; 
            sendMessage(ip, port, message);
        }
        
    }

    public static void sendMessage(String ip, int port,String message)
    {
        try 
        {
            // Set up Socket 
            DatagramSocket socket = new DatagramSocket();

            // Prepare a packet to send a Message 
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
