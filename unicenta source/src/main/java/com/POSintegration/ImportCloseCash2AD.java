package com.ghintech.puntocom.process;  //mmhmm

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.adempiere.exceptions.AdempiereException;
import org.apache.activemq.transport.stomp.StompConnection;
import org.apache.activemq.transport.stomp.StompFrame;
import org.compiere.model.I_C_DocType;
import org.compiere.model.MBankAccount;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPriceList;
import org.compiere.model.MTax;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.model.X_I_Invoice;
import org.compiere.model.X_I_Payment;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoLog;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ImportCloseCash2AD
  extends SvrProcess
{
  private static int cnt = 0;
  private static String p_Test = "N";
  private static String p_Link = "/queue/ClosedCash";
  private static String p_LinkURL = "localhost";
  private static int Difference = 0;
  private static int AD_Org_ID = 0;
  private static int invoice_created = 0;
  private static int payment_created = 0;
  
  private boolean createInvoice(NodeList details)
  {
    X_I_Invoice invoice = new X_I_Invoice(Env.getCtx(), 0, null);
    invoice.set_ValueOfColumn("AD_PInstance_ID", Integer.valueOf(getProcessInfo().getAD_PInstance_ID()));
    System.out.println("factura: " + getProcessInfo().getAD_PInstance_ID());
    invoice.setDateInvoiced(Env.getContextAsDate(getCtx(), "#Date"));
    invoice.setDateAcct(Env.getContextAsDate(getCtx(), "#Date"));
    MTax tax = 
      (MTax)new Query(Env.getCtx(), "C_Tax", "AD_Client_ID=" + Env.getAD_Client_ID(Env.getCtx()) + " AND " + "IsTaxExempt" + "='Y' AND " + "Rate" + "=0", null).first();
    if (tax == null)
    {
      getProcessInfo().setError(true);
      getProcessInfo().addLog(new ProcessInfoLog(getProcessInfo().getAD_Process_ID(), new Timestamp(System.currentTimeMillis()), null, "Falta Crear Tasa de Impuesto Exenta en el sistema"));
      this.log.log(Level.SEVERE, "Falta Crear Tasa de Impuesto Exenta en el sistema");
      return false;
    }
    invoice.setC_Tax_ID(tax.getC_Tax_ID());
    
    invoice.setC_DocType_ID(1000051);
    MPriceList pricelist = 
      (MPriceList)new Query(Env.getCtx(), "M_PriceList", "AD_Client_ID=" + Env.getAD_Client_ID(Env.getCtx()) + " AND " + "IsDefault" + "='Y'", null).first();
    if (pricelist == null)
    {
      getProcessInfo().setError(true);
      getProcessInfo().addLog(new ProcessInfoLog(getProcessInfo().getAD_Process_ID(), new Timestamp(System.currentTimeMillis()), null, "No existe lista de precios por defecto"));
      this.log.log(Level.SEVERE, "No Existe lista de precios por defecto");
      return false;
    }
    invoice.setM_PriceList_ID(pricelist.get_ID());
    invoice.setQtyOrdered(new BigDecimal(1));
    for (int j = 0; j < details.getLength(); j++)
    {
      Node n = details.item(j);
      String column = n.getNodeName();
      if (!column.equals("DateStart")) {
        if (column.equals("AD_Client_ID"))
        {
          invoice.set_ValueOfColumn("AD_Client_ID", Integer.valueOf(Integer.parseInt(n.getTextContent())));
        }
        else if (column.equals("POSLocatorName"))
        {
          invoice.setAD_Org_ID(AD_Org_ID);
        }
        else if (column.equals("UserName"))
        {
          MUser User = 
          
            (MUser)new Query(Env.getCtx(), "AD_User", "AD_User_ID=?", null).setParameters(new Object[] {Integer.valueOf(Integer.parseInt(n.getTextContent())) }).first();
          invoice.setAD_User_ID(User.getAD_User_ID());
          invoice.setC_BPartner_ID(User.getC_BPartner_ID());
          invoice.setC_BPartner_Location_ID(User.getC_BPartner_Location_ID());
          invoice.set_ValueOfColumn("Description", "Cuenta por Cobrar por descuadre de caja a Vendedor: " + User.getName());
        }
        else if (column.equals("Difference"))
        {
          invoice.setPriceActual(BigDecimal.valueOf(Double.parseDouble(n.getTextContent())).abs());
        }
      }
    }
    invoice.save();
    invoice_created += 1;
    
    addBufferLog(invoice.getI_Invoice_ID(), invoice.getDateInvoiced(), 
      null, invoice.getC_DocType().getPrintName() + " " + invoice.getDocumentNo(), 
      598, invoice.getI_Invoice_ID());
    return true;
  }
  
  private boolean createPayment(NodeList details, int OrgBPartner)
  {
    X_I_Payment Payment = new X_I_Payment(Env.getCtx(), 0, null);
    Payment.set_ValueOfColumn("AD_PInstance_ID", Integer.valueOf(getProcessInfo().getAD_PInstance_ID()));
    System.out.println("pago: " + getProcessInfo().getAD_PInstance_ID());
    MBankAccount BankAccount = 
    
      (MBankAccount)new Query(Env.getCtx(), "C_BankAccount", "BankAccountType= 'B' AND IsDefault='Y' AND AD_Org_ID=" + AD_Org_ID, null).first();
    if (BankAccount == null)
    {
      this.log.log(Level.SEVERE, "No Existe Caja Predeterminada para la Organizaci��n: " + AD_Org_ID);
      return false;
    }
    Payment.setC_BankAccount_ID(BankAccount.get_ID());
    
    Payment.setC_DocType_ID(1000052);
    
    Payment.setIsReceipt(true);
    Payment.setTenderType("A");
    Payment.setTrxType("S");
    for (int j = 0; j < details.getLength(); j++)
    {
      Node n = details.item(j);
      String column = n.getNodeName();
      if (column.equals("datestart"))
      {
        Payment.setDateTrx(Timestamp.valueOf(n.getTextContent()));
        Payment.setDateAcct(Timestamp.valueOf(n.getTextContent()));
      }
      else if (column.equals("AD_Client_ID"))
      {
        Payment.set_ValueOfColumn("AD_Client_ID", Integer.valueOf(Integer.parseInt(n.getTextContent())));
      }
      else if (column.equals("POSLocatorName"))
      {
        Payment.setAD_Org_ID(AD_Org_ID);
      }
      else if (column.equals("UserName"))
      {
        MUser User = 
        
          (MUser)new Query(Env.getCtx(), "AD_User", "AD_User_ID=?", null).setParameters(new Object[] {Integer.valueOf(Integer.parseInt(n.getTextContent())) }).first();
        Payment.set_ValueOfColumn("Description", "Ingreso Creado por diferencia en Vendedor: " + User.getName());
        
        Payment.setC_BPartner_ID(OrgBPartner);
      }
      else if (column.equals("Difference"))
      {
        Payment.setPayAmt(BigDecimal.valueOf(Double.parseDouble(n.getTextContent())).abs());
      }
    }
    Payment.saveEx();
    payment_created += 1;
    
    addBufferLog(Payment.getI_Payment_ID(), Payment.getDateTrx(), 
      null, Payment.getC_DocType().getPrintName() + " " + Payment.getDocumentNo(), 
      597, Payment.getI_Payment_ID());
    return true;
  }
  
  private boolean parseXMLString(String message)
    throws SAXException, ParserConfigurationException, IOException
  {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new ByteArrayInputStream(message.getBytes()));
    Element docEle = doc.getDocumentElement();
    NodeList records = docEle.getElementsByTagName("detail");
    int difference = 0;
    cnt = 0;
    try
    {
      for (int i = 0; i < records.getLength(); i++) {
        if (!records.item(i).getFirstChild().getTextContent().equals("Closed Cash"))
        {
          cnt += 1;
        }
        else
        {
          String TypeCloseCash = null;
          NodeList details = records.item(i).getChildNodes();
          
          int OrgBPartner = 0;
          for (int ii = 0; ii < details.getLength(); ii++)
          {
            Node node = details.item(ii);
            String nodename = node.getNodeName();
            if (nodename.equals("Difference"))
            {
              difference = BigDecimal.valueOf(Double.parseDouble(node.getTextContent())).compareTo(Env.ZERO);
              if (difference < 0)
              {
                TypeCloseCash = "Invoice";
              }
              else if (difference > 0)
              {
                TypeCloseCash = "Payment";
              }
              else
              {
                TypeCloseCash = "No Document";
                break;
              }
            }
            else if (nodename.equals("POSLocatorName"))
            {
              MWarehouse Warehouse = 
              
                (MWarehouse)new Query(Env.getCtx(), "M_Warehouse", "Name=?", null).setParameters(new Object[] {node.getTextContent() }).first();
              if (Warehouse == null)
              {
                addLog("Almacen Desconocido: " + node.getTextContent());
                this.log.log(Level.SEVERE, "Almacen Desconocido: " + node.getTextContent());
                throw new AdempiereException("Almacen Desconocido: " + node.getTextContent());
              }
              MOrgInfo orginfo = 
                (MOrgInfo)new Query(getCtx(), "AD_OrgInfo", "AD_Org_ID= ?", get_TrxName()).setParameters(new Object[] {Integer.valueOf(Warehouse.getAD_Org_ID()) }).first();
              AD_Org_ID = orginfo.getAD_Org_ID();
              OrgBPartner = orginfo.get_ValueAsInt("C_BPartner_ID");
            }
          }
          System.out.println("Diferencia: " + difference + " Tipo de Documento a Crear " + TypeCloseCash);
          cnt += 1;
          if (TypeCloseCash.equals("Invoice"))
          {
            if (!createInvoice(details)) {
              return false;
            }
          }
          else if ((TypeCloseCash.equals("Payment")) && 
            (!createPayment(details, OrgBPartner))) {
            return false;
          }
        }
      }
    }
    catch (Exception localException)
    {
      System.out.println("ALGO MALO PAS��");
    }
    Difference = difference;
    System.out.println(records.getLength());
    System.out.println(cnt);
    return records.getLength() == cnt;
  }
  
  protected void prepare()
  {
    ProcessInfoParameter[] para = getParameter();
    cnt = 0;
    for (int i = 0; i < para.length; i++)
    {
      String name = para[i].getParameterName();
      if (name.equals("Test")) {
        p_Test = (String)para[i].getParameter();
      } else if (name.equals("Link")) {
        p_Link = (String)para[i].getParameter();
      } else if (name.equals("LinkURL")) {
        p_LinkURL = (String)para[i].getParameter();
      } else {
        this.log.log(Level.SEVERE, "Unknown Parameter: " + name);
      }
    }
  }
  
  protected String doIt()
    throws Exception
  {
    StompConnection connection = new StompConnection();
    connection.open(p_LinkURL, 61613);
    connection.connect("", "");
    connection.subscribe(p_Link, "client");
    connection.begin("MQOrders");
    int AD_PInstance_ID = getProcessInfo().getAD_PInstance_ID();
    int msjscnt = 0;
    System.out.println(AD_PInstance_ID);
    int no = 0;
    for (;;)
    {
      StompFrame message = null;
      try
      {
        message = connection.receive();
      }
      catch (Exception localException)
      {
        System.out.print("Fin de Lectura de mensaje");
        break;
      }
      System.out.println(message.getBody());
      msjscnt++;
      String data = message.getBody();
      if (parseXMLString(data))
      {
        System.out.println("SUCCESS: Records equal to I_Invoice");
      }
      else
      {
        connection.commit("MQOrders");
        connection.disconnect();
        String sql = "DELETE FROM I_Payment WHERE AD_PInstance_ID = " + AD_PInstance_ID;
        no = DB.executeUpdate(sql, get_TrxName());
        if (this.log.isLoggable(Level.INFO)) {
          this.log.info("Reset=" + no);
        }
        sql = "DELETE FROM I_Invoice WHERE AD_PInstance_ID = " + AD_PInstance_ID;
        no = DB.executeUpdate(sql, get_TrxName());
        if (this.log.isLoggable(Level.INFO)) {
          this.log.info("Reset=" + no);
        }
        commitEx();
        System.out.println("ERROR: Records not equal to I_Invoice");
        throw new AdempiereException(getProcessInfo().getLogInfo());
      }
      if (p_Test.equals("N")) {
        connection.ack(message, "MQOrders");
      }
    }
    connection.commit("MQOrders");
    connection.disconnect();
    
    return "<html>Cantidad de Registros Importados: " + Integer.toString(invoice_created + payment_created) + "<br />" + 
      "Cantidad de Diferencias Negativas = " + Integer.toString(invoice_created) + "<br />" + 
      "Cantidad de Diferencias Positivas = " + Integer.toString(payment_created) + "<br />" + 
      "Mensajes Leidos = " + msjscnt + "</html>";
  }
}

