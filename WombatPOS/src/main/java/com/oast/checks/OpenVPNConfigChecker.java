package com.checks;

import java.io.File;

import com.oast.Oast;
import com.oast.SwtFrame;

public class OpenVPNConfigChecker implements Checker {
    private static final String CANNOT_FIND_CONF = "Can't find configuration file for OpenVPN";
    private static final String PLEASE_CHECK_SETTINGS = "Please check OAST settings.";
    
    private String message;
    
    public boolean coninueValidation() {
        return true;
    }

    public String getMessage() {
        return message;
    }

    public boolean validate(SwtFrame swtFrame) {
        final String config = Oast.getConfigurationManager().getConfigFile();
        
        message = null;
        validateConfig(config, CANNOT_FIND_CONF);
        
        if (message != null) {
            message += PLEASE_CHECK_SETTINGS;
            return false;
        } else {
            return true;
        }
    }
    
    private void validateConfig(String configFile, String errorMessage) {
        if (! new File(configFile).exists()) {
            message = (message == null ? "" : message) + errorMessage + "\n";
        }
    }

}
