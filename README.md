# Advanced Banking Simulator

Aplicacion bancaria de consola desarrollada en Java 17+ que implementa Programación Orientada a Objetos (POO), manejo de transacciones financieras mediante `BigDecimal` y gestion de cuentas en memoria.

## Descripción del Proyecto

El sistema simula las operaciones principales de una entidad bancaria, permitiendo la autenticación de usuarios, consulta de saldos, depósitos, retiros con restricciones avanzadas, transferencias interbancarias y aplicación automatizada de intereses y comisiones de cierre de mes.

El desarrollo enfatiza el uso de buenas prácticas de programación, encapsulamiento, polimorfismo, manejo seguro de excepciones personalizadas y renderizado de tablas de historial formateadas.

## Funcionalidades Principales

- **Gestión de Cuentas:**
  - Cuentas de Ahorros (`SavingsAccount`): Control de límites de retiros mensuales y cálculo de tasa de interés anual.
  - Cuentas Corrientes (`CheckingAccount`): Cobertura con límite de sobregiro (*overdraft*) y cobro de comisiones de mantenimiento.
- **Operaciones Financieras:**
  - Depósitos y retiros validados.
  - Transferencias directas entre cuentas registradas con verificación de PIN.
  - Cálculo de precisión monetaria utilizando `BigDecimal` para evitar errores de redondeo de punto flotante.
- **Historial y Filtros:**
  - Registro de transacciones con identificadores únicos (UUID).
  - Consulta de movimientos en formato tabular.
  - Filtrado de historial por tipo de transacción o por rango de fechas (`LocalDateTime`).
- **Procesamiento Masivo:**
  - Cierre de mes global para la aplicación automática de comisiones e intereses.

## Arquitectura y Estructura del Código

El proyecto está diseñado bajo una estructura modular en un solo archivo principal (`BankingSimulatorApp.java`):

- **Excepciones de Dominio:** `BankingException`, `InsufficientFundsException`, `InvalidPinException`, `AccountNotFoundException`.
- **Entidades de Dominio:** `BankAccount` (Clase abstracta), `SavingsAccount`, `CheckingAccount`, `Transaction`, `TransactionType`.
- **Capa de Negocio:** `BankManager` (Gestión centralizada de colecciones `Map<String, BankAccount>`).
- **Interfaz de Usuario:** `BankingSimulatorApp` (Menú interactivo de consola y formateo con `Locale`).

## Requisitos del Sistema

- Java Development Kit (JDK) 17 o superior.
- Git (opcional, para control de versiones).
