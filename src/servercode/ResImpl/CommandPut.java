package servercode.ResImpl;

import flightcode.ResImpl.Flight;
import flightcode.ResImpl.FlightManagerImpl;
import hotelcode.ResImpl.Hotel;
import hotelcode.ResImpl.HotelManagerImpl;
import servercode.ResInterface.ItemManager;
import servercode.ResInterface.ResourceManager;
import carcode.ResImpl.Car;
import carcode.ResImpl.CarManagerImpl;


public class CommandPut extends Command{
	
	private int id;
	private String itemId;
	private ReservableItem newObj;	
	
	
	public CommandPut(int id, String itemId, ReservableItem newObj){
		this.id = id;
		this.itemId = itemId;
		this.newObj = newObj;	
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

	@Override
	public void execute(ResourceManager mw) {
		// TODO Auto-generated method stub
		
	}
	

}
