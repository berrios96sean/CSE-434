import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List; 
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner; 
import java.io.Serializable; 

public class Customer {

//#region Local Variables 

    // Customer Variables 
    public static InetAddress customerIP;
    public static String myIp; 
    public static int customerPort; 
    public static DatagramSocket mySocket; 
    public static String customerName; 
    public static Double customerBalance; 

    // Bank Variables 
    public static String bankIpString; 
    public static int bankPortNum; 

    // HashMap to track cohort info 
    public static Map<String,Object> hMap = new HashMap<>();
    public static ArrayList<Map<String,Object>> cohort = new ArrayList<>(); 


/** 
 * @param args
 */
//#endregion

//#region Main Method 

    // Clean up this main and make it less cluttered 
    public static void main(String[] args) {

        // Initialize string command 
        String command = " ";
        boolean exitCustomer = false; 

        // Check to see that commands were actually issued 
        if (args.length == 0)
        {
            System.out.println("No commands issued please ensure you issue a command");
            // Exit program do not return an error 
            System.exit(0);
        }
        else
        {
            command = args[0]+" ";
        }
        
        // Variable will stop program from continuing if Bank info was not entered 
        boolean bankInfoSet = false; 
        
        // ip is for general3 may need to change if using a different server. Can assign from the command line if needed 
        // General3 Server IP "10.120.70.105";
        // I will be using port 45000 for the host in all cases. 
        // assigned ports are between 45000-45499 for group number 89 
        // int port = 45000;

        // Initialize message variable 
        String message = "";

        if (command.equals("set-bank-info "))
        {
            bankIpString = args[1];
            bankPortNum = Integer.parseInt(args[2]); 
            bankInfoSet = true; 
        }
        if (bankInfoSet == false)
        {
            System.out.println("Please enter Bank Info before issuing any commands");
            // Exit Program Do not rerurn an error 
            System.exit(0);
        }
        else
        {
            // Continue to listen for commands until the customer exits the bank 
            while (exitCustomer == false)
            {
                exitCustomer = startApplication(command, message, exitCustomer);
                
            }
        }
       
    }

//#endregion

//#region Helper Methods 

    public static boolean startApplication(String command, String message, boolean exitCustomer)
    {
            // Create a scanner to get the message for the command 
            Scanner scanner = new Scanner(System.in); 
            System.out.println("Please Enter a Command to Send to the Bank");
            message = scanner.nextLine(); 

            // parse the message 
            String[] parsedMessage = message.split(" ");
            command = parsedMessage[0]+" ";
            // Need to create a socket so that customer listens at its port number for p2p communication 
            // need to ensure that this stays open to keep track of customers info and status of cohort assignments 
            if (command.equals("open "))
            {
                String customer = parsedMessage[1]+" ";
                String arg2 = parsedMessage[2]+" ";
                String ip4Adress = parsedMessage[3]+" ";
                myIp = parsedMessage[3];
                String porta = parsedMessage[4]+" ";
                String portb = parsedMessage[5]+" ";
                customerPort = Integer.parseInt(parsedMessage[5]);
                message = command + customer + arg2 + ip4Adress + porta + portb;
                sendOpenMessage(bankIpString, bankPortNum,message);
                return false;
            }
            if (command.equals("new-cohort "))
            {
                String customer = parsedMessage[1]+" ";
                String arg2 = parsedMessage[2]+" ";
                message = command + customer + arg2;
                newCohortMessage(bankIpString, bankPortNum,message,customer);
                return false;
            }
            if (command.equals("delete-cohort "))
            {
                String customer = parsedMessage[1]+" ";
                message = command + customer; 
                sendMessage(bankIpString, bankPortNum,message);
                try
                {
                    
                    String request = listen(customerIP, customerPort);
                    if (request.equals("delete-cohort"))
                    {
                        cohort.clear();
                        message = "SUCCESS";
                    }
                    else
                    {
                        message = "FAILURE: cohort not cleared";
                    }
                    sendMessage(bankIpString, bankPortNum,message);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                return false;
            }
            if (command.equals("exit "))
            {

                // This will close the customer service 
                String customer = parsedMessage[1];
                message = command + customer; 
                sendMessage(bankIpString, bankPortNum, message);
                // After sending message wait for response from the bank 
                String response = listen(customerIP, customerPort);
                if (response.equals("SUCCESS"))
                {
                    scanner.close(); 
                    return true; 
                }
                else 
                {
                    System.out.println("Customer Not Deleted");
                    return false;
                }
                
                
            }
            // will send a message to Bank to close the bank service 
            if (command.equals("close-bank "))
            {
                message = command; 
                sendMessage(bankIpString, bankPortNum,message);
                return false;
            }
            // After opening a bank if not issuing a cohort command issue this command to ensure 
            // Cohort info is received 
            if (command.equals("listen-for-cohort "))
            {
                listenForCohort(customerIP, customerPort);
                //printCohort();
                return false;
            }
            if (command.equals("listen "))
            {
                //String customer = parsedMessage[1]+" ";
                try
                {
                    String request = listen(customerIP, customerPort);
                    if (request.equals("delete-cohort"))
                    {
                        cohort.clear();
                        message = "SUCCESS";
                    }
                    else
                    {
                        message = "FAILURE: cohort not cleared";
                    }
                    sendMessage(bankIpString, bankPortNum,message);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                

                return false;
            }
            if (command.equals("print-cohort "))
            {
                printCohort();
                return false; 
            }
            if (command.equals("Test "))
            {
                message = command; 
                sendMessage(bankIpString, bankPortNum, message);
            }
            return false;
    }
    
    // This function will be used to listen to messages after a customer has opened an account at the port specified by the customer 
    public static String listen(InetAddress ip,int port)
    {
        String result = "";
        //boolean listening = true; 
        try
        {
            
            DatagramSocket socket = new DatagramSocket(port,ip);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer,buffer.length);

            System.out.println("Customer: "+customerName+" is listening at port: "+port);

            socket.receive(packet);
            String message = new String(packet.getData(),0,packet.getLength());
            System.out.println("Received Message: "+message);
            result = message; 
            System.out.println("|"+result+"|");

            socket.close(); 
            return result; 
        }
    
            catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println(result);
        return result; 
    }

    public static void listenForCohort(InetAddress ip, int port)
    {
        cohort = receiveList(ip, port);
    }
   
    // This function is for commands that do not need a response. 
    // Main purpose for this function is to close the bank using 'close-bank' command
    public static void sendMessage(String ip, int port,String message)
    {
        try 
        {
            // Set up Socket 
            DatagramSocket bankSocket = new DatagramSocket();
            // Prepare a packet to send a Message 
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

            // Send the message via packet
            bankSocket.send(packet);
            System.out.println("Sent Message: "+ message);
            bankSocket.close();
            
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    // This function is for commands that do not need a response. 
    // Main purpose for this function is to close the bank using 'close-bank' command
    public static void newCohortMessage(String ip, int port,String message,String customer)
    {
        try 
        {
            // Set up Socket 
            DatagramSocket bankSocket = new DatagramSocket();
            // Prepare a packet to send a Message 
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

            // Send the message via packet
            bankSocket.send(packet);
            System.out.println("Sent Message: "+ message);
            bankSocket.close();

            // will wait for a response from the Bank as a message before closing 
            // The message displays the result of the operation 
            customerIP = InetAddress.getByName(myIp);
            mySocket = new DatagramSocket(customerPort,customerIP);
            byte[] myBuffer = new byte[1024];
            DatagramPacket myPacket = new DatagramPacket(myBuffer, myBuffer.length);
            mySocket.receive(myPacket);
            receiveMessage(myPacket);
            mySocket.close();
            

            // Create a socket to receive the hashmap data structure 
            cohort = receiveList(customerIP, customerPort);
            // Print the received hashmap 
            //printCohort();


            // send cohort info to remaining customers 
            sendListToCohort(customer); 
            
            
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    // Adding listening functionality for new-cohort commands that may involve the customer will close after receipt
    public static void sendOpenMessage(String ip, int port,String message)
    {
        try 
        {
            // Set up Socket 
            DatagramSocket bankSocket = new DatagramSocket();
            // Prepare a packet to send a Message 
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

            // Send the message via packet
            bankSocket.send(packet);
            System.out.println("Sent Message: "+ message);
            bankSocket.close();

            // will wait for a response from the Bank before closing 
            customerIP = InetAddress.getByName(myIp);
            mySocket = new DatagramSocket(customerPort,customerIP);
            byte[] myBuffer = new byte[1024];
            DatagramPacket myPacket = new DatagramPacket(myBuffer, myBuffer.length);
            
            mySocket.receive(myPacket);
            receiveMessage(myPacket);
            mySocket.close();


            //mySocket.receive(myPacket);
            //receiveHashMap(myPacket);
            //mySocket.close(); 
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    // Print contents of the hashmap, for this case the cohort result, 
    public static void printMap()
    {
        for (String key : hMap.keySet())
        {
            System.out.println(key + " "+hMap.get(key));
        }
    }

    // Print contents of the hashmap, for this case the cohort result, 
    public static void printCohort()
    {
        for (Map<String,Object> lItem : cohort)
        {
            System.out.println(lItem.toString());
        }
    }

    // Process a packet as a message to make implementation easier 
    public static void receiveMessage(DatagramPacket packet)
    {
        String message = new String(packet.getData(),0,packet.getLength());
        System.out.println("Received Message: "+message);
    }

    // Process a Message as a HashMap To make Implementation easier 
    public static Map<String, Map<String,Object>> receiveHashMap(InetAddress ip, int port)
    {
        Map<String,Map<String,Object>> tempMap = null; 
        try
        {
            DatagramSocket socket = new DatagramSocket(port, ip);
            DatagramPacket packet = new DatagramPacket(new byte[1024],1024);
            socket.receive(packet);

            byte[] mapData = packet.getData();
            ByteArrayInputStream byteInput = new ByteArrayInputStream(mapData);
            ObjectInputStream objInput = new ObjectInputStream(byteInput);
            tempMap = (HashMap<String, Map<String,Object>>) objInput.readObject();

            socket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace(); 
        }

        return tempMap; 

    }

    // This function recieves a packet as an array list 
    public static ArrayList<Map<String,Object>> receiveList(InetAddress ip, int port)
    {
        ArrayList<Map<String,Object>> tempList = null; 
        try
        {
            DatagramSocket socket = new DatagramSocket(port, ip);
            DatagramPacket packet = new DatagramPacket(new byte[1024],1024);
            socket.receive(packet);

            byte[] listData = packet.getData();
            ByteArrayInputStream byteInput = new ByteArrayInputStream(listData);
            ObjectInputStream objInput = new ObjectInputStream(byteInput);
            tempList = (ArrayList<Map<String,Object>>) objInput.readObject();

            socket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace(); 
        }

        return tempList; 
    }

    // Sends an array list packet to all other customers in the cohort and also ensure that the 
    // Data Structure is locked during the process
    public static void sendListToCohort(String customer)
    {
        // Lock the cohort data structure so no one else can access it 
        synchronized(cohort)
        {
            // Create data structure of the cohort without the initiating customer 
            List<Map<String,Object>> remainingCohort = new ArrayList<>(); 
            for (Map<String,Object> member : cohort)
            {
                String name = (String) member.get("name");
                if (name.equals(customer) == false)
                {
                    remainingCohort.add(member); 
                }
            }

            for (Map<String,Object> mem : remainingCohort)
            {
                try
                {
                    String ipAddy = (String) mem.get("ipv4_Address");
                    int port = (int) mem.get("portb");
                    DatagramSocket socket = new DatagramSocket();
                    InetAddress address = InetAddress.getByName(ipAddy); 

                    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                    ObjectOutputStream objOutput = new ObjectOutputStream(byteOutput);
                    objOutput.writeObject(cohort);
                    byte[] listData = byteOutput.toByteArray();
                    DatagramPacket packet = new DatagramPacket(listData, listData.length, address,port);
                    socket.send(packet); 
                    socket.close();

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

//#endregion

}
