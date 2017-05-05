package EzShare.unitTest;

public class TestCase {
    public String[] getClientArgs() {
        return clientArgs;
    }

    public String getExpectedRequestJson() {
        return expectedRequestJson;
    }

    public String getExpectedResponseJson() {
        return expectedResponseJson;
    }

    String[] clientArgs;
    String expectedRequestJson;
    String expectedResponseJson;

    public TestCase(String[] clientArgs, String expectedRequestJson, String expectedResponseJson) {
        this.clientArgs = clientArgs;
        this.expectedRequestJson = expectedRequestJson;
        this.expectedResponseJson = expectedResponseJson;
    }
}
