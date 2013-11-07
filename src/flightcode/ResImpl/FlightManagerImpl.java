package flightcode.ResImpl;


import hotelcode.ResImpl.Hotel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Vector;

import carcode.ResImpl.Car;

import servercode.ResInterface.*;
import servercode.ResImpl.*;
import LockManager.*;

public class FlightManagerImpl implements ItemManager {
    
    protected RMHashtable flightTable = new RMHashtable();
    
    private LockManager lm = new LockManager();
    private WorkingSetItem ws = new WorkingSetItem();
    
    public static void main(String args[]) {
    	
        int port = 5007;
        FlightManagerImpl obj = new FlightManagerImpl();

        if (args.length != 1) {            
            System.err.println("Usage: java flightcode.ResImpl.FlightManagerImpl <rmi port>");
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
            registry.rebind("Group5_FlightManager", rm);

            System.err.println("Flight Server ready");
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
    public boolean addItem(int id, String flightNum, int flightSeats, int flightPrice)
        throws RemoteException, DeadlockException {
    	
    	//Acquire write lock
    	try {
    		lm.Lock(id, flightNum, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, flightNum);
        }
    	
    	int nflightNum = Integer.valueOf(flightNum);
    	Flight curObj;
    	
    	if (ws.hasItem(flightNum)){
    		curObj = (Flight) ws.getItem(flightNum);    		
    	} else {
    		curObj = fetchFlight(id, Flight.getKey(nflightNum));    
    		
    		if (curObj != null) {
    			curObj = curObj.getCopy();
    		}
      	} 
    	
        if (curObj == null) {
            // If Flight doesn't exist, create it and add it to 
            // the manager's hash table.
            Flight newObj = new Flight(nflightNum, flightSeats, flightPrice);
            
            ws.addCommand(id, new CommandPut(id, newObj.getKey(), newObj, this));
            ws.sendCurrentState(newObj);
            ws.addLocationToTxn(id, flightNum);
            
            Trace.info("RM::addFlight(" + id + ") created new flight "
                    + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
        }
        else {
            // If the Flight already exists, update its quantity (by adding
            // the new quantity) and update its price (only if the new price
            // is positive).
        	                    	
            curObj.setCount(curObj.getCount() + flightSeats);
            if (flightPrice > 0) {
                curObj.setPrice(flightPrice);
            }
            
            ws.addCommand(id, new CommandPut(id, Flight.getKey(nflightNum), curObj, this));
            ws.sendCurrentState(curObj);            
            ws.addLocationToTxn(id, flightNum);
            
            Trace.info("RM::addFlight(" + id + ") modified existing flight "
                    + flightNum + ", seats=" + curObj.getCount() + ", price=$"
                    + curObj.getPrice());
        }
                
        return true;
    }

    @Override
    public boolean deleteItem(int id, String flightNum) throws RemoteException, DeadlockException {
    	
    	try {
    		lm.Lock(id, flightNum, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, flightNum);
        }
    	
    	int nflightNum = Integer.valueOf(flightNum);
    	
    	String itemId = Flight.getKey(nflightNum);
        Flight curObj;
        
        if (ws.hasItem(flightNum)){
    		curObj = (Flight) ws.getItem(flightNum);    		
    	} else {
    		curObj = fetchFlight(id, itemId);    		
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
            	ws.addLocationToTxn(id, flightNum);
            	
                Trace.info("RM::deleteItem(" + id + ", " + itemId + ") item deleted");
                return true;
            }
            else {
            	Trace.info("RM::deleteItem("+ id+ ", "+ itemId + ") item can't be deleted because some customers reserved it");
                return false;
            }
        } 
    }

    @Override
    public int queryItemQuantity(int id, String flightNum) throws RemoteException, DeadlockException {
    	
    	try {
    		lm.Lock(id, flightNum, LockType.READ);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, flightNum);
        }  
    	
    	int nflightNum = Integer.valueOf(flightNum);
    	Flight curObj;
    	
    	if (ws.hasItem(flightNum)){
    		curObj = (Flight) ws.getItem(flightNum);    		
    	} else {
    		curObj = fetchFlight(id, Flight.getKey(nflightNum));	
    		if (curObj != null) {
    			curObj = curObj.getCopy();
    		    		
    			ws.sendCurrentState(curObj);
    			ws.addLocationToTxn(id, flightNum);
    		}
    	}
        if (curObj != null) {
            return curObj.getCount();
        }
        
        return 0;
    }

    @Override
    public int queryItemPrice(int id, String flightNum) throws RemoteException, DeadlockException {
    	
    	try {
    		lm.Lock(id, flightNum, LockType.READ);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, flightNum);
        }  
    	
    	int nflightNum = Integer.valueOf(flightNum);
    	
    	Flight curObj;
    	
    	if (ws.hasItem(flightNum)){
    		curObj = (Flight) ws.getItem(flightNum);    		
    	} else {
    		curObj = fetchFlight(id, Flight.getKey(nflightNum));	
    		if (curObj != null) {
    			curObj = curObj.getCopy();

    			ws.sendCurrentState(curObj);
    			ws.addLocationToTxn(id, flightNum);
    		}
    	}
    	
        if (curObj != null) {
            return curObj.getPrice();
        }
        return 0;   
    }
   
    @Override
    public ReservedItem reserveItem(int id, String customerId, String flightNum)
        throws RemoteException, DeadlockException {
    	
    	try {
    		lm.Lock(id, flightNum, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, flightNum);
        }  
    	
    	int nflightNum = Integer.valueOf(flightNum);
    	
    	Flight curObj;
    	
    	if (ws.hasItem(flightNum)){
    		curObj = (Flight) ws.getItem(flightNum);    		
    	} else {
    		curObj = fetchFlight(id, Flight.getKey(nflightNum));
    		if (curObj != null) {
    			curObj = curObj.getCopy();
    			ws.sendCurrentState(curObj);
        		ws.addLocationToTxn(id,  flightNum);
    		}
    	}
        
        if (curObj == null) {
        	Trace.warn("RM::reserveItem( " + id + ", " + customerId + ", " + flightNum + ") failed--item doesn't exist");        	
            return null;
        }
        else if (curObj.getCount() == 0) {
        	Trace.warn("RM::reserveItem( " + id + ", " + customerId + ", " + flightNum + ") failed--No more items");        	
            return null;
        }
        else {               	
            String key = Flight.getKey(nflightNum);
            
            // decrease the number of available items in the storage
            curObj.setCount(curObj.getCount() - 1);
            curObj.setReserved(curObj.getReserved() + 1);

            ws.addCommand(id, new CommandPut(id, key, (ReservableItem)curObj, this));
            
            Trace.info("RM::reserveItem( " + id + ", " + customerId + ", " + key + ") succeeded");            
            
            return new ReservedItem(key, curObj.getLocation(), 1, curObj.getPrice());
        }
    }
    
    public boolean cancelItem(int id, String flightKey, int count)
    	throws RemoteException, DeadlockException {
    	
    	System.out.println("cancelItem( " + id + ", " + flightKey + ", " + count + " )");
    	
    	String segments[] = flightKey.split("-");    	
    	String flightNum = segments[1];
    	
    	try {
    		lm.Lock(id, flightNum, LockType.WRITE);    		
    	} catch (DeadlockException deadlock) {
            throw new DeadlockException(id, flightNum);
        } 
    	    	
    	Flight curObj;
    	
    	if (ws.hasItem(flightNum)){
    		curObj = (Flight) ws.getItem(flightNum);    		
    	} else {
    		curObj = fetchFlight(id, flightKey);    		
    		if (curObj != null) {
    			curObj = curObj.getCopy();
    			ws.sendCurrentState(curObj);
        		ws.addLocationToTxn(id,  flightNum);
    		}
    	}
    	
    	if (curObj == null) {
    		Trace.warn("Flight " + flightKey + " can't be cancelled because item doesn't exists");
    		return false;
    	}
    	
    	//adjust available quantity
    	curObj.setCount(curObj.getCount() + count);
    	curObj.setReserved(curObj.getReserved() - count);

    	ws.addCommand(id, new CommandPut(id, flightKey, (ReservableItem)curObj, this));
    	
    	Trace.info("Reservation of flight " + flightKey + " cancelled.");
    	
    	return true;
    }
            
    private Flight fetchFlight(int id, String itemId) {
        synchronized (flightTable) {
            return (Flight)flightTable.get(itemId);
        }
    }
    
    public void putFlight(int id, String itemId, Flight Flight) {
        synchronized (flightTable) {
        	flightTable.put(itemId, Flight);            
        }
    }
    
    public void deleteFlight(int id, String itemId) {
        synchronized (flightTable) {
        	flightTable.remove(itemId);
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
	
	public void recoverItemState(int id, ReservableItem flightBackup){
    	Flight curObj = fetchFlight(id, flightBackup.getKey());
    	
    	curObj.setCount(flightBackup.getCount());
    	curObj.setPrice(flightBackup.getPrice());
    	curObj.setReserved(flightBackup.getReserved());
    }
	
}
