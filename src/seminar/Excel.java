package seminar;

//general import 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
// excel import 
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// cplex import 
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.*;

public class Excel {
	
	Workbook wbOut; 
	
	public Excel() {
		wbOut = new XSSFWorkbook();
	}
	
	public void printExcel(String file, IloNumVar[][][] X) {
		Sheet sheet1 = wbOut.createSheet("X"); 
		Row row = sheet1.createRow((short)0);
		row.createCell(0).setCellValue("Pilot i");
		
		row.createCell(1).setCellValue("TotalCosts");
		row.createCell(2).setCellValue("ClusterStability");
		row.createCell(3).setCellValue("Nr Arriving");
		row.createCell(4).setCellValue("Nr Leaving");
		row.createCell(5).setCellValue("TotalTime");
		row.createCell(6).setCellValue("Nr Clusters");
	    for (int k = 0; k < nrClusters ; k++) {
	    	row.createCell(k+7).setCellValue("Cluster " + Integer.toString(k));
	    }
	    for (int z = 0; z < nrClusters ; z++) {
	    	row.createCell(z+7+nrClusters).setCellValue("Clusterservicetime " + Integer.toString(z));
	    }
		
		int firstDay = 1; 
		if (resultList.size() > 0) {
			for (int i = firstDay; i <= resultList.size()-1; i++){
			  	row = sheet1.createRow((short)i);
			    Day d = resultList.get(i); 
			    row.createCell(0).setCellValue(i);
			    row.createCell(1).setCellValue(d.getTotalCosts());
			    row.createCell(2).setCellValue(d.getClusterStability());
			    row.createCell(3).setCellValue(d.getNrArriving());
			    row.createCell(4).setCellValue(d.getNrLeaving());
			    row.createCell(5).setCellValue(d.getTotalTime());
			    row.createCell(6).setCellValue(d.getNrClusters());
			    for (int k = 0; k < nrClusters ; k++) {
			    	row.createCell(k+7).setCellValue(d.getNrCustomers(k));
			    }
			    for (int z = 0; z < nrClusters ; z++) {
			    	row.createCell(z+7+nrClusters).setCellValue(d.getTime(z));
			    }
			}
			int lastday = resultList.size();
			row = sheet1.createRow((short)lastday);
			row.createCell(0).setCellValue(lastday);
			Day d = resultList.get(lastday); 
			row.createCell(2).setCellValue(d.getClusterStability());
		}
		
		
		Sheet sheet2 = wb.createSheet("Agents"); 
		Row row2 = sheet2.createRow((short)0);
		row2.createCell(0).setCellValue("Day");
		row2.createCell(1).setCellValue("ID");
		row2.createCell(2).setCellValue("Lat");
		row2.createCell(3).setCellValue("Lon");
		row2.createCell(4).setCellValue("DropFrequency");
		row2.createCell(5).setCellValue("Cluster");
		 
		if (agentPerDay.size()>0) {
			int m = 1; 
			for (int j = 0; j < (nrDays/clusterperiod)+1; j++) {
				HashMap<String, Agent> agentList = agentPerDay.get(j*clusterperiod+1);
				for (String k: agentList.keySet()) {
					row2 = sheet2.createRow((short)m);
					Agent a = agentList.get(k); 
					row2.createCell(0).setCellValue(j*clusterperiod+1);
					row2.createCell(1).setCellValue(a.getID());
					row2.createCell(2).setCellValue(a.getLat()); 
					row2.createCell(3).setCellValue(a.getLon());
					row2.createCell(4).setCellValue(a.getDropFreq());
					row2.createCell(5).setCellValue(a.getCluster());
					m++; 
				}
			}
		}
		
		Sheet sheet3 = wb.createSheet("Comments");
		ArrayList<String> comments = myWorld.getDMComments();
		int m = 0; 
		for (String c: comments) {
			try {
				Row row3 = sheet3.createRow((short)m);
				row3.createCell(0).setCellValue(c);
				m++; 	
			}
			catch (IllegalArgumentException e){
				System.out.println("illegal argument has been found. comment for loop is ended.");
				break;
			}
		}
		
		// Write the output to a file
		FileOutputStream fileOut = new FileOutputStream("C:\\Users\\Gebruiker\\Documents\\Florine\\"+ outputFile + ".xlsx");
	    wb.write(fileOut);
	    wb.close(); 
	    fileOut.close(); 
	}
}
