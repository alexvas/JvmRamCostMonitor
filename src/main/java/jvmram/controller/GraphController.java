package jvmram.controller;

public interface GraphController {

    static GraphController getInstance() {
        return GraphControllerImpl.INSTANCE;
    }

    void update();

    void addRenderer(GraphRenderer renderer);
}
