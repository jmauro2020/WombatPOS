package com.checks;

import com.oast.SwtFrame;

public interface Checker {
    
    /**
     * @return whether validation passed.
     */
    public boolean validate(SwtFrame swtFrame);
    
    /**
     * @return some message for user, null if no message.
     */
    public String getMessage();
    
    /**
     * @return whether should run remaining checkers
     */
    public boolean coninueValidation();
}
