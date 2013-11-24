package servercode.ResImpl;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

import servercode.ResInterface.ItemManager;
import servercode.ResInterface.ResourceManager;

public class TransactionManager {


	private ItemManager rmCar;
	private ItemManager rmFlight; 
	private ItemManager rmHotel;
	private ResourceManager rm;
	
	//Same as hashMap but thread safe
	//KEY=> TransactionId  Value=>vector of RMs (RMs are represented as strings)
	private ConcurrentHashMap<Integer, Vector<String>> xidsToRMNames = new ConcurrentHashMap<Integer, Vector<String>>();	
	private ConcurrentHashMap<Integer,	Long> timeToLiveMap = new ConcurrentHashMap<Integer, Long>(); 
	private HashMap<Integer, Boolean> transactionStatus = new HashMap<Integer, Boolean>();
	private int numberOfTransactions = 0;	
	private static TransactionManager instance = null;
	
	private long TIMEOUT = 120000;
	private Crash crashCondition;
	
	////Singleton class so private constructor
	private TransactionManager(ItemManager carRm, ItemManager flightRm,
			ItemManager hotelRm, ResourceManager rm) {
		super();
		this.rmCar = carRm;
		this.rmFlight = flightRm;
		this.rmHotel = hotelRm;
		this.rm = rm;
	}
	
	
	public static TransactionManager getInstance(ItemManager carRm, ItemManager flightRm, ItemManager hotelRm, ResourceManager rm) {		
		if(instance == null) {
			instance = new TransactionManager(carRm, flightRm, hotelRm, rm);
	    }
	
		return instance;		
	}
	
	public int start(){
		int id = 1;
				
		numberOfTransactions++;
		
		//Generate random numbers until we get one that is not already used
		while (xidsToRMNames.contains(id) || id == 1) {
			id = new Random().nextInt(10000) + 1; //+1 because can return 0
		}
		xidsToRMNames.put(id, new Vector<String>());
		
		timeToLiveMap.put(id, System.currentTimeMillis());
		
		return id;
	}
	 
	//Adds a RM as used by transaction
	public void enlist(int id, String rm) {
		Vector<String> v = xidsToRMNames.get(id);
		
		if (!v.contains(rm)) {
			v.add(rm);
		}
		
		xidsToRMNames.put(id, v);	
		timeToLiveMap.put(id, System.currentTimeMillis());
	}
	
	public boolean commit(int xid) {
		Vector<String> rms = xidsToRMNames.get(xid);
		
		int answers = 0;
		
		for (String rm: rms) {
			if (rm.equals("car")) {
				try {
					answers += rmCar.prepare(xid);
				}
				catch (RemoteException | InvalidTransactionException e) {
					
				}
			}
		}		

		// When it's time to implement logs, don't forget to NOT delete
		// the log for the current transaction if ANY rm throws CrashException.
		boolean result = answers == rms.size();
		transactionStatus.put(xid, result);
		if (result) {
			for(String rm: rms) {
				if (rm.equals("car")) {
					try {
						rmCar.commit(xid);
						disassociate(xid, "car");
					} catch (RemoteException e) {
						System.out.println("The car manager could not commit!!!");
					}
				}
			}
		}
		else {
			for(String rm: rms) {
				if (rm.equals("car")) { 
					try {
						rmCar.abort(xid);		
						disassociate(xid, "car");
					} catch (RemoteException e) {
						System.out.println("The car manager is not available!!!");
					}
				}
			}			
		}
				
		timeToLiveMap.remove(xid);
		
		return result;		
	}
	
	private void disassociate(int xid, String rmname) {
		Vector<String> rms = xidsToRMNames.get(xid);
		rms.remove(rmname);
		if (rms.isEmpty()) {
			xidsToRMNames.remove(xid);
			numberOfTransactions--;
		}
	}
	
	// For now, there isn't much difference between commit and abort !!??
	public Vector<String> abort(int id) {
		Vector<String> v = xidsToRMNames.get(id);		
		xidsToRMNames.remove(id);
		timeToLiveMap.remove(id);
		
		numberOfTransactions--;
		return v;
	}
	
	public boolean isValidTransaction(int id) {
		return xidsToRMNames.get(id) != null ? true: false;		
	}
	
	public boolean canShutdown(){
		return numberOfTransactions == 0 ? true: false;
	}
	
	public Vector<Integer> getTimedOutTransactions(){
		
		Long currentTime = System.currentTimeMillis();		
		Vector<Integer> timedOut = new Vector<Integer>();
		
		for (ConcurrentHashMap.Entry<Integer, Long> entry : timeToLiveMap.entrySet()) {
			if (currentTime - entry.getValue() > TIMEOUT){
				timedOut.add(entry.getKey());
				timeToLiveMap.remove(entry.getKey());
			}
		}
		
		return timedOut;
	}

	public void setCrashCondition(Crash crashCondition) {
		this.crashCondition = crashCondition;
	}
	
	public boolean getTransactionStatus(int xid) {
		Boolean res = transactionStatus.get(xid);
		if (res == null)
			return false;
		return res;
	}
}
	
	

