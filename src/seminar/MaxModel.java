package seminar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;

public class MaxModel {

	private int timelim;  
	
	
	private IloCplex cplex;
	private ArrayList<Pilot> pilots; 
	private ArrayList<Training> trainings; 
//	private ArrayList<Plane> planes;
	private int I; 
	private int J; 
	private int K;
	private int Kair; 
	private int Ksim; 
	private int T; 
	
	private IloNumVar[][][] X;
	private IloNumVar[][] V; 
	private IloNumVar[][] Z; 
	
	private IloNumVar[] Y; // for extra airplanes above robustness 
	
	private IloNumVar[][] Holiday ;
	private IloNumVar[][] Office; 
	private IloNumVar[][] QRA; 
	private IloNumVar[][] RestDay; 
	private IloNumVar[] Course;
	private IloNumVar[][] DutyFree; 
	private IloNumVar[][] LongHoliday; 
	
	private int[][] q_done; 
	private int[][] q_more;
	private int[][] q_current; 
	boolean q_filled; 
		
	private double beta;
	
	public MaxModel(ArrayList<Pilot> pilotList, ArrayList<Training> trainingList, int nrAircrafts, int nrSimulators, int lengthTimeFrame, double valueBeta, boolean max, double minGap, int time) throws IloException, objectNotFoundException{
		pilots = pilotList; 
		trainings = trainingList; 
//		planes = planeList; 
		I = pilots.size();
		J = trainings.size(); 
		Kair = nrAircrafts ;
		Ksim = nrSimulators; 
		K = nrAircrafts + nrSimulators ; 
		T = lengthTimeFrame; 
		beta = valueBeta; 
		
		timelim = time; 
		X = new IloNumVar[I][J][T];
		V = new IloNumVar[J][T]; 
		Z = new IloNumVar[I][J]; 
		
		Y = new IloNumVar[T];
		
		q_done = new int[I][J];
		q_more = new int[I][J];
		q_current = new int[I][J];
		q_filled = false; 
		
		cplex = new IloCplex();
		cplex.setParam(IloCplex.Param.TimeLimit, timelim); 
		cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, minGap); 
		cplex.setParam(IloCplex.StringParam.WorkDir, "C:\\Users\\Gebruiker\\Documents");
		cplex.setParam(IloCplex.IntParam.NodeFileInd, 2);
		cplex.setParam(IloCplex.DoubleParam.WorkMem, 1024.0);
		cplex.setParam(IloCplex.Param.Emphasis.Memory, true);
		
		initVars();
		if (max) {
			initCompleteTraining();
		}
		else {
			initCompleteTrainingMin(); 
		}
//		initMax1Training(); 
		initNrPlanesIsNrPilots(); 
		initNrPilotsPerTraining(); 
		//initRequiredMachine(); 
		if (max) {
			initObjective();
		}
		else {
			initMinObjective(); 
		}
		
	}
	
    ////////////////////////////
    // Initiliaze Variables ////
    ////////////////////////////	
	
	// initiates binary variables with two indices : x_ij /in {0,1}
	// initiates variable with value between 1 and n-1: u_i  
	public void initVars() throws IloException{
		// create X 
		for (int i = 0; i < I; i++) {
			for (int j = 0; j<J; j++) {
				for (int t= 0; t<T; t++) {
					IloNumVar varX = cplex.boolVar();
					X[i][j][t] = varX; 
				}
			}
		}
		
		// create V 
		for (int j = 0; j < J; j++) {
			for (int t = 0; t < T; t++) {
				IloNumVar varV = cplex.intVar(0,25);
				//IloNumVar varV = cplex.boolVar(); 
				V[j][t] = varV;
			}
		}		

		// create Z 
		for (int i = 0; i < I; i++) {
			for (int j = 0; j < J; j++) {
				IloNumVar varZ = cplex.intVar(0,25); 
					Z[i][j] = varZ;
			}
		}
	}

	public void initAdditionalVars() throws IloException {
		Holiday = new IloNumVar[I][T];
		Office = new IloNumVar[I][T]; 
		QRA = new IloNumVar[I][T];
		RestDay = new IloNumVar[I][T];
		Course = new IloNumVar[T];
		DutyFree = new IloNumVar[I][T];
		LongHoliday = new IloNumVar[I][T]; 
		for (int t = 0; t<T; t++) {
			for (int i = 0; i<I; i++) {
				IloNumVar Var1 = cplex.boolVar(); 
				Office[i][t] = Var1; 
				IloNumVar Var2 = cplex.boolVar(); 
				QRA[i][t] = Var2; 
				IloNumVar Var3 = cplex.boolVar(); 
				RestDay[i][t] = Var3; 
				IloNumVar Var5 = cplex.boolVar(); 
				DutyFree[i][t] = Var5; 
				IloNumVar Var6 = cplex.boolVar(); 
				LongHoliday[i][t] = Var6; 
				IloNumVar Var7 = cplex.boolVar(); 
				Holiday[i][t] = Var7; 
			}
			IloNumVar Var4 = cplex.boolVar(); 
			Course[t] = Var4; 
		}
	}
	
    ////////////////////////////
    // CONSTRAINTS /////////////
    ////////////////////////////
	
	// ensures completion training
	public void initCompleteTraining() throws IloException{
		for(int i = 0; i<I; i++) {
			for (int j = 0; j < J; j++) {
				IloNumExpr expr= cplex.numExpr();
				for (int t= 0; t<T; t++) { 
					IloNumVar var= X[i][j][t];
					expr = cplex.sum(expr, var);
				}
				IloNumExpr expr2 = cplex.numExpr(); 
				IloNumVar var2 = Z[i][j]; 
				expr2 = cplex.sum(expr2, var2);
				expr2 = cplex.sum(expr2, pilots.get(i).getQij(j));
				if (expr != null && expr2 != null){
				cplex.addLe(expr, expr2);	
				}
			}
		}
	}
	
	// max number of aircrafts available per day
	public void initCompleteTrainingMin() throws IloException{
		for(int i = 0; i<I; i++) {
			for (int j = 0; j < J; j++) {
				//lhs 
				IloNumExpr expr= cplex.numExpr();
				for (int t= 0; t<T; t++) { 
					IloNumVar var= X[i][j][t];
					expr = cplex.sum(expr, var);
				}
				if (expr != null){
				cplex.addGe(expr, pilots.get(i).getQij(j));	
				}
			}
		}
	}
	
	// max number of simulators available per day
	public void initNrPlanesIsNrPilots() throws IloException{
		for (int t = 0; t < T; t++) {
			//IloNumExpr expr1a = cplex.numExpr();
			IloNumExpr expr2a = cplex.numExpr();
			IloNumExpr expr3a = cplex.numExpr();
			
			for(int i = 0; i < I; i++) {
				for(int j = 0; j < J; j++) {
					//expr1a = cplex.sum(expr1a, X[i][j][t]);
					
					// lhs first constraint 
					IloNumExpr temp = cplex.numExpr();
					temp = cplex.sum(temp, X[i][j][t]);
					temp = cplex.prod(temp, trainings.get(j).getR());
					expr2a = cplex.sum(expr2a, temp);
					
					// lhs second constraint 
					IloNumExpr temp2 = cplex.numExpr();
					temp2 = cplex.sum(temp2, X[i][j][t]);
					temp2 = cplex.prod(temp2, 1-trainings.get(j).getR()); 
					
					expr3a = cplex.sum(expr3a, temp2);
				}
			}
			
			//cplex.addLe(expr1a, K);
			cplex.addLe(expr2a, Kair);
			cplex.addLe(expr3a, Ksim);
		}
	} 
	
	// number of pilots requried per event
	public void initNrPilotsPerTraining()throws IloException{
		for (int j = 0 ; j< J ; j++) {
			for (int t = 0 ; t<T; t++) {
				IloNumExpr expr = cplex.numExpr();
				for (int i = 0; i < I; i++) {
					IloNumVar var= X[i][j][t];
					expr = cplex.sum(expr, var);
				}
				IloNumExpr expr2 = cplex.numExpr();
				IloNumVar var2 = V[j][t]; 
				expr2 = cplex.sum(expr2, var2);
				expr2 = cplex.prod(expr2, (trainings.get(j)).getN());
				
				if (expr != null && expr2 != null){
					cplex.addEq(expr, expr2);	
				}
			}
		}
	}

	///////////////////////////
	// ADDITIONAL CONSTRAINTS//
	///////////////////////////
	
	// 0 wave of live night flights in summer
	public void initSummer() throws IloException {
		for (int t = 0; t < T; t++) {
			IloNumExpr expr1 = cplex.numExpr();
			for(int i = 0; i < I; i++) {
				for(int j = 0; j < J; j++) {
					// lhs 1
					IloNumExpr temp1 = cplex.numExpr();
					temp1 = cplex.sum(temp1, 1- trainings.get(j).getE());
					temp1 = cplex.prod(temp1, this.X[i][j][t]);
					temp1 = cplex.prod(temp1, trainings.get(j).getR());
					
					expr1 = cplex.sum(expr1, temp1);
				}
			}
			cplex.addLe(expr1, 0);
		}		
	}
	
	// 1 wave of live night flights in winter
	// 1 wave of live day flights in winter
	public void initWinter() throws IloException {
		for (int t = 0; t < T; t++) {
			IloNumExpr expr1 = cplex.numExpr();
			IloNumExpr expr2 = cplex.numExpr();
			for(int i = 0; i < I; i++) {
				for(int j = 0; j < J; j++) {
					
					// lhs 1
					IloNumExpr temp1 = cplex.numExpr();
					temp1 = cplex.sum(temp1, 1- trainings.get(j).getE());
					temp1 = cplex.prod(temp1, this.X[i][j][t]);
					temp1 = cplex.prod(temp1, trainings.get(j).getR());
				
					// lhs 2
					IloNumExpr temp2 = cplex.numExpr();
					temp2 = cplex.sum(temp2, trainings.get(j).getE());
					temp2 = cplex.prod(temp2, this.X[i][j][t]);
					temp2 = cplex.prod(temp2, trainings.get(j).getR());
					
					expr1 = cplex.sum(expr1, temp1);
					expr2 = cplex.sum(expr2, temp2);
				}
			}
			cplex.addLe(expr1, 0.5*Kair);
			cplex.addLe(expr2, 0.5*Kair);
		}
	}

	// 1 task per pilot per time unit
	public void initAssignTasks() throws IloException {
		for (int i = 0; i < I ; i++) {
			for (int t = 0; t<T; t++) {
				IloNumExpr expr = cplex.numExpr(); 
				expr = cplex.sum(expr, Holiday[i][t]); 
				expr = cplex.sum(expr, Office[i][t]);
				expr = cplex.sum(expr, QRA[i][t]);
				if (t > 1) {
					expr = cplex.sum(expr, QRA[i][t-1]);
					expr = cplex.sum(expr, QRA[i][t-2]);
				}
				expr = cplex.sum(expr, RestDay[i][t]); 
				expr = cplex.sum(expr, Course[t]);
				expr = cplex.sum(expr, DutyFree[i][t]); 
				for (int j = 0; j<J; j++) {
					expr = cplex.sum(expr, X[i][j][t]); 
				}
				cplex.addEq(expr, 1); 
			}
		}
	}

	// 26 holidays per pilot in whole year
	// long holiday break of 10 workdays in whole year
	// at least one long break
	public void initHolidays(int nrHolidays, int nrLongHolidays, int nrPilotsLong, int firstPilotLong) throws IloException {
		// constraint for assuring x nr of holidays 
		for (int i = 0; i < I; i++) {
			IloNumExpr expr = cplex.numExpr();  // for constraint of nrHolidays per period  
			for (int t = 0; t<T; t++) {
				expr = cplex.sum(expr, Holiday[i][t]);
			}
			if (i >= firstPilotLong && i < firstPilotLong + nrPilotsLong) {
				cplex.addEq(expr, nrHolidays + nrLongHolidays);
			}
			else {
				cplex.addEq(expr, nrHolidays);
			}	
		}
		
		// Subset of pilots that has to have a long break in this period.
		// Assuring long break of 'nrLongHolidays'
		// Assuring at least one long break per pilot in subset
		for (int i = firstPilotLong; i < firstPilotLong + nrPilotsLong; i++) {
			IloNumExpr expr4 = cplex.numExpr(); // lhs of requiring one long holiday 
			for (int t = 0; t <= T-nrLongHolidays; t++) {
				IloNumExpr expr2 = cplex.numExpr(); //lhs of long holiday of 10 days 
				for(int s = t; s < t + nrLongHolidays; s++) {
					expr2 = cplex.sum(expr2, Holiday[i][s]);
				}	
				IloNumExpr expr3 = cplex.numExpr(); // rhs of long holiday of 10 days 
				expr3 = cplex.sum(expr3, LongHoliday[i][t]);
				expr3 = cplex.prod(expr3, nrLongHolidays);
				
				// Assuring long break of 'nrLongHolidays'
				cplex.addGe(expr2, expr3);
				
				expr4 = cplex.sum(expr4, LongHoliday[i][t]);
			}
			// Assuring at least one long break per pilot in subset
			cplex.addEq(expr4, 1);
		}
	}
	
	// number of office tasks a year per pilot
	public void initOfficeTasks(int nrOHexp, int nrOHinexp) throws IloException {
		for (int i=0; i<I; i++) {
			IloNumExpr expr = cplex.numExpr();
			
			for (int t = 0 ; t<T; t++) {
				expr = cplex.sum(expr, Office[i][t]); 
			}
			if (pilots.get(i).getExperienced()) {
				cplex.addEq(expr, nrOHexp);
			}
			else {
				cplex.addEq(expr, nrOHinexp);
			}
			 
		}
	}
	
	// 2 pilots per day for QRA during Q1 and Q3
	// 1 QRA per week
	// weekend QRA shifts
	public void initQRA() throws IloException {
		for (int t = 0; t<T; t++) {
			IloNumExpr expr = cplex.numExpr();
			
			for (int i = 0; i<I ; i++) {
				expr = cplex.sum(expr, QRA[i][t]);
			}
			cplex.addEq(expr, 2); 
			
			if(t % 5 == 0){
				IloNumExpr expr5 = cplex.numExpr();
				
				for (int i = 0; i < I; i++) {
					IloNumExpr expr1 = cplex.numExpr();
					int thetaMax; 
					if (t > T-5) {
						thetaMax = T; 
					}
					else {
						thetaMax = t+4; 
					}
					for (int theta = t; theta < thetaMax; theta++) {
						expr1 = cplex.sum(expr1, QRA[i][theta]);
					}
					
					IloNumExpr expr2 = cplex.numExpr();
					IloNumExpr expr3 = cplex.numExpr();
					
					if (t < T-5) {
						expr2 = cplex.sum(expr2, RestDay[i][t+5]);
					}
					expr3 = cplex.sum(expr1, expr2);
					
					cplex.addLe(expr3, 1);
					
					expr5 = cplex.sum(expr5, RestDay[i][t]);
				}
							
				cplex.addEq(expr5, 4);
			}
		}
	}
	
	//////////////////////////
	//PREFERENCE CONSTRAINTS//
	//////////////////////////

	// Preference constraint: max nr qra shifts per quarter
	public void initMaxNrQra(int max) throws IloException {
			for (int i = 0; i < I ; i++) {
				IloNumExpr expr = cplex.numExpr(); 
				for (int t = 0; t < T; t++) {
					expr = cplex.sum(expr,QRA[i][t]); 
				}
				cplex.addLe(expr, max);
			}
		}
		
	// Preference constraint: min nr qra shifts per quarter
	public void initMinNrQra(int min) throws IloException {
					for (int i = 0; i < I ; i++) {
						IloNumExpr expr = cplex.numExpr(); 
						for (int t = 0; t < T; t++) {
							expr = cplex.sum(expr,QRA[i][t]); 
						}
						cplex.addGe(expr, min);
					}
				}	

	// Preference constraint: min x weekend qra per quarter
	public void initMinWeekendQRA(int min) throws IloException {
		for (int i = 0; i < I ; i++) {
			IloNumExpr expr = cplex.numExpr(); 
			for (int t = 0; t < T; t++) {
				expr = cplex.sum(expr,RestDay[i][t]); 
			}
			cplex.addGe(expr, min);
		}
	}
	
	// Preference constraint: max x weekend qra per quarter 
	public void initMaxWeekendQRA(int max) throws IloException {
		for (int i = 0; i < I ; i++) {
			IloNumExpr expr = cplex.numExpr(); 
			for (int t = 0; t < T; t++) {
				expr = cplex.sum(expr,RestDay[i][t]); 
			}
			cplex.addLe(expr, max);
		}
	}	
	
	// Preference constraint: max nr total qra shifts per quarter
	public void initMaxTotalQRA(int max) throws IloException {
			for (int i = 0; i < I ; i++) {
				IloNumExpr expr = cplex.numExpr(); 
				for (int t = 0; t < T; t++) {
					expr = cplex.sum(expr,QRA[i][t]);
					expr = cplex.sum(expr,RestDay[i][t]);
				}
				cplex.addLe(expr, max);
			}
		}
		
	// Preference constraint: min nr total qra shifts per quarter
	public void initMinTotalQRA(int min) throws IloException {
					for (int i = 0; i < I ; i++) {
						IloNumExpr expr = cplex.numExpr(); 
						for (int t = 0; t < T; t++) {
							expr = cplex.sum(expr,QRA[i][t]);
							expr = cplex.sum(expr,RestDay[i][t]);
						}
						cplex.addGe(expr, min);
					}
				}	

	// Preference constraint: courses
	public void initCourses(int nrCourses) throws IloException {
		IloNumExpr expr = cplex.numExpr(); 
		for (int t = 0 ; t<T; t++) {	
			expr = cplex.sum(expr, Course[t]); 
		}
		if (expr != null) {
			cplex.addEq(expr, nrCourses);	
		}
	}
	
	// Preference constraint: max 1 course per week 
	public void initMax1CourseWeek() throws IloException {
		for (int t =0; t<T; t++) {
			if (t % 5 == 0 && t+5 < T) {
				IloNumExpr expr = cplex.numExpr(); 
				for (int s = t; s < t+5; s++) {
					expr = cplex.sum(expr, Course[s]); 
				}
				if (expr != null) {
					cplex.addLe(expr, 1); 
				}
			}
		}
	}

	// Preference constraint: at most x pilots with same holiday per day
	public void initMaxPilotsHoliday(int max) throws IloException {
		// to do  
		for (int t = 0; t < T; t++) {
			IloNumExpr expr = cplex.numExpr();
			for (int i = 0; i < I ; i++) {
				expr = cplex.sum(expr, Holiday[i][t]);
			}
			cplex.addLe(expr, max); 
		}
	}
	
	// PREFERENCE CONSTRAINT: not more than 70% of aircrafts used (42 aircrafts), penalty is more are used 
	// same for simulation, necessary??
	public void initRobustNrAircrafts(int Krobust) throws IloException{
		// create Y 
		for (int t = 0; t < T; t++) {
			IloNumVar varY = cplex.boolVar();
			Y[t] = varY;
		}	
		// create extra constraint 
		for (int t = 0; t < T; t++) {
			//lhs 
			IloNumExpr expr = cplex.numExpr(); 
			for (int i = 0; i < I; i++) {
				for (int j = 0; j < 23 ; j++) {
					expr = cplex.sum(expr, X[i][j][t]); 
				}
			}
			//rhs 
			IloNumExpr expr2 = cplex.numExpr(); 
			expr2 = cplex.sum(expr2, Y[t]);
			expr2 = cplex.diff(Krobust ,expr2);
			
			cplex.addLe(expr, expr2);  
		}
	}
		
	///////////////////////////
	//OBJECTIVES///////////////
	///////////////////////////
	
	// objective
	public void initObjective() throws IloException, objectNotFoundException{
	IloNumExpr expr= cplex.linearNumExpr();
	
	for(int i = 0; i < I; i++) {
		for (int j = 0; j< J; j++) {
			for (int t = 0; t< T; t++) { 
				IloNumExpr term = cplex.prod(trainings.get(j).getN(), X[i][j][t] );
				expr = cplex.sum(expr, term);
			}
			IloNumExpr term2 = cplex.prod(beta, Z[i][j]);
			term2 = cplex.prod(term2, trainings.get(j).getN());	
			expr = cplex.diff(expr, term2);
		}
	}
	cplex.addMaximize(expr);
	}
	
	// minimum objective
	public void initMinObjective() throws IloException, objectNotFoundException{
	IloNumExpr expr= cplex.linearNumExpr();
	
	for(int i = 0; i < I; i++) {
		for (int j = 0; j< J; j++) {
			for (int t = 0; t< T; t++) { 
				expr = cplex.sum(expr, X[i][j][t]);
			}
		}
	}
	cplex.addMinimize(expr);
	}
	
	///////////////////////////
	//OPERATIONS///////////////
	///////////////////////////	
	
	// update pilots with schedule 
	public void setSchedulePilot(int tstart) throws UnknownObjectException, IloException, objectNotFoundException {
		for (int i = 0 ; i<I ; i++) {
			for (int t=0;t<T;t++) {
				boolean pilotDoesEvent = false; 
				for (int j = 0 ; j<J; j++) {
					if (cplex.getValue(X[i][j][t]) == 1) {
						pilots.get(i).setSchedule(tstart + t, j);
						pilotDoesEvent = true; 
					}
				}

				if (!pilotDoesEvent) {
					if ((int) cplex.getValue(DutyFree[i][t]) ==1) {
						pilots.get(i).setSchedule(tstart + t,46);
					}			
					else if ((int) cplex.getValue(QRA[i][t]) ==1){
						pilots.get(i).setSchedule(tstart + t,47);
					}
					else if ((int) cplex.getValue(RestDay[i][t]) ==1) {
						pilots.get(i).setSchedule(tstart + t,48);
					}
					else if ((int) cplex.getValue(Holiday[i][t]) ==1) {
						pilots.get(i).setSchedule(tstart + t,49);
					}
					else if ((int) cplex.getValue(Course[t]) ==1) {
						pilots.get(i).setSchedule(tstart + t,50);
					}
					else if ((int) cplex.getValue(Office[i][t]) ==1) {
						pilots.get(i).setSchedule(tstart + t,51);
					}
					else {
						throw new objectNotFoundException("No Task at all - MisMatch");
					}
				}
			}	
		}
	}
	
	public boolean solve() throws IloException{
		return cplex.solve();
	}
	
	public double getSolution() throws IloException{
		return cplex.getObjValue(); 
	}
	
	public int getObjectiveX() throws UnknownObjectException, IloException {
		int obj = 0; 
		for (int i = 0; i<I; i++) {
			for (int j = 0; j<J; j++) {
				for (int t = 0; t<T; t++) {
					obj += cplex.getValue(X[i][j][t]); 
				}
			}
		}
		return obj; 
	}
	
	public void printSolution(String name) throws IloException, IOException, objectNotFoundException{
		if (q_filled) {
			System.out.println("Solution value: "+cplex.getObjValue());
			int[][] Xvalue = new int[J][T];
			for (int j=0; j<J; j++) {
				for (int t=0; t<T; t++) {
					Xvalue[j][t] = (int) cplex.getValue(X[1][j][t]);
				}
			}
			int[][] nrPlanes = new int [1][T]; 
			for (int t= 0; t <T; t++) {
				int sum = 0; 
				for (int i = 0; i < I; i++) {
					for (int j = 0 ; j < J ; j++) {
						if (cplex.getValue(X[i][j][t])==1) {
							sum ++ ; 
						}
					}
				nrPlanes[0][t] = sum; 	
				}
			}
			
			Excel newExcel = new Excel();
			newExcel.addExcelWorksheet("Pilot 1", Xvalue, "j", "t");
			newExcel.addExcelWorksheet("NrPlanes", nrPlanes, "", "t");
			newExcel.addExcelWorksheet("Holiday", convertVariable(Holiday), "i", "t");
			newExcel.addExcelWorksheet("Long Holiday", convertVariable(LongHoliday), "i", "t");
			newExcel.addExcelWorksheet("Office Hours", convertVariable(Office), "i", "t");
			newExcel.addExcelWorksheet("QRA", convertVariable(QRA), "i", "t");
			newExcel.addExcelWorksheet("RestDay", convertVariable(RestDay), "i", "t");
			newExcel.addExcelWorksheet("DutyFree", convertVariable(DutyFree), "i", "j");
			newExcel.addExcelWorksheet("Q_done", q_done, "i", "j");
			newExcel.addExcelWorksheet("Q_more", q_more, "i", "j");
			newExcel.addExcelWorksheet("Q_current", q_current, "i", "j");
			newExcel.writeExcelFile(name);
		}
		else {
			throw new objectNotFoundException("Convert Qij before printing solution");
		}
	}
	
	private int[][] convertVariable(IloNumVar[][] X) throws UnknownObjectException, IloException{
		int M = X.length; //row 
		int N = X[0].length; //column 
		int[][] Xvalue = new int[M][N]; 
		for (int m = 0; m <M; m++) {
			for (int n = 0; n <N; n++) {
				try {
				Xvalue[m][n] = (int) cplex.getValue(X[m][n]);
				}
				catch(UnknownObjectException e) {
					Xvalue[m][n] = 0;
				}
			}
		}
		return Xvalue;
	}
	
	public ArrayList<Pilot> updateQij() throws UnknownObjectException, IloException, objectNotFoundException{
		for (Pilot i: pilots) {
			int[] q = i.getQij(); 
			int nri = i.getNr(); 
			for (int j = 0; j<J; j++) {
				int sumT = 0; 
				for (int t = 0; t< T; t++) {
					sumT += cplex.getValue(X[nri][j][t]); 
				}
				if (q[j]-sumT  >= 0) {
					q[j] = q[j]- sumT;	
					q_done[nri][j] = sumT; 
					q_current[nri][j] = q[j];
				}
				else {
					q_done[nri][j] = q[j];
					q_more[nri][j] = sumT - q[j];
					q[j]= 0; 
					q_current[nri][j] = q[j];
				}
			}
			i.addQj(q);
		}
		q_filled = true; 
		return pilots; 
	}
	
	public void writeSolution(String Name) throws IloException{
		cplex.writeMIPStart(Name + ".mst");
	}
	
	public void readSolution(String Name) throws IloException{
		cplex.readMIPStarts(Name + ".mst");
	}
	
	public void closeModel(){
		cplex.end(); 
	}
}


