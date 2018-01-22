package seminar;

import java.util.ArrayList;

import ilog.concert.IloException;

public class Main {

	public static void main(String [ ] args) throws IloException, objectNotFoundException {
		World thisWorld = new World();  
		thisWorld.runMaxModel();
	}
}
