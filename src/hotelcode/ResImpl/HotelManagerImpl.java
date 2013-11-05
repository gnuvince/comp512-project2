package hotelcode.ResImpl;

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

import carcode.ResImpl.Car;
import servercode.ResInterface.*;
import servercode.ResImpl.*;
import LockManager.*;

public class HotelManagerImpl implements ItemManager {
    
    protected RMHashtable roomsTable = new RMHashtable();
    
    private LockManager lm = new LockManager();
    private UndoManager undoManager = new UndoManager(this);
    
    public static void main(String args[]) {
    	
        int port = 5008;
        HotelManagerImpl obj = new HotelManagerImpl();

        if (args.length != 1) {            
            System.err.println("Usage: java hotelcode.ResImpl.HotelManagerImpl <rmi port>");
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
            registry.rebind("Group5_HotelManager", rm);

            System.err.println("Hotel Server ready");
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
    	
        Hotel curObj = (Hotel) fetchHotel(id, Hotel.getKey(location));
        if (curObj == null) {
            // If hotel doesn't exist, create it and add it to 
            // the manager's hash table.
            Hotel newObj = new Hotel(location, quantity, price);
            
            //UNDO: Create UndoAdd command that reverses the AddItem operation
            undoManager.addUndoCommand(id, new DeleteCommand(id, newObj));
            
            putHotel(id, newObj.getKey(), newObj);
            Trace.info("RM::addHotel(" + id + ") created new location "
                    + location + ", count=" + quantity + ", price=$" + price);
        }
        else {
            // If the hotel already exists, update its quantity (by adding
            // the new quantity) and update its price (only if the new price
            // is positive).
        	
        	//UNDO: Backup current state of the object and wrap it in an UndoCommand
        	ReservableItem undoObj = new Hotel(location, curObj.getCount(), curObj.getPrice());
        	undoObj.setReserved(curObj.getReserved());
            undoManager.addUndoCommand(id, new UndoByBackup(id, undoObj));
            
            curObj.setCount(curObj.getCount() + quantity);
            if (price > 0) {
                curObj.setPrice(price);
            }
            putHotel(id, Hotel.getKey(location), curObj);
            Trace.info("RM::addHotel(" + id + ") modified existing location "
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
    	
    	String itemId = Hotel.getKey(location);
        Hotel curObj = fetchHotel(id, itemId);
                
        if (curObj == null) {
        	Trace.warn("RM::deleteItem(" + id + ", " + itemId
                    + ") failed--item doesn't exist"); 
            return false;
        }
        else {
            if (curObj.getReserved() == 0) {
            	
            	//UNDO: create UndoDelete command that reverses the delete operation            	
            	Hotel undoObj = new Hotel(location, curObj.getCount(), curObj.getPrice());            	
                undoManager.addUndoCommand(id, new AddCommand(id, undoObj));
            	
                deleteHotel(id, curObj.getKey());
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
    	
        Hotel curObj = fetchHotel(id, Hotel.getKey(location));
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
    	
        Hotel curObj = fetchHotel(id, Hotel.getKey(location));
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
    	    	
        Hotel curObj = fetchHotel(id, Hotel.getKey(location));
        
        if (curObj == null) {        	
        	Trace.warn("RM::reserveRoom( " + id + ", " + customerId + ", " + location + ") failed--item doesn't exist"); 
            return null;
        }
        else if (curObj.getCount() == 0) {
        	System.out.println("Room in " + location + " couldn't be reserved because they are all reserved");
        	Trace.warn("RM::reserveRoom( " + id + ", " + customerId + ", " + location + ") failed--No more items");
            return null;
        }
        else {      
        	//UNDO: Backup current state of the object and wrap it in an UndoCommand
        	ReservableItem undoObj = new Hotel(location, curObj.getCount(), curObj.getPrice());
        	undoObj.setReserved(curObj.getReserved());
            undoManager.addUndoCommand(id, new UndoByBackup(id, undoObj));   
            
            String key = Hotel.getKey(location);
            
            // decrease the number of available items in the storage
            curObj.setCount(curObj.getCount() - 1);
            curObj.setReserved(curObj.getReserved() + 1);

            putHotel(id, key, curObj);
                        
            Trace.info("RM::reserveRoom( " + id + ", " + customerId + ", " + key + ") succeeded");
            return new ReservedItem(key, curObj.getLocation(), 1, curObj.getPrice());
        }
    }
    
    public boolean cancelItem(int id, String hotelKey, int count)
    	throws RemoteException, DeadlockException {
    	
    	System.out.println("cancelItem( " + id + ", " + hotelKey + ", " + count + " )");
    	    	
    	Hotel curObj = fetchHotel(id, hotelKey);
    	if (curObj == null) {
    		System.out.println("Room " + hotelKey + " can't be cancelled because none exists");
    		return false;
    	}
    	
    	String location = curObj.getLocation();
    	
    	try {
    		lm.Lock(id, location, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, location);
        }  
    	
    	//UNDO: Backup current state of the object and wrap it in an UndoCommand
    	ReservableItem undoObj = new Hotel(location, curObj.getCount(), curObj.getPrice());
    	undoObj.setReserved(curObj.getReserved());
        undoManager.addUndoCommand(id, new UndoByBackup(id, undoObj));
    	
    	//adjust available quantity
    	curObj.setCount(curObj.getCount() + count);
    	curObj.setReserved(curObj.getReserved() - count);
    	
    	return true;
    }
    
    private Hotel fetchHotel(int id, String itemId) {
        synchronized (roomsTable) {
            return (Hotel)roomsTable.get(itemId);
        }
    }
    
    private void putHotel(int id, String itemId, Hotel hotel) {
        synchronized (roomsTable) {
            roomsTable.put(itemId, hotel);            
        }
    }
    
    private void deleteHotel(int id, String itemId) {
        synchronized (roomsTable) {
            roomsTable.remove(itemId);
        }
    }

	@Override
	public boolean commit(int id) throws RemoteException {
		undoManager.clearUndoHistory(id);
		return lm.UnlockAll(id);
	}

	@Override
	public void abort(int id) throws RemoteException {
		undoManager.performAllUndoCommands(id);
		lm.UnlockAll(id);
	}

	@Override
	public void recoverItemState(int id, ReservableItem backup) {
		Hotel curObj = fetchHotel(id, backup.getKey());
    	
    	curObj.setCount(backup.getCount());
    	curObj.setPrice(backup.getPrice());
    	curObj.setReserved(backup.getReserved());		
	}


}
