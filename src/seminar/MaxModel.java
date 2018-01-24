package seminar;

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
	private ArrayList<Plane> planes;
	private int I; 
	private int J; 
	private int K; 
	private int T;
	
	private IloNumVar[][][] X;
	private IloNumVar[][] Y;
	private IloNumVar[][][] V; 
	private IloNumVar[][] Z; 
	
	private IloNumVar[][] Holiday ;
	private IloNumVar[][] Office; 
	private IloNumVar[][] QRA; 
	private IloNumVar[][] RestDay; 
	private IloNumVar[] Course;
	private IloNumVar[][] DutyFree; 
	private IloNumVar[][] LongHoliday; 
	
	private double beta ;
	
//	private HashMap<HashMap<HashMap<IloNumVar, Integer>, Integer>,Integer> varMapX; 
//	private HashMap<Integer, HashMap<Integer, HashMap<Integer, IloNumVar>>> itemMapX; 
//	private HashMap<Integer, HashMap<Integer, IloNumVar>> itemMapY; 
//	private HashMap<HashMap<IloNumVar, Integer>, Integer> varMapY; 
//	private HashMap<Integer, HashMap<Integer, IloNumVar>> itemMapV; 
//	private HashMap<HashMap<IloNumVar, Integer>, Integer> varMapV; 
	
	public MaxModel(ArrayList<Pilot> pilotList, ArrayList<Training> trainingList, ArrayList<Plane> planeList, int lengthTimeFrame, double valueBeta) throws IloException, objectNotFoundException{
		pilots = pilotList; 
		trainings = trainingList; 
		planes = planeList; 
		I = pilots.size();
		J = trainings.size(); 
		K = planes.size();
		T = lengthTimeFrame; 
		beta = valueBeta; 
//		varMapX = new HashMap<HashMap<HashMap<IloNumVar, Integer>, Integer>, Integer>();  
//		itemMapX = new HashMap<Integer, HashMap<Integer, HashMap<Integer, IloNumVar>>>();  
//		itemMapY = new HashMap<Integer, HashMap<Integer, IloNumVar>>(); 
//		varMapY = new HashMap<HashMap<IloNumVar, Integer>, Integer>(); 
//		itemMapV = new HashMap<Integer, HashMap<Integer, IloNumVar>>(); 
//		varMapV = new HashMap<HashMap<IloNumVar, Integer>, Integer>();
		X = new IloNumVar[I][J][T];
		Y = new IloNumVar[K][T];
		V = new IloNumVar[C][J][T]; 
		Z = new IloNumVar[I][J]; 
		
		cplex = new IloCplex();
		cplex.setParam(	IloCplex.Param.TimeLimit, timelim); 
		
		initVars();
		initCompleteTraining(); 
		initMax1Training(); 
		initNrPlanesIsNrPilots(); 
		initNrPilotsPerTraining(); 
		initRequiredMachine(); 
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
		// create Y 
		for (int k = 0; k <K; k++) {
			for (int t = 0; t < T; t++) {
				IloNumVar varY = cplex.boolVar();
				Y[k][t] = varY;
			}
		}
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
		for (int t = 0; t<I; t++) {
			for (int i = 0; i<T; i++) {
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
	
	//C5
	public void initNrPlanesIsNrPilots() throws IloException{
		for (int t = 0; t<T; t++) {
			IloNumExpr expr = cplex.numExpr();
			for (int k = 0 ; k< K; k++) {
				IloNumVar var= Y[k][t];
				expr = cplex.sum(expr, var); 
			}
			IloNumExpr expr2 = cplex.numExpr();
			for(int i = 0; i< I; i++) {
				for (int j = 0; j < J; j++) {
					IloNumVar var= X[i][j][t];
					expr2 = cplex.sum(expr2, var);
				}
			}
			if (expr != null && expr2 != null){
			cplex.addEq(expr, expr2);	
			}
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
	public void initRequiredMachine() throws IloException{
		for (int t = 0; t < T; t++) {
			// left side
			IloNumExpr expr = cplex.numExpr();
			for (int k = 0; k < K ; k++) {
				IloNumExpr term = cplex.prod(planes.get(k).getType(), Y[k][t]);
				expr = cplex.sum(expr, term); 
			}
			// right side 
			IloNumExpr expr2 = cplex.numExpr();
			for (int j = 0; j<J; j++) {
				IloNumExpr expr2temp = cplex.numExpr();
				for (int i = 0; i<I; i++) {
					IloNumVar var = X[i][j][t];
					expr2temp = cplex.sum(expr2temp, var); 
				}
				expr2temp = cplex.prod(expr2temp, trainings.get(j).getR());
				expr2 = cplex.sum(expr2, expr2temp); 
			}
			if (expr != null && expr2 != null){
				cplex.addEq(expr, expr2);	
			}
		}
	}
	
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
				expr = cplex.sum(expr, QRA[i][t-1]);
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
	
	public void initDayNight() {
	}
	
	public void initHolidays(int nrHolidays, int nrLongHolidays, int nrPilotsLong, int firstPilotLong) throws IloException {
		for (int i = 0; i <I; i++) {
			IloNumExpr expr = cplex.numExpr();  // for constraint of nrHolidays per year 
			IloNumExpr expr2 = cplex.numExpr();  // for constraint of 1 long holiday per year 
			for (int t = 0; t<T; t++) {
				expr = cplex.sum(expr, Holiday[i][t]);
				expr2 = cplex.sum(expr2, LongHoliday[i][t]);
				
				IloNumExpr expr3 = cplex.numExpr(); // left side of constraint for length long holiday
				for (int time = t; time < t+9; time++) { 
					expr3 = cplex.sum(expr3, Holiday[i][time]); 
				}
				IloNumExpr expr4 = cplex.numExpr();  // right side of constraint for length long holiday
				expr4 = cplex.prod(LongHoliday[i][t], nrLongHolidays); 
				cplex.addGe(expr3, expr4); 
			}
			cplex.addEq(expr, nrHolidays); 
			if (i >= firstPilotLong && i <= firstPilotLong + nrPilotsLong) {
				cplex.addGe(expr2, 1);	
			}
			 
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
	
	public void initQRA() throws IloException {
		for (int t = 0; t<T; t++) {
			IloNumExpr expr = cplex.numExpr();
			for (int i = 0; i<I ; i++) {
				expr = cplex.sum(expr, QRA[i][t]);
			}
			cplex.addEq(expr, 2); 
		}
	}
	
	public void initCourses(int nrCourses) throws IloException {
		IloNumExpr expr = cplex.numExpr(); 
		for (int t =0 ; t<T; t++) {	
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
	
	public void printSolution() throws IloException{
		System.out.println("Solution value: "+cplex.getObjValue());
		for (int j=0; j<J; j++) {
			for (int t=0; t<T; t++) {
				IloNumVar var = X[1][j][t];
				System.out.println("Pilot 1, Training " + j + " and time " + t+ " : " + cplex.getValue(var));;
			}
		}
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


