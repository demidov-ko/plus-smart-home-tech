package ru.yandex.practicum.collector.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.collector.controller.SensorEventController;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.service.SensorEventService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorEventController.class)
class SensorEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SensorEventService service;

    @Test
    void shouldAccept_LightSensorEvent_andDispatchToService() throws Exception {
        String json = """
                {
                    "id": "sensor.light.3",
                    "hubId": "hub-2",
                    "timestamp": "2026-08-17T20:10:25.150Z",
                    "type": "LIGHT_SENSOR_EVENT",
                    "linkQuality": 75,
                    "luminosity": 59
                }
                """;

        mockMvc.perform(post("/events/sensors")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk());

        verify(service).collect(any(SensorEvent.class));
    }

    @Test
    void shouldAccept_SwitchSensorEvent_andDispatchToService() throws Exception {
        String json = """
                {
                    "id": "sensor.switch.4356",
                    "hubId": "hub-1",
                    "timestamp": "2026-08-17T20:10:25.150Z",
                    "type": "SWITCH_SENSOR_EVENT",
                    "state": true
                }
                """;

        mockMvc.perform(post("/events/sensors")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk());

        verify(service).collect(any(SensorEvent.class));
    }

    @Test
    void shouldReturn400_whenRequiredFieldMissing() throws Exception {
        // отсутствует обязательное поле id
        String json = """
                {
                    "hubId": "hub-2",
                    "type": "LIGHT_SENSOR_EVENT",
                    "linkQuality": 75,
                    "luminosity": 59
                }
                """;

        mockMvc.perform(post("/events/sensors")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_whenTypeFieldUnknown() throws Exception {
        String json = """
                {
                    "id": "sensor.light.3",
                    "hubId": "hub-2",
                    "timestamp": "2026-08-17T20:10:25.150Z",
                    "type": "UNKNOWN_EVENT_TYPE",
                    "linkQuality": 75
                }
                """;

        mockMvc.perform(post("/events/sensors")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
