package seminar;

public class Plane {

	private int nr; 
	private boolean type; // true if aircraft,false if simulator 
	
	public Plane(int k, boolean t) {
		nr = k; 
		type = t; 
	}
	
	/**
	 * getters
	 */
	
	public int getType() {
		int t; 
		if (type) {
			t = 1; 
		}
		else {
			t = 0; 
		}
		return t; 
	}
	
	public int getNr() {
		return nr; 
	}
}
