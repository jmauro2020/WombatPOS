package com.ghintech.puntocom.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

public class FillZreportSequence
  extends SvrProcess
{
  private int p_AD_Org_ID = 0;
  private Timestamp p_from = null;
  private Timestamp p_to = null;
  
  protected void prepare()
  {
    ProcessInfoParameter[] para = getParameter();
    for (int i = 0; i < para.length; i++)
    {
      String name = para[i].getParameterName();
      if (name.equals("AD_Org_ID")) {
        this.p_AD_Org_ID = para[i].getParameterAsInt();
      } else if (name.equals("from")) {
        this.p_from = ((Timestamp)para[i].getParameter());
      } else if (name.equals("to")) {
        this.p_to = ((Timestamp)para[i].getParameter());
      } else {
        this.log.log(Level.SEVERE, "Unknown Parameter: " + name);
      }
    }
  }
  
  protected String doIt()
    throws Exception
  {
    StringBuilder sql = new StringBuilder();
    String from = DB.TO_DATE(this.p_from);
    String to = DB.TO_DATE(this.p_to);
    sql.append("SELECT fiscalprint_serial FROM lve_salesbook_fiscal WHERE tipodocumento='F'")
      .append(" AND fiscal_zreport is not null AND fiscalprint_serial is not null")
      .append(" AND ").append(" datefilter between ").append(from).append(" AND ").append(to)
      .append(" AND ").append(" AD_Org_ID = ").append(this.p_AD_Org_ID)
      .append(" GROUP by fiscalprint_serial")
      .append(" ORDER by fiscalprint_serial");
    
    List<String> fiscalprint = new ArrayList();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
    rs = pstmt.executeQuery();
    while (rs.next()) {
      fiscalprint.add(rs.getString(1));
    }
    for (int i = 0; i < fiscalprint.size(); i++)
    {
      new StringBuilder("SELECT max(cast(fiscal_zreport AS int)) AS minZ FROM lve_salesbook_fiscal WHERE tipodocumento='F'  AND fiscal_zreport is not null  AND fiscalprint_serial is not null  AND datefilter < ")
      
        .append(from)
        .append(" AND AD_Org_ID = ").append(this.p_AD_Org_ID)
        .append(" GROUP BY fiscalprint_serial ORDER BY  fiscalprint_serial").toString();
      pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
      rs = pstmt.executeQuery();
      rs.first();
      rs.getInt("minZ");
    }
    return "";
  }
}

