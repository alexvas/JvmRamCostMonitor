package jvmram.swing.client;

import jvmram.model.graph.GraphKey;
import jvmram.model.graph.GraphPoint;
import jvmram.model.graph.GraphPointQueues;

import java.time.Instant;
import java.util.Collection;
import java.util.NoSuchElementException;

public interface GraphPointQueuesMiniMax extends GraphPointQueues {

    /**
     * Имеются ли какие-либо данные внутри хранилища или оно пустое?
     *
     * @return пустое или нет.
     */
    boolean isEmpty();

    /**
     * Левая граница данных. Вызывать при непустых данных.
     *
     * @return минимальное время для всех измеренных точек
     * @throws NoSuchElementException если данных нет.
     */
    Instant minMoment();

    /**
     * Правая граница данных. Вызывать при непустых данных.
     *
     * @return максимальное время для всех измеренных точек
     * @throws NoSuchElementException если данных нет.
     */
    Instant maxMoment();

    /**
     * Верхняя граница данных. Вызывать при непустых данных.
     *
     * @return максимальное количество байт для всех измеренных точек
     * @throws NoSuchElementException если данных нет.
     */
    long maxBytes();
}
