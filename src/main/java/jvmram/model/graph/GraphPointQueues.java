package jvmram.model.graph;

import jvmram.metrics.GraphPoint;
import jvmram.model.graph.UpdateResult.Exceed;
import jvmram.model.metrics.MetricType;

import java.time.Instant;
import java.util.Collection;
import java.util.NoSuchElementException;

public interface GraphPointQueues {

    /**
     * Добавить точку измерения потребления определённого типа памяти для определённого PID в свою очередь.
     *
     * @param pid        - к какому процессу относится измерение
     * @param metricType - тип памяти
     * @param graphPoint - числовое значение (время / количество потребляемых байт)
     * @return
     */
    UpdateResult add(long pid, MetricType metricType, GraphPoint graphPoint);

    void handleExceed(Collection<Exceed> exceeds);

    /**
     * Какие очереди (графики) по типу памяти и номеру процесса содержит модель.
     *
     * @return ключи-указатели на графики
     */
    Collection<GraphKey> keys();

    /**
     * Отдаёт очередь (график) данных для определённого типа памяти и нужного PID.
     *
     * @param key ключ-указатель на график
     * @return очередь
     */
    Collection<GraphPoint> getPoints(GraphKey key);

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

    static GraphPointQueues getInstance() {
        return GraphPointQueuesImpl.INSTANCE;
    }
}
