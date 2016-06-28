/*    */ package com.ghintech.puntocom.process;
/*    */ 
/*    */ import com.ghintech.puntocom.model.M_POSCloseCash;
/*    */ import com.ghintech.puntocom.model.M_POSCloseCashLine;
/*    */ import java.sql.PreparedStatement;
/*    */ import java.sql.ResultSet;
/*    */ import java.sql.SQLException;
/*    */ import java.util.logging.Level;
/*    */ import org.compiere.model.MOrg;
/*    */ import org.compiere.process.ProcessInfoParameter;
/*    */ import org.compiere.process.SvrProcess;
/*    */ import org.compiere.util.CLogger;
/*    */ import org.compiere.util.DB;
/*    */ 
/*    */ public class ImportLinesFromBankAccounts extends SvrProcess
/*    */ {
/* 17 */   private int m_POS_Close_Cash_ID = 0;
/* 18 */   private String mainMsg = "";
/* 19 */   private int count = 0;
/*    */   
/*    */   protected void prepare()
/*    */   {
/* 23 */     ProcessInfoParameter[] para = getParameter();
/* 24 */     for (int i = 0; i < para.length; i++)
/*    */     {
/* 26 */       String name = para[i].getParameterName();
/* 27 */       if (name.equals("POS_Close_Cash_ID")) {
/* 28 */         this.m_POS_Close_Cash_ID = Integer.parseInt(para[i].getParameter().toString());
/*    */       } else {
/* 30 */         this.log.log(Level.SEVERE, "Unknown Parameter: " + name);
/*    */       }
/*    */     }
/*    */   }
/*    */   
/*    */   protected String doIt()
/*    */     throws Exception
/*    */   {
/* 38 */     if (this.m_POS_Close_Cash_ID != 0)
/*    */     {
/* 40 */       M_POSCloseCash CloseCash = new M_POSCloseCash(getCtx(), this.m_POS_Close_Cash_ID, get_TrxName());
/* 41 */       int orgID = CloseCash.getAD_Org_ID();
/* 42 */       MOrg org = new MOrg(getCtx(), orgID, null);
/* 43 */       if (orgID == 0)
/* 44 */         throw new IllegalStateException("Falló al Crear Lineas: Debe Crear Registro en una Organización diferente a " + org.getName());
/* 45 */       String sql = "SELECT a.C_Bank_ID,a.C_BankAccount_ID FROM C_BankAccount a WHERE a.isActive='Y' AND (a.AD_Org_ID = ? OR AD_Org_ID = 1000009) AND BankAccountType!='B' AND C_BankAccount_ID NOT IN (SELECT C_BankAccount_ID FROM POS_Close_Cash_Line WHERE POS_Close_Cash_ID = ?)";
/*    */       
/*    */       try
/*    */       {
/* 49 */         PreparedStatement pstmt = null;
/* 50 */         ResultSet rs = null;
/* 51 */         pstmt = DB.prepareStatement(sql, null);
/* 52 */         pstmt.setInt(1, orgID);
/* 53 */         pstmt.setInt(2, this.m_POS_Close_Cash_ID);
/* 54 */         rs = pstmt.executeQuery();
/* 55 */         while (rs.next()) {
/* 56 */           this.count += 1;
/* 57 */           M_POSCloseCashLine line = new M_POSCloseCashLine(getCtx());
/* 58 */           line.setPOS_Close_Cash_ID(this.m_POS_Close_Cash_ID);
/* 59 */           line.setAD_Org_ID(orgID);
/* 60 */           line.setC_Bank_ID(rs.getInt("C_Bank_ID"));
/* 61 */           line.setC_BankAccount_ID(rs.getInt("C_BankAccount_ID"));
/*    */           
/* 63 */           line.save();
/*    */         }
/* 65 */         if (this.count == 0)
/* 66 */           throw new IllegalStateException("Falló al Crear Lineas: No existen mas Cuentas Bancarias para la Organización " + org.getName());
/* 67 */         this.mainMsg = ("Se crearon " + this.count + " lineas");
/*    */       }
/*    */       catch (SQLException e)
/*    */       {
/* 71 */         this.log.log(Level.SEVERE, "Line - " + sql.toString(), e);
/*    */       }
/*    */     }
/* 74 */     return this.mainMsg;
/*    */   }
/*    */ }


/* Location:              /home/james/Desktop/POSIntegration/!/ImportLinesFromBankAccounts.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */