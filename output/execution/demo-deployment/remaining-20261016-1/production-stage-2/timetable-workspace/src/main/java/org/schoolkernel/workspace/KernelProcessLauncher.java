package org.schoolkernel.workspace;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class KernelProcessLauncher {
    public Process start(List<String> arguments) throws IOException {
        return new ProcessBuilder(arguments).start();
    }
}
