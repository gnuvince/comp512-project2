package carcode.ResImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import servercode.ResInterface.*;
import servercode.ResImpl.*;
import LockManager.*;

public class CarManagerImpl implements ItemManager {

    protected RMHashtable carTable = new RMHashtable();
    
    private LockManager lm = new LockManager();

    public static void main(String args[]) {
    	
        int port = 5006;
        
        CarManagerImpl obj = new CarManagerImpl();

        if (args.length != 1) {            
            System.err.println("Usage: java carcode.ResImpl.CarManagerImpl <rmi port>");
            System.exit(1);
        }
        else {
            port = Integer.parseInt(args[0]);
        }
        
        try 
        {
            // create a new Server object
            // dynamically generate the stub (client proxy)
            ItemManager rm = (ItemManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("Group5_CarManager", rm);

            System.err.println("Car Server ready");
        } 
        catch (Exception e) 
        {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }
    
    @Override
    public boolean addItem(int id, String location, int quantity, int price)
        throws RemoteException, DeadlockException {

    	//Acquire write lock
    	try {
    		lm.Lock(id, location, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, location);
        }  
    	
        Car curObj = (Car) fetchCar(id, Car.getKey(location));
        if (curObj == null) {
            // If Car doesn't exist, create it and add it to 
            // the manager's hash table.
            Car newObj = new Car(location, quantity, price);
            putCar(id, newObj.getKey(), newObj);
            Trace.info("RM::addCars(" + id + ") created new location "
                + location + ", count=" + quantity + ", price=$" + price);
        }
        else {
            // If the Car already exists, update its quantity (by adding
            // the new quantity) and update its price (only if the new price
            // is positive).
            curObj.setCount(curObj.getCount() + quantity);
            if (price > 0) {
                curObj.setPrice(price);
            }
            putCar(id, Car.getKey(location), curObj);
            Trace.info("RM::addCars(" + id + ") modified existing location "
                + location + ", count=" + curObj.getCount() + ", price=$"
                + curObj.getPrice());
        }

        return true;
    }

    @Override
    public boolean deleteItem(int id, String location) throws RemoteException, DeadlockException {
    	
    	try {
    		lm.Lock(id, location, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, location);
        }
    	
        String itemId = Car.getKey(location);
        Car curObj = fetchCar(id, itemId);

        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + id + ", " + itemId
                + ") failed--item doesn't exist"); 
            return false;
        }
        else {
            if (curObj.getReserved() == 0) {
                deleteCar(id, curObj.getKey());
                Trace.info("RM::deleteItem(" + id + ", " + itemId
                    + ") item deleted");
                return true;
            }
            else {
                Trace.info("RM::deleteItem("
                    + id
                    + ", "
                    + itemId
                    + ") item can't be deleted because some customers reserved it");
                return false;
            }
        } 
    }

    @Override
    public int queryItemQuantity(int id, String location) throws RemoteException, DeadlockException {
    	
    	try {
    		lm.Lock(id, location, LockType.READ);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, location);
        }    	
    	
        Car curObj = fetchCar(id, Car.getKey(location));
        if (curObj != null) {
            return curObj.getCount();
        }

        return 0;
    }

    @Override
    public int queryItemPrice(int id, String location) throws RemoteException, DeadlockException {
    	
    	try {
    		lm.Lock(id, location, LockType.READ);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, location);
        }  
    	
        Car curObj = fetchCar(id, Car.getKey(location));
        if (curObj != null) {
            return curObj.getPrice();
        }
        return 0;   
    }


    @Override
    public ReservedItem reserveItem(int id, String customerId, String location)
        throws RemoteException, DeadlockException {

    	try {
    		lm.Lock(id, location, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, location);
        }  
    	
        Car curObj = fetchCar(id, Car.getKey(location));

        if (curObj == null) {        	
            Trace.warn("RM::reserveCar( " + id + ", " + customerId + ", " + location + ") failed--item doesn't exist"); 
            return null;
        }
        else if (curObj.getCount() == 0) {
            Trace.warn("RM::reserveCar( " + id + ", " + customerId + ", " + location + ") failed--No more items");
            return null;
        }
        else {        	
            String key = Car.getKey(location);

            // decrease the number of available items in the storage
            curObj.setCount(curObj.getCount() - 1);
            curObj.setReserved(curObj.getReserved() + 1);

            putCar(id, key, curObj);

            Trace.info("RM::reserveCar( " + id + ", " + customerId + ", " + key + ") succeeded");  
            return new ReservedItem(key, curObj.getLocation(), 1, curObj.getPrice());
        }
    }

    public boolean cancelItem(int id, String carKey, int count)
        throws RemoteException, DeadlockException {
    	
    	Car curObj = fetchCar(id, carKey);
    	if (curObj == null) {
            System.out.println("Car " + carKey + " can't be cancelled because none exists");
            return false;
        }
    	
    	String location = curObj.getLocation();
    	
    	try {
    		lm.Lock(id, location, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, location);
        }  

        System.out.println("cancelItem( " + id + ", " + carKey + ", " + count + " )");
        
        //adjust available quantity
        curObj.setCount(curObj.getCount() + count);
        curObj.setReserved(curObj.getReserved() - count);

        return true;
    }

    private Car fetchCar(int id, String itemId) {
        synchronized (carTable) {
            return (Car)carTable.get(itemId);
        }
    }

    private void putCar(int id, String itemId, Car car) {
        synchronized (carTable) {
            carTable.put(itemId, car);            
        }
    }

    private void deleteCar(int id, String itemId) {
        synchronized (carTable) {
            carTable.remove(itemId);
        }
    }

	@Override
	public boolean commit(int id) throws RemoteException {
				
		return lm.UnlockAll(id);
	}

	@Override
	public void abort(int id) throws RemoteException {
		
		
	}


}
