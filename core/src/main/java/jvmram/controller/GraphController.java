package jvmram.controller;

import jvmram.controller.impl.GraphControllerImpl;

public interface GraphController {

    static GraphController getInstance() {
        return GraphControllerImpl.INSTANCE;
    }

    void update();

    void addRenderer(GraphRenderer renderer);
}
