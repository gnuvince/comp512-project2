package servercode.ResImpl;

import servercode.ResInterface.ItemManager;

public abstract class UndoCommand {

	protected int id;
	protected ReservableItem item;
	
	public UndoCommand(int txnId, ReservableItem i){
		id = txnId;
		item = i;		
	}	
	
	public abstract void undo(ItemManager itemManager);
	
	public String getKey() {
		return item.getLocation();
	}	
}
