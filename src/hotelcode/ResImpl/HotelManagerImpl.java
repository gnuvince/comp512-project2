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
    private WorkingSetItem ws = new WorkingSetItem();
    
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
    	
    	Hotel curObj;
    	
    	if (ws.hasItem(location)){
    		curObj = (Hotel) ws.getItem(location);    		
    	} else {
    		curObj = fetchHotel(id, Hotel.getKey(location));    
    		
    		if (curObj != null) {
    			curObj = curObj.getCopy();
    		}
      	}    	
        
        if (curObj == null) {
            // If hotel doesn't exist, create it and add it to 
            // the manager's hash table.
            Hotel newObj = new Hotel(location, quantity, price);
            
            ws.addCommand(id, new CommandPut(id, newObj.getKey(), newObj, this));
            ws.sendCurrentState(newObj);
            ws.addLocationToTxn(id, location);            
            
            Trace.info("RM::addHotel(" + id + ") created new location "
                    + location + ", count=" + quantity + ", price=$" + price);
        }
        else {
            // If the hotel already exists, update its quantity (by adding
            // the new quantity) and update its price (only if the new price
            // is positive).
        	            
            curObj.setCount(curObj.getCount() + quantity);
            if (price > 0) {
                curObj.setPrice(price);
            }
            
            ws.addCommand(id, new CommandPut(id, Hotel.getKey(location), curObj, this));
            ws.sendCurrentState(curObj);            
            ws.addLocationToTxn(id, location);

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
        Hotel curObj;
        
        if (ws.hasItem(location)){
    		curObj = (Hotel) ws.getItem(location);    		
    	} else {
    		curObj = fetchHotel(id, itemId);    		
    		if (curObj != null) {
    			curObj = curObj.getCopy();
    		}
    	}
                
        if (curObj == null) {
        	Trace.warn("RM::deleteItem(" + id + ", " + itemId
                    + ") failed--item doesn't exist"); 
            return false;
        }
        else {
            if (curObj.getReserved() == 0) {            	
            	
            	ws.sendCurrentState(curObj);
            	
            	ws.deleteItem(curObj.getLocation()); //the item stays in ws but its current state is set to null
            	
            	ws.addCommand(id, new CommandDelete(id, curObj.getKey(), this));
            	ws.addLocationToTxn(id, location);

                Trace.info("RM::deleteItem(" + id + ", " + itemId+ ") item deleted");
                return true;
            }
            else {
            	Trace.info("RM::deleteItem("+ id + ", " + itemId + ") item can't be deleted because some customers reserved it");
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
    	
    	Hotel curObj;
    	
    	if (ws.hasItem(location)){
    		curObj = (Hotel) ws.getItem(location);    		
    	} else {
    		curObj = fetchHotel(id, Hotel.getKey(location));	
    		if (curObj != null) {
    			curObj = curObj.getCopy();
    		    		
    			ws.sendCurrentState(curObj);
    			ws.addLocationToTxn(id, location);
    		}
    	}
    	
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
    	
    	Hotel curObj;
    	
    	if (ws.hasItem(location)){
    		curObj = (Hotel) ws.getItem(location);    		
    	} else {
    		curObj = fetchHotel(id, Hotel.getKey(location));	
    		if (curObj != null) {
    			curObj = curObj.getCopy();

    			ws.sendCurrentState(curObj);
    			ws.addLocationToTxn(id, location);
    		}
    	}
    	
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
    	    	
    	Hotel curObj;
    	
    	if (ws.hasItem(location)){
    		curObj = (Hotel) ws.getItem(location);    		
    	} else {
    		curObj = fetchHotel(id, Hotel.getKey(location));
    		if (curObj != null) {
    			curObj = curObj.getCopy();
    			ws.sendCurrentState(curObj);
        		ws.addLocationToTxn(id,  location);
    		}
    	}
        
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
            String key = Hotel.getKey(location);
            
            // decrease the number of available items in the storage
            curObj.setCount(curObj.getCount() - 1);
            curObj.setReserved(curObj.getReserved() + 1);

            ws.addCommand(id, new CommandPut(id, key, (ReservableItem)curObj, this));
                        
            Trace.info("RM::reserveRoom( " + id + ", " + customerId + ", " + key + ") succeeded");
            return new ReservedItem(key, curObj.getLocation(), 1, curObj.getPrice());
        }
    }
    
    public boolean cancelItem(int id, String hotelKey, int count)
    	throws RemoteException, DeadlockException {
    	
    	System.out.println("cancelItem( " + id + ", " + hotelKey + ", " + count + " )");
    	
    	String segments[] = hotelKey.split("-");    	
    	String location = segments[1];
    	
    	try {
    		lm.Lock(id, location, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, location);
        }  
    	    	    	
    	Hotel curObj;
    	
    	if (ws.hasItem(location)){
    		curObj = (Hotel) ws.getItem(location);    		
    	} else {
    		curObj = fetchHotel(id, hotelKey);    		
    		if (curObj != null) {
    			curObj = curObj.getCopy();
    			ws.sendCurrentState(curObj);
        		ws.addLocationToTxn(id,  location);
    		}
    	}
    	
    	if (curObj == null) {
    		System.out.println("Room " + hotelKey + " can't be cancelled because none exists");
    		return false;
    	}
    	    	
    	//adjust available quantity
    	curObj.setCount(curObj.getCount() + count);
    	curObj.setReserved(curObj.getReserved() - count);
    	
    	ws.addCommand(id, new CommandPut(id, hotelKey, (ReservableItem)curObj, this));
    	
    	return true;
    }
    
    private Hotel fetchHotel(int id, String itemId) {
        synchronized (roomsTable) {
            return (Hotel)roomsTable.get(itemId);
        }
    }
    
    public void putHotel(int id, String itemId, Hotel hotel) {
        synchronized (roomsTable) {
            roomsTable.put(itemId, hotel);            
        }
    }
    
    public void deleteHotel(int id, String itemId) {
        synchronized (roomsTable) {
            roomsTable.remove(itemId);
        }
    }

	@Override
	public boolean commit(int id) throws RemoteException {
		ws.commit(id);		
		return lm.UnlockAll(id);
	}

	@Override
	public void abort(int id) throws RemoteException {
		ws.abort(id);
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
