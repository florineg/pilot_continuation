package seminar;

import java.io.IOException;
import java.util.ArrayList;

import ilog.concert.IloException;

public class Main {

	public static void main(String [ ] args) throws IloException, objectNotFoundException, IOException {
		World thisWorld = new World();  
		thisWorld.runMaxModel();
	}
}
