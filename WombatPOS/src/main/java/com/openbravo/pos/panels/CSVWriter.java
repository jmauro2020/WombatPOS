/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.openbravo.pos.panels;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;


/**
 *
 * @author Matthew
 */
public class CSVWriter {
    private PrintWriter csv_pw;
    
    public CSVWriter(File file){
        try{
            this.csv_pw = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void write(List<String> arr){
        StringBuilder newline = new StringBuilder();
        for(String elem: arr){
            newline.append(elem);
            newline.append(',');
        }
        newline.setCharAt(newline.length()-1,'\n');
        System.out.println(newline.toString());
        this.csv_pw.print(newline.toString());
    }
    
    public void close(){
        this.csv_pw.close();
    }
    
    
}
