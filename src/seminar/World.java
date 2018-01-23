package seminar;

import java.util.ArrayList;

import ilog.concert.IloException;

public class World {
	//private int[] qij_exp = {8,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,2,4,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2}; 
	//private int[] qij_inexp = {12,6,6,2,3,4,4,2,3,4,4,2,3,4,4,2,3,4,4,2,3,4,2,8,2,4,2,3,2,4,2,3,2,4,2,3,2,4,2,3,2,4,2,3,2,2};
	private int[] nj = {1,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,2,1,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,2};
	private int[] ej = {1,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,0,1,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,0}; 
	

	private ArrayList<Pilot> pilots ; 
	private ArrayList<Event> events; 
	private ArrayList<Plane> planes; 
	
	public World() {
		// create pilots 
		pilots = new ArrayList<Pilot>(); 
		for (int i=0; i <27; i++) { //experienced pilots
			Pilot p = new Pilot(i, true);
			pilots.add(p);
		}
		for (int i2=27; i2<47;i2++) { // inexperienced pilots 
			Pilot p = new Pilot(i2,false);
			pilots.add(p);
		}	
		
		// create events
		events = new ArrayList<Event>(); 
		for (int j=0; j < 46; j++) { // on aircraft
			boolean bool_e; 
			if (ej[j]==1) {
				bool_e = true; 
			}
			else {
				bool_e = false; 
			}
			if (j< 23) {
				Event e = new Event(j,nj[j],true, bool_e);
				events.add(e);
			}
			else {
				Event e = new Event(j,nj[j],false, bool_e); 
				events.add(e);
			} 
		}
		
		// create planes 
		planes = new ArrayList<Plane>(); 
		for (int k = 0; k < 68; k++) {
			if (k<60) {
				Plane pl = new Plane(k, true);
				planes.add(pl);
			}
			else {
				Plane pl = new Plane(k, false);
				planes.add(pl);
			}
		}
	}
	
	public void runModel() throws IloException, objectNotFoundException {
		Model m = new Model(pilots, events, planes);
		if(m.solve()) {
			m.printSolution();
		}
	}
	
	public void runMaxModel() throws IloException, objectNotFoundException {
		int totalObjective = 0; 
		int[] resultsObjective = new int[13];
		for (int d = 1; d<=12; d++) {
			MaxModel m = new MaxModel(pilots, events, planes, 20, 4.5);
			// add additional constraints
//			m.initAssignTasks(); 
//			m.initHolidays(); 
//			m.initOfficeTask(); 
			
			if(m.solve()) {
				m.printSolution();
				pilots = m.updateQij(); 
				totalObjective += m.getObjectiveX(); 
				resultsObjective[d-1] = m.getObjectiveX(); 
			}
		}
		MaxModel m = new MaxModel(pilots, events, planes, 14, 1.2);
		
		if(m.solve()) {
			m.printSolution();
			pilots = m.updateQij(); 
			totalObjective += m.getObjectiveX(); 
			resultsObjective[12] = m.getObjectiveX(); 
		}
		int leftQ = 0; 
		for (int i = 0; i < pilots.size(); i++) {
			for (int j = 0; j < events.size(); j++) {
				leftQ += pilots.get(i).getQij(j);
			}
		}
		System.out.println("The final solution has " + leftQ + " trainings left to plan in total");
		System.out.println("The total objective value is " + totalObjective);
		for (int i = 0; i<13 ; i++) {
			System.out.println("Month "+ i + "gives objective " + resultsObjective[i]);	
		}
		
	}
}
