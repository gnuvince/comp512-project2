package servercode.ResImpl;

import java.io.Serializable;

import servercode.ResInterface.ItemManager;
import servercode.ResInterface.ResourceManager;

public abstract class Command implements Serializable {
	
	public Command(){		
		
	}
	
	public abstract void execute(ItemManager im);
	public abstract void execute(ResourceManager mw);

}
