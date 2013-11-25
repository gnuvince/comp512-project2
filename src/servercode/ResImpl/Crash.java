package servercode.ResImpl;


public enum Crash {
	
	P_B_SAVEWS, 		//txn will be aborted
	P_A_SAVEWS,			//txnManager will catch exception so txn will be aborted
	P_A_COMMITRECV,		//txnManager decides; RM ask at recovery time
	C_B_2PC,
	C_B_VR, 
	C_A_PREPAREREQUEST,
	C_A_ALLYES,
	C_A_SELFCOMMIT,
	C_A_SENDCOMMIT;

}
