package com.proofpoint.rackserver;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.proofpoint.configuration.ConfigurationModule;
import com.proofpoint.http.server.TheServlet;

import javax.servlet.Servlet;
import java.util.HashMap;
import java.util.Map;

public class MainModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        ConfigurationModule.bindConfig(binder).to(RackServletConfig.class);
        binder.bind(Servlet.class).annotatedWith(TheServlet.class).to(RackServlet.class).in(Scopes.SINGLETON);
    }

    @Provides
    @TheServlet
    public Map<String, String> createTheServletParams()
    {
        Map<String, String> initParams = new HashMap<String, String>();
        return initParams;
    }
}
