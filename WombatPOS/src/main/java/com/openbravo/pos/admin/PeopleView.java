//    uniCenta oPOS  - Touch Friendly Point Of Sale
//    Copyright (c) 2009-2015 uniCenta
//    http://www.unicenta.com
//
//    This file is part of uniCenta oPOS
//
//    uniCenta oPOS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//   uniCenta oPOS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with uniCenta oPOS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.admin;

import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.monitor.DeviceMonitor;
import com.dalsemi.onewire.application.monitor.DeviceMonitorEvent;
import com.dalsemi.onewire.application.monitor.DeviceMonitorEventListener;
import com.dalsemi.onewire.application.monitor.DeviceMonitorException;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.utils.Address;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.user.*;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.util.Hashcypher;
import com.openbravo.pos.util.StringUtils;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.UUID;
import javax.swing.*;

/**
 *
 * @author adrianromero
 */
public class PeopleView extends JPanel implements EditorRecord, DeviceMonitorEventListener {

    private Object m_oId;
    private String m_sPassword;
    private DataLogicSystem m_dlSystem;
    
    private final DirtyManager m_Dirty;
    
    private final SentenceList m_sentrole;
    private ComboBoxValModel m_RoleModel;  
    
    /** Creates new form PeopleEditor
     * @param dlAdmin
     * @param dirty */
    public PeopleView(DataLogicAdmin dlAdmin, DirtyManager dirty) {
        initComponents();
                
        // El modelo de roles
        m_sentrole = dlAdmin.getRolesList();
        m_RoleModel = new ComboBoxValModel();
        
        m_Dirty = dirty;
        m_jName.getDocument().addDocumentListener(dirty);
        m_jRole.addActionListener(dirty);
        m_jVisible.addActionListener(dirty);
        m_jImage.addPropertyChangeListener("image", dirty);

        
        writeValueEOF();
    }

    private DSPortAdapter m_oneWireAdapter;
    private DeviceMonitor m_oneWireMonitor;
    
    private void initIButtonMonitor() {
    // the 1-wire monitor should not be reinitialised.
        assert m_oneWireMonitor == null;

        try
        {
            m_oneWireAdapter = OneWireAccessProvider.getDefaultAdapter();
            System.out.println();
            System.out.println("1-Wire Adapter: " + m_oneWireAdapter.getAdapterName()
                            + " Port: " + m_oneWireAdapter.getPortName());
            System.out.println();

            // restrict the search to 'serial number' iButtons - NOTE: these
            // restrictions do not filter the DeviceMonitorEvents
            m_oneWireAdapter.setSearchAllDevices();
            m_oneWireAdapter.targetFamily(0x01);
            m_oneWireAdapter.setSpeed(DSPortAdapter.SPEED_REGULAR);

            m_oneWireMonitor = new DeviceMonitor(m_oneWireAdapter);

            // trigger a REMOVAL event the n-th time an iButton is found to be
            // missing.  Reducing this value can speed up detection but may
            // result in false 'removals' if the user fumbles with the iButton.
            m_oneWireMonitor.setMaxStateCount(100);

            m_oneWireMonitor.addDeviceMonitorEventListener(this);

            System.out.println("Starting 1-wire monitor: " + m_oneWireMonitor.toString());
            new Thread(m_oneWireMonitor).start();
        }
        catch (OneWireException e) {
            JMessageDialog.showMessage(this, new MessageInf(MessageInf.SGN_WARNING, "No iButton adapter found.", e));
        }
    }

    private void shutdownIButtonMonitor() {
        if(m_oneWireMonitor != null)
        {
            System.out.println("Killing 1-wire monitor: " + m_oneWireMonitor.toString());
            m_oneWireMonitor.killMonitor();

            try {
                m_oneWireAdapter.freePort();
            }
            catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public void releaseResources() {
        shutdownIButtonMonitor();
    }

    final static int UNIQUE_KEY_FAMILY = 0x01;

    private boolean isDeviceRelevant(OneWireContainer container)
    {
        String iButtonId = container.getAddressAsString();

        // an adapter can have its own internal address - don't confuse this
        // with an arriving key
        try {
            if(container.getAdapter().getAdapterAddress().equals(iButtonId))
                return false;
        }
        catch(OneWireException e) {}

        // only respond to the 'serial number' family of iButtons (the least
        // significant byte of the address defines the device family)
        int familyNumber = Address.toByteArray(iButtonId)[0];
        return (familyNumber == UNIQUE_KEY_FAMILY);
    }
   
    String iButtonGlobal = null;
    
    /** Called when an iButton is inserted.
     * @param devt */
    @Override
    public void deviceArrival(DeviceMonitorEvent devt)
    {
        assert m_dlSystem != null;
        
        for (int i = 0; i < devt.getDeviceCount(); i++) {
            OneWireContainer container = devt.getContainerAt(i);            
            if(!isDeviceRelevant(container))
                continue;
            
            String iButtonId = devt.getAddressAsStringAt(i);
            System.out.println("iButton inserted: "
                    + iButtonId + ", "
                    + container.getName() + ", "
                    + container.getAlternateNames() + ", "
                    + container.getDescription());
            iButtonGlobal = iButtonGlobal + iButtonId;
     
        }
    }

    /** Called when an iButton is removed.
     * @param devt */
    @Override
    public void deviceDeparture(DeviceMonitorEvent devt)
    {
        for(int i = 0; i < devt.getDeviceCount(); i++) {
            OneWireContainer container = devt.getContainerAt(i);
            if(!isDeviceRelevant(container))
                continue;

            String iButtonId = devt.getAddressAsStringAt(i);
            System.out.println("iButton removed: " + iButtonId);
            
//            if(m_principalapp != null) {
//                AppUser currentUser = m_principalapp.getUser();
//                if(currentUser != null && currentUser.getCard().equals(iButtonId))
//                    closeAppView();
//            }
        }
    }
    
    /**
     *
     * @param dexc
     */
    @Override
    public void networkException(DeviceMonitorException dexc)
    {
        System.out.println("ERROR: " + dexc.toString());
    }
    
    /**
     *
     */
    @Override
    public void writeValueEOF() {
        m_oId = null;
        m_jName.setText(null);
        m_sPassword = null;
        m_RoleModel.setSelectedKey(null);
        m_jVisible.setSelected(false);
        jibutton.setText(null);
        m_jImage.setImage(null);
        m_jName.setEnabled(false);
        m_jRole.setEnabled(false);
        m_jVisible.setEnabled(false);
        jibutton.setEnabled(false);
        m_jImage.setEnabled(false);
        jButton1.setEnabled(false);
        jButton2.setEnabled(false);
        jButton3.setEnabled(false);
    }
    
    /**
     *
     */
    @Override
    public void writeValueInsert() {
        m_oId = null;
        m_jName.setText(null);
        m_sPassword = null;
        m_RoleModel.setSelectedKey(null);
        m_jVisible.setSelected(true);
        jibutton.setText(null);
        m_jImage.setImage(null);
        m_jName.setEnabled(true);
        m_jRole.setEnabled(true);
        m_jVisible.setEnabled(true);
        jibutton.setEnabled(true);
        m_jImage.setEnabled(true);
        jButton1.setEnabled(true);
        jButton2.setEnabled(true);
        jButton3.setEnabled(true);
    }
    
    /**
     *
     * @param value
     */
    @Override
    public void writeValueDelete(Object value) {
        Object[] people = (Object[]) value;
        m_oId = people[0];
        m_jName.setText(Formats.STRING.formatValue(people[1]));
        m_sPassword = Formats.STRING.formatValue(people[2]);
        m_RoleModel.setSelectedKey(people[3]);
        m_jVisible.setSelected(((Boolean) people[4]).booleanValue());
        jibutton.setText(Formats.STRING.formatValue(people[5]));
        m_jImage.setImage((BufferedImage) people[6]);
        m_jName.setEnabled(false);
        m_jRole.setEnabled(false);
        m_jVisible.setEnabled(false);
        jibutton.setEnabled(false);
        m_jImage.setEnabled(false);        
        jButton1.setEnabled(false);
        jButton2.setEnabled(false);
        jButton3.setEnabled(false);
    }

    /**
     *
     * @param value
     */
    @Override
    public void writeValueEdit(Object value) {
        Object[] people = (Object[]) value;
        m_oId = people[0];
        m_jName.setText(Formats.STRING.formatValue(people[1]));
        m_sPassword = Formats.STRING.formatValue(people[2]);
        m_RoleModel.setSelectedKey(people[3]);
        m_jVisible.setSelected(((Boolean) people[4]).booleanValue());
        jibutton.setText(Formats.STRING.formatValue(people[5]));
        m_jImage.setImage((BufferedImage) people[6]);
        
        m_jName.setEnabled(true);
        m_jRole.setEnabled(true);
        m_jVisible.setEnabled(true);
        jibutton.setEnabled(true);
        m_jImage.setEnabled(true);
        jButton1.setEnabled(true);
        jButton2.setEnabled(true);
        jButton3.setEnabled(true);
    }
    
    /**
     *
     * @return
     * @throws BasicException
     */
    @Override
    public Object createValue() throws BasicException {
        Object[] people = new Object[7];
        people[0] = m_oId == null ? UUID.randomUUID().toString() : m_oId;
        people[1] = Formats.STRING.parseValue(m_jName.getText());
        people[2] = Formats.STRING.parseValue(m_sPassword);
        people[3] = m_RoleModel.getSelectedKey();
        people[4] = Boolean.valueOf(m_jVisible.isSelected());
        people[5] = Formats.STRING.parseValue(jibutton.getText());
        people[6] = m_jImage.getImage();
        return people;
    }

    /**
     *
     * @return
     */
    @Override
    public Component getComponent() {
        return this;
    }

    /**
     *
     * @throws BasicException
     */
    public void activate() throws BasicException {
        
        m_RoleModel = new ComboBoxValModel(m_sentrole.list());
        m_jRole.setModel(m_RoleModel);
    }
    
    /**
     *
     */
    @Override
    public void refresh() {
    }
     
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton3 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        m_jName = new javax.swing.JTextField();
        m_jVisible = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        m_jImage = new com.openbravo.data.gui.JImageEditor();
        jButton1 = new javax.swing.JButton();
        m_jRole = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jibutton = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();

        setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        setPreferredSize(new java.awt.Dimension(531, 400));

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/fileclose.png"))); // NOI18N
        jButton3.setToolTipText("Clear iButton");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel1.setText(AppLocal.getIntString("label.peoplename")); // NOI18N

        m_jName.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        m_jVisible.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel3.setText(AppLocal.getIntString("label.peoplevisible")); // NOI18N

        jLabel4.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel4.setText(AppLocal.getIntString("label.peopleimage")); // NOI18N

        m_jImage.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jButton1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButton1.setText(AppLocal.getIntString("button.peoplepassword")); // NOI18N
        jButton1.setPreferredSize(new java.awt.Dimension(79, 32));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        m_jRole.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N

        jLabel2.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel2.setText(AppLocal.getIntString("label.role")); // NOI18N

        jibutton.setEditable(false);
        jibutton.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/encrypted.png"))); // NOI18N
        jButton2.setToolTipText("Read iButton");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jLabel5.setText(AppLocal.getIntString("label.iButton")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_jVisible))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_jName, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_jImage, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_jRole, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jibutton, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)))
                .addContainerGap(117, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jName, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jibutton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jRole, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jVisible, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(m_jImage, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addContainerGap(27, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        String sNewPassword = Hashcypher.changePassword(this);
        if (sNewPassword != null) {
            m_sPassword = sNewPassword;
            m_Dirty.setDirty(true);
        }

    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
//if (JOptionPane.showConfirmDialog(this, AppLocal.getIntString("message.cardnew"), AppLocal.getIntString("title.editor"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {  
// JG 8 Jan 14 - Change "c" case to upper "C"        jcard.setText("c" + StringUtils.getCardNumber());
//            jibutton.setText(StringUtils.getCardNumber());
//        String iButtonId;
//        iButtonId = "12345";
            if (JOptionPane.showConfirmDialog(this, AppLocal.getIntString("message.cardnew"), AppLocal.getIntString("title.editor"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {  
                initIButtonMonitor();
                jibutton.setText("iButton: " + iButtonGlobal); 
                shutdownIButtonMonitor();
                m_Dirty.setDirty(true);
            } else {
            }

    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed

        if (JOptionPane.showConfirmDialog(this, AppLocal.getIntString("message.cardremove"), AppLocal.getIntString("title.editor"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {  
            jibutton.setText(null);
            m_Dirty.setDirty(true);
        }
        
    }//GEN-LAST:event_jButton3ActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JTextField jibutton;
    private com.openbravo.data.gui.JImageEditor m_jImage;
    private javax.swing.JTextField m_jName;
    private javax.swing.JComboBox m_jRole;
    private javax.swing.JCheckBox m_jVisible;
    // End of variables declaration//GEN-END:variables

}
