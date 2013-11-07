package servercode.ResImpl;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class WorkingSet {
	
	private ConcurrentHashMap<Integer, Vector<Command>> idToCommandsMap = new ConcurrentHashMap<Integer, Vector<Command>>();
	protected ConcurrentHashMap<Integer, Vector<String>> idToLocationsMap = new ConcurrentHashMap<Integer, Vector<String>>();
		
	public WorkingSet(){
		
	}
	
	public void addCommand(int id, Command cmd){
		Vector<Command> commands = idToCommandsMap.get(id);		
		
		if (commands == null){
			commands = new Vector<Command>();
		}
		
		commands.add(cmd);
		idToCommandsMap.put(id, commands);		
	}
	
	public void setItemForTxn(int id, String item){
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
		
		idToCommandsMap.remove(id);		
		idToLocationsMap.remove(id);
	}
	
	protected void abort(int id){
		idToCommandsMap.remove(id);	
		idToLocationsMap.remove(id);
	}
	
	

}
