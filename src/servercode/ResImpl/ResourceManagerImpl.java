// -------------------------------
// adapated from Kevin T. Manley
// CSE 593
//
package servercode.ResImpl;

import servercode.ResInterface.*;

import java.util.*;
import java.rmi.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import carcode.ResImpl.Car;
import flightcode.ResImpl.Flight;

import LockManager.*;

//public class ResourceManagerImpl extends java.rmi.server.UnicastRemoteObject
public class ResourceManagerImpl implements ResourceManager {

    protected RMHashtable m_itemHT = new RMHashtable();
    protected TransactionManager txnManager = TransactionManager.getInstance();
    protected LockManager lm = new LockManager();
    protected WorkingSet<Customer> ws = new WorkingSet<Customer>();

    protected ItemManager rmHotel  = null;
    protected ItemManager rmCar    = null;
    protected ItemManager rmFlight = null;

    public static void main(String args[]) {
        
    	int rmiPort = 5005;        
        String carServer = "localhost";
        String flightServer = "localhost";
        String hotelServer = "localhost";
        int carPort = 5006;
        int flightPort = 5007;
        int hotelPort = 5008;

        if (args.length == 7) {
            rmiPort = Integer.parseInt(args[0]);
            carServer = args[1];
            carPort = Integer.parseInt(args[2]);
            flightServer = args[3];
            flightPort = Integer.parseInt(args[4]);
            hotelServer = args[5];
            hotelPort = Integer.parseInt(args[6]);            
        }
        else {
            System.err.println("Wrong usage");
            System.out.println("Usage: java ResImpl.ResourceManagerImpl [RMI port] [car server] [car port] [flight server] [flight port] [hotel server] [hotel port]");
            System.exit(1);
        }

        try {
            // create a new Server object
            ResourceManagerImpl obj = new ResourceManagerImpl(carServer, carPort, flightServer, flightPort, hotelServer, hotelPort);
            // dynamically generate the stub (client proxy)
            ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(rmiPort);
            registry.rebind("Group5_ResourceManager", rm);

            System.err.println("Server ready");
        }
        catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    public ResourceManagerImpl(String carServer, int carPort, String flightServer, int flightPort, String hotelServer, int hotelPort) throws RemoteException {

        try {
            // get a reference to the rmiregistry on Hotel's server
            Registry registry = LocateRegistry.getRegistry(hotelServer, hotelPort);
            // get the proxy and the remote reference by rmiregistry lookup
            rmHotel = (ItemManager) registry.lookup("Group5_HotelManager");
            if (rmHotel != null) {
                System.out.println("Successfully connected to the Hotel Manager");
            }
            else {
                System.out.println("Connection to the Hotel Manager failed");
            }
        }
        catch (Exception e) {
            System.err.println("Hotel exception: " + e.toString());
            e.printStackTrace();
        }

        try {
            Registry registry = LocateRegistry.getRegistry(carServer, carPort);
            rmCar = (ItemManager) registry.lookup("Group5_CarManager");
            if (rmCar != null) {
                System.out.println("Successfully connected to the Car Manager");
            }
            else {
                System.out.println("Connection to the Car Manager failed");
            }
        }
        catch (Exception e) {
            System.err.println("Car exception: " + e.toString());
            e.printStackTrace();
        }

        try {
            Registry registry = LocateRegistry.getRegistry(flightServer,
                flightPort);
            rmFlight = (ItemManager) registry.lookup("Group5_FlightManager");
            if (rmFlight != null) {
                System.out.println("Successfully connected to the Flight Manager");
            }
            else {
                System.out.println("Connection to the Flight Manager failed");
            }
        }
        catch (Exception e) {
            System.err.println("Flight exception: " + e.toString());
            e.printStackTrace();
        }
    }

    // Reads a data item
    private RMItem readData(int id, String key) {
        synchronized (m_itemHT) {
            return (RMItem) m_itemHT.get(key);
        }
    }

    // Writes a data item
    public void writeData(int id, String key, RMItem value) {
        synchronized (m_itemHT) {
            m_itemHT.put(key, value);
        }
    }

    // Remove the item out of storage
    public RMItem removeData(int id, String key) {
        synchronized (m_itemHT) {
            return (RMItem) m_itemHT.remove(key);
        }
    }

    protected Customer getCustomer(int customerID) {
        // Read customer object if it exists (and read lock it)
        Customer cust = (Customer) readData(0, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("Customer " + customerID + " doesn't exist");
        }

        return cust;
    }

    // Create a new flight, or add seats to existing flight
    // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its
    // current price
    public boolean addFlight(int id, int flightNum, int flightSeats,
        int flightPrice) throws RemoteException, InvalidTransactionException {
    	
    	//Sleep to test concurrency. Je voulais voir qu'est-ce qui se passe lorsque le middleware sleeps pis que 
    	//j'envoie un autre request à partir d'un autre client. Réponse: Ça ne bloque pas
    	/*try {
    		Thread.sleep(10000);
    	} catch (Exception e) {
    		
    	}*/
    	
    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $"+ flightPrice + ", " + flightSeats + ") called");

        try {
        	rmFlight.addItem(id, Integer.toString(flightNum), flightSeats, flightPrice);
        	txnManager.enlist(id, "flight");
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());        	
        	return false;
        }
        
        return true;
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException {

    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	boolean result;
    	
    	try {
    		result = rmFlight.deleteItem(id, Integer.toString(flightNum));
    		txnManager.enlist(id, "flight");
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	result = false;        	
        }
    	
        return result;
    }

    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains
    // its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException, InvalidTransactionException {
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
       
        if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
        
    	try {
    		rmHotel.addItem(id, location, count, price);
    		txnManager.enlist(id, "hotel");
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return false;        	
        }
        return true;
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location) throws RemoteException, InvalidTransactionException {
        Trace.info("RM::deleteRoom(" + id + ", " + location + ")");

        if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
        
        boolean result;
        
    	try {
    		result = rmHotel.deleteItem(id, location);
    		txnManager.enlist(id, "room");
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	result = false;        	
        }
        
        return result;
    }

    // Create a new car location or add cars to an existing location
    // NOTE: if price <= 0 and the location already exists, it maintains its
    // current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException, InvalidTransactionException {
        Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called");

        if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
        
    	try {
    		rmCar.addItem(id, location, count, price);
    		txnManager.enlist(id, "car");
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return false;        	
        }
        
        return true;
    }

    // Delete cars from a location
    public boolean deleteCars(int id, String location) throws RemoteException, InvalidTransactionException {
    	
    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	boolean result;
    	
    	try {
    		result = rmCar.deleteItem(id, location);
    		txnManager.enlist(id, "car");
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	result = false;        	
        }
    	
        return result;
    }

    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException {
    	
    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	try {
    		int qty = rmFlight.queryItemQuantity(id, Integer.toString(flightNum));
    		txnManager.enlist(id, "flight");
    		return qty;    		
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return -1;        	
        }
    
    }

    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum) throws RemoteException, InvalidTransactionException {
    	
    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	    	
    	try {
    		int price = rmFlight.queryItemPrice(id, Integer.toString(flightNum));
    		txnManager.enlist(id, "flight");
    		return price;
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return -1;        	
        }
        
    }

    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException { 
    	
    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	try {
    		int qty = rmHotel.queryItemQuantity(id, location);
    		txnManager.enlist(id, "hotel");
    		return qty;
    	} catch (DeadlockException e) {
    		Trace.error(e.getMessage());
    		return -1;        	
    	}        
    }

    // Returns room price at this location
    public int queryRoomsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
    	
    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	try {
    		int price = rmHotel.queryItemPrice(id, location);
    		txnManager.enlist(id, "hotel");
    		return price;
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return -1;        	
        }        
    }

    // Returns the number of cars available at a location
    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException {
    	
    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	try {
    		int qty = rmCar.queryItemQuantity(id, location);
    		txnManager.enlist(id, "car");
    		return qty;
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	//txnManager.abort(id);
        	return -1;        	
        }        
    }

    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
    	
    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	    	
    	try {
    		int price = rmCar.queryItemPrice(id, location);
    		txnManager.enlist(id, "car");
    		return price;
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	//throw new DeadlockException(id, location);
        	return -1;        	
        }        
    }

    // Returns data structure containing customer reservation info. Returns null
    // if the
    // customer doesn't exist. Returns empty RMHashtable if customer exists but
    // has no
    // reservations.
    /*public RMHashtable getCustomerReservations(int id, int customerID)
        throws RemoteException {
        Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called");
        
        try {
        	lm.Lock(id, String.valueOf(customerID), LockType.READ);
        } catch (DeadlockException e) {
        	
        }
        
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::getCustomerReservations failed(" + id + ", "
                + customerID + ") failed--customer doesn't exist");
            return null;
        }
        else {
            return cust.getReservations();
        } // if
    }*/

    // return a bill
    public String queryCustomerInfo(int id, int customerID)
        throws RemoteException, DeadlockException, InvalidTransactionException {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called");
        
        if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
        
        try {
        	lm.Lock(id, String.valueOf(customerID), LockType.READ);        	
        } catch (DeadlockException e) {
        	throw new DeadlockException(id, String.valueOf(customerID));
        }
        
        Customer cust;
        
        if (ws.hasItem(customerID+"")){
    		cust = (Customer) ws.getItem(customerID+"");    		
    	} else {
    		cust = (Customer) readData(id, Customer.getKey(customerID));    		    
      	}
        
        txnManager.enlist(id, "customer");
        if (cust == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID
                + ") failed--customer doesn't exist");
            return "Customer does not exist"; // NOTE: don't change this--WC counts on this value
            // indicating a customer does not exist...
        }
        else {
            String s = cust.printBill();
            
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID
                + "), bill follows...");
            System.out.println(s);
            return s;
        } // if
    }

    // customer functions
    // new customer just returns a unique customer identifier
    public int newCustomer(int id) throws RemoteException, DeadlockException, InvalidTransactionException {
        Trace.info("INFO: RM::newCustomer(" + id + ") called");
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt(String.valueOf(id)
            + String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND))
            + String.valueOf(Math.round(Math.random() * 100 + 1)));
        
        if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
        
        try {
        	lm.Lock(id, String.valueOf(cid), LockType.WRITE);
        } catch (DeadlockException e) {
        	throw new DeadlockException(id, String.valueOf(cid));
        }
        
        txnManager.enlist(id, "customer");
        Customer cust = new Customer(cid);
        
        ws.addCommand(id, new CommandCustomerPut(id,  cust.getKey(), cust, this));
        ws.sendCurrentState(cust.getKey(), cust);
        ws.addLocationToTxn(id,  cust.getKey());
        //writeData(id, cust.getKey(), cust);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID) throws RemoteException, DeadlockException, InvalidTransactionException {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called");
        
        if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
        
        try {
        	lm.Lock(id, String.valueOf(customerID), LockType.WRITE);
        } catch (DeadlockException e) {
        	throw new DeadlockException(id, String.valueOf(customerID));
        }
        
        txnManager.enlist(id, "customer");
        Customer cust;
        
        if (ws.hasItem(customerID+"")){
    		cust = (Customer) ws.getItem(customerID+"");    		
    	} else {
    		cust = (Customer) readData(id, Customer.getKey(customerID));   
    		
    		if (cust != null) {
    			cust = cust.getCopy();
    		}
      	} 
                
        if (cust == null) {
            cust = new Customer(customerID);
            
            ws.addCommand(id, new CommandCustomerPut(id,  cust.getKey(), cust, this));
            ws.sendCurrentState(customerID+"", cust);
            ws.addLocationToTxn(id,  customerID+"");            
            
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
                + ") created a new customer");
            return true;
        }
        else {
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
                + ") failed--customer already exists");
            return false;
        } // else
    }

    // Deletes customer from the database.
    public boolean deleteCustomer(int id, int customerID)
        throws RemoteException, DeadlockException, InvalidTransactionException {
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
        
        if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
        
        try {
        	lm.Lock(id, String.valueOf(customerID), LockType.WRITE);
        } catch (DeadlockException e) {
        	throw new DeadlockException(id, String.valueOf(customerID));
        }
        
        Customer cust;
    	
    	if (ws.hasItem(customerID+"")){
    		cust = (Customer) ws.getItem(customerID+"");    		
    	} else {
    		cust = (Customer) readData(id, Customer.getKey(customerID));
    		
    		if (cust != null) {
    			cust = cust.getCopy();
    			ws.sendCurrentState(customerID+"", cust);
        		ws.addLocationToTxn(id,  customerID+"");
    		}
      	}
        
        txnManager.enlist(id, "customer");
        if (cust == null) {
            Trace.warn("RM::deleteCustomer(" + id + ", " + customerID
                + ") failed--customer doesn't exist");
            return false;
        }
        else {
            // Un-reserve all reservations the customer had made        	
            RMHashtable reservationHT = cust.getReservations();
            for (Enumeration e = reservationHT.keys(); e.hasMoreElements();) {
                String reservedkey = (String) (e.nextElement());
                ReservedItem reserveditem = cust.getReservedItem(reservedkey);
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID
                    + ") has reserved " + reserveditem.getKey() + " "
                    + reserveditem.getCount() + " times");

                System.out.println("Cancelling reservation: "
                    + reserveditem.getKey());
                String itemType = reserveditem.getKey().split("\\-")[0];
                if (itemType.equals("room")) {
                	try {
                		rmHotel.cancelItem(id, reserveditem.getKey(), reserveditem.getCount());
                		txnManager.enlist(id, "hotel");
                    } catch (DeadlockException exc) {
                    	Trace.error(exc.getMessage());
                    	this.abort(id);
                    	throw exc;
                    }                    
                }
                else if (itemType.equals("car")) {
                	try {
                		rmCar.cancelItem(id, reserveditem.getKey(), reserveditem.getCount());
                		txnManager.enlist(id, "car");
                    } catch (DeadlockException exc) {
                    	Trace.error(exc.getMessage());
                    	this.abort(id);
                    	throw exc;
                    }                    
                }
                else if (itemType.equals("flight")) {
                	try {
                		rmFlight.cancelItem(id, reserveditem.getKey(), reserveditem.getCount());
                		txnManager.enlist(id, "flight");
                    } catch (DeadlockException exc) {
                    	Trace.error(exc.getMessage());
                    	this.abort(id);
                    	throw exc;
                    }                    
                }
            }

            // remove the customer from the storage
            ws.addCommand(id, new CommandCustomerDelete(id, cust.getKey(), this));            
            ws.deleteItem(customerID+"");
            
            Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded");
            return true;
        } 
    }

    // Adds car reservation to this customer.
    public boolean reserveCar(int id, int customerID, String location)
        throws RemoteException, InvalidTransactionException, DeadlockException {

    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	try {
    		lm.Lock(id, String.valueOf(customerID), LockType.WRITE);
        	txnManager.enlist(id, "customer");
    	} catch (DeadlockException exc) {
        	throw exc;
        }    
    	
        Customer cust;
    	
    	if (ws.hasItem(customerID+"")){
    		cust = (Customer) ws.getItem(customerID+"");    		
    	} else {
    		cust = getCustomer(customerID);
    		
    		if (cust != null) {
    			cust = cust.getCopy();
    			ws.sendCurrentState(customerID+"", cust);
        		ws.addLocationToTxn(id,  customerID+""); 
    		}
      	}
        
        if (cust == null) {
            return false;
        }      
        
        ReservedItem reservedItem = null;
        try {        	
        	reservedItem = rmCar.reserveItem(id, cust.getKey(), location);
        	txnManager.enlist(id, "car");        	
        } catch (DeadlockException exc) {
        	Trace.error(exc.getMessage());
        	this.abort(id);
        	throw exc;
        }         
        
        if (reservedItem != null) {
        	       	
            cust.reserve(reservedItem.getKey(), reservedItem.getLocation(), reservedItem.getPrice());
            ws.addCommand(id, new CommandCustomerPut(id, cust.getKey(), cust, this));

            return true;
        }

        return false;
    }

    // Adds room reservation to this customer.
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException, InvalidTransactionException, DeadlockException {

    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	try {
    		lm.Lock(id, String.valueOf(customerID), LockType.WRITE);
        	txnManager.enlist(id, "customer");
    	} catch (DeadlockException exc) {
        	throw exc;
        }  
    	
    	Customer cust;
    	
    	if (ws.hasItem(customerID+"")){
    		cust = (Customer) ws.getItem(customerID+"");    		
    	} else {
    		cust = getCustomer(customerID);
    		
    		if (cust != null) {
    			cust = cust.getCopy();
    			ws.sendCurrentState(customerID+"", cust);
        		ws.addLocationToTxn(id,  customerID+""); 
    		}
      	}
    	
        if (cust == null) {
            return false;
        }
        
        ReservedItem reservedItem = null;
        try {
        	reservedItem = rmHotel.reserveItem(id, cust.getKey(), location);
        	txnManager.enlist(id, "hotel");
        } catch (DeadlockException exc) {
        	Trace.error(exc.getMessage());
        	this.abort(id);
        	throw exc;
        }  

        if (reservedItem != null) {
            cust.reserve(reservedItem.getKey(), reservedItem.getLocation(), reservedItem.getPrice());
            ws.addCommand(id, new CommandCustomerPut(id, cust.getKey(), cust, this));
            return true;
        }

        return false;
    }

    // Adds flight reservation to this customer.
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException, InvalidTransactionException, DeadlockException {

    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	try {
    		lm.Lock(id, String.valueOf(customerID), LockType.WRITE);
        	txnManager.enlist(id, "customer");
    	} catch (DeadlockException exc) {
        	throw exc;
        }  
    	
    	Customer cust;
    	
    	if (ws.hasItem(customerID+"")){
    		cust = (Customer) ws.getItem(customerID+"");    		
    	} else {
    		cust = getCustomer(customerID);
    		
    		if (cust != null) {
    			cust = cust.getCopy();
    			ws.sendCurrentState(customerID+"", cust);
        		ws.addLocationToTxn(id,  customerID+""); 
    		}
      	}

    	if (cust == null) {
            return false;
        }

        String strflightNum = Integer.toString(flightNum);

        ReservedItem reservedItem = null;
        try {        	
        	reservedItem = rmFlight.reserveItem(id, cust.getKey(), strflightNum);        	
        	txnManager.enlist(id, "flight");
        } catch (DeadlockException exc) {
        	Trace.error(exc.getMessage());        
        	this.abort(id);
        	throw exc;
        }  
        
        if (reservedItem != null) {
            cust.reserve(reservedItem.getKey(), reservedItem.getLocation(), reservedItem.getPrice());
            ws.addCommand(id, new CommandCustomerPut(id, cust.getKey(), cust, this));
            return true;
        }

        return false;
    }

    /* reserve an itinerary */
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean car, boolean room) 
    		throws RemoteException, InvalidTransactionException, TransactionAbortedException, DeadlockException {
    	
    	//Unlike for the other operations, the transaction is aborted as soon as a component of the itinerary fails. 
    	
    	if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
    	
    	try {
    		lm.Lock(id, String.valueOf(customer), LockType.WRITE);
        	txnManager.enlist(id, "customer");
    	} catch (DeadlockException exc) {
        	throw exc;
        }  
    	
    	Customer cust;
    	
    	if (ws.hasItem(customer+"")){
    		cust = (Customer) ws.getItem(customer+"");    		
    	} else {
    		cust = getCustomer(customer);
    		
    		if (cust != null) {
    			cust = cust.getCopy();
    			ws.sendCurrentState(customer+"", cust);
        		ws.addLocationToTxn(id,  customer+""); 
    		}
      	}

        if (cust == null) {
        	this.abort(id);
            return false;
        }

        System.out.println("BOOKING ITINERARY");
        
        for (int i = 0; i < flightNumbers.size(); i++) {
            int flightNumber = Integer.parseInt((String)flightNumbers.get(i));
        	
            //the boolean is required when reserveFlight fails for reasons other than deadlocks
            //i.e. no more flights available
            boolean flightResult;            
        	try {
        		flightResult = reserveFlight(id, customer, flightNumber);
        	} catch (DeadlockException e) {
        		Trace.error(e.getMessage());
        		this.abort(id);
        		throw new TransactionAbortedException(id);
        	}
        	
        	if (!flightResult){        		
        		this.abort(id);
        		throw new TransactionAbortedException(id);
        	}      
        }
        
        if (car) {       	
        	boolean carResult;
        	try {
        		carResult = reserveCar(id, cust.getID(), location);        		
        	} catch (DeadlockException e) {
        		Trace.error(e.getMessage());
        		this.abort(id);
        		throw new TransactionAbortedException(id);
        	}
            
            if (!carResult) {
            	this.abort(id);
        		throw new TransactionAbortedException(id);
            }
        }

        if (room) {
            // Try to reserve a room at destination
        	boolean roomResult;
        	try {
        		roomResult = reserveRoom(id, cust.getID(), location);
        	} catch (DeadlockException e) {
        		Trace.error(e.getMessage());
        		this.abort(id);
        		throw new TransactionAbortedException(id);
        	}            
            
            if (!roomResult) {
            	this.abort(id);
        		throw new TransactionAbortedException(id);
            }
        }

        return true;
    }

	@Override
	public int start() throws RemoteException {		
		return txnManager.start();				
	}

	@Override
	public boolean commit(int id) throws RemoteException, InvalidTransactionException {
		
		if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
		
		System.out.println("Committing transaction: " + id);
		Vector<String> rms = txnManager.commit(id);
				 
		for(String rm: rms) {
			if (rm.equals("car"))
				rmCar.commit(id);
			else if (rm.equals("flight"))
				rmFlight.commit(id);
			else if (rm.equals("hotel"))
				rmHotel.commit(id);	
			else if (rm.equals("customer")){			
				lm.UnlockAll(id);
				ws.commit(id);
			}
		}
		
		return true; //Commits always succeed since there is no failures yet
	}

	@Override
	public void abort(int id) throws RemoteException, InvalidTransactionException {
		
		if (!txnManager.isValidTransaction(id)) {
        	throw new InvalidTransactionException(id);
        }
		
		System.out.println("Aborting transaction: " + id);
		Vector<String> rms = txnManager.abort(id);
		 
		for(String rm: rms) {
			if (rm.equals("car"))
				rmCar.abort(id);
			else if (rm.equals("flight"))
				rmFlight.abort(id);
			else if (rm.equals("hotel"))
				rmHotel.abort(id);	
			else if (rm.equals("customer")){
				lm.UnlockAll(id);
				ws.abort(id);
			}
		}
	}

	@Override
	public boolean shutdown() throws RemoteException {
		
		if (txnManager.canShutdown()) {
			
			System.out.println("SHUTTING SYSTEM DOWN");
			//TODO shut RMS down
			//...
			
			return true;
		}
		
		System.out.println("Can't shut system down since transactions are still alive");
		return false;
	}
	

}
