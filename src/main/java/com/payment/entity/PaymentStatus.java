package com.payment.entity;

public enum PaymentStatus {
  PENDING, // Esperando confirmación
  PROCESSING, // En procesamiento
  COMPLETED, // Exitoso
  FAILED, // Falló
  REFUNDED // Devuelto
}