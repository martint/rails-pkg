package com.proofpoint.rackserver;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.proofpoint.log.Logger;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyObjectAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassCache;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import static org.jruby.javasupport.JavaEmbedUtils.javaToRuby;

public class RackServlet
        implements Servlet
{
    private final IRubyObject rackApplication;
    private final Ruby runtime;
    private final RubyObjectAdapter adapter = JavaEmbedUtils.newObjectAdapter();
    private final Logger rackLogger = Logger.get(RackServlet.class);

    @Inject
    public RackServlet(RackServletConfig config)
            throws IOException
    {
        runtime = JavaEmbedUtils.initialize(new ArrayList(), createRuntimeConfig());

        // ENV['GEM_PATH'] = config.getGemPath();
        RubyHash env = (RubyHash) runtime.evalScriptlet("ENV");
        env.put("GEM_PATH", config.getGemPath()) ;

        // ENV['BUNDLE_GEMFILE'] = config.getGemfilePath();
        env.put("BUNDLE_GEMFILE", config.getGemfilePath()) ;

        InputStream stream = getClass().getClassLoader().getResourceAsStream("proofpoint/rack.rb");
        try {
            runtime.loadFile("proofpoint/rack.rb", stream, false);
        }
        finally {
            stream.close();
        }

        IRubyObject builder = runtime.evalScriptlet("Proofpoint::Rack::Builder.new");

        String rackScriptPath = config.getRackScriptPath();
        String rackScript = Files.toString(new File(rackScriptPath), Charsets.UTF_8);

        rackApplication = adapter.callMethod(builder, "build", new IRubyObject[] {
                javaToRuby(runtime, rackScriptPath),
                javaToRuby(runtime, rackScript),
                javaToRuby(runtime, rackLogger)
        });
    }

    @Override
    public void init(ServletConfig servletConfig)
            throws ServletException
    {
    }

    @Override
    public ServletConfig getServletConfig()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException
    {
        adapter.callMethod(rackApplication, "call", new IRubyObject[] { javaToRuby(runtime, request), javaToRuby(runtime, response) });
    }

    @Override
    public String getServletInfo()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy()
    {
        throw new UnsupportedOperationException();
    }

    private RubyInstanceConfig createRuntimeConfig()
    {
        RubyInstanceConfig config = new RubyInstanceConfig();
        ClassCache classCache = JavaEmbedUtils.createClassCache(Thread.currentThread().getContextClassLoader());
        config.setClassCache(classCache);

        URL resource = RubyInstanceConfig.class.getResource("/META-INF/jruby.home");
        if (resource.getProtocol().equals("jar")) {
            try { // http://weblogs.java.net/blog/2007/04/25/how-convert-javaneturl-javaiofile
                config.setJRubyHome(resource.toURI().getSchemeSpecificPart());
            }
            catch (URISyntaxException e) {
                config.setJRubyHome(resource.getPath());
            }
        }

        return config;
    }
}
