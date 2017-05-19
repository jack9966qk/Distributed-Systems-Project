package EZShare.unitTest;

import EZShare.Resource;
import org.junit.jupiter.api.*;

import java.util.ArrayList;


/**
 * Created by Zheping on 2017/4/19.
 */
class ResourceTest {

    @Test
    void matchesTemplate() {
        String[] testTagsJack = {"Male", "Single"};
        String[] testTagsLeo = {"Male", "Double"};
        String[] testTagsSingle = {"Single"};
        String[] testTagsDouble = {"Double"};

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

        ArrayList<Resource> templatesJack = new ArrayList<Resource>();
        ArrayList<Resource> templatesLeo = new ArrayList<Resource>();

        templatesJack.add(channelTemplate);
        templatesJack.add(ownerTemplateJack);
        templatesJack.add(tagsTemplateJack);
        templatesJack.add(UriTemplateJack);
        templatesJack.add(nameTemplateJack);
        templatesJack.add(descriptionTemplateJack);

        templatesLeo.add(channelTemplate);
        templatesLeo.add(ownerTemplateLeo);
        templatesLeo.add(UriTemplateLeo);
        templatesLeo.add(tagsTemplateLeo);
        templatesLeo.add(nameTemplateLeo);
        templatesLeo.add(descriptionTemplateLeo);

        Resource testResourceLeo = new Resource("Leo", "A good man", testTagsLeo, "goodManLeo", "nTr", "Noelle", "" );
        Resource testResourceJack = new Resource("Jack", "A geek man", testTagsJack, "geekManJack", "nTr", "Jack", "" );

        for (Resource r : templatesJack) {
            Assertions.assertTrue(testResourceJack.matchesTemplate(r));
        }

        for (Resource r : templatesLeo) {
            Assertions.assertTrue(testResourceLeo.matchesTemplate(r));
        }

    }

    @Test
    void normalised() {
        Resource emptyTemplate = new Resource("", "", new String[0], "", "", "", "");
        Resource normal = emptyTemplate.normalised();

        Assertions.assertTrue(normal.equals(emptyTemplate));
    }

}