package main.example.com;

import main.DemoClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MainApplicationTest {

    @Test
    public void getCorrectMessage() {
        DemoClass demoClass = new DemoClass();
        Assertions.assertEquals("first message", demoClass.getMessage());
    }
}
