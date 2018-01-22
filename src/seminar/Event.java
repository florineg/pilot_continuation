package seminar;

public class Event {
	private int j; 
	private int n; 
	private boolean r; // true (1) if aircraft, false (0) if simulator
	private boolean e; // true (1) if day, false (0) if night 
	
	public Event(int eventj, int nrPilots, boolean aircraft, boolean day) {
		j = eventj; 
		n = nrPilots; 
		r = aircraft; 
		e = day; 
	}
	
	/**
	 * setters
	 * @return 
	 */

	/**
	 * getters
	 */
	
	public int getN() {
		return n; 
	}
	
	public int getR() {
		int r_int; 
		if (r) {
			r_int = 1;
		}
		else {
			r_int = 0; 
		}
		return r_int; 
	}
	
	public int getNr() {
		return j; 
	}
	
	public int getE() {
		int e_int; 
		if (e) {
			e_int = 1; 
		}
		else {
			e_int = 0; 
		}
		return e_int; 
	}

}
