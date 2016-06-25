package com.ghintech.puntocom.process;

import com.ghintech.puntocom.model.M_POSCloseCash;
import com.ghintech.puntocom.model.M_POSCloseCashLine;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import org.compiere.model.MOrg;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

public class ImportLinesFromBankAccounts
  extends SvrProcess
{
  private int m_POS_Close_Cash_ID = 0;
  private String mainMsg = "";
  private int count = 0;
  
  protected void prepare()
  {
    ProcessInfoParameter[] para = getParameter();
    for (int i = 0; i < para.length; i++)
    {
      String name = para[i].getParameterName();
      if (name.equals("POS_Close_Cash_ID")) {
        this.m_POS_Close_Cash_ID = Integer.parseInt(para[i].getParameter().toString());
      } else {
        this.log.log(Level.SEVERE, "Unknown Parameter: " + name);
      }
    }
  }
  
  protected String doIt()
    throws Exception
  {
    if (this.m_POS_Close_Cash_ID != 0)
    {
      M_POSCloseCash CloseCash = new M_POSCloseCash(getCtx(), this.m_POS_Close_Cash_ID, get_TrxName());
      int orgID = CloseCash.getAD_Org_ID();
      MOrg org = new MOrg(getCtx(), orgID, null);
      if (orgID == 0) {
        throw new IllegalStateException("Fall�� al Crear Lineas: Debe Crear Registro en una Organizaci��n diferente a " + org.getName());
      }
      String sql = "SELECT a.C_Bank_ID,a.C_BankAccount_ID FROM C_BankAccount a WHERE a.isActive='Y' AND (a.AD_Org_ID = ? OR AD_Org_ID = 1000009) AND BankAccountType!='B' AND C_BankAccount_ID NOT IN (SELECT C_BankAccount_ID FROM POS_Close_Cash_Line WHERE POS_Close_Cash_ID = ?)";
      try
      {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        pstmt = DB.prepareStatement(sql, null);
        pstmt.setInt(1, orgID);
        pstmt.setInt(2, this.m_POS_Close_Cash_ID);
        rs = pstmt.executeQuery();
        while (rs.next())
        {
          this.count += 1;
          M_POSCloseCashLine line = new M_POSCloseCashLine(getCtx());
          line.setPOS_Close_Cash_ID(this.m_POS_Close_Cash_ID);
          line.setAD_Org_ID(orgID);
          line.setC_Bank_ID(rs.getInt("C_Bank_ID"));
          line.setC_BankAccount_ID(rs.getInt("C_BankAccount_ID"));
          
          line.save();
        }
        if (this.count == 0) {
          throw new IllegalStateException("Fall�� al Crear Lineas: No existen mas Cuentas Bancarias para la Organizaci��n " + org.getName());
        }
        this.mainMsg = ("Se crearon " + this.count + " lineas");
      }
      catch (SQLException e)
      {
        this.log.log(Level.SEVERE, "Line - " + sql.toString(), e);
      }
    }
    return this.mainMsg;
  }
}

