package com.allstars.project1.unitTest;

import com.allstars.project1.Server;
import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Zheping on 2017/4/25.
 */
class ServerTest {

    @BeforeEach
    void setUp() {
    }

    @Disabled
    @Test
    void isRunning() {
    }

    @Disabled
    @Test
    void stop() {
    }

    @Disabled
    @Test
    void startServer() {
    }

    @Disabled
    @Test
    void getOptions() {
    }

    @Test
    void main() {
        String[] args = {"java", "Server.java", "-advertisedhostname", "localhost", "-port", "2333", "-secret", "JackBigLeg"};

        Server.main(args);
    }

}