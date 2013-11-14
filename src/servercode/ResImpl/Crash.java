package servercode.ResImpl;


public enum Crash {
	
	P_A_PREPARE,       // Same as crash before prepare()
	P_A_COMMITRECV,    // Same as crash after send YES
	C_B_2PC,
	C_A_PREPAREREQUEST,
	C_A_ALLYES,
	C_A_SELFCOMMIT,
	C_A_SENDCOMMIT;

}
