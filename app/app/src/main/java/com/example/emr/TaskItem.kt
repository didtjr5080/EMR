package com.example.emr

data class TaskItem(
    var id: String = "",
    val patient_id: String = "",
    val patient_name: String = "",
    val room_number: String = "",
    val patient_code: String = "",
    val task_name: String = "",
    val description: String = "",
    val scheduled_time: String = "",
    val priority: String = "1",
    val status: String = "pending",
    val assigned_to: String = "",
    val assigned_to_code: String = "",

    val created_at_kst: String = "",
    val updated_at_kst: String = "",
    val completed_at_kst: String = "",
    val completed_by: String = "",
    val completed_by_code: String = "",
    val completed_device_id: String = "",

    val created_at: Any? = null,
    val updated_at: Any? = null,
    val completed_at: Any? = null
)