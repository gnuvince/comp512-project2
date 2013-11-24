package servercode.ResInterface;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;


//import flightcode.ResImpl.Flight;
import LockManager.DeadlockException;
import servercode.ResImpl.Crash;
import servercode.ResImpl.CrashException;
import servercode.ResImpl.InvalidTransactionException;
import servercode.ResImpl.ReservedItem;
import servercode.ResImpl.ReservableItem;

public interface ItemManager extends Remote, Serializable {
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
     * @throws DeadlockException 
     */
    public boolean addItem(int id, String itemId, int quantity, int price)
        throws RemoteException, DeadlockException;

    /**
     * Delete an item from the manager's hash table.
     * 
     * @param id
     *            unused
     * @param itemId
     *            location or flight number to delete
     * @return true if operation succeeded, false otherwise
     * @throws RemoteException
     * @throws DeadlockException 
     */
    public boolean deleteItem(int id, String itemId) throws RemoteException, DeadlockException;

    /**
     * Return the number of available items of the given itemId.
     * 
     * @param id
     *            unused
     * @param itemId
     *            location or flight number
     * @return number of available items; 0 if itemId does not exist
     * @throws RemoteException
     * @throws DeadlockException 
     */
    public int queryItemQuantity(int id, String itemId) throws RemoteException, DeadlockException;

    /**
     * Return the price of the given itemId.
     * 
     * @param id
     *            unused
     * @param itemId
     *            location or flight number
     * @return price of the item; 0 if itemId does not exist
     * @throws RemoteException
     * @throws DeadlockException 
     */
    public int queryItemPrice(int id, String itemId) throws RemoteException, DeadlockException;

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
     * @throws DeadlockException 
     */
    public ReservedItem reserveItem(int id, String customerId, String itemId)
        throws RemoteException, DeadlockException;

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
     * @throws DeadlockException 
     */
    public boolean cancelItem(int id, String itemId, int count)
        throws RemoteException, DeadlockException;
    
    /**
     * Commit a transaction.
     * 
     * @param id
     *            TXN id
     * @return true if commit is successful, false otherwise
     * @throws RemoteException
     */
    public boolean commit(int id) throws RemoteException;
    
    public void abort(int id) throws RemoteException;
    
    public void shutDown() throws RemoteException;
    
    public int prepare(int xid) throws RemoteException, InvalidTransactionException;    
    
    public void setCrashCondition(Crash crashCondition) throws RemoteException;
}
