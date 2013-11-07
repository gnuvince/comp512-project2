package servercode.ResImpl;

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class WorkingSetItem extends WorkingSet {

	private HashMap<String, ReservableItem> currentStatesMap = new HashMap<String, ReservableItem>();
	
	public WorkingSetItem() {		
		
	}
		
	public boolean hasItem(String location){
		//return currentStatesMap.contains(location);
		return currentStatesMap.containsKey(location);
	}
	
	public ReservableItem getItem(String location){
		return currentStatesMap.get(location);
	}
	
	public void sendCurrentState(ReservableItem item){
		currentStatesMap.put(item.getLocation(), item);		
	}
	
	public void deleteItem(String item){
		currentStatesMap.put(item, null);
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
