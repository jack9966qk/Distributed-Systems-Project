package EZShare.unitTest;

import EZShare.Resource;
import EZShare.ResourceStorage;
import org.junit.jupiter.api.*;

import java.util.ArrayList;

/**
 * Created by Zheping on 2017/4/19.
 */
class ResourceStorageTest {

    private String[] testTagsJack = {"Male", "Single"};
    private String[] testTagsLeo = {"Male", "Double"};
    private String[] testTagsSingle = {"Single"};
    private String[] testTagsDouble = {"Double"};

    private Resource testResourceLeo = new Resource("Leo", "A good man", testTagsLeo, "goodManLeo", "nTr", "Noelle", "");
    private Resource testResourceJack = new Resource("Jack", "A geek man", testTagsJack, "geekManJack", "nTr", "Jack", "");

    private ResourceStorage storage = new ResourceStorage();

    @Test
    void searchWithTemplate() {
        storage.add(testResourceJack);
        storage.add(testResourceLeo);

        Resource emptyTemplate = new Resource("", "", new String[0], "", "", "", "");
        Resource channelTemplate = new Resource("", "", new String[0], "", "nTr", "", "");
        Resource ownerTemplateLeo = new Resource("", "", new String[0], "", "nTr", "Noelle", "");
        Resource ownerTemplateJack = new Resource("", "", new String[0], "", "nTr", "Jack", "");
        Resource tagsTemplateJack = new Resource("", "",testTagsSingle, "", "nTr", "", "");
        Resource tagsTemplateLeo = new Resource("", "",testTagsDouble, "", "nTr", "", "");
        Resource UriTemplateLeo = new Resource("", "", new String[0], "goodManLeo", "nTr", "", "");
        Resource UriTemplateJack = new Resource("", "", new String[0], "geekManJack", "nTr", "", "");
        Resource nameTemplateJack = new Resource("Jac", "", new String[0], "", "nTr", "", "");
        Resource nameTemplateLeo = new Resource("Le", "", new String[0], "", "nTr", "", "");
        Resource descriptionTemplateLeo = new Resource("", "A good man", new String[0], "", "nTr", "", "");
        Resource descriptionTemplateJack = new Resource("", "A geek man", new String[0], "", "nTr", "", "");

        ArrayList<Resource> templates = new ArrayList<Resource>();
        templates.add(channelTemplate);
        templates.add(ownerTemplateJack);
        templates.add(ownerTemplateLeo);
        templates.add(tagsTemplateJack);
        templates.add(tagsTemplateLeo);
        templates.add(UriTemplateJack);
        templates.add(UriTemplateLeo);
        templates.add(nameTemplateJack);
        templates.add(nameTemplateLeo);
        templates.add(descriptionTemplateJack);
        templates.add(descriptionTemplateLeo);

        for (Resource r : templates) {
            Assertions.assertTrue(!storage.searchWithTemplate(r).isEmpty());
        }
    }

    @Test
    void add() {
        Resource jiDong = new Resource("JiDong", "sb", testTagsSingle, "", "nTr", "Ry", "");
        storage.add(jiDong);
        Assertions.assertTrue(storage.containsKey(jiDong));
    }

    @Test
    void remove() {
        storage.remove(testResourceLeo);
        Assertions.assertFalse(storage.containsKey(testResourceLeo));
    }

}