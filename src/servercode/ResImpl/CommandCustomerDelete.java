package servercode.ResImpl;

import servercode.ResInterface.ItemManager;
import servercode.ResInterface.ResourceManager;

public class CommandCustomerDelete extends Command {

	private int id;
	private String itemId;
	
	public CommandCustomerDelete(int id, String itemId){
		this.id = id;
		this.itemId = itemId;
	}

	@Override
	public void execute(ResourceManager mw) {
		((ResourceManagerImpl)mw).removeData(id, itemId);
	}
	
	public void execute(ItemManager im) {
		
	}
}
