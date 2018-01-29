package seminar;

import java.io.IOException;
import java.util.ArrayList;

import ilog.concert.IloException;

public class World {
	//private int[] qij_exp = {8,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,2,4,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2}; 
	//private int[] qij_inexp = {12,6,6,2,3,4,4,2,3,4,4,2,3,4,4,2,3,4,4,2,3,4,2,8,2,4,2,3,2,4,2,3,2,4,2,3,2,4,2,3,2,4,2,3,2,2};
	private int[] nj = {1,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,2,1,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,4,2,2};
	private int[] ej = {1,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,0,1,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,0}; 
	

	private ArrayList<Pilot> pilots ; 
	private ArrayList<Training> trainings; 
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
		
		// create trainings
		trainings = new ArrayList<Training>(); 
		for (int j=0; j < 46; j++) { // on aircraft
			boolean bool_e; 
			if (ej[j]==1) {
				bool_e = true; 
			}
			else {
				bool_e = false; 
			}
			if (j< 23) {
				Training e = new Training(j,nj[j],true, bool_e);
				trainings.add(e);
			}
			else {
				Training e = new Training(j,nj[j],false, bool_e); 
				trainings.add(e);
			} 
		}
		
//		// create planes 
//		planes = new ArrayList<Plane>(); 
//		for (int k = 0; k < 68; k++) {
//			if (k<60) {
//				Plane pl = new Plane(k, true);
//				planes.add(pl);
//			}
//			else {
//				Plane pl = new Plane(k, false);
//				planes.add(pl);
//			}
//		}
	}
	
	public void runModel() throws IloException, objectNotFoundException {
		Model m = new Model(pilots, trainings, planes);
		if(m.solve()) {
			m.printSolution();
		}
	}
	
	public void runMaxModel() throws IloException, objectNotFoundException, IOException {
		int totalObjective = 0; 
		int[] resultsObjective = new int[4];
		
		//Q1
		MaxModel m = new MaxModel(pilots, trainings, 60, 12, 60, 17, true,1000);
		m.initAdditionalVars();
		m.initAssignTasks();
		m.initWinter(); 
		m.initHolidays(4, 10, 11, 0);
		m.initOfficeTasks(11,6);
		m.initQRA();
		//m.initCourses(2);
		
//		m.readSolution("Q1");
		if(m.solve()) { 
			pilots = m.updateQij();
			m.printSolution("Q1");
			totalObjective += m.getObjectiveX(); 
			resultsObjective[0] = m.getObjectiveX();
			System.out.println("Total objective Q1: "+ totalObjective);
			m.writeSolution("Q1");
		} 
		 
		//Q2
		m = new MaxModel(pilots, trainings, 60, 12, 65, 17, true, 1000);
		m.initAdditionalVars();
		m.initAssignTasks();
		m.initSummer();
		m.initHolidays(4, 10, 12, 11);
		m.initOfficeTasks(12,6);
		//m.initQRA();
		m.initCourses(2); 
		
//		m.readSolution("Q2");
		if(m.solve()) {
			pilots = m.updateQij();
			m.printSolution("Q2"); 
			totalObjective += m.getObjectiveX(); 
			resultsObjective[1] = m.getObjectiveX(); 
			m.writeSolution("Q2");
		} 
		
//		//Q3
		m = new MaxModel(pilots, trainings, 60, 12, 65, 15, true, 1000);
		m.initAdditionalVars();
		m.initAssignTasks();
		m.initSummer();
		m.initHolidays(4, 10, 12, 23);
		m.initOfficeTasks(12,6);
		m.initQRA();
		//m.initCourses(2); 
		
//		m.readSolution("Q3");
		if(m.solve()) {
			pilots = m.updateQij();
			m.printSolution("Q3"); 
			totalObjective += m.getObjectiveX(); 
			resultsObjective[2] = m.getObjectiveX();
			m.writeSolution("Q3");
		} 
		
//		//Q4
		m = new MaxModel(pilots, trainings, 60, 12, 64, 1.2, false, 1000);
		m.initAdditionalVars();
		m.initAssignTasks();
		m.initWinter();
		m.initHolidays(4, 10, 12, 35);
		m.initOfficeTasks(12,7);
		//m.initQRA();
		m.initCourses(2); 
		
//		m.readSolution("Q4");
		if(m.solve()) {
			pilots = m.updateQij();
			m.printSolution("Q4"); 
			totalObjective += m.getObjectiveX(); 
			resultsObjective[3] = m.getObjectiveX();
			m.writeSolution("Q4");
		} 
		
		int leftQ = 0; 
		for (int i = 0; i < pilots.size(); i++) {
			for (int j = 0; j < trainings.size(); j++) {
				leftQ += pilots.get(i).getQij(j);
			}
		}
		
		System.out.println("The final solution has " + leftQ + " trainings left to plan in total");
		System.out.println("The total objective value is " + totalObjective);
		for (int i = 0; i<4 ; i++) {
			System.out.println("Month "+ i + "gives objective " + resultsObjective[i]);	
		}
	}
}
