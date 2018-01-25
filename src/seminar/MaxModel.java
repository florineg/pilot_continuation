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

	private static final int timelim = 300;  
	private static final int C = 20; 
	
	
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
	private IloNumVar[][][] V; 
	private IloNumVar[][] Z; 
	
	private IloNumVar[][] Holiday ;
	private IloNumVar[][] Office; 
	private IloNumVar[][] QRA; 
	private IloNumVar[][] RestDay; 
	private IloNumVar[] Course;
	private IloNumVar[][] DutyFree; 
	private IloNumVar[][] LongHoliday; 
	
	private double beta;
	
//	private HashMap<HashMap<HashMap<IloNumVar, Integer>, Integer>,Integer> varMapX; 
//	private HashMap<Integer, HashMap<Integer, HashMap<Integer, IloNumVar>>> itemMapX; 
//	private HashMap<Integer, HashMap<Integer, IloNumVar>> itemMapY; 
//	private HashMap<HashMap<IloNumVar, Integer>, Integer> varMapY; 
//	private HashMap<Integer, HashMap<Integer, IloNumVar>> itemMapV; 
//	private HashMap<HashMap<IloNumVar, Integer>, Integer> varMapV; 
	
	public MaxModel(ArrayList<Pilot> pilotList, ArrayList<Training> trainingList, int nrAircrafts, int nrSimulators, int lengthTimeFrame, double valueBeta) throws IloException, objectNotFoundException{
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
//		varMapX = new HashMap<HashMap<HashMap<IloNumVar, Integer>, Integer>, Integer>();  
//		itemMapX = new HashMap<Integer, HashMap<Integer, HashMap<Integer, IloNumVar>>>();  
//		itemMapY = new HashMap<Integer, HashMap<Integer, IloNumVar>>(); 
//		varMapY = new HashMap<HashMap<IloNumVar, Integer>, Integer>(); 
//		itemMapV = new HashMap<Integer, HashMap<Integer, IloNumVar>>(); 
//		varMapV = new HashMap<HashMap<IloNumVar, Integer>, Integer>();
		X = new IloNumVar[I][J][T];
//		Y = new IloNumVar[K][T];
		V = new IloNumVar[C][J][T]; 
		Z = new IloNumVar[I][J]; 
		
		cplex = new IloCplex();
		cplex.setParam(	IloCplex.Param.TimeLimit, timelim); 
		
		initVars();
		initCompleteTraining(); 
		initMax1Training(); 
		initNrPlanesIsNrPilots(); 
		initNrPilotsPerTraining(); 
		//initRequiredMachine(); 
		initObjective();
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
		for (int c = 0; c < C; c++) {
			for (int j = 0; j < J; j++) {
				for (int t = 0; t < T; t++) {
					IloNumVar varV = cplex.intVar(0,25);
					//IloNumVar varV = cplex.boolVar(); 
					V[c][j][t] = varV;
				}
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
	
	// C2: ensures max 1 Training per time t 
	public void initMax1Training() throws IloException{
		for (int i =0 ; i<I ; i++) {
			for (int t=0; t<T; t++) {
				IloNumExpr expr = cplex.numExpr();
				for (int j = 0 ; j<J; j++) {
					IloNumVar var= X[i][j][t];
					expr = cplex.sum(expr, var);
				}
				if (expr != null){
				cplex.addLe(expr, 1);	
				}
			}
		}
	}
	
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
					
					IloNumExpr temp = cplex.numExpr();
					temp = cplex.sum(temp, X[i][j][t]);
					temp = cplex.prod(temp, trainings.get(j).getR());
					expr2a = cplex.sum(expr2a, temp);
					
					IloNumExpr temp2 = cplex.numExpr();
					IloNumExpr temp3 = cplex.numExpr();
					temp2 = cplex.sum(temp2, X[i][j][t]);
					temp3 = cplex.sum(temp3, trainings.get(j).getR());
					temp3 = cplex.prod(temp2, -1);
					temp3 = cplex.sum(temp3, 1);
					
					expr3a = cplex.sum(expr3a, temp3);
				}
			}
			
			//IloNumExpr expr1b = cplex.numExpr();
			IloNumExpr expr2b = cplex.numExpr();
			IloNumExpr expr3b = cplex.numExpr();
			//expr1b = cplex.sum(expr1b, K);
			expr2b = cplex.sum(expr2b, Kair);
			expr3b = cplex.sum(expr2b, Ksim);
			
			//cplex.addLe(expr1a, expr1b);
			cplex.addLe(expr2a, expr2b);
			cplex.addLe(expr3a, expr3b);
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
				for (int c = 0; c < C; c++) {
					IloNumVar var2 = V[c][j][t]; 
					expr2 = cplex.sum(expr2, var2);
				}
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
				expr = cplex.sum(expr, X[i][j][t]);
			}
			IloNumExpr term2 = cplex.prod(beta, Z[i][j]); 
			expr = cplex.diff(expr, term2);
		}
	}
	cplex.addMaximize(expr);
	}
	
	// additional constraints from here 
	
	public void initAssignTasks() throws IloException {
		for (int i = 0; i < I ; i++) {
			for (int t = 0; t<T; t++) {
				IloNumExpr expr = cplex.numExpr(); 
				expr = cplex.sum(expr, Holiday[i][t]); 
				expr = cplex.sum(expr, Office[i][t]);
				expr = cplex.sum(expr, QRA[i][t]);
				if (t!= 0) {
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
					IloNumExpr temp1 = cplex.numExpr();
					temp1 = cplex.sum(temp1, trainings.get(j).getR());
					
					IloNumExpr temp2a = cplex.numExpr();
					temp2a = cplex.sum(temp2a, trainings.get(j).getE());
					temp2a = cplex.prod(temp2a, -1);
					temp2a = cplex.sum(temp2a, 1);
					
					IloNumExpr temp3 = cplex.numExpr();
					temp3 = cplex.sum(temp3, X[i][j][t]);
					
					IloNumExpr temp4 = cplex.numExpr();
					temp4 = cplex.prod(temp1, temp2a);
					temp4 = cplex.prod(temp4, temp3);
					
					expr1 = cplex.sum(expr1, temp4);
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
					IloNumExpr temp1 = cplex.numExpr();
					temp1 = cplex.sum(temp1, trainings.get(j).getR());
					
					IloNumExpr temp2a = cplex.numExpr();
					IloNumExpr temp2b = cplex.numExpr();
					temp2a = cplex.sum(temp2a, trainings.get(j).getE());
					temp2b = cplex.prod(temp2a, -1);
					temp2b = cplex.sum(temp2a, 1);
					
					IloNumExpr temp3 = cplex.numExpr();
					temp3 = cplex.sum(temp3, this.X[i][j][t]);
					
					IloNumExpr temp4 = cplex.numExpr();
					temp4 = cplex.prod(temp1, temp2b);
					temp4 = cplex.prod(temp4, temp3);
					
					IloNumExpr temp5 = cplex.numExpr();
					temp5 = cplex.prod(temp1, temp2a);
					temp5 = cplex.prod(temp5, temp3);
					
					expr1 = cplex.sum(expr1, temp4);
					expr2 = cplex.sum(expr2, temp4);
				}
			}
			
			cplex.addLe(expr1, 0.5*Kair);
			cplex.addLe(expr2, 0.5*Kair);
		}
	}
	
	public void initHolidays(int nrHolidays, int nrLongHolidays, int nrPilotsLong, int firstPilotLong) throws IloException {
		// constraint for assuring x nr of holidays 
		for (int i = 0; i < I; i++) {
			IloNumExpr expr = cplex.numExpr();  // for constraint of nrHolidays per year 
			for (int t = 0; t<T; t++) {
				expr = cplex.sum(expr, Holiday[i][t]);
			}
			if (i> firstPilotLong && i < firstPilotLong + nrPilotsLong) {
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
			IloNumExpr expr4 = cplex.numExpr();
			for (int t = 0; t < T; t++) {
				if(t <= T - nrLongHolidays) {
					IloNumExpr expr2 = cplex.numExpr();
					for(int s = t; s < t + nrLongHolidays; s++) {
						expr2 = cplex.sum(expr2, Holiday[i][t]);
					}
					
					IloNumExpr expr3 = cplex.numExpr();
					expr3 = cplex.sum(expr3, LongHoliday[i][t]);
					expr3 = cplex.prod(expr3, nrLongHolidays);
					
					// Assuring long break of 'nrLongHolidays'
					cplex.addGe(expr2, expr3);
				}
				
				expr4 = cplex.sum(expr4, LongHoliday[i][t]);
			}
			
			// Assuring at least one long break per pilot in subset
			cplex.addGe(expr4, 1);
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
			
			if(t % 5 == 0 && t != T){
				IloNumExpr expr5 = cplex.numExpr();
					
				for (int i = 0; i < I; i++) {
					IloNumExpr expr1 = cplex.numExpr();
					for (int theta = t; theta < t + 4; theta++) {
						expr1 = cplex.sum(expr1, QRA[i][theta]);
					}
					
					IloNumExpr expr2 = cplex.numExpr();
					IloNumExpr expr3 = cplex.numExpr();
					IloNumExpr expr4 = cplex.numExpr();
					expr2 = cplex.sum(expr2, RestDay[i][t+5]);
					expr3 = cplex.prod(expr1, expr2);
					expr4 = cplex.sum(expr4, 1);
					
					cplex.addLe(expr3, expr4);
					
					expr5 = cplex.sum(expr5, RestDay[i][t]);
				}
				
				IloNumExpr expr6 = cplex.numExpr();
				expr6 = cplex.sum(expr6, 4);
				
				cplex.addEq(expr5, expr6);
			}
		}
		
	}
	
	public void initCourses(int nrCourses) throws IloException {
		IloNumExpr expr = cplex.numExpr(); 
		for (int t = 0 ; t<T; t++) {	
			expr = cplex.sum(expr, Course[t]); 
		}
		cplex.addEq(expr, 4);
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
	
	public void printSolution() throws IloException, IOException{
		System.out.println("Solution value: "+cplex.getObjValue());
		int[][] Xvalue = new int[J][T];
		for (int j=0; j<J; j++) {
			for (int t=0; t<T; t++) {
				Xvalue[j][t] = (int) cplex.getValue(X[1][j][t]);
			}
		}
		Excel newExcel = new Excel();
		newExcel.addExcelWorksheet("Pilot 1", Xvalue, "j", "t");
		newExcel.writeExcelFile("TEST");
//		for(String i: itemMapX.keySet())
//		{
//			for (String j: itemMapX.keySet()){
//				if (!i.equals(j)){
//					IloNumVar var = itemMapX.get(i).get(j);
//					System.out.println("Agent "+i+ "and " + j+ "(X): " +cplex.getValue(var));
//				}
//			}
//		}
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
				}
				else {
					q[j]= 0; 
				}
				 
			}
			i.addQj(q);
		}
		return pilots; 
	}
	
	public void closeModel(){
		cplex.end(); 
	}
}


