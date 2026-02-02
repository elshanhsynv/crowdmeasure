package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransportType { WIFI, CELL, OTHER, NONE }

@Serializable
enum class RecordState { PENDING, READY_TO_UPLOAD, UPLOADED, FAILED }

@Serializable
enum class ProtocolType { HTTP1_1, HTTP2, UNKNOWN }