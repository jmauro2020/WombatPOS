/*     */ package com.ghintech.puntocom.process;
/*     */ 
/*     */ import java.io.ByteArrayInputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.PrintStream;
/*     */ import java.math.BigDecimal;
/*     */ import java.sql.Timestamp;
/*     */ import java.util.logging.Level;
/*     */ import javax.xml.parsers.DocumentBuilder;
/*     */ import javax.xml.parsers.DocumentBuilderFactory;
/*     */ import javax.xml.parsers.ParserConfigurationException;
/*     */ import org.adempiere.exceptions.AdempiereException;
/*     */ import org.apache.activemq.transport.stomp.StompConnection;
/*     */ import org.apache.activemq.transport.stomp.StompFrame;
/*     */ import org.compiere.model.I_C_DocType;
/*     */ import org.compiere.model.MBankAccount;
/*     */ import org.compiere.model.MOrgInfo;
/*     */ import org.compiere.model.MPriceList;
/*     */ import org.compiere.model.MTax;
/*     */ import org.compiere.model.MUser;
/*     */ import org.compiere.model.MWarehouse;
/*     */ import org.compiere.model.Query;
/*     */ import org.compiere.model.X_I_Invoice;
/*     */ import org.compiere.model.X_I_Payment;
/*     */ import org.compiere.process.ProcessInfo;
/*     */ import org.compiere.process.ProcessInfoLog;
/*     */ import org.compiere.process.ProcessInfoParameter;
/*     */ import org.compiere.process.SvrProcess;
/*     */ import org.compiere.util.CLogger;
/*     */ import org.compiere.util.DB;
/*     */ import org.compiere.util.Env;
/*     */ import org.w3c.dom.Document;
/*     */ import org.w3c.dom.Element;
/*     */ import org.w3c.dom.Node;
/*     */ import org.w3c.dom.NodeList;
/*     */ import org.xml.sax.SAXException;
/*     */ 
/*     */ public class ImportCloseCash2AD extends SvrProcess
/*     */ {
/*  40 */   private static int cnt = 0;
/*  41 */   private static String p_Test = "N";
/*  42 */   private static String p_Link = "/queue/ClosedCash";
/*  43 */   private static String p_LinkURL = "localhost";
/*  44 */   private static int Difference = 0;
/*  45 */   private static int AD_Org_ID = 0;
/*  46 */   private static int invoice_created = 0;
/*  47 */   private static int payment_created = 0;
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
/*     */   private boolean createInvoice(NodeList details)
/*     */   {
/*  72 */     X_I_Invoice invoice = new X_I_Invoice(Env.getCtx(), 0, null);
/*  73 */     invoice.set_ValueOfColumn("AD_PInstance_ID", Integer.valueOf(getProcessInfo().getAD_PInstance_ID()));
/*  74 */     System.out.println("factura: " + getProcessInfo().getAD_PInstance_ID());
/*  75 */     invoice.setDateInvoiced(Env.getContextAsDate(getCtx(), "#Date"));
/*  76 */     invoice.setDateAcct(Env.getContextAsDate(getCtx(), "#Date"));
/*  77 */     MTax tax = 
/*  78 */       (MTax)new Query(Env.getCtx(), "C_Tax", "AD_Client_ID=" + Env.getAD_Client_ID(Env.getCtx()) + " AND " + "IsTaxExempt" + "='Y' AND " + "Rate" + "=0", null).first();
/*  79 */     if (tax == null) {
/*  80 */       getProcessInfo().setError(true);
/*  81 */       getProcessInfo().addLog(new ProcessInfoLog(getProcessInfo().getAD_Process_ID(), new Timestamp(System.currentTimeMillis()), null, "Falta Crear Tasa de Impuesto Exenta en el sistema"));
/*  82 */       this.log.log(Level.SEVERE, "Falta Crear Tasa de Impuesto Exenta en el sistema");
/*  83 */       return false;
/*     */     }
/*  85 */     invoice.setC_Tax_ID(tax.getC_Tax_ID());
/*     */     
/*     */ 
/*  88 */     invoice.setC_DocType_ID(1000051);
/*  89 */     MPriceList pricelist = 
/*  90 */       (MPriceList)new Query(Env.getCtx(), "M_PriceList", "AD_Client_ID=" + Env.getAD_Client_ID(Env.getCtx()) + " AND " + "IsDefault" + "='Y'", null).first();
/*  91 */     if (pricelist == null) {
/*  92 */       getProcessInfo().setError(true);
/*  93 */       getProcessInfo().addLog(new ProcessInfoLog(getProcessInfo().getAD_Process_ID(), new Timestamp(System.currentTimeMillis()), null, "No existe lista de precios por defecto"));
/*  94 */       this.log.log(Level.SEVERE, "No Existe lista de precios por defecto");
/*  95 */       return false;
/*     */     }
/*  97 */     invoice.setM_PriceList_ID(pricelist.get_ID());
/*  98 */     invoice.setQtyOrdered(new BigDecimal(1));
/*  99 */     for (int j = 0; j < details.getLength(); j++) {
/* 100 */       Node n = details.item(j);
/* 101 */       String column = n.getNodeName();
/*     */       
/* 103 */       if (!column.equals("DateStart"))
/*     */       {
/*     */ 
/*     */ 
/*     */ 
/* 108 */         if (column.equals("AD_Client_ID")) {
/* 109 */           invoice.set_ValueOfColumn("AD_Client_ID", Integer.valueOf(Integer.parseInt(n.getTextContent())));
/* 110 */         } else if (column.equals("POSLocatorName")) {
/* 111 */           invoice.setAD_Org_ID(AD_Org_ID);
/*     */         }
/* 113 */         else if (column.equals("UserName")) {
/* 114 */           MUser User = 
/*     */           
/* 116 */             (MUser)new Query(Env.getCtx(), "AD_User", "AD_User_ID=?", null).setParameters(new Object[] {Integer.valueOf(Integer.parseInt(n.getTextContent())) }).first();
/* 117 */           invoice.setAD_User_ID(User.getAD_User_ID());
/* 118 */           invoice.setC_BPartner_ID(User.getC_BPartner_ID());
/* 119 */           invoice.setC_BPartner_Location_ID(User.getC_BPartner_Location_ID());
/* 120 */           invoice.set_ValueOfColumn("Description", "Cuenta por Cobrar por descuadre de caja a Vendedor: " + User.getName());
/*     */         }
/* 122 */         else if (column.equals("Difference")) {
/* 123 */           invoice.setPriceActual(BigDecimal.valueOf(Double.parseDouble(n.getTextContent())).abs());
/*     */         }
/*     */       }
/*     */     }
/* 127 */     invoice.save();
/* 128 */     invoice_created += 1;
/*     */     
/*     */ 
/* 131 */     addBufferLog(invoice.getI_Invoice_ID(), invoice.getDateInvoiced(), 
/* 132 */       null, invoice.getC_DocType().getPrintName() + " " + invoice.getDocumentNo(), 
/* 133 */       598, invoice.getI_Invoice_ID());
/* 134 */     return true;
/*     */   }
/*     */   
/*     */   private boolean createPayment(NodeList details, int OrgBPartner) {
/* 138 */     X_I_Payment Payment = new X_I_Payment(Env.getCtx(), 0, null);
/* 139 */     Payment.set_ValueOfColumn("AD_PInstance_ID", Integer.valueOf(getProcessInfo().getAD_PInstance_ID()));
/* 140 */     System.out.println("pago: " + getProcessInfo().getAD_PInstance_ID());
/* 141 */     MBankAccount BankAccount = 
/*     */     
/* 143 */       (MBankAccount)new Query(Env.getCtx(), "C_BankAccount", "BankAccountType= 'B' AND IsDefault='Y' AND AD_Org_ID=" + AD_Org_ID, null).first();
/* 144 */     if (BankAccount == null) {
/* 145 */       this.log.log(Level.SEVERE, "No Existe Caja Predeterminada para la Organización: " + AD_Org_ID);
/* 146 */       return false;
/*     */     }
/* 148 */     Payment.setC_BankAccount_ID(BankAccount.get_ID());
/*     */     
/* 150 */     Payment.setC_DocType_ID(1000052);
/*     */     
/* 152 */     Payment.setIsReceipt(true);
/* 153 */     Payment.setTenderType("A");
/* 154 */     Payment.setTrxType("S");
/*     */     
/* 156 */     for (int j = 0; j < details.getLength(); j++) {
/* 157 */       Node n = details.item(j);
/* 158 */       String column = n.getNodeName();
/* 159 */       if (column.equals("datestart")) {
/* 160 */         Payment.setDateTrx(Timestamp.valueOf(n.getTextContent()));
/* 161 */         Payment.setDateAcct(Timestamp.valueOf(n.getTextContent()));
/*     */       }
/* 163 */       else if (column.equals("AD_Client_ID")) {
/* 164 */         Payment.set_ValueOfColumn("AD_Client_ID", Integer.valueOf(Integer.parseInt(n.getTextContent())));
/* 165 */       } else if (column.equals("POSLocatorName")) {
/* 166 */         Payment.setAD_Org_ID(AD_Org_ID);
/*     */       }
/* 168 */       else if (column.equals("UserName")) {
/* 169 */         MUser User = 
/*     */         
/* 171 */           (MUser)new Query(Env.getCtx(), "AD_User", "AD_User_ID=?", null).setParameters(new Object[] {Integer.valueOf(Integer.parseInt(n.getTextContent())) }).first();
/* 172 */         Payment.set_ValueOfColumn("Description", "Ingreso Creado por diferencia en Vendedor: " + User.getName());
/*     */         
/* 174 */         Payment.setC_BPartner_ID(OrgBPartner);
/*     */       }
/* 176 */       else if (column.equals("Difference")) {
/* 177 */         Payment.setPayAmt(BigDecimal.valueOf(Double.parseDouble(n.getTextContent())).abs());
/*     */       }
/*     */     }
/*     */     
/* 181 */     Payment.saveEx();
/* 182 */     payment_created += 1;
/*     */     
/*     */ 
/* 185 */     addBufferLog(Payment.getI_Payment_ID(), Payment.getDateTrx(), 
/* 186 */       null, Payment.getC_DocType().getPrintName() + " " + Payment.getDocumentNo(), 
/* 187 */       597, Payment.getI_Payment_ID());
/* 188 */     return true;
/*     */   }
/*     */   
/*     */   private boolean parseXMLString(String message) throws SAXException, ParserConfigurationException, IOException {
/* 192 */     DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
/* 193 */     DocumentBuilder db = dbf.newDocumentBuilder();
/* 194 */     Document doc = db.parse(new ByteArrayInputStream(message.getBytes()));
/* 195 */     Element docEle = doc.getDocumentElement();
/* 196 */     NodeList records = docEle.getElementsByTagName("detail");
/* 197 */     int difference = 0;
/* 198 */     cnt = 0;
/*     */     try {
/* 200 */       for (int i = 0; i < records.getLength(); i++)
/*     */       {
/* 202 */         if (!records.item(i).getFirstChild().getTextContent().equals("Closed Cash")) {
/* 203 */           cnt += 1;
/*     */         }
/*     */         else {
/* 206 */           String TypeCloseCash = null;
/* 207 */           NodeList details = records.item(i).getChildNodes();
/*     */           
/* 209 */           int OrgBPartner = 0;
/* 210 */           for (int ii = 0; ii < details.getLength(); ii++) {
/* 211 */             Node node = details.item(ii);
/* 212 */             String nodename = node.getNodeName();
/* 213 */             if (nodename.equals("Difference")) {
/* 214 */               difference = BigDecimal.valueOf(Double.parseDouble(node.getTextContent())).compareTo(Env.ZERO);
/*     */               
/* 216 */               if (difference < 0) {
/* 217 */                 TypeCloseCash = "Invoice";
/* 218 */               } else if (difference > 0) {
/* 219 */                 TypeCloseCash = "Payment";
/*     */               } else {
/* 221 */                 TypeCloseCash = "No Document";
/* 222 */                 break;
/*     */               }
/*     */             }
/* 225 */             else if (nodename.equals("POSLocatorName")) {
/* 226 */               MWarehouse Warehouse = 
/*     */               
/* 228 */                 (MWarehouse)new Query(Env.getCtx(), "M_Warehouse", "Name=?", null).setParameters(new Object[] {node.getTextContent() }).first();
/* 229 */               if (Warehouse == null) {
/* 230 */                 addLog("Almacen Desconocido: " + node.getTextContent());
/* 231 */                 this.log.log(Level.SEVERE, "Almacen Desconocido: " + node.getTextContent());
/* 232 */                 throw new AdempiereException("Almacen Desconocido: " + node.getTextContent());
/*     */               }
/* 234 */               MOrgInfo orginfo = 
/* 235 */                 (MOrgInfo)new Query(getCtx(), "AD_OrgInfo", "AD_Org_ID= ?", get_TrxName()).setParameters(new Object[] {Integer.valueOf(Warehouse.getAD_Org_ID()) }).first();
/* 236 */               AD_Org_ID = orginfo.getAD_Org_ID();
/* 237 */               OrgBPartner = orginfo.get_ValueAsInt("C_BPartner_ID");
/*     */             }
/*     */           }
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
/* 259 */           System.out.println("Diferencia: " + difference + " Tipo de Documento a Crear " + TypeCloseCash);
/* 260 */           cnt += 1;
/* 261 */           if (TypeCloseCash.equals("Invoice")) {
/* 262 */             if (!createInvoice(details))
/* 263 */               return false;
/* 264 */           } else if ((TypeCloseCash.equals("Payment")) && 
/* 265 */             (!createPayment(details, OrgBPartner)))
/* 266 */             return false;
/*     */         }
/*     */       }
/*     */     } catch (Exception localException) {
/* 270 */       System.out.println("ALGO MALO PASÓ");
/*     */     }
/*     */     
/*     */ 
/* 274 */     Difference = difference;
/* 275 */     System.out.println(records.getLength());
/* 276 */     System.out.println(cnt);
/* 277 */     return records.getLength() == cnt;
/*     */   }
/*     */   
/*     */ 
/*     */   protected void prepare()
/*     */   {
/* 283 */     ProcessInfoParameter[] para = getParameter();
/* 284 */     cnt = 0;
/* 285 */     for (int i = 0; i < para.length; i++)
/*     */     {
/* 287 */       String name = para[i].getParameterName();
/*     */       
/* 289 */       if (name.equals("Test")) {
/* 290 */         p_Test = (String)para[i].getParameter();
/* 291 */       } else if (name.equals("Link")) {
/* 292 */         p_Link = (String)para[i].getParameter();
/* 293 */       } else if (name.equals("LinkURL")) {
/* 294 */         p_LinkURL = (String)para[i].getParameter();
/*     */       }
/*     */       else {
/* 297 */         this.log.log(Level.SEVERE, "Unknown Parameter: " + name);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   protected String doIt()
/*     */     throws Exception
/*     */   {
/* 305 */     StompConnection connection = new StompConnection();
/* 306 */     connection.open(p_LinkURL, 61613);
/* 307 */     connection.connect("", "");
/* 308 */     connection.subscribe(p_Link, "client");
/* 309 */     connection.begin("MQOrders");
/* 310 */     int AD_PInstance_ID = getProcessInfo().getAD_PInstance_ID();
/* 311 */     int msjscnt = 0;
/* 312 */     System.out.println(AD_PInstance_ID);
/* 313 */     int no = 0;
/*     */     for (;;)
/*     */     {
/* 316 */       StompFrame message = null;
/*     */       try {
/* 318 */         message = connection.receive();
/*     */       } catch (Exception localException) {
/* 320 */         System.out.print("Fin de Lectura de mensaje");
/* 321 */         break;
/*     */       }
/* 323 */       System.out.println(message.getBody());
/* 324 */       msjscnt++;
/* 325 */       String data = message.getBody();
/* 326 */       if (parseXMLString(data)) {
/* 327 */         System.out.println("SUCCESS: Records equal to I_Invoice");
/*     */       } else {
/* 329 */         connection.commit("MQOrders");
/* 330 */         connection.disconnect();
/* 331 */         String sql = "DELETE FROM I_Payment WHERE AD_PInstance_ID = " + AD_PInstance_ID;
/* 332 */         no = DB.executeUpdate(sql, get_TrxName());
/* 333 */         if (this.log.isLoggable(Level.INFO)) this.log.info("Reset=" + no);
/* 334 */         sql = "DELETE FROM I_Invoice WHERE AD_PInstance_ID = " + AD_PInstance_ID;
/* 335 */         no = DB.executeUpdate(sql, get_TrxName());
/* 336 */         if (this.log.isLoggable(Level.INFO)) this.log.info("Reset=" + no);
/* 337 */         commitEx();
/* 338 */         System.out.println("ERROR: Records not equal to I_Invoice");
/* 339 */         throw new AdempiereException(getProcessInfo().getLogInfo());
/*     */       }
/*     */       
/*     */ 
/* 343 */       if (p_Test.equals("N"))
/* 344 */         connection.ack(message, "MQOrders");
/*     */     }
/* 346 */     connection.commit("MQOrders");
/* 347 */     connection.disconnect();
/*     */     
/*     */ 
/*     */ 
/* 351 */     return "<html>Cantidad de Registros Importados: " + Integer.toString(invoice_created + payment_created) + "<br />" + 
/* 352 */       "Cantidad de Diferencias Negativas = " + Integer.toString(invoice_created) + "<br />" + 
/* 353 */       "Cantidad de Diferencias Positivas = " + Integer.toString(payment_created) + "<br />" + 
/* 354 */       "Mensajes Leidos = " + msjscnt + "</html>";
/*     */   }
/*     */ }


/* Location:              /home/james/Desktop/POSIntegration/!/ImportCloseCash2AD.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */