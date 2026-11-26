package io.github.kocoding.taskmanager.task.dto;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// nullは省略、Optional.empty()はJSONのnull
public class UpdateTaskRequest {

    private Optional<@NotBlank @Size(max = 128) String> title;
    private Optional<@Size(max = 255) String> description;
    private Optional<LocalDate> dueDate;
    private Optional<Long> assignedTo;

    public Optional<String> getTitle() {
        return title;
    }

    public void setTitle(Optional<String> title) {
        this.title = title;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(Optional<String> description) {
        this.description = description;
    }

    public Optional<LocalDate> getDueDate() {
        return dueDate;
    }

    public void setDueDate(Optional<LocalDate> dueDate) {
        this.dueDate = dueDate;
    }

    public Optional<Long> getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Optional<Long> assignedTo) {
        this.assignedTo = assignedTo;
    }

}
