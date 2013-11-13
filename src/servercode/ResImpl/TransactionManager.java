package servercode.ResImpl;

import java.rmi.RemoteException;
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
	private ConcurrentHashMap<Integer, Vector<String>> hashMap = new ConcurrentHashMap<Integer, Vector<String>>();	
	private ConcurrentHashMap<Integer,	Long> timeToLiveMap = new ConcurrentHashMap<Integer, Long>(); 
	private int numberOfTransactions = 0;	
	private static TransactionManager instance = null;
	
	private long TIMEOUT = 120000;
	
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
		while (hashMap.contains(id) || id == 1) {
			id = new Random().nextInt(10000) + 1; //+1 because can return 0
		}
		hashMap.put(id, new Vector<String>());
		
		timeToLiveMap.put(id, System.currentTimeMillis());
		
		return id;
	}
	 
	//Adds a RM as used by transaction
	public void enlist(int id, String rm) {
		Vector<String> v = hashMap.get(id);
		
		if (!v.contains(rm)) {
			v.add(rm);
		}
		
		hashMap.put(id, v);	
		timeToLiveMap.put(id, System.currentTimeMillis());
	}
	
	public Vector<String> commit(int xid) {
		Vector<String> v = hashMap.get(xid);
		
		/*
		 * for each rm in v:
		 *   Thread.run {
		 *     rm.prepare(xid)
		 *     wait for response
		 *   }
		 * if all YES:
		 *   commit
		 *   send commit to v
		 * else:
		 *   abort
		 *   send abort to v 
		 */
		
		int answers = 0;
		
		for (String rm: v) {
			if (rm.equals("car")) {
				try {
					answers += rmCar.prepare(xid);
				}
				catch (RemoteException | InvalidTransactionException e) {
					// Nothing.
				}
			}
		}
		
		if (answers == v.size()) {
			for(String rm: v) {
				if (rm.equals("car")) {
					try {
						rmCar.commit(xid);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else {
			for(String rm: v) {
				if (rm.equals("car")) { 
					try {
						rmCar.abort(xid);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		hashMap.remove(xid);
		timeToLiveMap.remove(xid);
		
		numberOfTransactions--;
		return v;		
	}
	
	// For now, there isn't much difference between commit and abort !!??
	public Vector<String> abort(int id) {
		Vector<String> v = hashMap.get(id);		
		hashMap.remove(id);
		timeToLiveMap.remove(id);
		
		numberOfTransactions--;
		return v;
	}
	
	public boolean isValidTransaction(int id) {
		return hashMap.get(id) != null ? true: false;		
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
}
	
	

