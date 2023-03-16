import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List; 
import java.util.Random; 

public class Bank {

//#region Local Variables 

    // This is a hashmap within hashmap that contains the Customers info that have opened an account
    public static Map<String,Map<String,Object>> customers = new HashMap<>(); 

    // Hash map for cohorts
    // Using this to create a map with issuing customers name as key. With Arraylist of the cohort as the value. 
    // Each customer will still be searchable within a cohort since a customer will only be allowed to be assigned to one cohort at once
    public static Map<String, List<Map<String,Object>>> cohorts = new HashMap<>(); 

    // Variables for controlling Bank Info 
    public static String bankIpString; 
    public static int bankPortNum; 


/** 
 * @param args
 */
//#endregion

//#region Main Function 

    // Need to clean this up at some point so that code is not cluttered inside main function 
    public static void main(String[] args) 
    {
        //Initializing the bank command so that I can set parameters for Bank Ip and Port Number 
        String bankCommand = " "; 
        boolean bankInfoSet = false;  
        if (args.length != 0)
        {
            bankCommand = args[0];
        }
        if (bankCommand.equals("set-bank-info"))
        {
            bankIpString = args[1];
            bankPortNum = Integer.parseInt(args[2]);
            bankInfoSet = true; 
        }
        if (bankInfoSet == false)
        {
            System.out.println("please set bank info prior to starting bank service.");
            System.exit(0);
        }
        else
        {
            communicate(); 
        }

    }

//#endregion

//#region Helper Methods 

    
    public static void communicate()
    {
        try 
        {
            // Set up address for bank 
            InetAddress address = InetAddress.getByName(bankIpString); 
            // assigned ports are between 45000-45499
	        int port = bankPortNum; 
            DatagramSocket socket = new DatagramSocket(port, address);
           
	        String ip = address.getHostAddress();  
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
                    String customer = parsedMessage[1];
                    double balance = Double.parseDouble(parsedMessage[2]);
                    String ip4 = parsedMessage[3];
                    int porta = Integer.parseInt(parsedMessage[4]);
                    int portb = Integer.parseInt(parsedMessage[5]);
                    String result = open(customer,balance,ip4,porta,portb);
                    
                    sendPacketAsMessage(portb, ip4, result);

                    // Test sending a hashmap as a packet -- delete or comment out this is not needed 
                    //ByteArrayOutputStream byteArray = new ByteArrayOutputStream(); 
                    //ObjectOutputStream objOutput = new ObjectOutputStream(byteArray);
                    //objOutput.writeObject(customers);
                    //byte[] mapData = byteArray.toByteArray();
                    //custPacket = new DatagramPacket(mapData,mapData.length, custAddress,custPort);
                    //customerSocket.send(custPacket);
                    
                    //customerSocket.close(); 
                }
                if (command.equals("new-cohort"))
                {
                    System.out.println("NEW-COHORT");
                    String customer = parsedMessage[1];
                    int portCustomer = Integer.parseInt(parsedMessage[2]);
                    String result = makeCohort(customer, portCustomer);
                    String ipAddress = getCustomrIP(customer);
                    InetAddress addy = InetAddress.getByName(ipAddress);
                    System.out.println("Customer IP is: "+ipAddress);
                    int portNumber = getCustomrPort(customer);
                    System.out.println("port: "+portNumber);
                    sendPacketAsMessage(portNumber, ipAddress, result);
                    // Send cohort as Arraylist 
                    sendPacketAsArrayList(addy, portNumber, customer);
                }
                if (command.equals("delete-cohort"))
                {
                    String customer = parsedMessage[1];
                    System.out.println("DELETECOHORT");
                    // Send message to each member of cohort that initiates the delete cohort method
                    //socket.receive(packet);
                 
                    // Process incoming message
                    //message = new String(packet.getData(), 0, packet.getLength());
                    //System.out.println("Received message: " + message);
                    deleteCohort(customer,socket);
                }
                if (command.equals("exit"))
                {
                    System.out.println("EXIT");
                    String customer = parsedMessage[1];
                    System.out.println(customer);
                    //printCustomerMap();

                    // rick is just a placeholder name, it is not significant 
                    Map<String,Object> rick = customers.get(customer);
                    if (rick == null)
                    {
                        System.out.println("NOT FOUND");
                    }
                    String cIP = (String) rick.get("ipv4_Address");
                    int cPort = (int) rick.get("portb");
                    String response = deleteCustomer(customer);
                    //String cIP = getCustomrIP(customer);
                    //System.out.println(cIP);
                    //int cPort = getCustomrPort(customer);
                    //System.out.println(cPort);
                    sendPacketAsMessage(cPort, cIP, response);
                }
                // This command will be to close the bank service with a call from the customer 'close-bank'
                if (command.equals("close-bank"))
                {
                    listening = false;
                    break;
                }
                // Command for testing purposes 
                if (command.equals("Test"))
                {
                    System.out.println("CUSTOMER MAP: ");
                    printCustomerMap();
                    System.out.println("COHORT MAP: ");
                    printCohortMap();
                }
                
            }
            socket.close();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
    
    // Delete cohort method 
    public static String deleteCohort(String customer, DatagramSocket bankSocket)
    {
        // Get an array list of the cohort for the customer 
        // no need to lock since sending to only one customer at a time
        ArrayList<Map<String,Object>> cohortList = getCohortList(customer);
        String message = "delete-cohort";
        System.out.println("TEST: "+ message);
        // Iterate through array list send message to each member to delete 
        for (Map<String,Object> member : cohortList)
        {
            try
            {
                String ipAddy = (String) member.get("ipv4_Address");
                int port = (int) member.get("portb");
                DatagramSocket socket = new DatagramSocket();
                InetAddress address = InetAddress.getByName(ipAddy); 

                // Prepare a packet to send a Message 
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

                socket.send(packet); 
                socket.close();

                // Listen for response 
                byte[] responseBuff = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(responseBuff, responseBuff.length);
                bankSocket.receive(responsePacket);
                String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                System.out.println("Received message: " + response);
                if (response == "FAILURE: cohort not cleared")
                {
                    
                    return "FAILURE";
                }
                socket.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return "SUCCESS";
    }

    public static String listen(InetAddress ip,int port)
    {
        String result = "";
        //boolean listening = true; 
        try
        {
            
            DatagramSocket socket = new DatagramSocket(port, ip);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer,buffer.length);

            System.out.println("Bank is listening at port: "+port);

            socket.receive(packet);
            String message = new String(packet.getData(),0,packet.getLength());
            System.out.println("Received Message: "+message);
            result = message; 

        socket.close(); 
        }
    
            catch (Exception e)
        {
            e.printStackTrace();
        }

        return result; 

    }

    // Sends a packer as a message that can be printed to terminal 
    public static void sendPacketAsMessage(int port, String ip, String result)
    {
        try
        {
            // Create a customer socket to send message 
            // Set up a UDP socket on a specific IP address and port
            // assigned ports are between 45000-45499
            int custPort = port; 
            DatagramSocket customerSocket = new DatagramSocket();
            // Create a packet to send to the customer 
                // Prepare a packet to send a Message 
            //printCustomerMap();  
            byte[] custBuffer = result.getBytes();
            InetAddress custAddress = InetAddress.getByName(ip);
            DatagramPacket custPacket = new DatagramPacket(custBuffer, custBuffer.length, custAddress, custPort);
            customerSocket.send(custPacket);
            System.out.println("Sent Message: "+result);
            customerSocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    // Return a string for the result of the operation
    // minimum cohort size is greater than or equal to two
    public static String makeCohort(String customer, int sizeOfCohort)
    {
        // Determine if a failure should occur 
        if (sizeOfCohort < 2)
        {
            return "FAILURE: Not enough customers in Bank";
        }
        if (customers.containsKey(customer) == false)
        {
            return "FAILURE: Customer is not in database ";
        }
        if (sizeOfCohort > customers.size())
        {
            System.out.println(customers.size());
            return "FAILURE: Requsted Cohort size is greater than number of customers ";
        }
        
        // Lock the customers hashmap so that it cannot be processed by any other threads while a cohort is being formed 
        synchronized(customers)
        {
            // make cohorts as an arraylist that will be able to be accessed via a hashmap 
            List<Map<String,Object>> cohortList = new ArrayList<>(); 

            // Create a random object to get random customers to the list 
            Random rand = new Random(); 
            
            // add customer initiating the call to the cohort 
            Map<String,Object> initialCustomer = customers.get(customer);
            if (initialCustomer == null)
            {
                return "Failure: initial customer null";
            }
            cohortList.add(initialCustomer);

            // To get random customers initiate a list of customers with the initial customer removed 
            List<String> remainingCustomers = new ArrayList<>(customers.keySet()); 
            remainingCustomers.remove(customer);

            // Add random remaining customers to the cohort 
            while (cohortList.size() < sizeOfCohort)
            {
                // Get a random index and get the name of the customer at that index 
                int i = rand.nextInt(remainingCustomers.size());
                String name = remainingCustomers.get(i);

                // Get the Map from the Customers Hashmap data structure for the named customer 
                Map<String,Object> selectedCustomer = customers.get(name);
                if (selectedCustomer == null)
                {
                    return "Failure: selected customer null";
                }
                System.out.println(selectedCustomer.get("in_Cohort"));
                // Check that selected customer wasn't already added and that they are not already assigned to a cohort 
                if (!cohortList.contains(selectedCustomer) && !(Boolean) selectedCustomer.get("in_Cohort"))
                {
                    cohortList.add(selectedCustomer);
                    remainingCustomers.remove(i);
                    selectedCustomer.put("in_Cohort",true);
                    System.out.println("TEST");
                }

            }

                // Add newly formed cohort list to cohorts hashmap while datastructure is locked 
                cohorts.put(customer, cohortList);
                // For testing print cohort map after creation 
                //printCohortMap(); 
                return "SUCCESS";
        }


    }

    // Return a string for the result of the operation 
    public static String deleteCustomer(String customer)
    {
        // Lock data structure 
        synchronized (customers)
        {
            if (customers.containsKey(customer))
            {
                customers.remove(customer);
                return "SUCCESS";
            }
        }
        return "FAILURE";
    }

    // This function processes an open command and returns the result to the customer. 
    // Implementing as a String so that it can be used later when sending a packet and reduce clutter 
    public static String open(String customerName, double balance, String ipv4Address, int porta, int portb) {
        
        if (customers.containsKey(customerName)) {
            return "FAILURE"; 
        }
    
        // create individual map for new customer 
        Map<String, Object> customer = new HashMap<>();
        customer.put("name",customerName);
        customer.put("balance", balance);
        customer.put("ipv4_Address", ipv4Address);
        customer.put("porta", porta);
        customer.put("portb", portb);
        //This ensures a customer can only be added to a single cohort 
        customer.put("in_Cohort",false);

        // add new customer to the customers map
        customers.put(customerName, customer);
    
        return "SUCCESS";
    }

    // This function is used for testing so that I can verify the contents of the hashmap
    public static void printCustomerMap()
    {
        for (String key : customers.keySet())
        {
            System.out.println(key + " "+customers.get(key));
        }
    }
    
    // This function is used for testing so that I can verify the contents of the hashmap
    public static void printCohortMap()
    {
        for (String key : cohorts.keySet())
        {
            System.out.println(key + " "+cohorts.get(key));
        }
    }

    // Itererates through list of cohorts and gets the cohort for any customer that is assigned to the cohort returns as arraylist
    public static ArrayList<Map<String,Object>> getCohortList(String customer)
    {
        ArrayList<Map<String,Object>> tempList = null; 
        for (Map.Entry<String,List<Map<String,Object>>> listEntry : cohorts.entrySet())
        {
            List<Map<String,Object>> cohortList = listEntry.getValue();
            for (Map<String,Object> cust : cohortList)
            {
                tempList = new ArrayList<>(cohortList);
                break; 
            }
            if (tempList != null)
            {
                break; 
            }
        }
        return tempList; 
    }
    
    //#region Customers HashMap Helper Methods 

    public static void sendPacketAsArrayList(InetAddress ip, int port,String customer)
    {
        try
        {
            DatagramSocket socket = new DatagramSocket();
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ObjectOutputStream objOutput = new ObjectOutputStream(byteOutput);
            ArrayList<Map<String,Object>> aListToSend = getCohortList(customer);
            objOutput.writeObject(aListToSend);
            byte[] buffer = byteOutput.toByteArray(); 
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, port);
            socket.send(packet);
            socket.close();
    
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendPacketForCohort(InetAddress ip, int port,String customer)
    {
        try
        {
            DatagramSocket socket = new DatagramSocket();
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ObjectOutputStream objOutput = new ObjectOutputStream(byteOutput);
            ArrayList<Map<String,Object>> aListToSend = getCohortList(customer);
            objOutput.writeObject(aListToSend);
            byte[] buffer = byteOutput.toByteArray(); 
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, port);
            socket.send(packet);
            socket.close();
    
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String getCustomrIP(String customer)
    {
        // Create a Map instance of customer info 
        Map<String,Object> customerInfo = customers.get(customer); 
        if (customerInfo == null)
        {
            return "Customer not found";
        }
        if(customer!=null)
        {
            return (String) customerInfo.get("ipv4_Address");
        }

        return "Customer not found in Database";
    }

    public static int getCustomrPort(String customer)
    {
        // Create a Map instance of customer info 
        Map<String,Object> customerInfo = customers.get(customer); 
        if(customer!=null)
        {
            return (int) customerInfo.get("portb");
        }

        return -1;
    }

    public static int getBankPort(String customer)
    {
        // Create a Map instance of customer info 
        Map<String,Object> customerInfo = customers.get(customer); 
        if(customer!=null)
        {
            return (int) customerInfo.get("porta");
        }

        return -1;
    }

    //#endregion

//#endregion

}
