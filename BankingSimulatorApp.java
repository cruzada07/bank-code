import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

class BankingException extends RuntimeException {
    public BankingException(String message) {
        super(message);
    }
}

class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

class InvalidPinException extends BankingException {
    public InvalidPinException(String message) {
        super(message);
    }
}

class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

enum TransactionType {
    DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST, FEE
}

class Transaction {
    private final String id;
    private final LocalDateTime timestamp;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;

    public Transaction(TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String description) {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public Transaction(TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String description, LocalDateTime timestamp) {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
}

abstract class BankAccount {
    protected final String accountNumber;
    protected final String ownerName;
    protected BigDecimal balance;
    protected final String pin;
    protected final List<Transaction> transactions;

    public BankAccount(String accountNumber, String ownerName, BigDecimal initialBalance, String pin) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = initialBalance;
        this.pin = pin;
        this.transactions = new ArrayList<>();
    }

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public BigDecimal getBalance() { return balance; }
    public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }
    public abstract String getAccountType();
    public abstract void withdraw(BigDecimal amount, String description);

    public boolean verifyPin(String inputPin) {
        return this.pin.equals(inputPin);
    }

    public void deposit(BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("El monto de depósito debe ser mayor a cero.");
        }
        this.balance = this.balance.add(amount);
        this.transactions.add(new Transaction(TransactionType.DEPOSIT, amount, this.balance, description));
    }

    protected void forceSetBalance(BigDecimal newBalance) {
        this.balance = newBalance;
    }

    protected void addHistory(Transaction transaction) {
        this.transactions.add(transaction);
    }
}

class SavingsAccount extends BankAccount {
    private final BigDecimal annualInterestRate;
    private final int maxMonthlyWithdrawals;
    private int currentMonthWithdrawals;

    public SavingsAccount(String accountNumber, String owner, BigDecimal balance, String pin, BigDecimal rate, int maxW) {
        super(accountNumber, owner, balance, pin);
        this.annualInterestRate = rate;
        this.maxMonthlyWithdrawals = maxW;
        this.currentMonthWithdrawals = 0;
    }

    @Override
    public void withdraw(BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("El monto a retirar debe ser mayor a cero.");
        }
        if (currentMonthWithdrawals >= maxMonthlyWithdrawals) {
            throw new BankingException("Se ha alcanzado el límite mensual de retiros para esta cuenta de ahorros.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Fondos insuficientes en la cuenta de ahorros.");
        }
        this.balance = this.balance.subtract(amount);
        this.currentMonthWithdrawals++;
        this.transactions.add(new Transaction(TransactionType.WITHDRAWAL, amount, this.balance, description));
    }

    public void applyMonthlyInterest() {
        BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(interest);
            this.transactions.add(new Transaction(TransactionType.INTEREST, interest, this.balance, "Interés mensual aplicado"));
        }
        this.currentMonthWithdrawals = 0;
    }

    @Override
    public String getAccountType() { return "Ahorros"; }
}

class CheckingAccount extends BankAccount {
    private final BigDecimal overdraftLimit;
    private final BigDecimal monthlyMaintenanceFee;

    public CheckingAccount(String accountNumber, String owner, BigDecimal balance, String pin, BigDecimal overdraft, BigDecimal fee) {
        super(accountNumber, owner, balance, pin);
        this.overdraftLimit = overdraft;
        this.monthlyMaintenanceFee = fee;
    }

    public BigDecimal getOverdraftLimit() { return overdraftLimit; }

    @Override
    public void withdraw(BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("El monto a retirar debe ser mayor a cero.");
        }
        BigDecimal availableFunds = this.balance.add(overdraftLimit);
        if (availableFunds.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Fondos insuficientes, límite de sobregiro excedido.");
        }
        this.balance = this.balance.subtract(amount);
        this.transactions.add(new Transaction(TransactionType.WITHDRAWAL, amount, this.balance, description));
    }

    public void chargeMaintenanceFee() {
        this.balance = this.balance.subtract(monthlyMaintenanceFee);
        this.transactions.add(new Transaction(TransactionType.FEE, monthlyMaintenanceFee, this.balance, "Comisión de mantenimiento mensual"));
    }

    @Override
    public String getAccountType() { return "Corriente"; }
}

class BankManager {
    private final Map<String, BankAccount> accounts = new HashMap<>();

    public void registerAccount(BankAccount account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public BankAccount authenticate(String accountNumber, String pin) {
        BankAccount account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("El número de cuenta ingresado no existe.");
        }
        if (!account.verifyPin(pin)) {
            throw new InvalidPinException("El PIN ingresado es incorrecto.");
        }
        return account;
    }

    public void processTransfer(BankAccount source, String targetAccountNumber, BigDecimal amount, String pin) {
        if (!source.verifyPin(pin)) {
            throw new InvalidPinException("PIN incorrecto para confirmar la transferencia.");
        }
        if (source.getAccountNumber().equals(targetAccountNumber)) {
            throw new BankingException("No está permitido transferir a la misma cuenta.");
        }
        BankAccount target = accounts.get(targetAccountNumber);
        if (target == null) {
            throw new AccountNotFoundException("La cuenta destino no se encuentra registrada.");
        }
        
        source.withdraw(amount, "Transferencia enviada a " + targetAccountNumber);
        
        List<Transaction> sourceTx = source.getTransactions();
        Transaction lastWithdrawal = sourceTx.get(sourceTx.size() - 1);
        source.transactions.remove(sourceTx.size() - 1);
        source.transactions.add(new Transaction(TransactionType.TRANSFER_OUT, amount, lastWithdrawal.getBalanceAfter(), lastWithdrawal.getDescription()));

        target.forceSetBalance(target.getBalance().add(amount));
        target.addHistory(new Transaction(TransactionType.TRANSFER_IN, amount, target.getBalance(), "Transferencia recibida de " + source.getAccountNumber()));
    }

    public void executeMonthEndProcess() {
        for (BankAccount account : accounts.values()) {
            if (account instanceof SavingsAccount savings) {
                savings.applyMonthlyInterest();
            } else if (account instanceof CheckingAccount checking) {
                checking.chargeMaintenanceFee();
            }
        }
    }

    public List<Transaction> getFilteredHistory(BankAccount account, LocalDateTime start, TransactionType type) {
        return account.getTransactions().stream()
            .filter(t -> start == null || !t.getTimestamp().isBefore(start))
            .filter(t -> type == null || t.getType() == type)
            .collect(Collectors.toList());
    }
}

public class BankingSimulatorApp {
    private static final Scanner scanner = new Scanner(System.in);
    private static final BankManager bankManager = new BankManager();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("es", "PE"));
    private static BankAccount currentSession = null;

    public static void main(String[] args) {
        loadInitialData();
        boolean running = true;
        
        while (running) {
            try {
                if (currentSession == null) {
                    running = displayLoginMenu();
                } else {
                    displayAccountMenu();
                }
            } catch (BankingException ex) {
                System.out.println("\n[OPERACIÓN DENEGADA]: " + ex.getMessage());
            } catch (Exception ex) {
                System.out.println("\n[ERROR DE SISTEMA]: Entrada inválida o error inesperado.");
                scanner.nextLine();
            }
        }
        System.out.println("Cerrando Simulador Bancario. ¡Hasta pronto!");
    }

    private static boolean displayLoginMenu() {
        System.out.println("\n=======================================");
        System.out.println("      SIMULADOR BANCARIO AVANZADO      ");
        System.out.println("=======================================");
        System.out.println("1. Iniciar Sesión");
        System.out.println("2. Salir del Sistema");
        System.out.print("Seleccione una opción: ");
        
        String choice = scanner.nextLine().trim();
        
        if ("2".equals(choice)) {
            return false;
        }
        if (!"1".equals(choice)) {
            System.out.println("Opción no válida. Intente nuevamente.");
            return true;
        }

        System.out.print("Ingrese su Número de Cuenta: ");
        String accNum = scanner.nextLine().trim();
        System.out.print("Ingrese su PIN de Seguridad: ");
        String pin = scanner.nextLine().trim();

        currentSession = bankManager.authenticate(accNum, pin);
        System.out.println("\nAcceso concedido. Bienvenido/a, " + currentSession.getOwnerName() + ".");
        return true;
    }

    private static void displayAccountMenu() {
        System.out.println("\n--- PANEL DE CONTROL: " + currentSession.getAccountNumber() + " ---");
        System.out.println("1. Consultar Saldo y Detalles");
        System.out.println("2. Realizar Depósito");
        System.out.println("3. Realizar Retiro");
        System.out.println("4. Realizar Transferencia");
        System.out.println("5. Ver Historial de Movimientos");
        System.out.println("6. Ejecutar Cierre de Mes (Global)");
        System.out.println("7. Cerrar Sesión");
        System.out.print("Seleccione una operación: ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1" -> showAccountDetails();
            case "2" -> executeDeposit();
            case "3" -> executeWithdrawal();
            case "4" -> executeTransfer();
            case "5" -> displayTransactionHistory();
            case "6" -> {
                bankManager.executeMonthEndProcess();
                System.out.println("\n[ÉXITO]: Cierre de mes procesado para todas las cuentas activas.");
            }
            case "7" -> {
                currentSession = null;
                System.out.println("\nSesión cerrada de forma segura.");
            }
            default -> System.out.println("\nOpción no reconocida.");
        }
    }

    private static void showAccountDetails() {
        System.out.println("\n[DETALLES DE LA CUENTA]");
        System.out.println("Titular: " + currentSession.getOwnerName());
        System.out.println("Tipo de Cuenta: " + currentSession.getAccountType());
        System.out.println("Saldo Disponible: " + formatCurrency(currentSession.getBalance()));
        if (currentSession instanceof CheckingAccount ca) {
            System.out.println("Límite de Sobregiro: " + formatCurrency(ca.getOverdraftLimit()));
        }
    }

    private static BigDecimal readAmountFromConsole(String prompt) {
        System.out.print(prompt);
        try {
            return new BigDecimal(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new BankingException("El formato del monto ingresado es inválido.");
        }
    }

    private static void executeDeposit() {
        BigDecimal amount = readAmountFromConsole("Ingrese el monto a depositar: ");
        System.out.print("Ingrese una referencia o descripción (opcional): ");
        String description = scanner.nextLine().trim();
        if (description.isEmpty()) description = "Depósito regular";
        
        currentSession.deposit(amount, description);
        System.out.println("\n[ÉXITO]: Depósito procesado. Nuevo saldo: " + formatCurrency(currentSession.getBalance()));
    }

    private static void executeWithdrawal() {
        BigDecimal amount = readAmountFromConsole("Ingrese el monto a retirar: ");
        System.out.print("Ingrese una referencia o descripción (opcional): ");
        String description = scanner.nextLine().trim();
        if (description.isEmpty()) description = "Retiro de efectivo";
        
        currentSession.withdraw(amount, description);
        System.out.println("\n[ÉXITO]: Retiro procesado. Nuevo saldo: " + formatCurrency(currentSession.getBalance()));
    }

    private static void executeTransfer() {
        System.out.print("Ingrese el número de la cuenta destino: ");
        String targetAccount = scanner.nextLine().trim();
        BigDecimal amount = readAmountFromConsole("Ingrese el monto a transferir: ");
        System.out.print("Ingrese su PIN actual para confirmar la transacción: ");
        String pin = scanner.nextLine().trim();
        
        bankManager.processTransfer(currentSession, targetAccount, amount, pin);
        System.out.println("\n[ÉXITO]: Transferencia enviada correctamente. Nuevo saldo: " + formatCurrency(currentSession.getBalance()));
    }

    private static void displayTransactionHistory() {
        System.out.println("\n[FILTROS DE BÚSQUEDA]");
        System.out.println("1. Mostrar todos los movimientos");
        System.out.println("2. Filtrar por tipo (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST, FEE)");
        System.out.println("3. Filtrar desde fecha de inicio (formato: yyyy-MM-dd HH:mm)");
        System.out.print("Seleccione un filtro: ");
        String filterType = scanner.nextLine().trim();

        TransactionType typeFilter = null;
        LocalDateTime startFilter = null;

        if ("2".equals(filterType)) {
            System.out.print("Ingrese el tipo exacto: ");
            try {
                typeFilter = TransactionType.valueOf(scanner.nextLine().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Tipo no reconocido. Se ignorará el filtro.");
            }
        } else if ("3".equals(filterType)) {
            System.out.print("Ingrese la fecha de inicio (yyyy-MM-dd HH:mm): ");
            try {
                startFilter = LocalDateTime.parse(scanner.nextLine().trim(), DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("Formato de fecha inválido. Se ignorará el filtro.");
            }
        }

        List<Transaction> records = bankManager.getFilteredHistory(currentSession, startFilter, typeFilter);
        renderTable(records);
    }

    private static void renderTable(List<Transaction> transactions) {
        System.out.println("\n-----------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-16s | %-14s | %-14s | %-14s | %-20s%n", 
                "ID Trans.", "Fecha/Hora", "Tipo", "Monto", "Saldo Result.", "Descripción");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        
        if (transactions.isEmpty()) {
            System.out.println("No existen transacciones que coincidan con los criterios.");
        } else {
            for (Transaction tx : transactions) {
                String shortDesc = tx.getDescription().length() > 20 ? tx.getDescription().substring(0, 17) + "..." : tx.getDescription();
                System.out.printf("%-10s | %-16s | %-14s | %-14s | %-14s | %-20s%n",
                    tx.getId(),
                    tx.getTimestamp().format(DATE_FORMATTER),
                    tx.getType(),
                    formatCurrency(tx.getAmount()),
                    formatCurrency(tx.getBalanceAfter()),
                    shortDesc
                );
            }
        }
        System.out.println("-----------------------------------------------------------------------------------------------------");
    }

    private static String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    private static void loadInitialData() {
        SavingsAccount kevinAccount = new SavingsAccount("1001", "Kevin De la Cruz", new BigDecimal("4800.00"), "1234", new BigDecimal("0.05"), 4);
        CheckingAccount catalinaAccount = new CheckingAccount("2001", "Catalina Mendoza", new BigDecimal("1400.00"), "4321", new BigDecimal("500.00"), new BigDecimal("15.00"));
        SavingsAccount univAccount = new SavingsAccount("1002", "Universidad UNI", new BigDecimal("15000.00"), "0000", new BigDecimal("0.03"), 10);

        LocalDateTime now = LocalDateTime.now();
        kevinAccount.addHistory(new Transaction(TransactionType.DEPOSIT, new BigDecimal("5000.00"), new BigDecimal("5000.00"), "Apertura de cuenta", now.minusDays(30)));
        kevinAccount.addHistory(new Transaction(TransactionType.WITHDRAWAL, new BigDecimal("200.00"), new BigDecimal("4800.00"), "Retiro Cajero Ate", now.minusDays(15)));

        catalinaAccount.addHistory(new Transaction(TransactionType.DEPOSIT, new BigDecimal("1500.00"), new BigDecimal("1500.00"), "Apertura de cuenta", now.minusDays(20)));
        catalinaAccount.addHistory(new Transaction(TransactionType.WITHDRAWAL, new BigDecimal("100.00"), new BigDecimal("1400.00"), "Consumo restaurante KFC", now.minusDays(5)));

        univAccount.addHistory(new Transaction(TransactionType.DEPOSIT, new BigDecimal("15000.00"), new BigDecimal("15000.00"), "Fondo Institucional", now.minusDays(40)));

        bankManager.registerAccount(kevinAccount);
        bankManager.registerAccount(catalinaAccount);
        bankManager.registerAccount(univAccount);
    }
}
