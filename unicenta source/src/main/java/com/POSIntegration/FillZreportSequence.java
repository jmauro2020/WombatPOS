/*     */ package com.ghintech.puntocom.process;
/*     */ 
/*     */ import java.sql.PreparedStatement;
/*     */ import java.sql.ResultSet;
/*     */ import java.sql.Timestamp;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.logging.Level;
/*     */ import org.compiere.process.ProcessInfoParameter;
/*     */ import org.compiere.process.SvrProcess;
/*     */ import org.compiere.util.CLogger;
/*     */ import org.compiere.util.DB;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class FillZreportSequence
/*     */   extends SvrProcess
/*     */ {
/*  21 */   private int p_AD_Org_ID = 0;
/*  22 */   private Timestamp p_from = null;
/*  23 */   private Timestamp p_to = null;
/*     */   
/*     */   protected void prepare() {
/*  26 */     ProcessInfoParameter[] para = getParameter();
/*  27 */     for (int i = 0; i < para.length; i++)
/*     */     {
/*  29 */       String name = para[i].getParameterName();
/*  30 */       if (name.equals("AD_Org_ID")) {
/*  31 */         this.p_AD_Org_ID = para[i].getParameterAsInt();
/*  32 */       } else if (name.equals("from")) {
/*  33 */         this.p_from = ((Timestamp)para[i].getParameter());
/*  34 */       } else if (name.equals("to")) {
/*  35 */         this.p_to = ((Timestamp)para[i].getParameter());
/*     */       } else {
/*  37 */         this.log.log(Level.SEVERE, "Unknown Parameter: " + name);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   protected String doIt()
/*     */     throws Exception
/*     */   {
/*  45 */     StringBuilder sql = new StringBuilder();
/*  46 */     String from = DB.TO_DATE(this.p_from);
/*  47 */     String to = DB.TO_DATE(this.p_to);
/*  48 */     sql.append("SELECT fiscalprint_serial FROM lve_salesbook_fiscal WHERE tipodocumento='F'")
/*  49 */       .append(" AND fiscal_zreport is not null AND fiscalprint_serial is not null")
/*  50 */       .append(" AND ").append(" datefilter between ").append(from).append(" AND ").append(to)
/*  51 */       .append(" AND ").append(" AD_Org_ID = ").append(this.p_AD_Org_ID)
/*  52 */       .append(" GROUP by fiscalprint_serial")
/*  53 */       .append(" ORDER by fiscalprint_serial");
/*     */     
/*  55 */     List<String> fiscalprint = new ArrayList();
/*  56 */     PreparedStatement pstmt = null;
/*  57 */     ResultSet rs = null;
/*  58 */     pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
/*  59 */     rs = pstmt.executeQuery();
/*  60 */     while (rs.next())
/*     */     {
/*  62 */       fiscalprint.add(rs.getString(1));
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*  68 */     for (int i = 0; i < fiscalprint.size(); i++)
/*     */     {
/*  70 */       new StringBuilder("SELECT max(cast(fiscal_zreport AS int)) AS minZ FROM lve_salesbook_fiscal WHERE tipodocumento='F'  AND fiscal_zreport is not null  AND fiscalprint_serial is not null  AND datefilter < ")
/*     */       
/*     */ 
/*  73 */         .append(from)
/*  74 */         .append(" AND AD_Org_ID = ").append(this.p_AD_Org_ID)
/*  75 */         .append(" GROUP BY fiscalprint_serial ORDER BY  fiscalprint_serial").toString();
/*  76 */       pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
/*  77 */       rs = pstmt.executeQuery();
/*  78 */       rs.first();
/*  79 */       rs.getInt("minZ");
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 154 */     return "";
/*     */   }
/*     */ }


/* Location:              /home/james/Desktop/POSIntegration/!/FillZreportSequence.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */