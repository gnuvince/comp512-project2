package servercode.ResImpl;

import servercode.ResInterface.ItemManager;

//Undo an operation using value backup taken before the item is modified
public class UndoByBackup extends UndoCommand {		
		
	public UndoByBackup(int id, ReservableItem item){
		super(id, item);				
	}
				
	@Override
	public void undo(ItemManager itemManager) {
		
		try {			
			itemManager.recoverItemState(id, item);
		} catch (Exception e) {
			//should not get exception since when we undo we already have all the locks we need.			
		}
	}
}