package servercode.ResInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import servercode.ResImpl.ReservedItem;

public interface ItemManager extends Remote {
    /**
     * Add an item to the manager's hash table.
     * 
     * @param id
     *            unused
     * @param itemId
     *            location or flight number (flight number is actually an int)
     * @param quantity
     * @param price
     * @return true if operation succeeded, false otherwise
     * @throws RemoteException
     */
    public boolean addItem(int id, String itemId, int quantity, int price)
        throws RemoteException;

    /**
     * Delete an item from the manager's hash table.
     * 
     * @param id
     *            unused
     * @param itemId
     *            location or flight number to delete
     * @return true if operation succeeded, false otherwise
     * @throws RemoteException
     */
    public boolean deleteItem(int id, String itemId) throws RemoteException;

    /**
     * Return the number of available items of the given itemId.
     * 
     * @param id
     *            unused
     * @param itemId
     *            location or flight number
     * @return number of available items; 0 if itemId does not exist
     * @throws RemoteException
     */
    public int queryItemQuantity(int id, String itemId) throws RemoteException;

    /**
     * Return the price of the given itemId.
     * 
     * @param id
     *            unused
     * @param itemId
     *            location or flight number
     * @return price of the item; 0 if itemId does not exist
     * @throws RemoteException
     */
    public int queryItemPrice(int id, String itemId) throws RemoteException;

    /**
     * Make a reservation of itemId for customer. The reservation is done in
     * both directions: - item is added to customer's list of reservation -
     * customer is added to item manager's hash table
     * 
     * @param id
     *            unused
     * @param customer
     *            the customer making the reservation
     * @param itemId
     *            location or flight number
     * @return true if reservation is successful, false otherwise
     * @throws RemoteException
     */
    public ReservedItem reserveItem(int id, String customerId, String itemId)
        throws RemoteException;

    /**
     * Cancel a reservation - item quantity is adjusted in hash table
     * 
     * @param id
     *            unused
     * @param itemId
     *            the item being cancelled
     * @param count
     *            the number of item being cancelled
     * @return true if cancellation is successful, false otherwise
     * @throws RemoteException
     */
    public boolean cancelItem(int id, String itemId, int count)
        throws RemoteException;
}
