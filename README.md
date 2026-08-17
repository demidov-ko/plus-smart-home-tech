## Collector

Сервис приёма телеметрии от хабов умного дома. Принимает JSON-события по HTTP от сервиса Hub Router, конвертирует их в бинарный формат Apache Avro и публикует в Kafka.
Часть многомодульного проекта `plus-smart-home-tech`, модуль `telemetry/collector`.

### Что делает сервис
* Принимает события датчиков (/events/sensors) и события хабов/сценариев (/events/hubs) в формате JSON
* Валидирует и десериализует их в иерархию Java-классов (полиморфизм через Jackson @JsonTypeInfo/@JsonSubTypes)
* Конвертирует в Avro-объекты (SensorEventAvro, HubEventAvro)
* Публикует в Kafka-топики:
  * telemetry.sensors.v1 — показания датчиков
  * telemetry.hubs.v1 — события хабов и сценариев

### API
```avroidl
Метод   Путь                Описание
POST    /events/sensors     Событие датчика (Climate, Light, Motion, Switch, Temperature)
POST    /events/hubs        Событие хаба (DeviceAdded, DeviceRemoved, ScenarioAdded, ScenarioRemoved)
```


### Запуск
* Поднять контейнеры `docker compose up -d`
* Собрать проект `mvn clean install`
* Запустить `CollectorApplication.java`
* Проверить работу через `hub-router` (скрипт сам определит git-ветку)
    ```
    1. cd hub-router
    2. .run-tests.bat
    ```
* Проверить содержимое топика в Kafka напрямую
```avroidl
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic telemetry.sensors.v1 --from-beginning
```
