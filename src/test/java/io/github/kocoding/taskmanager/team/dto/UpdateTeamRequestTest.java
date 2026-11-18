package io.github.kocoding.taskmanager.team.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

public class UpdateTeamRequestTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void teamUpdateRequest_省略した場合はnullになる() {
        String jsonString = "{\"name\":\"abc\"}";
        UpdateTeamRequest request = mapper.readValue(jsonString, UpdateTeamRequest.class);

        assertEquals(Optional.of("abc"), request.getName());
        assertNull(request.getDescription());
    }

    @Test
    void teamUpdateRequest_明示的にnullにした場合はOptional_emptyになる() {
        String jsonString = "{\"name\":\"abc\",\"description\":null}";
        UpdateTeamRequest request = mapper.readValue(jsonString, UpdateTeamRequest.class);

        assertEquals(Optional.of("abc"), request.getName());
        assertEquals(Optional.empty(), request.getDescription());
    }

    @Test
    void teamUpdateRequest_値を指定した場合はその値になる() {
        String jsonString = "{\"name\":\"abc\",\"description\":\"xyz\"}";
        UpdateTeamRequest request = mapper.readValue(jsonString, UpdateTeamRequest.class);

        assertEquals(Optional.of("abc"), request.getName());
        assertEquals(Optional.of("xyz"), request.getDescription());
    }

}
