import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
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

//#endregion

//#region Main Function 

    /**
     * The purpose of this main is to start the Bank process to listen at a specific ip and port. If these things are not specified in 
     * the command line the Bank process will not start. 
     * @param args
     */
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

    /***
     * Receives the commands from the customer to perform operations intended 
     * to only be called in Main 
     */
    public static void communicate()
    {
        try 
        {
            // Set up address for bank 
            InetAddress address = InetAddress.getByName(bankIpString); 
            // assigned ports are between 45000-45499
	        int port = bankPortNum; 
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
                    String customer = parsedMessage[1];
                    double balance = Double.parseDouble(parsedMessage[2]);
                    String ip4 = parsedMessage[3];
                    int porta = Integer.parseInt(parsedMessage[4]);
                    int portb = Integer.parseInt(parsedMessage[5]);
                    String result = open(customer,balance,ip4,porta,portb);
                    
                    sendPacketAsMessage(portb, ip4, result);
                }
                if (command.equals("new-cohort"))
                {
                    String customer = parsedMessage[1];
                    int portCustomer = Integer.parseInt(parsedMessage[2]);
                    String result = makeCohort(customer, portCustomer);
                    String ipAddress = getCustomrIP(customer);
                    InetAddress addy = InetAddress.getByName(ipAddress);
                    int portNumber = getCustomrPort(customer);
                    sendPacketAsMessage(portNumber, ipAddress, result);
                    // Send cohort as Arraylist 
                    sendPacketAsArrayList(addy, portNumber, customer);
                }
                if (command.equals("delete-cohort"))
                {
                    String customer = parsedMessage[1];
                    System.out.println("DELETECOHORT");

                    // delete the customers cohorts
                    deleteCohort(customer,socket);

                    // delete the associated cohort with the bank 
                    deleteCohort2(customer);
                }
                if (command.equals("exit"))
                {
                    String customer = parsedMessage[1];
                    // rick is just a placeholder name, it is not significant 
                    Map<String,Object> rick = customers.get(customer);
                    if (rick == null)
                    {
                        System.out.println("NOT FOUND");
                    }
                    String cIP = (String) rick.get("ipv4_Address");
                    int cPort = (int) rick.get("portb");
                    String response = deleteCustomer(customer);
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
                if (command.equals("receive-checkpoint"))
                {
                    ArrayList<Map<String,Object>> tempList = new ArrayList<Map<String,Object>>();
                    String customer = parsedMessage[1]; 
                    System.out.println("Received a checkpoint from "+customer);
                    DatagramPacket packet2 = new DatagramPacket(new byte[1024],1024);
                    socket.receive(packet2);

                    byte[] listData = packet2.getData();
                    ByteArrayInputStream byteInput = new ByteArrayInputStream(listData);
                    ObjectInputStream objInput = new ObjectInputStream(byteInput);
                    tempList = (ArrayList<Map<String,Object>>) objInput.readObject();

                    updateCohortList(customer, tempList);
                    ArrayList<Map<String,Object>> updatedList = getCohortList(customer);

                    for (Map<String,Object> cust : tempList)
                    {
                            String name = (String) cust.get("name");
                            double balance = (double) cust.get("balance");

                            Map<String,Object> temp = customers.get(name);
                            temp.put("balance", balance);
                    }
                }
                if (command.equals("rollback"))
                {
                    String custIP = parsedMessage[1];
                    InetAddress custAddress = InetAddress.getByName(custIP);
                    int custPort = Integer.parseInt(parsedMessage[2]);
                    String custName = parsedMessage[3]; 
                    System.out.println("|"+custName+"|");
                    sendPacketAsArrayList(custAddress, custPort, custName);
                }
            }
            socket.close();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
    
    /***
     * Deletes cohorts in all customer processes in the cohort does not delete the cohort from 
     * the Bank 
     * @param customer customer name to get the correct cohort 
     * @param bankSocket pass in the current socket, since bank should already be listening continuosly 
     * @return 
     */
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
                System.out.println("IP = "+ipAddy);
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

    /***
     * this method deletes the cohort from the bank. Seperating to make calls seperate from each other 
     * to help with troubleshooting 
     * @param customerName customer name to get the correct cohort 
     */
    public static void deleteCohort2(String customerName) 
    {
        for (Map.Entry<String, List<Map<String,Object>>> entry : cohorts.entrySet()) 
        {
            List<Map<String,Object>> customers = entry.getValue();
            for (Map<String,Object> customer : customers) 
            {
                if (customer.get("name").equals(customerName)) 
                {
                    cohorts.remove(entry.getKey());
                    return;
                }
            }
        }
    }

    /***
     * listen at ip and port 
     * @param ip pass in as an InetAddress
     * @param port port number passed in as int. 
     * @return
     */
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

    /***
     * sends a packet as a message 
     * @param port passed in as an int
     * @param ip passed in as a string
     * @param result this is the message that is being send pass in as a string 
     */
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

    /***
     * makes a cohort of n customers for the bank 
     * @param customer name of customer initiating cohort
     * @param sizeOfCohort size or ammount of people to add into the cohort as int
     * @return
     */
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
                //System.out.println(selectedCustomer.get("in_Cohort"));
                // Check that selected customer wasn't already added and that they are not already assigned to a cohort 
                if (!cohortList.contains(selectedCustomer) && !(Boolean) selectedCustomer.get("in_Cohort"))
                {
                    cohortList.add(selectedCustomer);
                    remainingCustomers.remove(i);
                    selectedCustomer.put("in_Cohort",true);
                    //System.out.println("TEST");
                }

            }

                // Add newly formed cohort list to cohorts hashmap while datastructure is locked 
                cohorts.put(customer, cohortList);
                // For testing print cohort map after creation 
                //printCohortMap(); 
                return "SUCCESS";
        }


    }

    /***
     * deletes a customer and the cohort they are assigned to. 
     * doing this because it makes more sense to delete the cohort otherwise 
     * communication issues could arise. 
     * Could look into furthering implementation to delete only this customer from the cohort 
     * and checking size of remaining customers in cohort to determine if entire cohort should 
     * be deleted. 
     * @param customer customer in cohort or customer maps. 
     * @return
     */
    public static String deleteCustomer(String customer)
    {
        // Lock data structure 
        synchronized (customers)
        {
            if (customers.containsKey(customer))
            {
                customers.remove(customer);
                deleteCohort2(customer);
                return "SUCCESS";
            }
        }
        return "FAILURE";
    }

    /***
     * opens an account for a customer 
     * @param customerName customer name to add to database
     * @param balance balance to add in the database
     * @param ipv4Address ip address for the customer as a string to add in the database
     * @param porta port number for the bank 
     * @param portb port number for the customer
     * @return
     */
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

    /***
     * prints the customers in the bank, used for testing using Test command from customer 
     */
    public static void printCustomerMap()
    {
        for (String key : customers.keySet())
        {
            System.out.println(key + " "+customers.get(key));
        }
    }
    
    /***
     * prints the cohorts in the bank used for testing using Test command from customer 
     */
    // This function is used for testing so that I can verify the contents of the hashmap
    public static void printCohortMap()
    {
        for (String key : cohorts.keySet())
        {
            System.out.println(key + " "+cohorts.get(key));
        }
    }

    /***
     * Itererates through list of cohorts and gets the cohort for any customer that is assigned to the cohort returns as arraylist
     * @param customer customer in the hashmap of the list
     * @return Arraylist of a cohort 
     */
    public static ArrayList<Map<String,Object>> getCohortList(String customer)
    {
        ArrayList<Map<String,Object>> tempList = null; 
        for (Map.Entry<String,List<Map<String,Object>>> listEntry : cohorts.entrySet())
        {
            List<Map<String,Object>> cohortList = listEntry.getValue();
            for (Map<String,Object> cust : cohortList)
            {
                String name = (String) cust.get("name");
                if (name.equals(customer))
                {
                    tempList = new ArrayList<>(cohortList);
                }

            }
            if (tempList != null)
            {
                break; 
            }
        }
        return tempList; 
    }

    /**
     * Updates the banks cohort with new information when a checkpoint happens to use the Bank as a global database
     * @param customer customer within the cohort hashmap of the arraylist 
     * @param list the new array list containing the new cohort information 
     */
    public static void updateCohortList(String customer, ArrayList<Map<String,Object>> list)
    {
        
        for (Map.Entry<String,List<Map<String,Object>>> listEntry : cohorts.entrySet())
        {
            List<Map<String,Object>> cohortList = listEntry.getValue();
            for (Map<String,Object> cust : cohortList)
            {
                String name = (String) cust.get("name");
                if (name.equals(customer))
                {
                    System.out.println("Updating .... ");
                    cohorts.put(listEntry.getKey(), list);
                    System.out.println("Update Complete!");
                }
                
                
            }
        }

    }
    
//#endregion

//#region Customers HashMap Helper Methods 

    /***
     * sends a packet as an array list 
     * @param ip ip as an inetaddress
     * @param port port number as an int 
     * @param customer customer name to send to, will get the cohort as an Array list to send for any customer in the list
     */
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

    /***
     * This is basically the same as sending a packet as an array list but can be used to send to others in a cohort.  
     * @param ip ip as an inet address
     * @param port port as an int
     * @param customer customer in the cohort for the list to send 
     */
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

    /***
     * get the customer ip of a specific customer in the customers map 
     * @param customer customer name as a string 
     * @return customers IP as a string 
     */
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

    /***
     * get the customers port for specific customer in the customers map 
     * @param customer customer name 
     * @return customer port as an int
     */
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

    /***
     * get the banks port number not sure why I made this, the bank has a local variable to store this. However this can be used to test 
     * in the event of a communication error that the customer has the correct port number for the bank 
     * @param customer customer name in the customers map 
     * @return bank port number as an int 
     */
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

    /**
     * Receives an arraylist as a message. Can store as another arraylist. 
     * @param ip ip to listen to should be your own 
     * @param port port to listen to should be your own 
     * @return arraylist of a cohort 
     */
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

    /**
     * Makes a deep copy of the Cohort so that when changes are made to the original, no changes are made to the old database.
     * this ensures that an earlier checkpoint can be recovered. Not sure this is needed anymore, used this when storing old 
     * cohorts in the chanell objects for the customer. It can still be useful anytime you will need a list to not change when 
     * changes are made to the original since hashmaps are used within the list those need to be deep copied. 
     * @param cohort arraylist as a cohort that points to the original 
     * @return deep copied arraylist 
     */
    public static ArrayList<Map<String,Object>> deepCopy(ArrayList<Map<String,Object>> cohort) 
    {
        ArrayList<Map<String,Object>> newCohort = new ArrayList<>();
    
        // Iterate through all map items in the cohort 
        for (Map<String,Object> map : cohort) 
        {
            // initialize each copy of the map object as new hashmap. 
            Map<String,Object> newMap = new HashMap<>();
    
            // Iterate through every entry inside of the Map 
            for (Map.Entry<String,Object> entry : map.entrySet()) 
            {
                // copy the contents into the newMap
                String key = entry.getKey();
                Object value = entry.getValue();
                newMap.put(key, value);
            }
            
            // copy the new map into the cohort
            newCohort.add(newMap);
        }
    
        // This should make it now so that when changes are made to cohort the old values of the cohort will 
        // be saved here and wont change. Hopefully. 
        return newCohort;
    }
    
    /**
     * this function is useful because you can pass in an arraylist and print the contents of it for testing purposes 
     * @param list array list to print must be arraylist<map<string,object>> 
     */
    public static void printAList(ArrayList<Map<String,Object>> list)
    {
        for (Map<String,Object> lItem : list)
        {
            System.out.println(lItem.toString());
        }
    }

//#endregion



}
