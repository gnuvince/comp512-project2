package servercode.ResImpl;

import servercode.ResInterface.ItemManager;

//Encapsulate the command to undo a deleteItem operation
public class AddCommand extends UndoCommand {		
		
	public AddCommand(int id, ReservableItem item){
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
