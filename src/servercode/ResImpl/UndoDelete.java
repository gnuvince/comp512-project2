package servercode.ResImpl;

import servercode.ResInterface.ItemManager;

//Encapsulate the command to undo a deleteItem operation
public class UndoDelete extends UndoCommand {		
		
	public UndoDelete(int id, ReservableItem item){
		super(id, item);				
	}
				
	@Override
	public void undo(ItemManager itemManager) {
		
		try {
			itemManager.addItem(id, item.getLocation(), item.getCount(), item.getPrice());
		} catch (Exception e) {
			//should not get exception since when we undo we already have all the locks we need.			
		}
	}
}
