package servercode.ResImpl;

import servercode.ResInterface.ItemManager;

//Encapsulate the command to undo an AddItem operation
public class DeleteCommand extends UndoCommand {
	
	public DeleteCommand(int id, ReservableItem item){
		super(id, item);			
	}
	
	@Override
	public void undo(ItemManager itemManager) {
		
		try {
			itemManager.deleteItem(id, item.getLocation());				
		} catch (Exception e) {
			//should not get exception since when we undo we already have all the locks we need.			
		}
	}	
}
