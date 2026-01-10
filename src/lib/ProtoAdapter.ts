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

function covertMoment(moment: Timestamp): number {
    const deciSeconds = Number(moment.seconds) * 10 + Math.round((moment.nanos ?? 0) / 100_000_000);
    return deciSeconds;
}

export async function listenGraphQueues(
    listener: (pid: bigint, metricType: MetricType, moment: number, bytes: bigint) => void
) {

    const unlisten = await listen<GraphQueues>("graph-queues-updated", (event) => {
        const pid = BigInt(event.payload.pid);
        event.payload.queues.forEach((queue) => {
            const metricType = fromProtoMetricType(queue.metric_type);
            queue.points.forEach((protoPoint) => {
                const moment = covertMoment(protoPoint.moment!);
                const bytes = BigInt(protoPoint.bytes);
                if (bytes < 0n) {
                    console.error(`Bytes in GraphPoint pid ${pid}, metric type ${metricType} must be positive: ${bytes}`);
                } else {
                    listener(pid, metricType, moment, bytes);
                }
            });
        });
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
                const pid = BigInt(proc.pid);
                return [pid, proc];
            }),
        );
        listener(availableJvmProcesses)
    });

    return unlisten;
}