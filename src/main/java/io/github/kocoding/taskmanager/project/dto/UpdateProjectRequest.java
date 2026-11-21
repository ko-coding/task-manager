package io.github.kocoding.taskmanager.project.dto;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// nullは省略、Optional.empty()はJSONのnull
public class UpdateProjectRequest {

    private Optional<@NotBlank @Size(max = 128) String> name;
    private Optional<@Size(max = 255) String> description;

    public Optional<String> getName() {
        return name;
    }

    public void setName(Optional<String> name) {
        this.name = name;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(Optional<String> description) {
        this.description = description;
    }

}
