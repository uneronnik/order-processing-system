# Order Processing System

Микросервисная система обработки заказов с асинхронной коммуникацией через Kafka, реализующая Saga-паттерн для распределённых транзакций.

## Архитектура

```
┌──────────┐    REST     ┌──────────────┐
│  Client  │────────────▶│ Auth Service │
└──────────┘             └──────────────┘
      │
      │ REST
      ▼
┌──────────────┐  order-events   ┌───────────────────┐  inventory-events  ┌─────────────────┐
│ Order Service│─────────────────▶│ Inventory Service │───────────────────▶│ Payment Service │
└──────────────┘                 └───────────────────┘                    └─────────────────┘
      ▲                                                                         │
      │                          payment-events                                 │
      └─────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
                              ┌──────────────────────┐
                              │ Notification Service  │
                              └──────────────────────┘
```

Все сервисы общаются асинхронно через Kafka. Синхронный REST используется только для клиентских запросов.

## Технологии

Java 17, Spring Boot 4, Apache Kafka (KRaft), PostgreSQL 16, Docker, Liquibase, JWT, Testcontainers, Gradle

## Сервисы

| Сервис | Порт | Роль |
|---|---|---|
| auth-service | 8085 | Регистрация, логин, выдача JWT-токенов |
| order-service | 8080 | Создание и управление заказами |
| inventory-service | 8084 | Управление складом, резервирование товаров |
| payment-service | 8081 | Обработка оплаты |
| notification-service | 8083 | Уведомления (лог + Telegram) |

Общие DTO для Kafka-событий вынесены в модуль `common`.

## Saga — поток заказа

Система использует Saga в варианте хореографии — сервисы реагируют на события без центрального координатора.

### Успешный заказ

```
Order (PENDING) → OrderCreatedEvent
    → Inventory (резервирует товар) → InventoryReservedEvent
        → Payment (списывает средства) → PaymentCompletedEvent
            → Inventory (подтверждает резерв, списывает со склада)
            → Order (CONFIRMED)
            → Notification (уведомление об успехе)
```

### Товара нет на складе

```
Order (PENDING) → OrderCreatedEvent
    → Inventory (недостаточно товара) → InventoryReservationFailedEvent
        → Order (CANCELLED)
        → Notification (уведомление об отмене)
```

### Оплата не прошла

```
Order (PENDING) → OrderCreatedEvent
    → Inventory (резервирует товар) → InventoryReservedEvent
        → Payment (отказ) → PaymentFailedEvent
            → Inventory (отменяет резерв — компенсация)
            → Order (CANCELLED)
            → Notification (уведомление об отказе)
```

## Быстрый старт

### Docker (рекомендуется)

```bash
docker-compose up --build
```

Поднимутся все 5 сервисов, PostgreSQL и Kafka. Готово к использованию.

### Локально

Требуется запущенный PostgreSQL и Kafka.

```bash
# 1. Создать базу данных
createdb orderdb

# 2. Запустить все сервисы
./gradlew bootRun --parallel
```

## API

После запуска Swagger UI доступен для каждого сервиса:

- Auth: `http://localhost:8085/swagger-ui.html`
- Orders: `http://localhost:8080/swagger-ui.html`
- Inventory: `http://localhost:8084/swagger-ui.html`

### Симуляция оплаты

Payment Service работает в режиме симуляции: заказы с суммой менее 100 000 проходят успешно, от 100 000 и выше — отклоняются. Сумма рассчитывается автоматически: цена товара × количество.

### Основные эндпоинты

**Аутентификация**

```
POST /api/auth/register    — регистрация (email, password)
POST /api/auth/login       — логин, возвращает JWT-токен
```

**Заказы** (требуется JWT)

```
POST   /api/orders         — создать заказ (productId, quantity)
GET    /api/orders          — мои заказы (фильтр по статусу)
GET    /api/orders/{id}     — заказ по ID (только свой)
GET    /api/orders/admin/all — все заказы (только ADMIN)
```

**Склад** (GET — authenticated, POST/PUT — ADMIN)

```
POST   /api/inventory                    — добавить товар
GET    /api/inventory/{productId}        — информация о товаре
GET    /api/inventory                    — все товары
PUT    /api/inventory/{productId}/restock — пополнить склад
```

### Пример использования

```bash
# Регистрация
curl -X POST http://localhost:8085/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'

# Ответ: {"token": "eyJhbG..."}

# Создание заказа
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbG..." \
  -d '{"productId": "LAPTOP-001", "quantity": 1}'
```

## Тестирование

Проект покрыт unit и интеграционными тестами.

**Unit-тесты** — Mockito, изолированная проверка бизнес-логики каждого сервиса. Покрыты все ключевые сценарии: успех, ошибки, граничные случаи, идемпотентность.

**Интеграционные тесты** — Testcontainers (PostgreSQL, Kafka), проверка HTTP-эндпоинтов, Saga-потока, взаимодействия с базой.

```bash
# Запуск всех тестов
./gradlew test

# Тесты конкретного сервиса
./gradlew :payment-service:test
./gradlew :inventory-service:test
./gradlew :order-service:test
./gradlew :auth-service:test
./gradlew :notification-service:test
```

## Архитектурные решения (ADR)

Ключевые решения задокументированы в `docs/decisions/`:

| ADR | Решение |
|---|---|
| [001](docs/decisions/001-monorepo.md) | Мультимодульный монорепозиторий |
| [002](docs/decisions/002-testcontainers.md) | Testcontainers вместо H2 |
| [003](docs/decisions/003-kafka-vs-rabbitmq.md) | Kafka вместо RabbitMQ |
| [004](docs/decisions/004-idempotency.md) | Идемпотентность через UNIQUE constraint |
| [005](docs/decisions/005-notification-strategy.md) | Strategy-паттерн для уведомлений |
| [006](docs/decisions/006-saga-choreography.md) | Saga-хореография вместо оркестрации |
| [007](docs/decisions/007-jwt-vs-sessions.md) | JWT вместо серверных сессий |

## Возможные улучшения

- **Отмена заказа пользователем** — `PUT /api/orders/{id}/cancel` с компенсацией резерва
- **Пагинация** — `?page=0&size=20` для списков заказов и товаров
- **Корзина** — несколько товаров в одном заказе
- **Мониторинг** — Prometheus + Grafana, структурированные JSON-логи
- **Таймаут заказов** — `@Scheduled` для автоматической отмены зависших заказов
- **OAuth2** — замена самописного JWT на Spring Authorization Server
- **Удаление товаров** — удаление товаров со склада полностью