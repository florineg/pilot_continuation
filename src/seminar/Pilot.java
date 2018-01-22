package seminar;

import java.util.ArrayList;

public class Pilot {

	private static int[] qij_exp = {8,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}; 
	private static int[] qij_inexp = {12,6,6,2,3,4,4,2,3,4,4,2,3,4,4,2,3,4,4,2,3,4,2,4,1,2,1,1,1,2,1,1,1,2,1,1,1,2,1,1,1,2,1,1,1,1};
	
	private int nri;
	private boolean experienced; 
	private int[] q_j; 
	
	public Pilot(int i, boolean exp) {
		nri = i; 
		experienced = exp;
		q_j = new int[46];
		if (exp) {
			for (int s = 0; s<46; s++) {
				q_j[s] = qij_exp[s];
			}
		}
		else {
			for (int s = 0; s<46; s++) {
				q_j[s] = qij_inexp[s];
			}
		}
		 
	}
	
	/**
	 * Setters
	 * @throws objectNotFoundException 
	 */
	public void addQj(int[] qij) throws objectNotFoundException {
		if (qij.length == q_j.length) {
			q_j = qij; 
		}
		else {
			throw new objectNotFoundException("length of new qij is not the same as old");
		}
	}
	
	/**
	 * Getters
	 */
	public int getQij(int j) {
		return q_j[j]; 
	}
	
	public int getNr() {
		return nri; 
	}
	
	public int[] getQij() {
		return q_j; 
	}
}
