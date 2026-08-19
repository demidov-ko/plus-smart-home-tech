package ru.yandex.practicum.collector.model.hub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.practicum.collector.model.hub.types.HubEventType;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioAddedEvent extends HubEvent {
    @NotBlank
    @Size(min = 3, message = "name должен содержать не менее 3 символов")
    private String name;    //Название добавленного сценария

    @NotEmpty
    @Valid
    private List<ScenarioCondition> conditions; // Список условий, которые связаны со сценарием

    @NotEmpty
    @Valid
    private List<DeviceAction> actions; // Список действий, которые должны быть выполнены в рамках сценария

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_ADDED;
    }
}
