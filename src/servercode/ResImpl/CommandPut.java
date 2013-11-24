package servercode.ResImpl;

import flightcode.ResImpl.Flight;
import flightcode.ResImpl.FlightManagerImpl;
import hotelcode.ResImpl.Hotel;
import hotelcode.ResImpl.HotelManagerImpl;
import servercode.ResInterface.ItemManager;
import carcode.ResImpl.Car;
import carcode.ResImpl.CarManagerImpl;


public class CommandPut extends Command{
	
	private ItemManager itemManager;
	private int id;
	private String itemId;
	private ReservableItem newObj;	
	
	
	public CommandPut(int id, String itemId, ReservableItem newObj, ItemManager im){
		this.itemManager = im;
		this.id = id;
		this.itemId = itemId;
		this.newObj = newObj;	
	}

	public void execute() {
		this.execute(this.itemManager);	
	}
	
	public void execute(ItemManager im) {
		
		if (im instanceof CarManagerImpl){
			((CarManagerImpl) im).putCar(id, itemId, (Car)newObj);
		}
		
		if (im instanceof HotelManagerImpl){
			((HotelManagerImpl) im).putHotel(id, itemId, (Hotel)newObj);
		}
		
		if (im instanceof FlightManagerImpl){
			((FlightManagerImpl) im).putFlight(id, itemId, (Flight)newObj);
		}
	}
	

}
