package servercode.ResImpl;

import java.io.Serializable;

import servercode.ResInterface.ItemManager;

public abstract class Command implements Serializable {
	
	public Command(){		
		
	}
	
	public abstract void execute();
	public abstract void execute(ItemManager im);

}
