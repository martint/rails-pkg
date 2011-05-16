package com.proofpoint.rackserver;

import com.proofpoint.configuration.Config;

import javax.validation.constraints.NotNull;

public class RackServletConfig
{
    private String rackScriptPath = "/Users/martin/proofpoint/rails/src/main/rails/config.ru";
    private String gemfilePath = "/Users/martin/proofpoint/rails/src/main/rails/Gemfile";
    private String gemPath = "/Users/martin/proofpoint/rails/src/main/gems";

    @NotNull
    public String getRackScriptPath()
    {
        return rackScriptPath;
    }

    @Config("rack.script-path")
    public void setRackScriptPath(String rackScriptPath)
    {
        this.rackScriptPath = rackScriptPath;
    }

    @NotNull
    public String getGemPath()
    {
        return gemPath;
    }

    @Config("ruby.gem-path")
    public void setGemPath(String path)
    {
        this.gemPath = path;
    }

    @NotNull
    public String getGemfilePath()
    {
        return gemfilePath;
    }

    @Config("bundler.gemfile-path")
    public void setGemfilePath(String gemfilePath)
    {
        this.gemfilePath = gemfilePath;
    }

}
