/*     */ package com.ghintech.puntocom.process;
/*     */ 
/*     */ import com.ghintech.puntocom.model.MBankAccount_Commission;
/*     */ import com.ghintech.puntocom.model.M_POSCloseCash;
/*     */ import com.ghintech.puntocom.model.M_POSCloseCashLine;
/*     */ import java.io.PrintStream;
/*     */ import java.math.BigDecimal;
/*     */ import java.sql.PreparedStatement;
/*     */ import java.sql.ResultSet;
/*     */ import java.sql.SQLException;
/*     */ import java.sql.Timestamp;
/*     */ import java.util.logging.Level;
/*     */ import org.compiere.model.I_C_DocType;
/*     */ import org.compiere.model.MBankAccount;
/*     */ import org.compiere.model.MOrg;
/*     */ import org.compiere.model.MPayment;
/*     */ import org.compiere.model.MSysConfig;
/*     */ import org.compiere.model.PO;
/*     */ import org.compiere.model.Query;
/*     */ import org.compiere.process.ProcessInfo;
/*     */ import org.compiere.process.ProcessInfoParameter;
/*     */ import org.compiere.process.SvrProcess;
/*     */ import org.compiere.util.CLogger;
/*     */ import org.compiere.util.DB;
/*     */ import org.compiere.util.Env;
/*     */ import org.compiere.util.Util;
/*     */ 
/*     */ public class POSProcessCloseCash extends SvrProcess
/*     */ {
/*  30 */   private int p_Record_ID = 0;
/*  31 */   private String docAction = "";
/*  32 */   private String p_DocumentNo = "";
/*  33 */   private String p_Description = "";
/*  34 */   private String CurSymbol = "";
/*  35 */   private int p_C_BPartner_ID = 0;
/*  36 */   private int p_C_Currency_ID = 0;
/*  37 */   private int p_C_ConversionType_ID = 0;
/*  38 */   private int p_C_Charge_ID = 0;
/*     */   
/*  40 */   private BigDecimal p_Amount = Env.ZERO;
/*  41 */   private int p_From_C_BankAccount_ID = 0;
/*  42 */   private int p_To_C_BankAccount_ID = 0;
/*  43 */   private Timestamp p_StatementDate = null;
/*  44 */   private Timestamp p_DateAcct = null;
/*  45 */   private int m_created = 0;
/*     */   
/*     */   protected void prepare()
/*     */   {
/*  49 */     ProcessInfoParameter[] para = getParameter();
/*  50 */     ProcessInfoParameter[] arrayOfProcessInfoParameter1; int j = (arrayOfProcessInfoParameter1 = para).length; for (int i = 0; i < j; i++) { ProcessInfoParameter p = arrayOfProcessInfoParameter1[i];
/*  51 */       String name = p.getParameterName();
/*  52 */       if (name != null)
/*     */       {
/*  54 */         if (name.equals("DocAction")) {
/*  55 */           this.docAction = p.getParameter().toString();
/*     */         } else
/*  57 */           this.log.severe("Unknown Parameter: " + name); }
/*     */     }
/*  59 */     this.p_Record_ID = getRecord_ID();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   protected String doIt()
/*     */     throws Exception
/*     */   {
/*  67 */     M_POSCloseCash CloseCash = new M_POSCloseCash(getCtx(), this.p_Record_ID, get_TrxName());
/*  68 */     if (this.docAction.equals("CO")) {
/*  69 */       String result = completeIt(this.p_Record_ID);
/*     */       
/*  71 */       int no = 0;
/*  72 */       if (this.log.isLoggable(Level.FINE)) this.log.fine("Set Processed=" + no);
/*  73 */       
/*     */       
/*  75 */         new StringBuilder("UPDATE POS_Close_Cash_Line ").append("SET Processed='Y' ").append("WHERE POS_Close_Cash_ID = ").append(this.p_Record_ID);
/*     */       
/*  77 */       if (no != 0) {
/*  78 */         this.log.warning("Processed Failed=" + no);
/*     */       }
/*  80 */       return result;
/*     */     }
/*     */     
/*  83 */     if (this.docAction.equals("VO")) {
/*  84 */       return CloseCash.voidIt(this.p_Record_ID);
/*     */     }
/*  86 */     return null;
/*     */   }
/*     */   
/*     */   public String completeIt(int p_Record_ID)
/*     */   {
/*  91 */     if (this.log.isLoggable(Level.INFO)) this.log.info("From Bank=" + this.p_From_C_BankAccount_ID + " - To Bank=" + this.p_To_C_BankAccount_ID + 
/*  92 */         " - C_BPartner_ID=" + this.p_C_BPartner_ID + "- C_Charge_ID= " + this.p_C_Charge_ID + " - Amount=" + this.p_Amount + " - DocumentNo=" + this.p_DocumentNo + 
/*  93 */         " - Description=" + this.p_Description + " - Statement Date=" + this.p_StatementDate + 
/*  94 */         " - Date Account=" + this.p_DateAcct);
/*  95 */     M_POSCloseCash CloseCash = new M_POSCloseCash(getCtx(), p_Record_ID, get_TrxName());
/*  96 */     MOrg org = new MOrg(getCtx(), CloseCash.getAD_Org_ID(), null);
/*  97 */     this.p_C_BPartner_ID = new Query(getCtx(), "AD_OrgInfo", "AD_Org_ID = " + CloseCash.getAD_Org_ID(), get_TrxName()).first().get_ValueAsInt("C_BPartner_ID");
/*  98 */     if (this.p_C_BPartner_ID == 0) throw new IllegalStateException("No Existe un representante para la Organización: " + org.getName());
/*  99 */     this.p_C_Charge_ID = new Query(getCtx(), "AD_OrgInfo", "AD_Org_ID = " + CloseCash.getAD_Org_ID(), get_TrxName()).first().get_ValueAsInt("C_Charge_ID");
/* 100 */     if (this.p_C_Charge_ID == 0) throw new IllegalStateException("No Existe un Cargo POS para la Organización: " + org.getName());
/* 101 */     this.p_From_C_BankAccount_ID = new Query(getCtx(), "C_BankAccount", "IsDefault = 'Y' AND BankAccountType = 'B' AND AD_Org_ID = " + CloseCash.getAD_Org_ID(), get_TrxName()).first().get_ValueAsInt("C_BankAccount_ID");
/* 102 */     if (this.p_From_C_BankAccount_ID == 0) throw new IllegalStateException("No Existe una Caja POS para la Organización: " + org.getName());
/* 103 */     this.p_C_Currency_ID = new Query(getCtx(), "C_AcctSchema", "C_AcctSchema.AD_Client_ID = " + CloseCash.getAD_Client_ID(), get_TrxName()).addJoinClause("INNER JOIN C_Currency ON C_Currency.C_Currency_ID = C_AcctSchema.C_Currency_ID").first().get_ValueAsInt("C_Currency_ID");
/* 104 */     this.CurSymbol = new Query(getCtx(), "C_Currency", "C_AcctSchema.AD_Client_ID = " + CloseCash.getAD_Client_ID(), get_TrxName()).addJoinClause("INNER JOIN C_AcctSchema ON C_Currency.C_Currency_ID = C_AcctSchema.C_Currency_ID").first().get_ValueAsString("CurSymbol");
/*     */     
/*     */ 
/* 107 */     String sql = "SELECT POS_Close_Cash_Line_ID FROM POS_Close_Cash_Line WHERE POS_Close_Cash_ID = " + CloseCash.getPOS_Close_Cash_ID() + " ORDER BY POS_Close_Cash_Line_ID DESC";
/* 108 */     this.p_StatementDate = CloseCash.getDateTrx();
/* 109 */     this.p_DateAcct = CloseCash.getDateTrx();
/* 110 */     PreparedStatement pstmt1 = null;
/*     */     try {
/* 112 */       pstmt1 = DB.prepareStatement(sql, get_TrxName());
/* 113 */       ResultSet rs3 = pstmt1.executeQuery();
/*     */       
/* 115 */       while (rs3.next()) {
/* 116 */         M_POSCloseCashLine line = new M_POSCloseCashLine(CloseCash.getCtx(), rs3.getInt("POS_Close_Cash_Line_ID"), CloseCash.get_TrxName());
/* 117 */         this.p_To_C_BankAccount_ID = line.getC_BankAccount_ID();
/* 118 */         this.p_Amount = line.getCashAmt();
/* 119 */         this.p_Description = ("Transferencia por Monto en Efectivo: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
/* 120 */         if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0)
/* 121 */           generateBankTransfer();
/* 122 */         this.p_Amount = line.getCheckAmt();
/* 123 */         this.p_Description = ("Transferencia por Monto en Cheque: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
/* 124 */         if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0) {
/* 125 */           generateBankTransfer();
/*     */         }
/*     */         
/* 128 */         this.p_Amount = line.getCreditCardAmt();
/* 129 */         this.p_Description = ("Transferencia por Monto en TDC: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
/* 130 */         if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0) {
/* 131 */           generateBankCommissions(line, "C");
/* 132 */           generateBankTransfer();
/*     */         }
/*     */         
/*     */ 
/* 136 */         this.p_Amount = line.getDebitCardAmt();
/* 137 */         this.p_Description = ("Transferencia por Monto en TDD: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
/* 138 */         if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0) {
/* 139 */           generateBankCommissions(line, "D");
/* 140 */           generateBankTransfer();
/*     */         }
/*     */         
/* 143 */         this.p_Amount = line.getWireTransferAmt();
/* 144 */         this.p_Description = ("Transferencia por Monto en Transferencia: " + this.p_Amount + " " + this.CurSymbol + " para Cierre de Caja");
/* 145 */         if (this.p_Amount.compareTo(BigDecimal.ZERO) > 0) {
/* 146 */           generateBankTransfer();
/*     */         }
/*     */       }
/*     */     }
/*     */     catch (SQLException e) {
/* 151 */       this.log.log(Level.SEVERE, "Line - " + sql.toString(), e);
/* 152 */       throw new IllegalStateException("Error Procesando Pagos");
/*     */     }
/* 154 */     StringBuilder msgreturn = new StringBuilder("@Created@ = ").append(this.m_created);
/* 155 */     if (this.m_created != 0) {
/* 156 */       CloseCash.setDocStatus("CO");
/* 157 */       CloseCash.setProcessed(true);
/* 158 */       CloseCash.set_ValueOfColumn("DateProcessed", new Timestamp(System.currentTimeMillis()));
/* 159 */       CloseCash.saveEx();
/*     */       try {
/* 161 */         pstmt1 = DB.prepareStatement(sql, get_TrxName());
/* 162 */         ResultSet rs3 = pstmt1.executeQuery();
/* 163 */         while (rs3.next()) {
/* 164 */           M_POSCloseCashLine line = new M_POSCloseCashLine(CloseCash.getCtx(), rs3.getInt("POS_Close_Cash_Line_ID"), CloseCash.get_TrxName());
/* 165 */           line.set_ValueOfColumn("Processed", Boolean.valueOf(true));
/* 166 */           line.saveEx();
/*     */         }
/*     */       }
/*     */       catch (SQLException e) {
/* 170 */         this.log.log(Level.SEVERE, "Line - " + sql.toString(), e);
/* 171 */         throw new IllegalStateException("Error Procesando Lineas");
/*     */       }
/*     */     }
/*     */     else {
/* 175 */       throw new IllegalStateException("No hay lineas que Procesar ");
/*     */     }
/* 177 */     return msgreturn.toString();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private void generateBankTransfer()
/*     */   {
/* 185 */     int p_C_DocType_ID_From = MSysConfig.getIntValue("DoctypePaymentCloseCash", 0, getProcessInfo().getAD_Client_ID().intValue());
/* 186 */     int p_C_DocType_ID_To = MSysConfig.getIntValue("DoctypeReceiptCloseCash", 0, getProcessInfo().getAD_Client_ID().intValue());
/* 187 */     MBankAccount mBankFrom = new MBankAccount(getCtx(), this.p_From_C_BankAccount_ID, get_TrxName());
/* 188 */     MBankAccount mBankTo = new MBankAccount(getCtx(), this.p_To_C_BankAccount_ID, get_TrxName());
/* 189 */     MPayment paymentBankFrom = new MPayment(getCtx(), 0, get_TrxName());
/* 190 */     paymentBankFrom.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
/* 191 */     if (!Util.isEmpty(this.p_DocumentNo, true))
/* 192 */       paymentBankFrom.setDocumentNo(this.p_DocumentNo);
/* 193 */     paymentBankFrom.setDateAcct(this.p_DateAcct);
/* 194 */     paymentBankFrom.setDateTrx(this.p_StatementDate);
/* 195 */     paymentBankFrom.setTenderType("A");
/* 196 */     paymentBankFrom.setDescription(this.p_Description);
/* 197 */     paymentBankFrom.setC_BPartner_ID(this.p_C_BPartner_ID);
/* 198 */     paymentBankFrom.setC_Currency_ID(this.p_C_Currency_ID);
/* 199 */     if (this.p_C_ConversionType_ID > 0)
/* 200 */       paymentBankFrom.setC_ConversionType_ID(this.p_C_ConversionType_ID);
/* 201 */     paymentBankFrom.setPayAmt(this.p_Amount);
/* 202 */     paymentBankFrom.setOverUnderAmt(Env.ZERO);
/* 203 */     paymentBankFrom.setC_DocType_ID(p_C_DocType_ID_From);
/* 204 */     paymentBankFrom.setC_Charge_ID(this.p_C_Charge_ID);
/* 205 */     paymentBankFrom.setAD_Org_ID(mBankFrom.getAD_Org_ID());
/* 206 */     paymentBankFrom.set_CustomColumn("POS_Close_Cash_ID", Integer.valueOf(this.p_Record_ID));
/* 207 */     paymentBankFrom.saveEx();
/* 208 */     if (!paymentBankFrom.processIt("CO")) {
/* 209 */       this.log.warning("Payment Process Failed: " + paymentBankFrom + " - " + paymentBankFrom.getProcessMsg());
/* 210 */       throw new IllegalStateException("Payment Process Failed: " + paymentBankFrom + " - " + paymentBankFrom.getProcessMsg());
/*     */     }
/* 212 */     paymentBankFrom.saveEx();
/* 213 */     addBufferLog(paymentBankFrom.getC_Payment_ID(), paymentBankFrom.getDateTrx(), 
/* 214 */       null, paymentBankFrom.getC_DocType().getPrintName() + " " + paymentBankFrom.getDocumentNo(), 
/* 215 */       335, paymentBankFrom.getC_Payment_ID());
/* 216 */     this.m_created += 1;
/*     */     
/* 218 */     MPayment paymentBankTo = new MPayment(getCtx(), 0, get_TrxName());
/* 219 */     paymentBankTo.setC_BankAccount_ID(mBankTo.getC_BankAccount_ID());
/* 220 */     if (!Util.isEmpty(this.p_DocumentNo, true))
/* 221 */       paymentBankTo.setDocumentNo(this.p_DocumentNo);
/* 222 */     paymentBankTo.setDateAcct(this.p_DateAcct);
/* 223 */     paymentBankTo.setDateTrx(this.p_StatementDate);
/* 224 */     paymentBankTo.setTenderType("A");
/* 225 */     paymentBankTo.setDescription(this.p_Description);
/* 226 */     paymentBankTo.setC_BPartner_ID(this.p_C_BPartner_ID);
/* 227 */     paymentBankTo.setC_Currency_ID(this.p_C_Currency_ID);
/* 228 */     if (this.p_C_ConversionType_ID > 0)
/* 229 */       paymentBankFrom.setC_ConversionType_ID(this.p_C_ConversionType_ID);
/* 230 */     paymentBankTo.setPayAmt(this.p_Amount);
/* 231 */     paymentBankTo.setOverUnderAmt(Env.ZERO);
/* 232 */     paymentBankTo.setC_DocType_ID(p_C_DocType_ID_To);
/* 233 */     System.out.println("EL TIPO DE DOCUMENTO A GENERAR ES: " + p_C_DocType_ID_To);
/* 234 */     paymentBankTo.setAD_Org_ID(mBankTo.getAD_Org_ID());
/* 235 */     paymentBankTo.set_CustomColumn("POS_Close_Cash_ID", Integer.valueOf(this.p_Record_ID));
/* 236 */     paymentBankTo.saveEx();
/* 237 */     if (!paymentBankTo.processIt("CO")) {
/* 238 */       this.log.warning("Payment Process Failed: " + paymentBankTo + " - " + paymentBankTo.getProcessMsg());
/* 239 */       throw new IllegalStateException("Payment Process Failed: " + paymentBankTo + " - " + paymentBankTo.getProcessMsg());
/*     */     }
/* 241 */     paymentBankTo.saveEx();
/* 242 */     addBufferLog(paymentBankTo.getC_Payment_ID(), paymentBankTo.getDateTrx(), 
/* 243 */       null, paymentBankTo.getC_DocType().getPrintName() + " " + paymentBankTo.getDocumentNo(), 
/* 244 */       335, paymentBankTo.getC_Payment_ID());
/* 245 */     this.m_created += 1;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private void generateBankCommissions(M_POSCloseCashLine CloseCashLine, String TrxType)
/*     */   {
/* 252 */     MBankAccount BankAccount = new MBankAccount(getCtx(), this.p_To_C_BankAccount_ID, get_TrxName());
/* 253 */     M_POSCloseCash CloseCash = new M_POSCloseCash(getCtx(), CloseCashLine.getPOS_Close_Cash_ID(), get_TrxName());
/* 254 */     BigDecimal amt = this.p_Amount;
/* 255 */     BigDecimal totalComm = Env.ZERO;
/* 256 */     BigDecimal totalISLR = Env.ZERO;
/*     */     
/* 258 */     BigDecimal DebitRate = (BigDecimal)BankAccount.get_Value("DebitRate");
/* 259 */     BigDecimal CreditRate = (BigDecimal)BankAccount.get_Value("CreditRate");
/* 260 */     BigDecimal ISLRRate = (BigDecimal)BankAccount.get_Value("ISLRRate");
/*     */     
/* 262 */     MBankAccount_Commission baCommision = 
/* 263 */       (MBankAccount_Commission)new Query(getCtx(), "C_BankAccount_Commission", "C_BankAccount_ID=? and AD_Org_ID=?", get_TrxName()).setParameters(new Object[] {Integer.valueOf(BankAccount.getC_BankAccount_ID()), Integer.valueOf(CloseCash.getAD_Org_ID()) }).first();
/* 264 */     if (baCommision != null) {
/* 265 */       DebitRate = baCommision.getdebitrate();
/* 266 */       CreditRate = baCommision.getcreditrate();
/* 267 */       ISLRRate = baCommision.getislrrate();
/*     */     }
/*     */     
/* 270 */     int Commissions_C_Charge_ID = 0;
/* 271 */     int ISLR_C_Charge_ID = 0;
/* 272 */     if ((DebitRate == null) || (CreditRate == null) || (ISLRRate == null)) {
/* 273 */       throw new IllegalStateException("Falta Asignar tasas para cuenta: " + BankAccount.getName());
/*     */     }
/* 275 */     if ((TrxType == "D") && (DebitRate.compareTo(Env.ZERO) > 0)) {
/* 276 */       totalComm = amt.multiply(DebitRate).divide(new BigDecimal(100));
/* 277 */     } else if ((TrxType == "C") && (CreditRate.compareTo(Env.ZERO) > 0)) {
/* 278 */       totalComm = amt.multiply(CreditRate).divide(new BigDecimal(100));
/* 279 */       totalISLR = amt.multiply(ISLRRate).divide(new BigDecimal(100));
/*     */     } else {
/* 281 */       return;
/*     */     }
/* 283 */     PreparedStatement pst = null;
/* 284 */     ResultSet rs = null;
/* 285 */     String sql = "SELECT Name,Value FROM AD_SysConfig WHERE Name IN ('ISLR_C_Charge_ID','Commissions_C_Charge_ID')";
/*     */     try
/*     */     {
/* 288 */       pst = DB.prepareStatement(sql, null);
/* 289 */       rs = pst.executeQuery();
/* 290 */       while (rs.next()) {
/* 291 */         if (rs.getString("Name").compareTo("ISLR_C_Charge_ID") == 0) {
/* 292 */           ISLR_C_Charge_ID = Integer.parseInt(rs.getString("Value"));
/*     */         }
/*     */         
/* 295 */         if (rs.getString("Name").compareTo("Commissions_C_Charge_ID") == 0) {
/* 296 */           Commissions_C_Charge_ID = Integer.parseInt(rs.getString("Value"));
/*     */         }
/*     */       }
/*     */     }
/*     */     catch (SQLException e) {
/* 301 */       this.log.log(Level.SEVERE, "Line - " + sql.toString(), e);
/*     */     }
/*     */     
/*     */ 
/* 305 */     MPayment paymentBank = new MPayment(getCtx(), 0, get_TrxName());
/*     */     
/* 307 */     MBankAccount mBankFrom = new MBankAccount(getCtx(), this.p_From_C_BankAccount_ID, get_TrxName());
/* 308 */     paymentBank.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
/* 309 */     if (!Util.isEmpty(this.p_DocumentNo, true))
/* 310 */       paymentBank.setDocumentNo(this.p_DocumentNo);
/* 311 */     paymentBank.setDateAcct(this.p_DateAcct);
/* 312 */     paymentBank.setDateTrx(this.p_StatementDate);
/* 313 */     paymentBank.setTenderType("A");
/* 314 */     paymentBank.setDescription("Comision Banco " + BankAccount.getName() + " para Cierre de Caja Nro: " + CloseCash.getPOS_Close_Cash_ID());
/* 315 */     paymentBank.setC_BPartner_ID(this.p_C_BPartner_ID);
/* 316 */     paymentBank.setC_Currency_ID(this.p_C_Currency_ID);
/* 317 */     if (this.p_C_ConversionType_ID > 0)
/* 318 */       paymentBank.setC_ConversionType_ID(this.p_C_ConversionType_ID);
/* 319 */     paymentBank.setPayAmt(totalComm);
/* 320 */     paymentBank.setOverUnderAmt(Env.ZERO);
/* 321 */     paymentBank.setC_DocType_ID(false);
/* 322 */     paymentBank.setC_Charge_ID(Commissions_C_Charge_ID);
/* 323 */     paymentBank.setAD_Org_ID(mBankFrom.getAD_Org_ID());
/* 324 */     paymentBank.set_CustomColumn("POS_Close_Cash_ID", Integer.valueOf(this.p_Record_ID));
/* 325 */     paymentBank.saveEx();
/* 326 */     if (!paymentBank.processIt("CO")) {
/* 327 */       this.log.warning("Payment Process Failed: " + paymentBank + " - " + paymentBank.getProcessMsg());
/* 328 */       throw new IllegalStateException("Payment Process Failed: " + paymentBank + " - " + paymentBank.getProcessMsg());
/*     */     }
/* 330 */     paymentBank.saveEx();
/* 331 */     addBufferLog(paymentBank.getC_Payment_ID(), paymentBank.getDateTrx(), 
/* 332 */       null, paymentBank.getC_DocType().getPrintName() + " " + paymentBank.getDocumentNo() + " Comisión Banco", 
/* 333 */       335, paymentBank.getC_Payment_ID());
/* 334 */     this.m_created += 1;
/*     */     
/*     */ 
/* 337 */     if (totalISLR.compareTo(Env.ZERO) > 0) {
/* 338 */       MPayment paymentBankISLR = new MPayment(getCtx(), 0, get_TrxName());
/*     */       
/* 340 */       paymentBankISLR.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
/* 341 */       if (!Util.isEmpty(this.p_DocumentNo, true))
/* 342 */         paymentBankISLR.setDocumentNo(this.p_DocumentNo);
/* 343 */       paymentBankISLR.setDateAcct(this.p_DateAcct);
/* 344 */       paymentBankISLR.setDateTrx(this.p_StatementDate);
/* 345 */       paymentBankISLR.setTenderType("A");
/* 346 */       paymentBankISLR.setDescription("Comision ISLR " + BankAccount.getName() + " para Cierre de Caja Nro: " + CloseCash.getPOS_Close_Cash_ID());
/* 347 */       paymentBankISLR.setC_BPartner_ID(this.p_C_BPartner_ID);
/* 348 */       paymentBankISLR.setC_Currency_ID(this.p_C_Currency_ID);
/* 349 */       if (this.p_C_ConversionType_ID > 0)
/* 350 */         paymentBankISLR.setC_ConversionType_ID(this.p_C_ConversionType_ID);
/* 351 */       paymentBankISLR.setPayAmt(totalISLR);
/* 352 */       paymentBankISLR.setOverUnderAmt(Env.ZERO);
/* 353 */       paymentBankISLR.setC_DocType_ID(false);
/* 354 */       paymentBankISLR.setC_Charge_ID(ISLR_C_Charge_ID);
/*     */       
/* 356 */       paymentBankISLR.setAD_Org_ID(mBankFrom.getAD_Org_ID());
/* 357 */       paymentBankISLR.set_CustomColumn("POS_Close_Cash_ID", Integer.valueOf(this.p_Record_ID));
/* 358 */       paymentBankISLR.saveEx();
/* 359 */       if (!paymentBankISLR.processIt("CO")) {
/* 360 */         this.log.warning("Payment Process Failed: " + paymentBankISLR + " - " + paymentBankISLR.getProcessMsg());
/* 361 */         throw new IllegalStateException("Payment Process Failed: " + paymentBankISLR + " - " + paymentBankISLR.getProcessMsg());
/*     */       }
/* 363 */       paymentBankISLR.saveEx();
/* 364 */       addBufferLog(paymentBankISLR.getC_Payment_ID(), paymentBankISLR.getDateTrx(), 
/* 365 */         null, paymentBankISLR.getC_DocType().getPrintName() + " " + paymentBankISLR.getDocumentNo() + " Comisión ISLR", 
/* 366 */         335, paymentBankISLR.getC_Payment_ID());
/* 367 */       this.m_created += 1;
/*     */     }
/*     */     
/* 370 */     this.p_Amount = this.p_Amount.subtract(totalISLR.add(totalComm));
/*     */   }
/*     */ }


/* Location:              /home/james/Desktop/POSIntegration/!/POSProcessCloseCash.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */