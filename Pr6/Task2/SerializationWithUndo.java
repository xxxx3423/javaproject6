import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

interface Displayable {
    void display();
}

interface Command {
    void execute();
    void undo();
}

class AddPersonCommand implements Command {
    private List<Person> personsList;
    private Person person;

    public AddPersonCommand(List<Person> personsList, Person person) {
        this.personsList = personsList;
        this.person = person;
    }

    public void execute() {
        personsList.add(person);
        CommandHistory.getInstance().addCommand(this);
    }

    public void undo() {
        personsList.remove(person);
    }
}

class CommandHistory {
    private static CommandHistory instance;
    private Stack<Command> history = new Stack<>();

    private CommandHistory() {}

    public static CommandHistory getInstance() {
        if (instance == null) {
            instance = new CommandHistory();
        }
        return instance;
    }

    public void addCommand(Command command) {
        history.push(command);
    }

    public void undoLastCommand() {
        if (!history.isEmpty()) {
            history.pop().undo();
        }
    }
}

class Person implements Serializable, Displayable {
    private static final long serialVersionUID = 1L;
    protected String name;
    protected int age;
    protected transient String password;

    public Person(String name, int age, String password) {
        this.name = name;
        this.age = age;
        this.password = password;
    }

    public int getAge() {
        return age;
    }

    public void display() {
        System.out.printf("| %-15s | %-3d |\n", name, age);
    }
}

class WorkerThread extends Thread {
    private BlockingQueue<Runnable> taskQueue;
    private volatile boolean isStopped = false;

    public WorkerThread(BlockingQueue<Runnable> taskQueue) {
        this.taskQueue = taskQueue;
    }

    public void run() {
        while (!isStopped) {
            try {
                Runnable task = taskQueue.take();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stopWorker() {
        isStopped = true;
        this.interrupt();
    }
}

public class SerializationWithUndo {
    private static final String FILE_NAME = "persons.ser";
    private static List<Person> personsList = new ArrayList<>();
    private static BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private static WorkerThread worker = new WorkerThread(taskQueue);
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        worker.start();
        
        while (true) {
            System.out.println("1. Додати особу\n2. Скасувати\n3. Зберегти\n4. Завантажити\n5. Дисплей\n6. Аналіз віку\n7. Вихід");
            System.out.print("Виберіть варіант: ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            
            switch (choice) {
                case 1:
                    addPerson();
                    break;
                case 2:
                    CommandHistory.getInstance().undoLastCommand();
                    break;
                case 3:
                    saveToFile();
                    break;
                case 4:
                    loadFromFile();
                    break;
                case 5:
                    displayPersons();
                    break;
                case 6:
                    analyzeAge();
                    break;
                case 7:
                    shutdownWorker();
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void addPerson() {
        System.out.print("Enter name: ");
        String name = scanner.nextLine();
        System.out.print("Enter age: ");
        int age = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        Person person = new Person(name, age, password);
        Command command = new AddPersonCommand(personsList, person);
        command.execute();
    }

    private static void saveToFile() {
        taskQueue.add(() -> {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
                oos.writeObject(personsList);
                System.out.println("Дані збережені.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void loadFromFile() {
        taskQueue.add(() -> {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
                personsList = (List<Person>) ois.readObject();
                System.out.println("Дані завантажені.");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    private static void displayPersons() {
        taskQueue.add(() -> {
            System.out.println("--------------------------------------");
            personsList.forEach(Person::display);
            System.out.println("--------------------------------------");
        });
    }

    private static void analyzeAge() {
        taskQueue.add(() -> {
            OptionalInt minAge = personsList.stream().mapToInt(Person::getAge).min();
            OptionalInt maxAge = personsList.stream().mapToInt(Person::getAge).max();
            OptionalDouble avgAge = personsList.stream().mapToInt(Person::getAge).average();

            System.out.println("Статистика віку:");
            System.out.println("Мінімальний вік: " + (minAge.isPresent() ? minAge.getAsInt() : "Немає даних"));
            System.out.println("Максимальний вік: " + (maxAge.isPresent() ? maxAge.getAsInt() : "Немає даних"));
            System.out.println("Середній вік: " + (avgAge.isPresent() ? avgAge.getAsDouble() : "Немає даних"));
        });
    }

    private static void shutdownWorker() {
        worker.stopWorker();
        System.out.println("Worker thread зупинено.");
    }
}