# Smart Home Technologies

Система телеметрии и автоматизации умного дома. 
Хабы пользователей отправляют показания датчиков и события устройств/сценариев, система агрегирует их в снапшоты 
состояния и исполняет пользовательские сценарии, отправляя команды обратно на хаб.

## Архитектура

```avroidl
Hub Router --(gRPC)--> Collector --> Kafka(telemetry.sensors.v1, telemetry.hubs.v1)
                                        |
                                        |--> Aggregator --> Kafka(telemetry.snapshots.v1)
                                        |
                                        └──> Analyzer -- (gRPC)--> Hub Router
                                            (сохраняет устройства/сценарии,
                                            проверяет снапшоты, шлёт команды)
```
* Сервисы
1. `Collector` - приём телеметрии от хабов (`вх.`gRPC (Protobuf) от Hub Router; `вых.`Kafka: telemetry.sensors.v1, telemetry.hubs.v1 (Avro) )
2. `Aggregator` - строит снапшоты состояния хаба (`вх.`Kafka: telemetry.sensors.v1; `вых.`Kafka: telemetry.snapshots.v1 (Avro))
3. `Analyzer` - хранит сценарии, исполняет их по снапшотам (`вх.`Kafka: telemetry.hubs.v1, telemetry.snapshots.v1 + Postgres; `вых.`gRPC-команды в Hub Router)

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
    2. .\run-tests.bat
    ```
* Проверить содержимое топика в Kafka напрямую
```avroidl
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic telemetry.sensors.v1 --from-beginning
```

## Aggregator

Читает события датчиков, строит и поддерживает снапшот текущего состояния каждого хаба, публикует изменения в Kafka. 
Без входящего сетевого интерфейса — только Kafka consumer/producer.

### Что делает

* `AggregationStarter` - ручной poll loop (KafkaConsumer<String, SensorEventAvro>)
* `SnapshotService.updateState(event)` - строит/обновляет SensorsSnapshotAvro по хабу:
    * если снапшота для хаба ещё нет - создаёт новый
    * если данные по датчику устарели (timestamp события раньше сохранённого) или не изменились (equals) - игнорирует, 
    возвращает Optional.empty()
    * иначе обновляет состояние и возвращает Optional.of(snapshot) — только тогда пишется в Kafka
* Дедупликация на уровне updateState гарантирует, что в telemetry.snapshots.v1 попадают только реальные изменения

## Analyzer

Хранит устройства и пользовательские сценарии в Postgres, при получении снапшота проверяет все сценарии хаба и исполняет 
сработавшие, отправляя команды в Hub Router по gRPC.

### Что делает

Работает в двух независимых потоках с двумя consumer-группами (разное отношение к повторной обработке):

* `HubEventProcessor` (Runnable, отдельный поток) - читает telemetry.hubs.v1, через HubEventHandler-диспетчер (по классу payload) 
сохраняет/удаляет Sensor/Scenario в БД. Коммит асинхронный (commitAsync) - повторная обработка не критична (add - идемпотентен, remove - идемпотентен).
* `SnapshotProcessor` (основной поток) - читает telemetry.snapshots.v1, для каждого снапшота загружает сценарии хаба 
(ScenarioLookupService, с eager-подгрузкой условий/действий - необходимо, потому что чтение происходит вне Spring-транзакции обычного контроллера),
проверяет каждый через ScenarioMatcher/ScenarioConditionEvaluator, при совпадении всех условий сценария - отправляет действия в Hub Router.
Коммит синхронный (commitSync) - повторная обработка снапшотов нежелательна.

### gRPC-клиент к Hub Router

`HubRouterClient` (@GrpcClient("hub-router"), HubRouterControllerBlockingStub) отправляет DeviceActionRequest 
(hub_id, scenario_name, action, timestamp) через handleDeviceAction. 
Ошибки gRPC-вызова логируются, но не прерывают обработку остальных действий/снапшотов.








