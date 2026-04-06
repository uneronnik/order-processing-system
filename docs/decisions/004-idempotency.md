# 004 — Идемпотентность через UNIQUE constraint

## Контекст
Kafka гарантирует at-least-once delivery. Нужна защита от дублирования.

## Решение
UNIQUE constraint на колонку order_id в таблице payments.

## Обоснование
- Простейшая и надёжнейшая реализация
- Защита на уровне БД
- Дополнительно: проверка existsByOrderId() в коде

## Альтернативы
- Redis с TTL для idempotency key — нужен при высоких нагрузках
- Transactional outbox pattern — для exactly-once семантики