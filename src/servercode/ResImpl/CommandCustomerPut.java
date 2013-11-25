package servercode.ResImpl;

import servercode.ResInterface.ItemManager;
import servercode.ResInterface.ResourceManager;

public class CommandCustomerPut extends Command {

	private int id;
	private String itemId;
	private Customer newCust;	
	
	public CommandCustomerPut(int id, String itemId, Customer newCustomer){
		this.id = id;
		this.itemId = itemId;
		this.newCust = newCustomer;	
	}

	@Override
	public void execute(ResourceManager mw) {
		((ResourceManagerImpl)mw).writeData(id, itemId, newCust);
	}
	
	public void execute(ItemManager im) {

	}
}
