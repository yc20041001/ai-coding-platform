package com.aicoding.platform.repository.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.workspace")
public class WorkspaceProperties {

    private String rootPath = "./workspace";

    public String getRootPath() { return rootPath; }
    public void setRootPath(String rootPath) { this.rootPath = rootPath; }
}
