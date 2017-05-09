package EzShare;

import java.util.ArrayList;

/**
 * Singleton class to store and generate id for subscription.
 *
 * Created by Zheping on 2017/5/9.
 */
public class IdGenerator {

    private static ArrayList<String> idStorage;
    private static IdGenerator idGenerator;
    private static Integer currentNumber;

    private IdGenerator() {
        this.idStorage = new ArrayList<String>();
        currentNumber = 0;
    }

    public static IdGenerator getIdGeneartor() {

        if (idGenerator == null) {
            idGenerator = new IdGenerator();
            return idGenerator;
        }
        return idGenerator;
    }

    public static String generateId() {
        //TODO generate id following some logic
        String id = currentNumber.toString();

        while (idStorage.contains(id)) {
            currentNumber++;
            id = currentNumber.toString();
        }

        idStorage.add(id);
        currentNumber++;

        return id;
    }

}
