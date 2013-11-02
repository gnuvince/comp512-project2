package servercode.ResImpl;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import servercode.ResInterface.ItemManager;

public class UndoManager {
	
	private ConcurrentHashMap<Integer, Vector<UndoCommand>> undoMap = new ConcurrentHashMap<Integer, Vector<UndoCommand>>();
	private ItemManager itemManager;
	
	public UndoManager(ItemManager im){
		itemManager = im;
	}
	
	public void addUndoCommand(int id, UndoCommand newUndo){		
		//Retrieve all undo commands for this transaction
	    Vector<UndoCommand> undoVector = undoMap.get(id);
	    
	    if (undoVector == null) {
	    	undoVector = new Vector<UndoCommand>();
	    }
	                
	    //Verify that we don't already have an undo command for this item 
	    for (UndoCommand undo: undoVector) {
	    	if (undo.getKey().equals(newUndo.getKey())){
	    		return ; //Just ignore the new undoCommand
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
		
		//clear hashMap of its undo commands for this transaction
		clearUndoHistory(id);
	}
	
	public void clearUndoHistory(int id){
		undoMap.remove(id);
	}
	

}
