package servercode.ResImpl;

import flightcode.ResImpl.FlightManagerImpl;
import hotelcode.ResImpl.HotelManagerImpl;
import servercode.ResInterface.ItemManager;
import servercode.ResInterface.ResourceManager;
import carcode.ResImpl.Car;
import carcode.ResImpl.CarManagerImpl;

public class CommandDelete extends Command {
	
	private int id;
	private String itemId;
	
	public CommandDelete(int id, String itemId){
		this.id = id;
		this.itemId = itemId;		
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

	@Override
	public void execute(ResourceManager mw) {
		// TODO Auto-generated method stub
		
	}

}
