package servercode.ResImpl;

import java.util.HashMap;
import java.util.Vector;

public class WorkingSetCustomer extends WorkingSet {

	private HashMap<Integer, Customer> currentStatesMap = new HashMap<Integer, Customer>();
	
	public boolean hasItem(Integer custId){
		return currentStatesMap.containsKey(custId);
	}
	
	public Customer getItem(Integer custId){
		return currentStatesMap.get(custId);
	}
	
	public void sendCurrentState(Customer customer){
		currentStatesMap.put(customer.getID(), customer);		
	}
	
	public void deleteItem(Integer custId){
		currentStatesMap.put(custId, null);
	}
	
	public void commit(int id){
		super.commit(id);
		currentStatesMap.remove(id);
	}
	
	public void abort(int id){
		Vector<String> items = idToLocationsMap.get(id);
		
		if (items != null){
			for(String item: items) {
				currentStatesMap.remove(item);
			}
		}
		
		super.abort(id);		
	}
}
