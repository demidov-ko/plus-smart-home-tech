package ru.yandex.practicum.collector.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.collector.controller.HubEventController;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.service.HubEventService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HubEventController.class)
class HubEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HubEventService service;

    @Test
    void shouldAccept_DeviceAddedEvent_andDispatchToService() throws Exception {
        String json = """
                {
                    "hubId": "hub-1",
                    "timestamp": "2026-08-17T20:10:25.150Z",
                    "id": "sensor.motion.1",
                    "deviceType": "MOTION_SENSOR",
                    "type": "DEVICE_ADDED"
                }
                """;

        mockMvc.perform(post("/events/hubs")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk());

        verify(service).collect(any(HubEvent.class));
    }

    @Test
    void shouldAccept_ScenarioAddedEvent_withNestedConditionsAndActions() throws Exception {
        String json = """
                {
                    "hubId": "hub-1",
                    "timestamp": "2026-08-17T20:10:25.150Z",
                    "type": "SCENARIO_ADDED",
                    "name": "Обогрев спальни",
                    "conditions": [
                        {
                            "sensorId": "sensor.temp.1",
                            "type": "TEMPERATURE",
                            "operation": "GREATER_THAN",
                            "value": 25
                        }
                    ],
                    "actions": [
                        {
                            "sensorId": "sensor.switch.1",
                            "type": "ACTIVATE"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/events/hubs")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk());

        verify(service).collect(any(HubEvent.class));
    }

    @Test
    void shouldReturn400_whenScenarioHasEmptyConditions() throws Exception {
        String json = """
                {
                    "hubId": "hub-1",
                    "timestamp": "2026-08-17T20:10:25.150Z",
                    "type": "SCENARIO_ADDED",
                    "name": "Пустой сценарий",
                    "conditions": [],
                    "actions": [
                        {
                            "sensorId": "sensor.switch.1",
                            "type": "ACTIVATE"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/events/hubs")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_whenHubIdMissing() throws Exception {
        String json = """
                {
                    "timestamp": "2026-08-17T20:10:25.150Z",
                    "id": "sensor.motion.1",
                    "deviceType": "MOTION_SENSOR",
                    "type": "DEVICE_ADDED"
                }
                """;

        mockMvc.perform(post("/events/hubs")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
