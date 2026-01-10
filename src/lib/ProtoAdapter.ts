import {
    MetricType as ProtoMetricType,
    SetVisibleRequest,
    SetInvisibleRequest,
    ApplicableMetricsResponse,
    type GraphPoint as ProtoGraphPoint,
    type GraphQueues,
    type JvmProcessListResponse
} from "$lib/generated/proto/protocol";
import type { Timestamp } from "$lib/generated/google/protobuf/timestamp";
import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import type { ProcInfo } from "./ProcHandle";
import { MetricType, type GraphPoint } from "./GraphStore";

/** Преобразование из protobuf MetricType в наш MetricType */
function fromProtoMetricType(protoType: ProtoMetricType): MetricType {
    if (protoType === ProtoMetricType.UNRECOGNIZED) {
        throw new Error(`UNRECOGNIZED MetricType is not supported: ${protoType}`);
    }
    return protoType as MetricType;
}

/** Преобразование из нашего MetricType в protobuf MetricType */
function toProtoMetricType(metricType: MetricType): ProtoMetricType {
    return metricType as ProtoMetricType;
}

export async function setVisible(mt: MetricType) {
    const protoType = toProtoMetricType(mt);
    const request = SetVisibleRequest.create({ metric_type: protoType });
    await invoke("set_visible", { request });
}

export async function setInvisible(mt: MetricType) {
    const protoType = toProtoMetricType(mt);
    const request = SetInvisibleRequest.create({ metric_type: protoType });
    await invoke("set_invisible", { request });
}


export async function getApplicableMetrics() {
    const response = await invoke<ApplicableMetricsResponse>(
        "get_applicable_metrics",
    );
    console.log("get applicable metrics response", response);
    return response.types.map(fromProtoMetricType);
}

function covertMoment(moment: Timestamp): Date {
    const millis = Number(moment.seconds) * 1000 + (moment.nanos ?? 0) / 1_000_000;
    return new Date(millis);
}

function fromProtoGraphPoint(input: ProtoGraphPoint): GraphPoint {
    if (input.moment === undefined) {
        throw new Error("GraphPoint moment is undefined");
    }
    if (input.bytes < 0n) {
        throw new Error(`Bytes in GraphPoint must be positive: ${input.bytes}`);
    }

    const moment = covertMoment(input.moment);

    return {
        moment: moment,
        bytes: BigInt(input.bytes)
    };
}

export async function listenGraphQueues(listener: (pid: bigint, metricType: MetricType, points: GraphPoint[]) => void) {

    const unlisten = await listen<GraphQueues>("graph-queues-updated", (event) => {
        const pid =
            typeof event.payload.pid === "bigint"
                ? event.payload.pid
                : BigInt(event.payload.pid);
        const queues = event.payload.queues;
        for (const queue of queues) {
            const metricType = fromProtoMetricType(queue.metric_type);
            const points = queue.points.map(fromProtoGraphPoint);
            listener(pid, metricType, points);
        }
    });

    return unlisten;
}

export async function listenJvmProcessList(listener: (procInfoMap: Map<bigint, ProcInfo>) => void) {
 
    const unlisten = await listen<JvmProcessListResponse>("available-jvm-processes-updated", (event) => {
        const sortedProcesses = [...event.payload.infos].sort((a, b) => {
          const pidA = typeof a.pid === "bigint" ? a.pid : BigInt(a.pid);
          const pidB = typeof b.pid === "bigint" ? b.pid : BigInt(b.pid);
          if (pidA < pidB) return -1;
          if (pidA > pidB) return 1;
          return 0;
        });
        const availableJvmProcesses = new Map(
          sortedProcesses.map((proc) => {
            const pid = typeof proc.pid === "bigint" ? proc.pid : BigInt(proc.pid);
            return [pid, proc];
          }),
        );
        listener(availableJvmProcesses)
      });
      
      return unlisten;
}