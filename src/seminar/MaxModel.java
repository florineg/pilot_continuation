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
//	private IloNumVar[][] Y;
	private IloNumVar[][] V; 
	private IloNumVar[][] Z; 
	
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
	
//	private HashMap<HashMap<HashMap<IloNumVar, Integer>, Integer>,Integer> varMapX; 
//	private HashMap<Integer, HashMap<Integer, HashMap<Integer, IloNumVar>>> itemMapX; 
//	private HashMap<Integer, HashMap<Integer, IloNumVar>> itemMapY; 
//	private HashMap<HashMap<IloNumVar, Integer>, Integer> varMapY; 
//	private HashMap<Integer, HashMap<Integer, IloNumVar>> itemMapV; 
//	private HashMap<HashMap<IloNumVar, Integer>, Integer> varMapV; 
	
	public MaxModel(ArrayList<Pilot> pilotList, ArrayList<Training> trainingList, int nrAircrafts, int nrSimulators, int lengthTimeFrame, double valueBeta, boolean max, int time) throws IloException, objectNotFoundException{
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
//		varMapX = new HashMap<HashMap<HashMap<IloNumVar, Integer>, Integer>, Integer>();  
//		itemMapX = new HashMap<Integer, HashMap<Integer, HashMap<Integer, IloNumVar>>>();  
//		itemMapY = new HashMap<Integer, HashMap<Integer, IloNumVar>>(); 
//		varMapY = new HashMap<HashMap<IloNumVar, Integer>, Integer>(); 
//		itemMapV = new HashMap<Integer, HashMap<Integer, IloNumVar>>(); 
//		varMapV = new HashMap<HashMap<IloNumVar, Integer>, Integer>();
		X = new IloNumVar[I][J][T];
//		Y = new IloNumVar[K][T];
		V = new IloNumVar[J][T]; 
		Z = new IloNumVar[I][J]; 
		
		q_done = new int[I][J];
		q_more = new int[I][J];
		q_current = new int[I][J];
		q_filled = false; 
		
		cplex = new IloCplex();
		cplex.setParam(	IloCplex.Param.TimeLimit, timelim); 
		
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
//		// create Y 
//		for (int k = 0; k <K; k++) {
//			for (int t = 0; t < T; t++) {
//				IloNumVar varY = cplex.boolVar();
//				Y[k][t] = varY;
//			}
//		}
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
	
	// C1: ensures completion training
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
	
	// C1: ensures completion training
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
	
	// C2: ensures max 1 Training per time t 
//	public void initMax1Training() throws IloException{
//		for (int i =0 ; i<I ; i++) {
//			for (int t=0; t<T; t++) {
//				IloNumExpr expr = cplex.numExpr();
//				for (int j = 0 ; j<J; j++) {
//					IloNumVar var= X[i][j][t];
//					expr = cplex.sum(expr, var);
//				}
//				if (expr != null){
//				cplex.addLe(expr, 1);	
//				}
//			}
//		}
//	}
	
	// Assuring max number of planes available
	public void initNrPlanesIsNrPilots() throws IloException{
//		for (int t = 0; t<T; t++) {
//			IloNumExpr expr = cplex.numExpr();
//			for (int k = 0 ; k< K; k++) {
//				IloNumVar var= Y[k][t];
//				expr = cplex.sum(expr, var); 
//			}
//			IloNumExpr expr2 = cplex.numExpr();
//			for(int i = 0; i< I; i++) {
//				for (int j = 0; j < J; j++) {
//					IloNumVar var= X[i][j][t];
//					expr2 = cplex.sum(expr2, var);
//				}
//			}
//			if (expr != null && expr2 != null){
//			cplex.addEq(expr, expr2);	
//			}
//		}
		
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
	
	//C6
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
	
	//C7
//	public void initRequiredMachine() throws IloException{
//		for (int t = 0; t < T; t++) {
//			// left side
//			IloNumExpr expr = cplex.numExpr();
//			for (int k = 0; k < K ; k++) {
//				IloNumExpr term = cplex.prod(planes.get(k).getType(), Y[k][t]);
//				expr = cplex.sum(expr, term); 
//			}
//			
//			// right side 
//			IloNumExpr expr2 = cplex.numExpr();
//			for (int j = 0; j<J; j++) {
//				IloNumExpr expr2temp = cplex.numExpr();
//				for (int i = 0; i<I; i++) {
//					IloNumVar var = X[i][j][t];
//					expr2temp = cplex.sum(expr2temp, var); 
//				}
//				expr2temp = cplex.prod(expr2temp, trainings.get(j).getR());
//				expr2 = cplex.sum(expr2, expr2temp); 
//			}
//			if (expr != null && expr2 != null){
//				cplex.addEq(expr, expr2);	
//			}
//		}
//	}
	
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
	
	// additional constraints from here 
	
	public void initAssignTasks() throws IloException {
		for (int i = 0; i < I ; i++) {
			for (int t = 0; t<T; t++) {
				IloNumExpr expr = cplex.numExpr(); 
				expr = cplex.sum(expr, Holiday[i][t]); 
				expr = cplex.sum(expr, Office[i][t]);
				expr = cplex.sum(expr, QRA[i][t]);
				if (t != 0) {
					expr = cplex.sum(expr, QRA[i][t-1]);
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
	
	// QRA
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
	
	public void initCourses(int nrCourses) throws IloException {
		IloNumExpr expr = cplex.numExpr(); 
		for (int t = 0 ; t<T; t++) {	
			expr = cplex.sum(expr, Course[t]); 
		}
		cplex.addEq(expr, nrCourses);
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
			Excel newExcel = new Excel();
			newExcel.addExcelWorksheet("Pilot 1", Xvalue, "j", "t");
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


