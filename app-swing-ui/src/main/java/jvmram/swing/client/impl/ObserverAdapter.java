package jvmram.swing.client.impl;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.synchronizedList;

class ObserverAdapter<GRPC, UI> implements StreamObserver<GRPC> {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Collection<Consumer<UI>> listeners = synchronizedList(new ArrayList<>());
    private final Function<GRPC, UI> transformer;

    private ObserverAdapter(Function<GRPC, UI> transformer) {
        this.transformer = transformer;
    }

    @Override
    public void onNext(GRPC input) {
        UI transformed = transformer.apply(input);
        listeners.forEach(it -> it.accept(transformed));
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.warn("Error while listening", throwable);
    }

    @Override
    public void onCompleted() {
        LOG.warn("Server disconnected");
    }

    void addListener(Consumer<UI> listener) {
        listeners.add(listener);
    }

    static <GRPC, UI> ObserverAdapter<GRPC, UI> create(Function<GRPC, UI> transformer) {
        return new ObserverAdapter<>(transformer);
    }
}
