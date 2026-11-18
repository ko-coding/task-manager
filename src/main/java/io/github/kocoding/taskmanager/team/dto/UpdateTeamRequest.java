package io.github.kocoding.taskmanager.team.dto;

import java.util.Optional;

// nullは省略、Optional.empty()はJSONのnull
public class UpdateTeamRequest {

    private Optional<String> name;
    private Optional<String> description;

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
