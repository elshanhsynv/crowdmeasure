package com.example.crowdmeasure.domain.model

data class CallSession(
    val sessionId: String,
    val startedAtUtcMs: Long,
    val endedAtUtcMs: Long?,
    val callType: CallType,
    val callSource: CallSource,
    val sampleIntervalSeconds: Int,
    val sampleCount: Int,
    val endReason: String?,
    val latestSample: CallCellSample?
)

data class CallSessionExport(
    val session: CallSession,
    val samples: List<CallCellSample>
)

data class CallCellSample(
    val id: Long,
    val sessionId: String,
    val sampledAtUtcMs: Long,
    val elapsedMs: Long,
    val cell: CellInfo,
    val rat: String?,
    val nrState: String?,
    val dbm: Int?,
    val rsrpDbm: Int?,
    val rsrqDb: Int?,
    val sinrDb: Int?,
    val pci: Int?,
    val tac: Int?,
    val band: Int?
)
