package servercode.ResImpl;

import servercode.ResInterface.ItemManager;

public class CommandCustomerDelete extends Command {

	private ResourceManagerImpl resourceManager;
	private int id;
	private String itemId;
	
	public CommandCustomerDelete(int id, String itemId, ResourceManagerImpl rm){
		this.resourceManager = rm;
		this.id = id;
		this.itemId = itemId;
	}

	@Override
	public void execute() {
		resourceManager.removeData(id, itemId);		
	}
	
	public void execute(ItemManager im) {
		
	}
}
