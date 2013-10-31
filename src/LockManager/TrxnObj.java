package LockManager;

public class TrxnObj extends XObj {
    protected String        strData  = null;
    protected LockType      lockType = null;

    // The data members inherited are
    // XObj::protected int xid = 0;

    TrxnObj() {
        super();
        this.strData = null;
        this.lockType = null;
    }

    TrxnObj(int xid, String strData, LockType lockType) {
        super(xid);
        this.strData = new String(strData);
        this.lockType = lockType;
    }

    public String toString() {
        String outString = new String(super.toString() + "::strData("
            + this.strData + ")::lockType(" + this.lockType + ")");
        return outString;
    }

    public boolean equals(Object t) {
        if (t == null) {
            return false;
        }

        return (t instanceof TrxnObj) 
            && (this.xid == ((TrxnObj) t).getXId()) 
            && (this.strData.equals(((TrxnObj) t).getDataName()))
            && (this.lockType == ((TrxnObj) t).getLockType());
    }

    public Object clone() {
        TrxnObj t = new TrxnObj(this.xid, this.strData, this.lockType);
        return t;
    }

    public void setDataName(String strData) {
        this.strData = new String(strData);
    }

    public String getDataName() {
        // Create a copy to avoid accidental reference sharing?
        String strData = new String(this.strData);
        return strData;
    }

    public void setLockType(LockType lockType) {
        this.lockType = lockType;
    }

    public LockType getLockType() {
        return this.lockType;
    }
}
