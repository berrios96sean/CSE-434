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
    public static String customerName = ""; 
    public static Double customerBalance = 0.0; 

    // Bank Variables 
    public static String bankIpString; 
    public static int bankPortNum; 

    // HashMap to track cohort info 
    public static Map<String,Object> hMap = new HashMap<>();
    public static ArrayList<Map<String,Object>> cohort = new ArrayList<>(); 

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

    /***
     * Contains the communication with stdin to send signals to the customer themselves
     * the Bank or other Customers. 
     * @param command 
     * @param message
     * @param exitCustomer
     * @return
     */
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
                setCustName(customer);
                String arg2 = parsedMessage[2]+" ";
                setBalance(Double.parseDouble(arg2));
                String ip4Adress = parsedMessage[3]+" ";
                setCustIP(parsedMessage[3]);
                String porta = parsedMessage[4]+" ";
                String portb = parsedMessage[5]+" ";
                customerPort = Integer.parseInt(parsedMessage[5]);
                setCustPort(customerPort);
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
            if (command.equals("deposit "))
            {
                deposit(Double.parseDouble(parsedMessage[1]));
            }
            if (command.equals("withdrawl "))
            {
                withdrawl(Double.parseDouble(parsedMessage[1]));
            }
            if (command.equals("transfer "))
            {
                System.out.println("placeholder for transfer");
                transfer(parsedMessage[1]);
                // create a local channel with the receiver and store the old cohort locally with the label ID
                // update the cohort info with the new balances locally 
                // decrement local balance 
                // send transfer message and wait for response 
                // if response is success then G2G
                // if response is unsuccessful will need to perform a checkpoint and rollback 
            }
            if (command.equals("listen-for-transfer "))
            {
                System.out.println("Listening for a transfer at Port: "+getCustPort());
                //String customer = parsedMessage[1]+" ";
                try
                {
                    String request = listen(customerIP, customerPort);
                    System.out.println(request);

                    //sendMessage(bankIpString, bankPortNum,message);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                

                return false;
            }
            if (command.equals("lost-transfer "))
            {
                System.out.println("Place holder for lost-transfer");
            }
            if (command.equals("checkpoint "))
            {
                System.out.println("Place holder for checkpoint");
            }
            if (command.equals("rollback "))
            {
                System.out.println("Place holder for rollback");
            }
            if (command.equals("create-dummy "))
            {
                createDummyCohort();
            }
            return false;
    }
    
    public static void transfer(String customer)
    {
        // get the Map object for customer transferring to 
        Map<String,Object> receiver = new HashMap<>(); 
        for (Map<String,Object> member : cohort)
        {
            String name = (String) member.get("name");
            if (name.equals(customer) == true)
            {
                receiver = member; 
            }
        }

        if (receiver == null)
        {
            System.out.println("Receiver not found or not set");
        }

        
        String ip = (String) receiver.get("ipv4_Address");
        System.out.println(ip);
        
    }

    /***
     * Used to listen for a delete-cohort message
     * @param ip
     * @param port
     * @return
     */
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

    /***
     * used to listen if customer is being added to cohorts 
     * @param ip
     * @param port
     */
    public static void listenForCohort(InetAddress ip, int port)
    {
        cohort = receiveList(ip, port);
    }
   
    /***
     * Send a message without a response 
     * @param ip
     * @param port
     * @param message
     */
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

    /***
     * create a new cohort 
     * @param ip
     * @param port
     * @param message
     * @param customer
     */
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

    /***
     * Sends a message to open a customer account
     * @param ip
     * @param port
     * @param message
     */
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
            //System.out.println("MY IP = |"+myIp+"|");
            customerIP = InetAddress.getByName(getCustIp());
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

    /***
     * print the hashmap of the cohort 
     */
    // Print contents of the hashmap, for this case the cohort result, 
    public static void printMap()
    {
        for (String key : hMap.keySet())
        {
            System.out.println(key + " "+hMap.get(key));
        }
    }

    /***
     * print the cohort 
     */
    // Print contents of the hashmap, for this case the cohort result, 
    public static void printCohort()
    {
        for (Map<String,Object> lItem : cohort)
        {
            System.out.println(lItem.toString());
        }
    }

    /***
     * process a packer as a message prints received message to console 
     * @param packet
     */
    // Process a packet as a message to make implementation easier 
    public static void receiveMessage(DatagramPacket packet)
    {
        String message = new String(packet.getData(),0,packet.getLength());
        System.out.println("Received Message: "+message);
    }

    /***
     * Receives a hashmap as a packet 
     * should update this implementation to take in a packet 
     * @param ip
     * @param port
     * @return
     */
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

    /***
     * process packet as an array list 
     * should update this implementation to take in a packet
     * @param ip
     * @param port
     * @return
     */
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

    /***
     * Sends an array list packet to all other customers in the cohort and also ensure that the 
     * Data Structure is locked during the process
     * @param customer
     */
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

//#region Setter Methods 

    /***
     * 
     * @param balance
     */
    public static void setBalance(Double balance)
    {
        customerBalance = balance; 
    }

    /***
     * 
     * @param name
     */
    public static void setCustName(String name)
    {
        customerName = name; 
    }

    /***
     * 
     * @param address
     */
    public static void setCustIP(String address)
    {
        myIp = address; 
    }

    /***
     * 
     * @param port
     */
    public static void setCustPort(int port)
    {
        customerPort = port; 
    }
//#endregion

//#region Getter Methods

    /***
     * 
     * @return
     */
    public static double getBalance()
    {
        return customerBalance; 
    }

    /***
     * 
     * @return
     */
    public static String getCustName()
    {
        return customerName;
    }

    /***
     * 
     * @return
     */
    public static String getCustIp()
    {
        return myIp; 
    }

    /***
     * 
     * @return
     */
    public static int getCustPort()
    {
        return customerPort; 
    }
//#endregion

//#region Customer Methods 

    public static void deposit(double amount)
    {
        double prevBalance = getBalance();
        System.out.println("Customer: "+getCustName()+" is Depositing $"+amount);
        System.out.println("Previous Balance for Customer: "+getCustName()+" was $"+getBalance());
        setBalance(prevBalance+amount);
        System.out.println("New Balance for Customer: "+getCustName()+" is now $"+getBalance());

    }

    public static void withdrawl(double amount)
    {
        double prevBalance = getBalance();
        System.out.println("Customer: "+getCustName()+" is Withdrawling $"+amount);
        System.out.println("Previous Balance for Customer: "+getCustName()+" was $"+getBalance());
        setBalance(prevBalance-amount);
        System.out.println("New Balance for Customer: "+getCustName()+" is now $"+getBalance());

    }

    /***
     * Creates a static cohort that i can use for testing 
     */
    public static void createDummyCohort() 
    {
        Map<String, Object> customer1 = new HashMap<>();
        customer1.put("name", "sean");
        customer1.put("balance", 100.0);
        customer1.put("ipv4_Address", "10.120.70.146");
        customer1.put("porta", 45000);
        customer1.put("portb", 45200);
        customer1.put("in_Cohort", false);
        cohort.add(customer1);

        Map<String, Object> customer2 = new HashMap<>();
        customer2.put("name", "rick");
        customer2.put("balance", 100.0);
        customer2.put("ipv4_Address", "10.120.70.113");
        customer2.put("porta", 45000);
        customer2.put("portb", 45200);
        customer2.put("in_Cohort", true);
        cohort.add(customer2);

        Map<String, Object> customer3 = new HashMap<>();
        customer3.put("name", "hank");
        customer3.put("balance", 100.0);
        customer3.put("ipv4_Address", "10.120.70.105");
        customer3.put("porta", 45000);
        customer3.put("portb", 45200);
        customer3.put("in_Cohort", true);
        cohort.add(customer3);
    }

//#endregion

}

class checkpoint
{
    /***
     * Constructor for creating a checkpoing 
     */
    public checkpoint()
    {

    }
}

class channel
{
    /***
     * Constructor for creating a channel 
     * Create a new channel only if a channel has not been established
     */
    public channel()
    {

    }
}