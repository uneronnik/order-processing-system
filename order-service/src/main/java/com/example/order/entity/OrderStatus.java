package com.example.order.entity;

public enum OrderStatus {
    PENDING,     // создан, ждёт оплаты
    CONFIRMED,   // оплата прошла
    CANCELLED,   // оплата не прошла или отменён
    FAILED       // техническая ошибка
}