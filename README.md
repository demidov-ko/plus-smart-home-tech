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


## Discovery server

Идея паттерна Service Discovery в том, чтобы сервисы находили друг друга по имени, а не по заранее прописанному адресу.

Для этого в системе нужен реестр сервисов. В нём хранится информация о запущенных экземплярах: имя сервиса, адрес, порт и технические метаданные.

### Запускаем сервер Eureka

* Запуск Config Server
* Запуск DiscoveryServer
* Запуск Aggregator, Analyzer и Collector
* Проверка на `http://localhost:8761`
  * откроется Eureka Dashboard — веб-интерфейс Eureka Server и в нем будут зарегестрированные приложения
* Проверяем реестр через HTTP `curl http://localhost:8761/eureka/apps` в терминале
  * Или проверка конкретного сервиса`http://localhost:8761/eureka/apps/AGGREGATOR`
* Запуск OrderService, ProductService и InventoryService
* Запуск приложения java -jar commerce/web-ui/web-ui.jar
  * Проверка на сайте http://localhost:8443

### Где хранится адрес Eureka Server
В общей конфигурации можно хранить адрес Eureka Server.
Это удобно, потому что Aggregator, Analyzer и Collector будут обращаться к одному и тому же реестру сервисов.

```avroidl
    Настройка Eureka в общий файл
		eureka:
		    client:
		        serviceUrl:
                    defaultZone: http://localhost:8761/eureka/ 
```
Так же добавлена зависимость `spring-cloud-starter-netflix-eureka-client` в каждый сервис

## OpenFeign
Реализована интеграция между сервисами через декларативный HTTP-клиент OpenFeign.

```avroidl
OrderService --(OpenFeign)--> ProductService  (GET /api/products/{id} — данные товара)
             --(OpenFeign)--> InventoryService (POST /reserve, /release — остатки)
                                |
                                └──> Eureka (Service Discovery)
```
* Сервисы
  * order-service — оркестратор заказа. Через ProductClient получает данные товара (название, цену, статус активности), 
  через InventoryClient — резервирует и снимает остатки. При срыве сценария выполняет компенсацию.
  * product-service — каталог товаров. Отдаёт ProductDto по id.
  * inventory-service — склад. Принимает запросы на резервирование и снятие остатков.
  * Eureka Server — реестр сервисов. Позволяет order-service находить оба зависимых сервиса по логическому имени.

`OrderService (с внедрением OpenFeign)`
Сервис оформления заказов.
Реализует бизнес-логику создания заказа с синхронной проверкой товаров через product-service 
и резервированием остатков через inventory-service

* Активация Feign: В главном классе приложения проставлена аннотация @EnableFeignClients для сканирования интерфейсов-клиентов.
* Два декларативных клиента:
  * ProductClient — получает данные товара из product-service
  * InventoryClient — резервирует остатки в inventory-service
* Оркестрация заказа (OrderOrchestrationServiceImpl):
  * Группирует позиции по productId (дубликаты суммируются)
  * Запрашивает данные каждого товара через ProductClient - один запрос на уникальный productId
  *  Проверяет, что товар активен (product.active() == true)
  *  Резервирует остатки через InventoryClient - один запрос на productId с суммарным количеством `inventoryClient.reserveStock(request);`
  *  Формирует снимок данных товара (OrderItemData: productId, name, price, quantity) и сохраняет заказ
  *  При ошибке резервирования или сохранения — запускает компенсацию: снимает все ранее созданные резервы через `inventoryClient.releaseStock()`

## Circuit Breaker в order-service

С помощью OpenFeign order-service научился обращаться к product-service и inventory-service: получать данные товара, 
резервировать остатки и создавать заказ на основе ответов соседних сервисов

`Circuit Breaker` — паттерн, который отслеживает результаты вызовов к зависимому сервису и временно блокирует новые обращения, 
если ошибок или медленных ответов стало слишком много. Он не чинит соседний сервис, но защищает вызывающую сторону: 
помогает не тратить ресурсы на заведомо проблемные запросы и не распространять сбой дальше по системе.

#### У Circuit Breaker есть три основных состояния:
- `CLOSED` (закрыт) - штатный режим
  - Все запросы идут напрямую к product-service / inventory-service
  - Resilience4j: Считает статусы и ошибки. Если процент ошибок превысит порог (например, 50% за последние N вызовов) — переходит в OPEN
- `OPEN` (открыт) - блокировка запросов
  - Resilience4j не даёт сделать сетевой вызов вообще. Сразу возвращает ошибку
- `HALF_OPEN` (полуоткрыт) - проверка восстановления
  - По истечении времени удержания в OPEN Resilience4j разрешает пробный запрос, чтобы проверить вернулся ли сервис в строй, не обрушивая его потоком
  - Пробный вызов идёт как обычный Feign-запрос:
  - Если он успешен → CB закрывается (CLOSED), все следующие запросы идут нормально.
  - Если снова ошибка → CB сразу уходит обратно в OPEN.

#### Сделано 
* Подключён Spring Cloud Circuit Breaker с Resilience4j в order-service - зависимость добавлена в pom.xml.
*  Включён Circuit Breaker для OpenFeign и настроен Resilience4j (размер окна, порог ошибок, время в открытом состоянии, таймаут) - конфигурация лежит в infra/config-repo/commerce/order-service.yml
*  Реализованы fallback-фабрики для ProductClient и InventoryClient через FallbackFactory 
   фабрики получают причину сбоя (Throwable cause), логируют её и выбрасывают типизированные исключения (ProductServiceUnavailableException, InventoryServiceUnavailableException).
*  Бизнес-ошибки не маскируются fallback'ом: 404 (товар не найден), 409 (недостаточно остатков), active = false - приводят к отказу в создании заказа. 
   Fallback срабатывает только на техническую недоступность (5xx, таймаут, открытый Circuit Breaker).
*  Введён ServiceCallResult<T> (sealed interface) - оркестратор явно различает три результата вызова: Success, Failure (бизнес-отказ), Degraded (техническая недоступность).
*  При технической деградации заказ сохраняется в статусе PENDING_CONFIRMATION:
  * 
      * если каталог недоступен - сохраняется productId, название-заглушка Товар #<id> (ожидает проверки) и цена 0; 
      * если склад недоступен - резервирование не подтверждается. В statusDetails записывается причина для ручной проверки.
*  Сохранена компенсация резерва: при срыве сценария после успешного резервирования снятие выполняется через inventory-service. 
   Компенсация — best-effort: при ошибке логируется, но не прерывает поток.