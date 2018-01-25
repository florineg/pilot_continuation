package seminar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
	
	public void addExcelWorksheet(String worksheet, int[][] X, String firstIndex, String secondIndex) {
		Sheet sheet1 = wbOut.createSheet(worksheet); 
		Row row = sheet1.createRow((short)0);
		row.createCell(0).setCellValue(firstIndex + " x " + secondIndex);
		int M = X.length; //row 
		int N = X[0].length; //column 
		for (int m =0; m <M ; m++) {
			row = sheet1.createRow((short)m+1); 
			for (int n = 0; n<N; n++) {
				int thisVar = X[m][n];
				row.createCell(n).setCellValue(thisVar);
			}
		}
	}
	
	public void writeExcelFile(String fileName) throws IOException {
		// Write the output to a file
		FileOutputStream fileOut = new FileOutputStream("C:\\"+ fileName + ".xlsx");
	    wbOut.write(fileOut);
	    wbOut.close(); 
	    fileOut.close(); 
	}
}
