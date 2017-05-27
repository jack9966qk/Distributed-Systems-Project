package EZShare;

import java.util.ArrayList;

/**
 * Singleton class to store and generate id for subscription.
 * Created by Zheping on 2017/5/9.
 */
public class IdGenerator {

    private static ArrayList<String> idStorage;
    private static IdGenerator idGenerator;
    private Integer currentNumber;

    /**
     * Constructor to create a new id storage, and set the current number to 0.
     */
    private IdGenerator() {
        idStorage = new ArrayList<>();
        currentNumber = 0;
    }

    /**
     * Get an instance of IdGenerator.
     *
     * @return an instance of IdGenerator
     */
    public static IdGenerator getIdGenerator() {

        // if the instance does not exist, create a new instance
        if (idGenerator == null) {
            idGenerator = new IdGenerator();
        }

        return idGenerator;
    }

    /**
     * Generate an id for subscription.
     *
     * @return an id in String type
     */
    public String generateId() {

        String id = currentNumber.toString();

        // while the id is existed in the storage
        while (idStorage.contains(id)) {
            // increment the id
            currentNumber++;
            id = currentNumber.toString();
        }

        // add the id into the storage
        idStorage.add(id);
        currentNumber++;

        return id;
    }

}
