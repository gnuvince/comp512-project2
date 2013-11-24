package servercode.ResImpl;

import flightcode.ResImpl.FlightManagerImpl;
import hotelcode.ResImpl.HotelManagerImpl;
import servercode.ResInterface.ItemManager;
import carcode.ResImpl.Car;
import carcode.ResImpl.CarManagerImpl;

public class CommandDelete extends Command {
	
	private ItemManager itemManager;
	private int id;
	private String itemId;
	
	public CommandDelete(int id, String itemId, ItemManager im){
		this.itemManager = im;
		this.id = id;
		this.itemId = itemId;		
	}
	
	public void execute() {
		this.execute(this.itemManager);	
	}
	
	public void execute(ItemManager im){
		
		if (im instanceof CarManagerImpl){
			((CarManagerImpl) im).deleteCar(id, itemId);			
		}
		
		if (im instanceof HotelManagerImpl){
			((HotelManagerImpl) im).deleteHotel(id, itemId);			
		}
		
		if (im instanceof FlightManagerImpl){
			((FlightManagerImpl) im).deleteFlight(id, itemId);			
		}
			
	}

}
