package com.aicoding.platform.project.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateProjectRequest {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;
    private List<String> techStack;
    private String icon;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTechStack() { return techStack; }
    public void setTechStack(List<String> techStack) { this.techStack = techStack; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
