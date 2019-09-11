package com.fnproject.fn.testing;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;

import java.io.PrintStream;

public class OCIAuthenticationFeature implements  FnTestingRuleFeature {


    public OCIAuthenticationFeature(AuthenticationDetailsProvider authenticationDetailsProvider) {

    }

    @Override
    public void prepareTest(ClassLoader functionClassLoader, PrintStream stderr, String cls, String method) {

    }

    @Override
    public void prepareFunctionClassLoader(FnTestingClassLoader cl) {
    }

    @Override
    public void afterTestComplete() {

    }
}
