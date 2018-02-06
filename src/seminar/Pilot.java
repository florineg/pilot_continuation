package seminar;

import java.util.ArrayList;

public class Pilot {
	
	private static final int NrExpOff =55;
	private static final int NrInExpOff =25;

	private static int[] qij_exp = {8,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,2,4,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2}; 
	private static int[] qij_inexp = {12,6,6,2,3,4,4,2,3,4,4,2,3,4,4,2,3,4,4,2,3,4,2,8,2,4,2,3,2,4,2,3,2,4,2,3,2,4,2,3,2,4,2,3,2,2};

	private int nri;
	private boolean experienced; 
	private int[] q_j; 
	private int off; 

	private int[] q_tijd; // Simulation
	private int[] q_unplanned; // Simulation
	private int ill; // Simulation
	private int[] schedule; // Simulation

	public Pilot(int i, boolean exp) {
		nri = i; 
		experienced = exp;
		q_j = new int[46];
		q_tijd = new int[46]; // Simulation
		q_unplanned = new int[46]; // Simulation
		ill = 0; // Simulation
		schedule = new int[254]; // Simulation
		if (exp) {
			for (int s = 0; s<46; s++) {
				q_j[s] = qij_exp[s];
				q_tijd[s] = qij_exp[s]; // Simulation
			}
			off = NrExpOff;
		}
		else {
			for (int s = 0; s<46; s++) {
				q_j[s] = qij_inexp[s];
				q_tijd[s] = qij_inexp[s]; // Simulation

			}
			off = NrInExpOff;
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
	
	public void setIllness(int NrDaysIll) { // Simulation
		ill = NrDaysIll; // Simulation
	}
	
	public void setSchedule(int t, int EventNr) { // Simulation
		schedule[t] = EventNr; // Simulation
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
	
	public int getOff() {
		return off; 
	}
	
	public boolean getExperienced() {
		return experienced; 
	}
	
	public int getIllness () { // Simulation
		return ill; // Simulation
	}
	
	public int getQtijd(int j) { // Simulation
		return q_tijd[j];  // Simulation
	}
	
	public int getQunplanned(int j) { // Simulation
		return q_unplanned[j]; // Simulation
	}
	
	public int getSchedule (int t) { // Simulation
		return schedule[t]; // Simulation
	}
	
	/**
	 * Method
	 * @throws objectNotFoundException 
	 */
	
	public void decreaseQtijd (int j) { // Simulation
		q_tijd[j] = q_tijd[j]-1; // Simulation
	}
	
	public void increaseQunplanned (int j) { // Simulation
		q_unplanned[j] = q_unplanned[j]+1; // Simulation
	}
	
	public void decreaseQunplanned (int j) { // Simulation
		q_unplanned[j] = q_unplanned[j]-1; // Simulation
	}
	
	public void zeroQunplanned (int j) { // Simulation
		q_unplanned[j] = 0; // Simulation
	}
	
	
}