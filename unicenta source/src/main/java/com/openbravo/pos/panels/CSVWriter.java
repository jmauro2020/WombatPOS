/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.openbravo.pos.panels;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;


/**
 *
 * @author Matthew
 */
public class CSVWriter {
    private final String filename;
    private PrintWriter csv_pw;
    
    public CSVWriter(String filename){
        this.filename = filename;
        try{
            this.csv_pw = new PrintWriter(new File(this.filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void write(String[] arr){
        StringBuilder newline = new StringBuilder();
        for(String elem: arr){
            newline.append(elem);
            newline.append(',');
        }
        newline.setCharAt(newline.length()-1,'\n');
        csv_pw.print(newline.toString());
    }
    
    public void close(){
        csv_pw.close();
    }
    
    
}
