package seminar;

import java.io.IOException;
import java.util.ArrayList;

import ilog.concert.IloException;

public class Main {

	public static void main(String [ ] args) throws IloException, objectNotFoundException, IOException {
		double before = System.nanoTime();
		World thisWorld = new World();  
		thisWorld.runMaxModel();
		double after = System.nanoTime(); 
		System.out.print("time to run program in hours" + (after-before)/1000000000/60/60);
	}
}
