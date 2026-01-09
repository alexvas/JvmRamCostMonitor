import type { MetricType, GraphPoint as ProtoGraphPoint } from "$lib/generated/proto/protocol";
import type { Timestamp } from "./generated/google/protobuf/timestamp";

/** Лимит точек на каждую метрику для каждого процесса */
const SIZE_LIMIT = 10_000;

export interface GraphKey {
  type: MetricType;
  pid: bigint;
}

export interface GraphPoint {
  moment: Date;
  bytes: bigint;
}

export interface MetricGraph {
  pid: bigint;
  metricType: MetricType;
  points: GraphPoint[];
  minMax: ProcessMinMax;
}

/** Данные по одному процессу: метрики, min/max по времени и байтам */
interface ProcessData {
  /** MetricType -> (timestamp_ms -> bytes), отсортировано по времени */
  metrics: Map<MetricType, Map<number, bigint>>;
  minMoment: Date;
  maxMoment: Date;
  maxBytes: bigint;
}

export interface ProcessMinMax {
  minMoment: Date;
  maxMoment: Date;
  maxBytes: bigint;
}

export class GraphStore {
  /** pid -> ProcessData */
  private data = new Map<bigint, ProcessData>();

  /** Все ключи графиков */
  keys(): GraphKey[] {
    const result: GraphKey[] = [];
    for (const [pid, processData] of this.data) {
      for (const type of processData.metrics.keys()) {
        result.push({ type, pid });
      }
    }
    return result;
  }

  /** Все графики для конкретного процесса */
  getGraphs(pid: bigint): MetricGraph[] {
    const processData = this.data.get(pid);
    if (!processData) return [];

    const minMax: ProcessMinMax = {
      minMoment: processData.minMoment,
      maxMoment: processData.maxMoment,
      maxBytes: processData.maxBytes,
    };

    const result: MetricGraph[] = [];
    for (const [metricType, pointsMap] of processData.metrics) {
      // Map гарантирует порядок вставки — сортировка не нужна,
      // если данные добавляются в хронологическом порядке
      const points: GraphPoint[] = [];
      for (const [ts, bytes] of pointsMap) {
        points.push({ moment: new Date(ts), bytes });
      }

      result.push({ pid, metricType, points, minMax });
    }

    return result;
  }

  /** Проверка на пустоту */
  isEmpty(): boolean {
    if (this.data.size === 0) return true;
    for (const processData of this.data.values()) {
      for (const points of processData.metrics.values()) {
        if (points.size > 0) return false;
      }
    }
    return true;
  }

  /** Проверка есть ли данные для конкретного процесса */
  hasProcess(pid: bigint): boolean {
    return this.data.has(pid);
  }

  /** Получить min/max для конкретного процесса */
  getProcessMinMax(pid: bigint): ProcessMinMax | null {
    const processData = this.data.get(pid);
    if (!processData) return null;
    return {
      minMoment: processData.minMoment,
      maxMoment: processData.maxMoment,
      maxBytes: processData.maxBytes,
    };
  }

  /** Добавить точки для метрики процесса */
  put(pid: bigint, metricType: MetricType, points: ProtoGraphPoint[]): void {
    for (const point of points) {
      this.addPoint(pid, metricType, point);
    }
  }

  /** Удалить все данные для конкретного процесса */
  deleteProcess(pid: bigint): boolean {
    return this.data.delete(pid);
  }

  /** Удалить все данные */
  clear(): void {
    this.data.clear();
  }

  private covertMoment(moment: Timestamp): Date {
    const millis = Number(moment.seconds) * 1000 + (moment.nanos ?? 0) / 1_000_000;
    return new Date(millis);
  }

  private addPoint(pid: bigint, metricType: MetricType, input: ProtoGraphPoint): void {
    if (input.moment === undefined) {
      throw new Error("GraphPoint moment is undefined");
    }
    if (input.bytes < 0n) {
      throw new Error(`Bytes in GraphPoint must be positive: ${input.bytes}`);
    }

    // input.moment приходит как Timestamp {seconds, nanos} или Date
    const moment = this.covertMoment(input.moment);

    let processData = this.data.get(pid);
    if (!processData) {
      processData = {
        metrics: new Map(),
        minMoment: new Date(8640000000000000), // Date.MAX
        maxMoment: new Date(-8640000000000000), // Date.MIN
        maxBytes: -1n,
      };
      this.data.set(pid, processData);
    }

    let points = processData.metrics.get(metricType);
    if (!points) {
      points = new Map();
      processData.metrics.set(metricType, points);
    }

    const ts = moment.getTime();

    // Если такой момент уже есть — пропускаем
    if (points.has(ts)) {
      return;
    }

    points.set(ts, input.bytes);

    // Обновляем min/max по времени для процесса
    if (moment < processData.minMoment) {
      processData.minMoment = moment;
    }
    if (moment > processData.maxMoment) {
      processData.maxMoment = moment;
    }

    // Обновляем max bytes для процесса
    if (input.bytes > processData.maxBytes) {
      processData.maxBytes = input.bytes;
    }

    // Trim если превышен лимит
    this.trimIfNeeded(processData, points);
  }

  private trimIfNeeded(processData: ProcessData, points: Map<number, bigint>): void {
    const toRemoveCount = points.size - SIZE_LIMIT;
    if (toRemoveCount <= 0) {
      return;
    }

    // Map хранит элементы в порядке вставки — первые элементы самые старые
    let removed = 0;
    let maxRemovedBytes = -1n;
    for (const [ts, bytes] of points) {
      if (removed >= toRemoveCount) break;
      if (bytes > maxRemovedBytes) {
        maxRemovedBytes = bytes;
      }
      points.delete(ts);
      removed++;
    }

    // Пересчитываем minMoment для всего процесса
    this.recalculateMinMoment(processData);

    // Пересчитываем maxBytes если удалённые точки содержали максимум
    if (maxRemovedBytes >= processData.maxBytes) {
      this.recalculateMaxBytes(processData);
    }
  }

  private recalculateMinMoment(processData: ProcessData): void {
    let minTs = Number.MAX_SAFE_INTEGER;
    for (const points of processData.metrics.values()) {
      // Map хранит элементы в хронологическом порядке — первый ключ минимальный
      const firstTs = points.keys().next().value;
      if (firstTs !== undefined && firstTs < minTs) {
        minTs = firstTs;
      }
    }
    if (minTs !== Number.MAX_SAFE_INTEGER) {
      processData.minMoment = new Date(minTs);
    }
  }

  private recalculateMaxBytes(processData: ProcessData): void {
    let maxBytes = -1n;
    for (const points of processData.metrics.values()) {
      for (const bytes of points.values()) {
        if (bytes > maxBytes) {
          maxBytes = bytes;
        }
      }
    }
    processData.maxBytes = maxBytes;
  }
}

/** Singleton instance */
export const graphStore = new GraphStore();
