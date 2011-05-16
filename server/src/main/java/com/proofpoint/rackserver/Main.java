package com.proofpoint.rackserver;

import com.proofpoint.bootstrap.Bootstrap;
import com.proofpoint.http.server.HttpServerModule;
import com.proofpoint.jmx.JmxModule;

public class Main
{
    public static void main(String[] args)
            throws Exception
    {
        Bootstrap app = new Bootstrap(
                new HttpServerModule(),
                new JmxModule(),
                new MainModule());

        app.initialize();
    }
}