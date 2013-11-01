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

import LockManager.*;

//public class ResourceManagerImpl extends java.rmi.server.UnicastRemoteObject
public class ResourceManagerImpl implements ResourceManager {

    protected RMHashtable m_itemHT = new RMHashtable();

    protected ItemManager rmHotel  = null;
    protected ItemManager rmCar    = null;
    protected ItemManager rmFlight = null;

    public static void main(String args[]) {
        // Figure out where server is running
        int port = 5005;
        String hotelServer = "localhost";
        String carServer = "localhost";
        String flightServer = "localhost";

        if (args.length == 4) {
            hotelServer = args[0];
            carServer = args[1];
            flightServer = args[2];
            port = Integer.parseInt(args[3]);
        }
        else {
            System.err.println("Wrong usage");
            System.out.println("Usage: java ResImpl.ResourceManagerImpl [hotel server] [car server] [flight server] [server port");
            System.exit(1);
        }

        try {
            // create a new Server object
            ResourceManagerImpl obj = new ResourceManagerImpl(hotelServer, carServer, flightServer);
            // dynamically generate the stub (client proxy)
            ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
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

    public ResourceManagerImpl(String hotelServer, String carServer,
        String flightServer) throws RemoteException {
        int hotelPort = 5005;
        int carPort = 5006;
        int flightPort = 5007;
/*
        try {
            // get a reference to the rmiregistry on Hotel's server
            Registry registry = LocateRegistry.getRegistry(hotelServer,
                hotelPort);
            // get the proxy and the remote reference by rmiregistry lookup
            rmHotel = (ItemManager) registry.lookup("Group5_HotelManager");
            if (rmHotel != null) {
                System.out
                    .println("Successfully connected to the Hotel Manager");
            }
            else {
                System.out.println("Connection to the Hotel Manager failed");
            }
        }
        catch (Exception e) {
            System.err.println("Hotel exception: " + e.toString());
            e.printStackTrace();
        }
*/
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
/*
        try {
            Registry registry = LocateRegistry.getRegistry(flightServer,
                flightPort);
            rmFlight = (ItemManager) registry.lookup("Group5_FlightManager");
            if (rmFlight != null) {
                System.out
                    .println("Successfully connected to the Flight Manager");
            }
            else {
                System.out.println("Connection to the Flight Manager failed");
            }
        }
        catch (Exception e) {
            System.err.println("Flight exception: " + e.toString());
            e.printStackTrace();
        }
 */
    }

    // Reads a data item
    private RMItem readData(int id, String key) {
        synchronized (m_itemHT) {
            return (RMItem) m_itemHT.get(key);
        }
    }

    // Writes a data item
    private void writeData(int id, String key, RMItem value) {
        synchronized (m_itemHT) {
            m_itemHT.put(key, value);
        }
    }

    // Remove the item out of storage
    protected RMItem removeData(int id, String key) {
        synchronized (m_itemHT) {
            return (RMItem) m_itemHT.remove(key);
        }
    }

    // deletes the entire item
    protected boolean deleteItem(int id, String key) {
        Trace.info("RM::deleteItem(" + id + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + id + ", " + key
                + ") failed--item doesn't exist");
            return false;
        }
        else {
            if (curObj.getReserved() == 0) {
                removeData(id, curObj.getKey());
                Trace.info("RM::deleteItem(" + id + ", " + key
                    + ") item deleted");
                return true;
            }
            else {
                Trace
                    .info("RM::deleteItem("
                        + id
                        + ", "
                        + key
                        + ") item can't be deleted because some customers reserved it");
                return false;
            }
        } // if
    }

    // query the number of available seats/rooms/cars
    protected int queryNum(int id, String key) {
        Trace.info("RM::queryNum(" + id + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getCount();
        } // else
        Trace.info("RM::queryNum(" + id + ", " + key + ") returns count="
            + value);
        return value;
    }

    // query the price of an item
    protected int queryPrice(int id, String key) {
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        } // else
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost=$"
            + value);
        return value;
    }

    // reserve an item
    protected boolean reserveItem(int id, int customerID, String key,
        String location) {
        Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", "
            + key + ", " + location + " ) called");
        // Read customer object if it exists (and read lock it)
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", "
                + key + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        // check if the item is available
        ReservableItem item = (ReservableItem) readData(id, key);
        if (item == null) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", "
                + key + ", " + location + ") failed--item doesn't exist");
            return false;
        }
        else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", "
                + key + ", " + location + ") failed--No more items");
            return false;
        }
        else {
            cust.reserve(key, location, item.getPrice());
            writeData(id, cust.getKey(), cust);

            // decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);

            Trace.info("RM::reserveItem( " + id + ", " + customerID + ", "
                + key + ", " + location + ") succeeded");
            return true;
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
        int flightPrice) throws RemoteException {
    	
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $"
            + flightPrice + ", " + flightSeats + ") called");

        try {
        	rmFlight.addItem(id, Integer.toString(flightNum), flightSeats, flightPrice);
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	
        	return false;
        }
        
        return true;
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException {

    	try {
    		rmFlight.deleteItem(id, Integer.toString(flightNum));
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());

        	return false;        	
        }
    	
        return true;
    }

    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains
    // its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException {
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count
            + ", $" + price + ") called");

        // Call HotelManager
    	try {
    		rmHotel.addItem(id, location, count, price);
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());

        	return false;        	
        }
        return true;
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location) throws RemoteException {
        Trace.info("RM::deleteRoom(" + id + ", " + location + ")");

    	try {
    		rmHotel.deleteItem(id, location);
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());

        	return false;        	
        }
        
        return true;
    }

    // Create a new car location or add cars to an existing location
    // NOTE: if price <= 0 and the location already exists, it maintains its
    // current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException {
        Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $"
            + price + ") called");

    	try {
    		rmCar.addItem(id, location, count, price);
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());

        	return false;        	
        }
        
        return true;
    }

    // Delete cars from a location
    public boolean deleteCars(int id, String location) throws RemoteException {
    	
    	try {
    		rmCar.deleteItem(id, location);
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());

        	return false;        	
        }
    	
        return true;
    }

    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum) throws RemoteException {
    	
    	try {
    		return rmFlight.queryItemQuantity(id, Integer.toString(flightNum));
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return 0;        	
        }
    
    }

    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum) throws RemoteException {
    	try {
    		return rmFlight.queryItemPrice(id, Integer.toString(flightNum));
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return 0;        	
        }
        
    }

    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location) throws RemoteException {    	
    	try {
    		return rmHotel.queryItemQuantity(id, location);
    	} catch (DeadlockException e) {
    		Trace.error(e.getMessage());
    		return 0;        	
    	}        
    }

    // Returns room price at this location
    public int queryRoomsPrice(int id, String location) throws RemoteException {
    	try {
    		return rmHotel.queryItemPrice(id, location);
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return 0;        	
        }        
    }

    // Returns the number of cars available at a location
    public int queryCars(int id, String location) throws RemoteException {
    	try {
    		return rmCar.queryItemQuantity(id, location);
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return 0;        	
        }        
    }

    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location) throws RemoteException {
    	try {
    		return rmCar.queryItemPrice(id, location);
        } catch (DeadlockException e) {
        	Trace.error(e.getMessage());
        	return 0;        	
        }        
    }

    // Returns data structure containing customer reservation info. Returns null
    // if the
    // customer doesn't exist. Returns empty RMHashtable if customer exists but
    // has no
    // reservations.
    public RMHashtable getCustomerReservations(int id, int customerID)
        throws RemoteException {
        Trace.info("RM::getCustomerReservations(" + id + ", " + customerID
            + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::getCustomerReservations failed(" + id + ", "
                + customerID + ") failed--customer doesn't exist");
            return null;
        }
        else {
            return cust.getReservations();
        } // if
    }

    // return a bill
    public String queryCustomerInfo(int id, int customerID)
        throws RemoteException {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID
            + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID
                + ") failed--customer doesn't exist");
            return ""; // NOTE: don't change this--WC counts on this value
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

    public int newCustomer(int id) throws RemoteException {
        Trace.info("INFO: RM::newCustomer(" + id + ") called");
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt(String.valueOf(id)
            + String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND))
            + String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer cust = new Customer(cid);
        writeData(id, cust.getKey(), cust);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID) throws RemoteException {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID
            + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
        if (cust == null) {
            cust = new Customer(customerID);
            writeData(id, cust.getKey(), cust);
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
        throws RemoteException {
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
        Customer cust = (Customer) readData(id, Customer.getKey(customerID));
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
                    } catch (DeadlockException exc) {
                    	Trace.error(exc.getMessage());
                    	//TODO: Abort TXN
                    }                    
                }
                else if (itemType.equals("car")) {
                	try {
                		rmCar.cancelItem(id, reserveditem.getKey(), reserveditem.getCount());
                    } catch (DeadlockException exc) {
                    	Trace.error(exc.getMessage());
                    	//Abort TXN
                    }                    
                }
                else if (itemType.equals("flight")) {
                	try {
                		rmFlight.cancelItem(id, reserveditem.getKey(), reserveditem.getCount());
                    } catch (DeadlockException exc) {
                    	Trace.error(exc.getMessage());
                    	//Abort TXN
                    }                    
                }

            }

            // remove the customer from the storage
            removeData(id, cust.getKey());

            Trace.info("RM::deleteCustomer(" + id + ", " + customerID
                + ") succeeded");
            return true;
        } 
    }

    // Adds car reservation to this customer.
    public boolean reserveCar(int id, int customerID, String location)
        throws RemoteException {

        Customer cust = getCustomer(customerID);
        if (cust == null) {
            return false;
        }

        ReservedItem reservedItem = null;
        try {
        	reservedItem = rmCar.reserveItem(id, cust.getKey(), location);
        } catch (DeadlockException exc) {
        	Trace.error(exc.getMessage());
        	//Abort TXN
        }         
        
        if (reservedItem != null) {
            cust.reserve(reservedItem.getKey(), reservedItem.getLocation(),
                reservedItem.getPrice());

            return true;
        }

        return false;
    }

    // Adds room reservation to this customer.
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException {

        Customer cust = getCustomer(customerID);
        if (cust == null) {
            return false;
        }
        
        ReservedItem reservedItem = null;
        try {
        	reservedItem = rmHotel.reserveItem(id, cust.getKey(), location);
        } catch (DeadlockException exc) {
        	Trace.error(exc.getMessage());
        	//Abort TXN
        }  

        if (reservedItem != null) {
            cust.reserve(reservedItem.getKey(), reservedItem.getLocation(), reservedItem.getPrice());

            return true;
        }

        return false;
    }

    // Adds flight reservation to this customer.
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException {

        Customer cust = getCustomer(customerID);
        if (cust == null) {
            return false;
        }

        String strflightNum = Integer.toString(flightNum);

        ReservedItem reservedItem = null;
        try {
        	reservedItem = rmFlight.reserveItem(id, cust.getKey(), strflightNum);
        } catch (DeadlockException exc) {
        	Trace.error(exc.getMessage());
        	//Abort TXN
        }  
        
        if (reservedItem != null) {
            cust.reserve(reservedItem.getKey(), reservedItem.getLocation(), reservedItem.getPrice());
            return true;
        }

        return false;
    }

    /* reserve an itinerary */
    public boolean itinerary(int id, int customer, Vector flightNumbers,
        String location, boolean Car, boolean Room) throws RemoteException {

        System.out.println("PLEASE PRINT THAT TEXT &^#^$&*$^*");
        System.out.println("BOOKING ITINERARY");

        Customer cust = getCustomer(customer);
        if (cust == null) {
            return false;
        }

        ArrayList<String> reservedItems = new ArrayList<String>();

        boolean result = false;

        for (int i = 0; i < flightNumbers.size(); i++) {
            int flightNumber = (Integer) flightNumbers.get(i);

            result = reserveFlight(id, customer, flightNumber);

            // If one flight reservation fails, we cancel the previous flights
            // reserved
            if (!result) {
                System.out.println(flightNumber + " reservation failed");
                cancelItemBatch(cust, reservedItems);
                return false;
            }

            reservedItems.add("flight-" + flightNumber);
            System.out.println(flightNumber + " reservation success");
        }

        if (Car) {
            // Try to reserve a car at destination        	
        	ReservedItem reservedCar = null;
        	try {
        		reservedCar = rmCar.reserveItem(1, cust.getKey(), location);
        	} catch (DeadlockException e) {
        		Trace.error(e.getMessage());
        	}
            
            if (reservedCar == null) {
                cancelItemBatch(cust, reservedItems);
                return false;
            }

            reservedItems.add(reservedCar.getKey());
            cust.reserve(reservedCar.getKey(), reservedCar.getLocation(), reservedCar.getPrice());
        }

        if (Room) {
            // Try to reserve a room at destination
        	ReservedItem reservedRoom = null;
        	try {
        		reservedRoom = rmHotel.reserveItem(1, cust.getKey(), location);
        	} catch (DeadlockException e) {
        		Trace.error(e.getMessage());
        	}            
            
            if (reservedRoom == null) {
                cancelItemBatch(cust, reservedItems);
                return false;
            }

            cust.reserve(reservedRoom.getKey(), reservedRoom.getLocation(), reservedRoom.getPrice());
        }

        return true;
    }

    private void cancelItemBatch(Customer cust, ArrayList<String> reservedItems) {
        for (String key : reservedItems) {
            cust.cancelReservation(key);
        }
    }

    public boolean test(String text) throws RemoteException {
        System.out.println(text);
        return true;
    }

}
