import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

// Пул потоків для обробки задач
class WorkerThreadPool {
    private final ExecutorService executor;

    public WorkerThreadPool(int numThreads) {
        executor = Executors.newFixedThreadPool(numThreads);
    }

    public void submitTask(Runnable task) {
        executor.submit(task);
    }

    public void shutdown() {
        executor.shutdown();
    }
}

// Паралельна обробка колекцій
class ParallelCollectionProcessor {
    private List<Double> numbers;

    public ParallelCollectionProcessor(List<Double> numbers) {
        this.numbers = numbers;
    }

    public double findMin() {
        return numbers.parallelStream().min(Double::compare).orElse(Double.NaN);
    }

    public double findMax() {
        return numbers.parallelStream().max(Double::compare).orElse(Double.NaN);
    }

    public double calculateAverage() {
        return numbers.parallelStream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    public List<Double> filterGreaterThan(double threshold) {
        return numbers.parallelStream().filter(n -> n > threshold).collect(Collectors.toList());
    }
}

// Основний клас
public class Main {
    public static void main(String[] args) {
        WorkerThreadPool workerPool = new WorkerThreadPool(4);
        List<Double> numbers = Arrays.asList(3.5, 7.2, 1.4, 9.8, 5.6, 4.3);
        ParallelCollectionProcessor processor = new ParallelCollectionProcessor(numbers);

        workerPool.submitTask(() -> System.out.println("Мінімум: " + processor.findMin()));
        workerPool.submitTask(() -> System.out.println("Максимум: " + processor.findMax()));
        workerPool.submitTask(() -> System.out.println("Середнє значення: " + processor.calculateAverage()));
        workerPool.submitTask(() -> System.out.println("Числа > 4: " + processor.filterGreaterThan(4)));
        
        workerPool.shutdown();
    }
}