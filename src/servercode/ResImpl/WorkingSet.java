package servercode.ResImpl;

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class WorkingSet<StateType> {
	
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
	
	public void commit(int id){
		Vector<Command> commands = idToCommandsMap.get(id);
		
		if (commands != null){
			for (Command c: commands) {
				c.execute();
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
	
	public void abort(int id){
		Vector<String> locations = idToLocationsMap.get(id);

		if (locations != null){
			for(String location: locations) {
				currentStatesMap.remove(location);
			}
		}

		idToCommandsMap.remove(id);	
		idToLocationsMap.remove(id);
	}
	
	
	
	public boolean hasItem(String location){
		return currentStatesMap.containsKey(location);
	}

	public StateType getItem(String location){
		return currentStatesMap.get(location);
	}

	public void sendCurrentState(String location, StateType item){
		currentStatesMap.put(location, item);		
	}

	public void deleteItem(String item){
		currentStatesMap.put(item, null);
	}
}
