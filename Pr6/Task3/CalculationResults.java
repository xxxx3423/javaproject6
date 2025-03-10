import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

interface MathOperation extends Serializable {
    double calculate(double a, double b);
    String getOperationName();
    String formatResult(double a, double b);
}

class Addition implements MathOperation {
    private static final long serialVersionUID = 1L;
    
    public double calculate(double a, double b) {
        return a + b;
    }
    
    public String getOperationName() {
        return "Додавання";
    }
    
    public String formatResult(double a, double b) {
        return String.format("| %-10s | %8.2f | %8.2f | %8.2f |", getOperationName(), a, b, calculate(a, b));
    }
}

class Multiplication implements MathOperation {
    private static final long serialVersionUID = 1L;
    
    public double calculate(double a, double b) {
        return a * b;
    }
    
    public String getOperationName() {
        return "Множення";
    }
    
    public String formatResult(double a, double b) {
        return String.format("| %-10s | %8.2f | %8.2f | %8.2f |", getOperationName(), a, b, calculate(a, b));
    }
}

class CalculationResultsManager {
    private static CalculationResultsManager instance;
    private List<String> results = Collections.synchronizedList(new ArrayList<>());
    private Stack<String> undoStack = new Stack<>();
    
    private CalculationResultsManager() {}
    
    public static CalculationResultsManager getInstance() {
        if (instance == null) {
            instance = new CalculationResultsManager();
        }
        return instance;
    }
    
    public void addResult(String result) {
        results.add(result);
        undoStack.push(result);
    }
    
    public void undoLastOperation() {
        if (!undoStack.isEmpty()) {
            results.remove(undoStack.pop());
            System.out.println("Останню операцію скасовано.");
        } else {
            System.out.println("Немає операцій для скасування.");
        }
    }
    
    public void displayResults() {
        results.forEach(System.out::println);
    }
    
    public List<Double> getResultValues() {
        return results.parallelStream()
                .map(s -> Double.parseDouble(s.replaceAll("[^0-9.]+", "").trim()))
                .collect(Collectors.toList());
    }
}

class WorkerThread extends Thread {
    private BlockingQueue<Runnable> taskQueue;
    private volatile boolean isStopped = false;
    
    public WorkerThread() {
        taskQueue = new LinkedBlockingQueue<>();
        start();
    }
    
    public void addTask(Runnable task) {
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void run() {
        while (!isStopped || !taskQueue.isEmpty()) {
            try {
                Runnable task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    task.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void stopWorker() {
        isStopped = true;
    }
}

public class CalculationResults {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        CalculationResultsManager manager = CalculationResultsManager.getInstance();
        WorkerThread worker = new WorkerThread();
        
        while (true) {
            System.out.println("Виберіть операцію: 1) Додавання 2) Множення 3) Показати результати 4) Скасувати 5) Аналіз 6) Вийти");
            int choice = scanner.nextInt();
            
            if (choice == 6) {
                worker.stopWorker();
                try {
                    worker.join(); // Чекаємо завершення WorkerThread
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                scanner.close();
                System.out.println("Програма завершена.");
                System.exit(0);
            }
            
            if (choice == 3) {
                worker.addTask(manager::displayResults);
                continue;
            }
            
            if (choice == 4) {
                worker.addTask(manager::undoLastOperation);
                continue;
            }
            
            if (choice == 5) {
                worker.addTask(() -> {
                    List<Double> values = manager.getResultValues();
                    System.out.println("Мінімум: " + values.parallelStream().min(Double::compare).orElse(Double.NaN));
                    System.out.println("Максимум: " + values.parallelStream().max(Double::compare).orElse(Double.NaN));
                    System.out.println("Середнє: " + values.parallelStream().mapToDouble(d -> d).average().orElse(Double.NaN));
                });
                continue;
            }
            
            System.out.println("Введіть два числа:");
            double a = scanner.nextDouble();
            double b = scanner.nextDouble();
            
            MathOperation operation = (choice == 1) ? new Addition() : new Multiplication();
            String result = operation.formatResult(a, b);
            
            worker.addTask(() -> manager.addResult(result));
            worker.addTask(() -> System.out.println("Результат: \n" + result));
        }
    }
}