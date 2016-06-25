package com.ghintech.puntocom.process;

import com.ghintech.puntocom.model.MBankAccount_Commission;
import com.ghintech.puntocom.model.M_POSCloseCash;
import com.ghintech.puntocom.model.M_POSCloseCashLine;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.compiere.model.I_C_DocType;
import org.compiere.model.MBankAccount;
import org.compiere.model.MOrg;
import org.compiere.model.MPayment;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

public class POSProcessCloseCash
  extends SvrProcess
{
  private int p_Record_ID = 0;
  private String docAction = "";
  private String p_DocumentNo = "";
  private String p_Description = "";
  private String CurSymbol = "";
  private int p_C_BPartner_ID = 0;
  private int p_C_Currency_ID = 0;
  private int p_C_ConversionType_ID = 0;
  private int p_C_Charge_ID = 0;
  private BigDecimal p_Amount = Env.ZERO;
  private int p_From_C_BankAccount_ID = 0;
  private int p_To_C_BankAccount_ID = 0;
  private Timestamp p_StatementDate = null;
  private Timestamp p_DateAcct = null;
  private int m_created = 0;
  
  protected void prepare()
  {
    ProcessInfoParameter[] para = getParameter();
    ProcessInfoParameter[] arrayOfProcessInfoParameter1;
    int j = (arrayOfProcessInfoParameter1 = para).length;
    for (int i = 0; i < j; i++)
    {
      ProcessInfoParameter p = arrayOfProcessInfoParameter1[i];
      String name = p.getParameterName();
      if (name != null) {
        if (name.equals("DocAction")) {
          this.docAction = p.getParameter().toString();
        } else {
          this.log.severe("Unknown Parameter: " + name);
        }
      }
    }
    this.p_Record_ID = getRecord_ID();
  }
  
  protected String doIt()
    throws Exception
  {
    M_POSCloseCash CloseCash = new M_POSCloseCash(getCtx(), this.p_Record_ID, get_TrxName());
    if (this.docAction.equals("CO"))
    {
      String result = completeIt(this.p_Record_ID);
      
      int no = 0;
      if (this.log.isLoggable(Level.FINE)) {
        this.log.fine("Set Processed=" + no);
      }
      
      
        new StringBuilder("UPDATE POS_Close_Cash_Line ").append("SET Processed='Y' ").append("WHERE POS_Close_Cash_ID = ").append(this.p_Record_ID);
      if (no != 0) {
        this.log.warning("Processed Failed=" + no);
      }
      return result;
    }
    if (this.docAction.equals("VO")) {
      return CloseCash.voidIt(this.p_Record_ID);
    }
    return null;
  }
  
  public String completeIt(int p_Record_ID)
  {
    if (this.log.isLoggable(Level.INFO)) {
      this.log.info("From Bank=" + this.p_From_C_BankAccount_ID + " - To Bank=" + this.p_To_C_BankAccount_ID + 
        " - C_BPartner_ID=" + this.p_C_BPartner_ID + "- C_Charge_ID= " + this.p_C_Charge_ID + " - Amount=" + this.p_Amount + " - DocumentNo=" + this.p_DocumentNo + 
        " - Description=" + this.p_Description + " - Statement Date=" + this.p_StatementDate + 
        " - Date Account=" + this.p_DateAcct);
    }
    M_POSCloseCash CloseCash = new M_POSCloseCash(getCtx(), p_Record_ID, get_TrxName());
    MOrg org = new MOrg(getCtx(), CloseCash.getAD_Org_ID(), null);
    this.p_C_BPartner_ID = new Query(getCtx(), "AD_OrgInfo", "AD_Org_ID = " + CloseCash.getAD_Org_ID(), get_TrxName()).first().get_ValueAsInt("C_BPartner_ID");
    if (this.p_C_BPartner_ID == 0) {
      throw new IllegalStateException("No Existe un representante para la Organizaci��n: " + org.getName());
    }
    this.p_C_Charge_ID = new Query(getCtx(), "AD_OrgInfo", "AD_Org_ID = " + CloseCash.getAD_Org_ID(), get_TrxName()).first().get_ValueAsInt("C_Charge_ID");
    if (this.p_C_Charge_ID == 0) {
      throw new IllegalStateException("No Existe un Cargo POS para la Organizaci��n: " + org.getName());
    }
    this.p_From_C_BankAccount_ID = new Query(getCtx(), "C_BankAccount", "IsDefault = 'Y' AND BankAccountType = 'B' AND AD_Org_ID = " + CloseCash.getAD_Org_ID(), get_TrxName()).first().get_ValueAsInt("C_BankAccount_ID");
    if (this.p_From_C_BankAccount_ID == 0) {
      throw new IllegalStateException("No Existe una Caja POS para la Organizaci��n: " + org.getName());
    }
    this.p_C_Currency_ID = new Query(getCtx(), "C_AcctSchema", "C_AcctSchema.AD_Client_ID = " + CloseCash.getAD_Client_ID(), get_TrxName()).addJoinClause("INNER JOIN C_Currency ON C_Currency.C_Currency_ID = C_AcctSchema.C_Currency_ID").first().get_ValueAsInt("C_Currency_ID");
    this.CurSymbol = new Query(getCtx(), "C_Currency", "C_AcctSchema.AD_Client_ID = " + CloseCash.getAD_Client_ID(), get_TrxName()).addJoinClause("INNER JOIN C_AcctSchema ON C_Currency.C_Currency_ID = C_AcctSchema.C_Currency_ID").first().get_ValueAsString("CurSymbol");
    
    String sql = "SELECT POS_Close_Cash_Line_ID FROM POS_Close_Cash_Line WHERE POS_Close_Cash_ID = " + CloseCash.getPOS_Close_Cash_ID() + " ORDER BY POS_Close_Cash_Line_ID DESC";
    this.p_StatementDate = CloseCash.getDateTrx();
    this.p_DateAcct = CloseCash.getDateTrx();
    PreparedStatement pstmt1 = null;
    try
    {
      pstmt1 = DB.prepareStatement(sql, get_TrxName());
      ResultSet rs3 = pstmt1.executeQuery();
      while (rs3.next())
      {
        M_POSCloseCashLine line = new M_POSCloseCashLine(CloseCash.getCtx(), rs3.getInt("POS_Close_Cash_Line_ID"), CloseCash.get_TrxName());
        this.p_To_C_BankAccount_ID = line.getC_BankAccount_ID();
        this.p_Amount = line.getCashAmt();
        this.p_Description = ("Transferencia por Monto en Efectivo: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
        if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0) {
          generateBankTransfer();
        }
        this.p_Amount = line.getCheckAmt();
        this.p_Description = ("Transferencia por Monto en Cheque: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
        if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0) {
          generateBankTransfer();
        }
        this.p_Amount = line.getCreditCardAmt();
        this.p_Description = ("Transferencia por Monto en TDC: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
        if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0)
        {
          generateBankCommissions(line, "C");
          generateBankTransfer();
        }
        this.p_Amount = line.getDebitCardAmt();
        this.p_Description = ("Transferencia por Monto en TDD: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
        if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0)
        {
          generateBankCommissions(line, "D");
          generateBankTransfer();
        }
        this.p_Amount = line.getWireTransferAmt();
        this.p_Description = ("Transferencia por Monto en Transferencia: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
        if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0) {
          generateBankTransfer();
        }
      }
    }
    catch (SQLException e)
    {
      this.log.log(Level.SEVERE, "Line - " + sql.toString(), e);
      throw new IllegalStateException("Error Procesando Pagos");
    }
    StringBuilder msgreturn = new StringBuilder("@Created@ = ").append(this.m_created);
    if (this.m_created != 0)
    {
      CloseCash.setDocStatus("CO");
      CloseCash.setProcessed(true);
      CloseCash.set_ValueOfColumn("DateProcessed", new Timestamp(System.currentTimeMillis()));
      CloseCash.saveEx();
      try
      {
        pstmt1 = DB.prepareStatement(sql, get_TrxName());
        ResultSet rs3 = pstmt1.executeQuery();
        while (rs3.next())
        {
          M_POSCloseCashLine line = new M_POSCloseCashLine(CloseCash.getCtx(), rs3.getInt("POS_Close_Cash_Line_ID"), CloseCash.get_TrxName());
          line.set_ValueOfColumn("Processed", Boolean.valueOf(true));
          line.saveEx();
        }
      }
      catch (SQLException e)
      {
        this.log.log(Level.SEVERE, "Line - " + sql.toString(), e);
        throw new IllegalStateException("Error Procesando Lineas");
      }
    }
    else
    {
      throw new IllegalStateException("No hay lineas que Procesar ");
    }
    return msgreturn.toString();
  }
  
  private void generateBankTransfer()
  {
    int p_C_DocType_ID_From = MSysConfig.getIntValue("DoctypePaymentCloseCash", 0, getProcessInfo().getAD_Client_ID().intValue());
    int p_C_DocType_ID_To = MSysConfig.getIntValue("DoctypeReceiptCloseCash", 0, getProcessInfo().getAD_Client_ID().intValue());
    MBankAccount mBankFrom = new MBankAccount(getCtx(), this.p_From_C_BankAccount_ID, get_TrxName());
    MBankAccount mBankTo = new MBankAccount(getCtx(), this.p_To_C_BankAccount_ID, get_TrxName());
    MPayment paymentBankFrom = new MPayment(getCtx(), 0, get_TrxName());
    paymentBankFrom.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
    if (!Util.isEmpty(this.p_DocumentNo, true)) {
      paymentBankFrom.setDocumentNo(this.p_DocumentNo);
    }
    paymentBankFrom.setDateAcct(this.p_DateAcct);
    paymentBankFrom.setDateTrx(this.p_StatementDate);
    paymentBankFrom.setTenderType("A");
    paymentBankFrom.setDescription(this.p_Description);
    paymentBankFrom.setC_BPartner_ID(this.p_C_BPartner_ID);
    paymentBankFrom.setC_Currency_ID(this.p_C_Currency_ID);
    if (this.p_C_ConversionType_ID > 0) {
      paymentBankFrom.setC_ConversionType_ID(this.p_C_ConversionType_ID);
    }
    paymentBankFrom.setPayAmt(this.p_Amount);
    paymentBankFrom.setOverUnderAmt(Env.ZERO);
    paymentBankFrom.setC_DocType_ID(p_C_DocType_ID_From);
    paymentBankFrom.setC_Charge_ID(this.p_C_Charge_ID);
    paymentBankFrom.setAD_Org_ID(mBankFrom.getAD_Org_ID());
    paymentBankFrom.set_CustomColumn("POS_Close_Cash_ID", Integer.valueOf(this.p_Record_ID));
    paymentBankFrom.saveEx();
    if (!paymentBankFrom.processIt("CO"))
    {
      this.log.warning("Payment Process Failed: " + paymentBankFrom + " - " + paymentBankFrom.getProcessMsg());
      throw new IllegalStateException("Payment Process Failed: " + paymentBankFrom + " - " + paymentBankFrom.getProcessMsg());
    }
    paymentBankFrom.saveEx();
    addBufferLog(paymentBankFrom.getC_Payment_ID(), paymentBankFrom.getDateTrx(), 
      null, paymentBankFrom.getC_DocType().getPrintName() + " " + paymentBankFrom.getDocumentNo(), 
      335, paymentBankFrom.getC_Payment_ID());
    this.m_created += 1;
    
    MPayment paymentBankTo = new MPayment(getCtx(), 0, get_TrxName());
    paymentBankTo.setC_BankAccount_ID(mBankTo.getC_BankAccount_ID());
    if (!Util.isEmpty(this.p_DocumentNo, true)) {
      paymentBankTo.setDocumentNo(this.p_DocumentNo);
    }
    paymentBankTo.setDateAcct(this.p_DateAcct);
    paymentBankTo.setDateTrx(this.p_StatementDate);
    paymentBankTo.setTenderType("A");
    paymentBankTo.setDescription(this.p_Description);
    paymentBankTo.setC_BPartner_ID(this.p_C_BPartner_ID);
    paymentBankTo.setC_Currency_ID(this.p_C_Currency_ID);
    if (this.p_C_ConversionType_ID > 0) {
      paymentBankFrom.setC_ConversionType_ID(this.p_C_ConversionType_ID);
    }
    paymentBankTo.setPayAmt(this.p_Amount);
    paymentBankTo.setOverUnderAmt(Env.ZERO);
    paymentBankTo.setC_DocType_ID(p_C_DocType_ID_To);
    System.out.println("EL TIPO DE DOCUMENTO A GENERAR ES: " + p_C_DocType_ID_To);
    paymentBankTo.setAD_Org_ID(mBankTo.getAD_Org_ID());
    paymentBankTo.set_CustomColumn("POS_Close_Cash_ID", Integer.valueOf(this.p_Record_ID));
    paymentBankTo.saveEx();
    if (!paymentBankTo.processIt("CO"))
    {
      this.log.warning("Payment Process Failed: " + paymentBankTo + " - " + paymentBankTo.getProcessMsg());
      throw new IllegalStateException("Payment Process Failed: " + paymentBankTo + " - " + paymentBankTo.getProcessMsg());
    }
    paymentBankTo.saveEx();
    addBufferLog(paymentBankTo.getC_Payment_ID(), paymentBankTo.getDateTrx(), 
      null, paymentBankTo.getC_DocType().getPrintName() + " " + paymentBankTo.getDocumentNo(), 
      335, paymentBankTo.getC_Payment_ID());
    this.m_created += 1;
  }
  
  private void generateBankCommissions(M_POSCloseCashLine CloseCashLine, String TrxType)
  {
    MBankAccount BankAccount = new MBankAccount(getCtx(), this.p_To_C_BankAccount_ID, get_TrxName());
    M_POSCloseCash CloseCash = new M_POSCloseCash(getCtx(), CloseCashLine.getPOS_Close_Cash_ID(), get_TrxName());
    BigDecimal amt = this.p_Amount;
    BigDecimal totalComm = Env.ZERO;
    BigDecimal totalISLR = Env.ZERO;
    
    BigDecimal DebitRate = (BigDecimal)BankAccount.get_Value("DebitRate");
    BigDecimal CreditRate = (BigDecimal)BankAccount.get_Value("CreditRate");
    BigDecimal ISLRRate = (BigDecimal)BankAccount.get_Value("ISLRRate");
    
    MBankAccount_Commission baCommision = 
      (MBankAccount_Commission)new Query(getCtx(), "C_BankAccount_Commission", "C_BankAccount_ID=? and AD_Org_ID=?", get_TrxName()).setParameters(new Object[] {Integer.valueOf(BankAccount.getC_BankAccount_ID()), Integer.valueOf(CloseCash.getAD_Org_ID()) }).first();
    if (baCommision != null)
    {
      DebitRate = baCommision.getdebitrate();
      CreditRate = baCommision.getcreditrate();
      ISLRRate = baCommision.getislrrate();
    }
    int Commissions_C_Charge_ID = 0;
    int ISLR_C_Charge_ID = 0;
    if ((DebitRate == null) || (CreditRate == null) || (ISLRRate == null)) {
      throw new IllegalStateException("Falta Asignar tasas para cuenta: " + BankAccount.getName());
    }
    if ((TrxType == "D") && (DebitRate.compareTo(Env.ZERO) > 0))
    {
      totalComm = amt.multiply(DebitRate).divide(new BigDecimal(100));
    }
    else if ((TrxType == "C") && (CreditRate.compareTo(Env.ZERO) > 0))
    {
      totalComm = amt.multiply(CreditRate).divide(new BigDecimal(100));
      totalISLR = amt.multiply(ISLRRate).divide(new BigDecimal(100));
    }
    else
    {
      return;
    }
    PreparedStatement pst = null;
    ResultSet rs = null;
    String sql = "SELECT Name,Value FROM AD_SysConfig WHERE Name IN ('ISLR_C_Charge_ID','Commissions_C_Charge_ID')";
    try
    {
      pst = DB.prepareStatement(sql, null);
      rs = pst.executeQuery();
      while (rs.next())
      {
        if (rs.getString("Name").compareTo("ISLR_C_Charge_ID") == 0) {
          ISLR_C_Charge_ID = Integer.parseInt(rs.getString("Value"));
        }
        if (rs.getString("Name").compareTo("Commissions_C_Charge_ID") == 0) {
          Commissions_C_Charge_ID = Integer.parseInt(rs.getString("Value"));
        }
      }
    }
    catch (SQLException e)
    {
      this.log.log(Level.SEVERE, "Line - " + sql.toString(), e);
    }
    MPayment paymentBank = new MPayment(getCtx(), 0, get_TrxName());
    
    MBankAccount mBankFrom = new MBankAccount(getCtx(), this.p_From_C_BankAccount_ID, get_TrxName());
    paymentBank.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
    if (!Util.isEmpty(this.p_DocumentNo, true)) {
      paymentBank.setDocumentNo(this.p_DocumentNo);
    }
    paymentBank.setDateAcct(this.p_DateAcct);
    paymentBank.setDateTrx(this.p_StatementDate);
    paymentBank.setTenderType("A");
    paymentBank.setDescription("Comision Banco " + BankAccount.getName() + " para Cierre de Caja Nro: " + CloseCash.getPOS_Close_Cash_ID());
    paymentBank.setC_BPartner_ID(this.p_C_BPartner_ID);
    paymentBank.setC_Currency_ID(this.p_C_Currency_ID);
    if (this.p_C_ConversionType_ID > 0) {
      paymentBank.setC_ConversionType_ID(this.p_C_ConversionType_ID);
    }
    paymentBank.setPayAmt(totalComm);
    paymentBank.setOverUnderAmt(Env.ZERO);
    paymentBank.setC_DocType_ID(false);
    paymentBank.setC_Charge_ID(Commissions_C_Charge_ID);
    paymentBank.setAD_Org_ID(mBankFrom.getAD_Org_ID());
    paymentBank.set_CustomColumn("POS_Close_Cash_ID", Integer.valueOf(this.p_Record_ID));
    paymentBank.saveEx();
    if (!paymentBank.processIt("CO"))
    {
      this.log.warning("Payment Process Failed: " + paymentBank + " - " + paymentBank.getProcessMsg());
      throw new IllegalStateException("Payment Process Failed: " + paymentBank + " - " + paymentBank.getProcessMsg());
    }
    paymentBank.saveEx();
    addBufferLog(paymentBank.getC_Payment_ID(), paymentBank.getDateTrx(), 
      null, paymentBank.getC_DocType().getPrintName() + " " + paymentBank.getDocumentNo() + " Comisi��n Banco", 
      335, paymentBank.getC_Payment_ID());
    this.m_created += 1;
    if (totalISLR.compareTo(Env.ZERO) > 0)
    {
      MPayment paymentBankISLR = new MPayment(getCtx(), 0, get_TrxName());
      
      paymentBankISLR.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
      if (!Util.isEmpty(this.p_DocumentNo, true)) {
        paymentBankISLR.setDocumentNo(this.p_DocumentNo);
      }
      paymentBankISLR.setDateAcct(this.p_DateAcct);
      paymentBankISLR.setDateTrx(this.p_StatementDate);
      paymentBankISLR.setTenderType("A");
      paymentBankISLR.setDescription("Comision ISLR " + BankAccount.getName() + " para Cierre de Caja Nro: " + CloseCash.getPOS_Close_Cash_ID());
      paymentBankISLR.setC_BPartner_ID(this.p_C_BPartner_ID);
      paymentBankISLR.setC_Currency_ID(this.p_C_Currency_ID);
      if (this.p_C_ConversionType_ID > 0) {
        paymentBankISLR.setC_ConversionType_ID(this.p_C_ConversionType_ID);
      }
      paymentBankISLR.setPayAmt(totalISLR);
      paymentBankISLR.setOverUnderAmt(Env.ZERO);
      paymentBankISLR.setC_DocType_ID(false);
      paymentBankISLR.setC_Charge_ID(ISLR_C_Charge_ID);
      
      paymentBankISLR.setAD_Org_ID(mBankFrom.getAD_Org_ID());
      paymentBankISLR.set_CustomColumn("POS_Close_Cash_ID", Integer.valueOf(this.p_Record_ID));
      paymentBankISLR.saveEx();
      if (!paymentBankISLR.processIt("CO"))
      {
        this.log.warning("Payment Process Failed: " + paymentBankISLR + " - " + paymentBankISLR.getProcessMsg());
        throw new IllegalStateException("Payment Process Failed: " + paymentBankISLR + " - " + paymentBankISLR.getProcessMsg());
      }
      paymentBankISLR.saveEx();
      addBufferLog(paymentBankISLR.getC_Payment_ID(), paymentBankISLR.getDateTrx(), 
        null, paymentBankISLR.getC_DocType().getPrintName() + " " + paymentBankISLR.getDocumentNo() + " Comisi��n ISLR", 
        335, paymentBankISLR.getC_Payment_ID());
      this.m_created += 1;
    }
    this.p_Amount = this.p_Amount.subtract(totalISLR.add(totalComm));
  }
}

