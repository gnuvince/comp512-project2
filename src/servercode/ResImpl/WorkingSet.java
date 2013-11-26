package servercode.ResImpl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import servercode.ResInterface.ItemManager;
import servercode.ResInterface.ResourceManager;

public class WorkingSet<StateType> implements Serializable {
	
	private ConcurrentHashMap<Integer, Vector<Command>> idToCommandsMap = new ConcurrentHashMap<Integer, Vector<Command>>();
	protected ConcurrentHashMap<Integer, Vector<String>> idToLocationsMap = new ConcurrentHashMap<Integer, Vector<String>>();
	private HashMap<String, StateType> currentStatesMap = new HashMap<String, StateType>();

		
	public WorkingSet() {
	}
	
	public void addCommand(int id, Command cmd){
		Vector<Command> commands = idToCommandsMap.get(id);		
		
		if (commands == null){
			commands = new Vector<Command>();
		}
		
		commands.add(cmd);
		idToCommandsMap.put(id, commands);		
	}
	
	public void addLocationToTxn(int id, String item){
		Vector<String> items = idToLocationsMap.get(id);
		if (items == null){
			items = new Vector<String>();
		}
		items.add(item);
		
		idToLocationsMap.put(id, items);		
	}
	
	public synchronized void commit(int id, ItemManager im){
		Vector<Command> commands = idToCommandsMap.get(id);
		if (commands == null) return; //in case MW crashed and resends decision but decision was already received at that specific RM
				
		if (commands != null){
			for (Command c: commands) {
				c.execute(im);
			}		
		}
		
		//Remove currentStates of object that the txn modified
		Vector<String> locations = idToLocationsMap.get(id);
		if (locations != null){
			for(String location: locations) {
				currentStatesMap.remove(location);			
			}
		}
		
		idToCommandsMap.remove(id);
		idToLocationsMap.remove(id);
	}
	
	public synchronized void commit(int id, ResourceManager mw){
		Vector<Command> commands = idToCommandsMap.get(id);
		if (commands == null) return;
		
		if (commands != null){
			for (Command c: commands) {
				c.execute(mw);
			}		
		}
		
		//Remove currentStates of object that the txn modified
		Vector<String> locations = idToLocationsMap.get(id);
		if (locations != null){
			for(String location: locations) {
				currentStatesMap.remove(location);			
			}
		}
		
		idToCommandsMap.remove(id);
		idToLocationsMap.remove(id);
	}
	
	
	public synchronized void abort(int id){
		Vector<String> locations = idToLocationsMap.get(id);

		if (locations != null){
			for(String location: locations) {
				currentStatesMap.remove(location);
			}
		}

		idToCommandsMap.remove(id);	
		idToLocationsMap.remove(id);
	}
	
	
	
	public synchronized boolean hasItem(String location){
		return currentStatesMap.containsKey(location);
	}

	public synchronized StateType getItem(String location){
		return currentStatesMap.get(location);
	}

	public synchronized void sendCurrentState(String location, StateType item){
		currentStatesMap.put(location, item);		
	}

	public synchronized void deleteItem(String item){
		currentStatesMap.put(item, null);
	}
	
	public Set<Integer> getAllTransactions() {
		return idToCommandsMap.keySet();
	}
}
