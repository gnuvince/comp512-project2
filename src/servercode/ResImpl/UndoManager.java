package servercode.ResImpl;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import servercode.ResInterface.ItemManager;

public class UndoManager {
	
	//Same as HashMap but thread safe
	//Key=>Transaction id  Value=>Vector of UndoCommand objects (subtypes of UndoCommand) 
	private ConcurrentHashMap<Integer, Vector<UndoCommand>> undoMap = new ConcurrentHashMap<Integer, Vector<UndoCommand>>();
	private ItemManager itemManager;

	public UndoManager(ItemManager im){
		//UndoManager needs to know on which RM it is running to call commands on the good one (through UndoCommands)
		itemManager = im;
	}
	
	//
	public void addUndoCommand(int id, UndoCommand newUndo){		
		//Retrieve all undo commands for this transaction
	    Vector<UndoCommand> undoVector = undoMap.get(id);
	    
	    if (undoVector == null) {
	    	undoVector = new Vector<UndoCommand>();
	    }
	                
	    //Verify that we don't already have an undo command for this item 
	    for (UndoCommand undo: undoVector) {
	    	if (undo.getKey().equals(newUndo.getKey())){
	    		return ; //Just ignore the new undoCommand because we got one for this transaction already
	    		//If many operations happen on the same resource, we want to keep only the first one.	    		
	    	}
	    }	    
	    	
	    undoVector.add(newUndo);
	    undoMap.put(id, undoVector);
	}
	
	public void performAllUndoCommands(int id) {
		Vector<UndoCommand> undoVector = undoMap.get(id);
		
		if (undoVector != null) {
			for (UndoCommand undo: undoVector) {
				undo.undo(itemManager);
			}
		}		
		
		clearUndoHistory(id);
	}
	
	//clear hashMap of its undo commands for this transaction
	public void clearUndoHistory(int id){
		undoMap.remove(id);
	}	

}
