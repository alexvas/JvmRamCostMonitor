/** Собственный enum для типов метрик (без UNRECOGNIZED) */
export enum MetricType {
  RSS = 0,
  PSS = 1,
  USS = 2,
  WS = 3,
  PB = 4,
  HEAP_USED = 5,
  HEAP_COMMITTED = 6,
  NMT_USED = 7,
  NMT_COMMITTED = 8,
}

/** Лимит точек на каждую метрику для каждого процесса */
const SIZE_LIMIT = 10_000;

export interface GraphKey {
  type: MetricType;
  pid: bigint;
}

export interface GraphPoint {
  moment: number;
  bytes: bigint;
}

export interface MetricGraph {
  pid: bigint;
  metricType: MetricType;
  points: GraphPoint[];
}

/** Данные по одному процессу: метрики, min/max по времени и байтам */
interface ProcessDatum {
  /** MetricType -> GraphPoint[], отсортировано по времени */
  points: Map<MetricType, GraphPoint[]>;
  timestamps: Map<MetricType, Set<number>>;
  minMax: ProcessMinMax;
}

export interface ProcessMinMax {
  minMoment: number;
  maxMoment: number;
  maxBytes: bigint;
}

export class GraphStore {

  /** pid -> ProcessData */
  private prosessData = new Map<bigint, ProcessDatum>();

  hasGraphDataForProcess(pid: bigint): boolean {
    return (this.prosessData.get(pid)?.timestamps?.size ?? 0) > 0;
  }

  /** Все графики для конкретного процесса */
  getGraphs(pid: bigint): Iterable<MetricGraph> {
    const processData = this.prosessData.get(pid);
    if (!processData) return [];

    return processData.points
      .entries()
      .map(([metricType, points]) => ({ pid, metricType, points }));
  }

  /** Проверка есть ли данные для конкретного процесса */
  hasProcess(pid: bigint): boolean {
    return this.prosessData.has(pid);
  }

  /** Получить min/max для конкретного процесса */
  getProcessMinMax(pid: bigint): ProcessMinMax | null {
    return this.prosessData.get(pid)?.minMax ?? null;
  }

  /** Удалить все данные для конкретного процесса */
  deleteProcess(pid: bigint): boolean {
    return this.prosessData.delete(pid);
  }

  /** Удалить все данные */
  clear(): void {
    this.prosessData.clear();
  }


  /** Добавить точки для метрики процесса */
  put(pid: bigint, metricType: MetricType, moment: number, bytes: bigint): void {

    let processDatum = this.prosessData.get(pid);
    if (!processDatum) {
      const minMax = {
        minMoment: Number.MAX_SAFE_INTEGER,
        maxMoment: Number.MIN_SAFE_INTEGER,
        maxBytes: -1n,
      };

      processDatum = {
        points: new Map(),
        timestamps: new Map(),
        minMax: minMax,
      };
      this.prosessData.set(pid, processDatum);
    }

    let points = processDatum.points.get(metricType);
    let timestamps = processDatum.timestamps.get(metricType);
    if (!points) {
      points = [];
      processDatum.points.set(metricType, points);
      timestamps = new Set();
      processDatum.timestamps.set(metricType, timestamps);
    }

    // Если такой момент уже есть — пропускаем
    if (timestamps!.has(moment)) {
      return;
    }
    timestamps!.add(moment);

    points.push({ moment: moment, bytes: bytes });

    let minMax = processDatum.minMax;
    // Обновляем min/max по времени для процесса
    if (minMax.minMoment > moment) {
      minMax.minMoment = moment;
    }
    if (minMax.maxMoment < moment) {
      minMax.maxMoment = moment;
    }

    // Обновляем max bytes для процесса
    if (minMax.maxBytes < bytes) {
      minMax.maxBytes = bytes;
    }

    // Trim если превышен лимит
    this.trimIfNeeded(processDatum, points, timestamps!);
  }

  private trimIfNeeded(processDatum: ProcessDatum, points: GraphPoint[], timestamps: Set<number>): void {
    const toRemoveCount = points.length - SIZE_LIMIT;
    if (toRemoveCount <= 0) {
      return;
    }

    // Array хранит элементы в порядке вставки — первые элементы самые старые
    let maxRemovedBytes = -1n;
    let removed = points.splice(0, toRemoveCount);
    removed.forEach(point => {
      timestamps.delete(point.moment);
      if (maxRemovedBytes < point.bytes) {
        maxRemovedBytes = point.bytes;
      }
    });

    const firstMomentOfTrimmedPoints = points[0].moment;

    // Пересчитываем minMoment для всего процесса
    this.recalculateMinMoment(processDatum, firstMomentOfTrimmedPoints);

    // Пересчитываем maxBytes если удалённые точки содержали максимум
    if (maxRemovedBytes >= processDatum.minMax.maxBytes) {
      this.recalculateMaxBytes(processDatum);
    }
  }

  private recalculateMinMoment(processData: ProcessDatum, newMinMoment: number): void {
    processData.minMax.minMoment = newMinMoment;
    for (const metricType of processData.points.keys()) {
      const points = processData.points.get(metricType);
      const timestamps = processData.timestamps.get(metricType);
      while (points && points.length > 0 && points[0].moment < newMinMoment) {
        const removed = points.shift();
        timestamps!.delete(removed!.moment);
      }
    }
  }

  private recalculateMaxBytes(processData: ProcessDatum): void {
    let maxBytes = -1n;
    for (const points of processData.points.values()) {
      for (const point of points) {
        if (maxBytes < point.bytes) {
          maxBytes = point.bytes;
        }
      }
    }
    processData.minMax.maxBytes = maxBytes;
  }
}

/** Singleton instance */
export const graphStore = new GraphStore();
