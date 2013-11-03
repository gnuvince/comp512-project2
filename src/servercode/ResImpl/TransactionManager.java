package servercode.ResImpl;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {

	//Same as hashMap but thread safe
	//KEY=> TransactionId  Value=>vector of RMs (RMs are represented as strings)
	private ConcurrentHashMap<Integer, Vector<String>> hashMap = new ConcurrentHashMap<Integer, Vector<String>>();	
	private int numberOfTransactions = 0;	
	private static TransactionManager instance = null;

	////Singleton class so private constructor
	private TransactionManager(){ }
	
	public static TransactionManager getInstance() {		
		if(instance == null) {
			instance = new TransactionManager();
	    }
	
		return instance;		
	}
	
	public int start(){
		int id = 1;
		
		numberOfTransactions++;
		
		//Generate random numbers until we get one that is not already used
		while (hashMap.contains(id) || id == 1) {
			id = new Random().nextInt(100) + 1; //+1 because can return 0
		}
		hashMap.put(id, new Vector<String>());
		
		return id;
	}
	 
	//Adds a RM as used by transaction
	public void enlist(int id, String rm) {
		Vector<String> v = hashMap.get(id);
		
		if (!v.contains(rm)) {
			v.add(rm);
		}
		
		hashMap.put(id, v);		
	}
	
	public Vector<String> commit(int id) {
		Vector<String> v = hashMap.get(id);
		hashMap.remove(id);
		
		numberOfTransactions--;
		return v;		
	}
	
	// For now, there isn't much difference between commit and abort !!??
	public Vector<String> abort(int id) {
		Vector<String> v = hashMap.get(id);		
		hashMap.remove(id);
		
		numberOfTransactions--;
		return v;
	}
	
	public boolean isValidTransaction(int id) {
		return hashMap.get(id) != null ? true: false;		
	}
	
	public boolean canShutdown(){
		return numberOfTransactions == 0 ? true: false;
	}
}
	
	

