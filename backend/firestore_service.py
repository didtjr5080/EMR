from typing import Optional
from datetime import datetime, timezone, timedelta

from firebase_admin import firestore
from google.cloud.firestore_v1 import FieldFilter

from backend.firebase_config import db


KST = timezone(timedelta(hours=9))


def now_kst_iso() -> str:
    return datetime.now(KST).strftime("%Y-%m-%dT%H:%M:%S")


def server_timestamp():
    return firestore.SERVER_TIMESTAMP


# -------------------------
# Patients
# -------------------------

def create_patient(
    patient_name: str,
    room_number: str,
    patient_code: Optional[str] = None,
):
    doc_ref = db.collection("patients").document()

    data = {
        "id": doc_ref.id,
        "patient_name": patient_name,
        "room_number": room_number,
        "patient_code": patient_code or "",
        "is_active": True,
        "created_at": server_timestamp(),
        "created_at_kst": now_kst_iso(),
    }

    doc_ref.set(data)
    return data


def get_patients():
    docs = (
        db.collection("patients")
        .where(filter=FieldFilter("is_active", "==", True))
        .stream()
    )

    patients = []
    for doc in docs:
        data = doc.to_dict() or {}
        data["id"] = doc.id
        patients.append(data)

    patients.sort(
        key=lambda x: (
            x.get("room_number", ""),
            x.get("patient_code", ""),
            x.get("patient_name", ""),
        )
    )
    return patients


def get_patient(patient_id: str):
    doc = db.collection("patients").document(patient_id).get()
    if not doc.exists:
        return None

    data = doc.to_dict() or {}
    data["id"] = doc.id
    return data


# -------------------------
# Tasks
# -------------------------

def create_task(
    patient_id: str,
    task_name: str,
    description: str = "",
    scheduled_time: str = "",
    priority: str = "1",
    assigned_to: str = "",
    assigned_to_code: str = "",
):
    patient = get_patient(patient_id)
    if not patient:
        raise ValueError("Patient not found")

    doc_ref = db.collection("tasks").document()

    data = {
        "id": doc_ref.id,

        "patient_id": patient_id,
        "patient_name": patient.get("patient_name", ""),
        "room_number": patient.get("room_number", ""),
        "patient_code": patient.get("patient_code", ""),

        "task_name": task_name,
        "description": description or "",
        "scheduled_time": scheduled_time or "",

        "priority": priority or "1",
        "status": "pending",
        "assigned_to": assigned_to or "",
        "assigned_to_code": assigned_to_code or "",

        "created_at": server_timestamp(),
        "updated_at": server_timestamp(),
        "created_at_kst": now_kst_iso(),
        "updated_at_kst": now_kst_iso(),

        "completed_at": None,
        "completed_at_kst": "",
        "completed_by": "",
        "completed_by_code": "",
        "completed_device_id": "",
    }

    doc_ref.set(data)
    return data


def get_tasks(status: Optional[str] = None):
    query = db.collection("tasks")

    if status:
        query = query.where(filter=FieldFilter("status", "==", status))

    docs = query.stream()

    tasks = []
    for doc in docs:
        data = doc.to_dict() or {}

        tasks.append({
            "id": doc.id,
            "patient_id": data.get("patient_id", ""),
            "patient_name": data.get("patient_name", ""),
            "room_number": data.get("room_number", ""),
            "patient_code": data.get("patient_code", ""),
            "task_name": data.get("task_name", ""),
            "description": data.get("description", ""),
            "scheduled_time": data.get("scheduled_time", ""),
            "priority": data.get("priority", ""),
            "status": data.get("status", "pending"),
            "assigned_to": data.get("assigned_to", ""),
            "assigned_to_code": data.get("assigned_to_code", ""),
            "completed_at_kst": data.get("completed_at_kst", ""),
            "completed_by": data.get("completed_by", ""),
            "completed_by_code": data.get("completed_by_code", ""),
            "completed_device_id": data.get("completed_device_id", ""),
        })

    def sort_key(item):
        status_order = 0 if item.get("status") == "pending" else 1
        room = item.get("room_number") or ""
        patient_code = item.get("patient_code") or ""
        task_order = item.get("priority") or "999"

        try:
            task_order = int(task_order)
        except (ValueError, TypeError):
            task_order = 999

        return (status_order, room, patient_code, task_order)

    tasks.sort(key=sort_key)
    return tasks


def get_task(task_id: str):
    doc = db.collection("tasks").document(task_id).get()
    if not doc.exists:
        return None

    data = doc.to_dict() or {}
    data["id"] = doc.id
    return data


def complete_task(
    task_id: str,
    completed_by: str = "",
    device_id: str = "",
    note: str = "",
):
    task_ref = db.collection("tasks").document(task_id)
    task_doc = task_ref.get()

    if not task_doc.exists:
        return {
            "success": False,
            "message": "Task not found",
            "task_id": task_id,
        }

    task = task_doc.to_dict() or {}
    task["id"] = task_doc.id

    if task.get("status") == "completed":
        return {
            "success": True,
            "message": "이미 완료된 업무입니다.",
            "task_id": task_id,
            "status": "completed",
            "completed_at_kst": task.get("completed_at_kst", ""),
        }

    completed_at_kst = now_kst_iso()

    task_ref.update(
        {
            "status": "completed",
            "updated_at": server_timestamp(),
            "updated_at_kst": completed_at_kst,
            "completed_at": server_timestamp(),
            "completed_at_kst": completed_at_kst,
            "completed_by": completed_by or "",
            "completed_device_id": device_id or "",
        }
    )

    log_ref = db.collection("task_logs").document()

    log_data = {
        "id": log_ref.id,

        "task_id": task_id,
        "patient_id": task.get("patient_id", ""),
        "patient_name": task.get("patient_name", ""),
        "room_number": task.get("room_number", ""),
        "patient_code": task.get("patient_code", ""),

        "task_name": task.get("task_name", ""),
        "description": task.get("description", ""),
        "scheduled_time": task.get("scheduled_time", ""),
        "priority": task.get("priority", ""),

        "assigned_to": task.get("assigned_to", ""),
        "assigned_to_code": task.get("assigned_to_code", ""),

        "action": "complete",
        "completed_at": server_timestamp(),
        "completed_at_kst": completed_at_kst,
        "completed_by": completed_by or "",
        "completed_by_code": "",
        "device_id": device_id or "",
        "note": note or "",
    }

    log_ref.set(log_data)

    return {
        "success": True,
        "message": "처치완료 시간이 Firebase 서버 기준으로 기록되었습니다.",
        "task_id": task_id,
        "status": "completed",
        "completed_at_kst": completed_at_kst,
    }


def delete_task(task_id: str):
    db.collection("tasks").document(task_id).delete()
    return True


# -------------------------
# Logs
# -------------------------

def get_task_logs():
    docs = db.collection("task_logs").stream()

    logs = []
    for doc in docs:
        data = doc.to_dict() or {}

        logs.append({
            "id": doc.id,
            "patient_code": data.get("patient_code", ""),
            "room_number": data.get("room_number", ""),
            "patient_name": data.get("patient_name", ""),
            "task_name": data.get("task_name", ""),
            "description": data.get("description", ""),
            "priority": data.get("priority", ""),
            "assigned_to": data.get("assigned_to", ""),
            "assigned_to_code": data.get("assigned_to_code", ""),
            "completed_at_kst": data.get("completed_at_kst", ""),
            "completed_by": data.get("completed_by", ""),
            "completed_by_code": data.get("completed_by_code", ""),
            "device_id": data.get("device_id", ""),
            "note": data.get("note", ""),
        })

    logs.sort(key=lambda x: x.get("completed_at_kst", ""), reverse=True)
    return logs


# -------------------------
# Task Presets
# -------------------------

def create_task_preset(
    task_name: str,
    description: str = "",
    priority: str = "1",
    assigned_to: str = "",
):
    doc_ref = db.collection("task_presets").document()

    data = {
        "id": doc_ref.id,
        "task_name": task_name,
        "description": description or "",
        "priority": priority or "1",
        "assigned_to": assigned_to or "",
        "created_at": server_timestamp(),
        "created_at_kst": now_kst_iso(),
    }

    doc_ref.set(data)
    return data


def get_task_presets():
    docs = db.collection("task_presets").stream()

    presets = []
    for doc in docs:
        data = doc.to_dict() or {}
        data["id"] = doc.id
        presets.append(data)

    def sort_key(item):
        task_order = item.get("priority") or "999"
        try:
            task_order = int(task_order)
        except (ValueError, TypeError):
            task_order = 999

        return (task_order, item.get("task_name", ""))

    presets.sort(key=sort_key)
    return presets


def delete_task_preset(preset_id: str):
    db.collection("task_presets").document(preset_id).delete()
    return True


def seed_default_task_presets():
    existing = get_task_presets()
    if existing:
        return {
            "success": True,
            "message": "이미 자주 쓰는 업무가 존재합니다.",
            "count": len(existing),
        }

    default_presets = [
        {
            "task_name": "활력징후 측정",
            "description": "혈압, 맥박, 호흡수, 체온을 측정합니다.",
            "priority": "1",
            "assigned_to": "",
        },
        {
            "task_name": "항생제 투여",
            "description": "처방된 항생제를 정해진 시간에 투여합니다.",
            "priority": "2",
            "assigned_to": "",
        },
        {
            "task_name": "수액 라인 확인",
            "description": "수액 속도, 라인 폐색 여부, 삽입 부위를 확인합니다.",
            "priority": "3",
            "assigned_to": "",
        },
        {
            "task_name": "혈당 체크",
            "description": "식전 또는 처방 기준에 따라 혈당을 측정합니다.",
            "priority": "4",
            "assigned_to": "",
        },
        {
            "task_name": "투약 전 확인",
            "description": "환자, 약물, 용량, 시간, 경로를 확인합니다.",
            "priority": "5",
            "assigned_to": "",
        },
        {
            "task_name": "격리 폐기물 정리",
            "description": "격리 병실 내 의료 폐기물 상태를 확인하고 정리합니다.",
            "priority": "6",
            "assigned_to": "",
        },
    ]

    for preset in default_presets:
        create_task_preset(**preset)

    return {
        "success": True,
        "message": "기본 자주 쓰는 업무가 생성되었습니다.",
        "count": len(default_presets),
    }


# -------------------------
# Staff / Responsible Users
# -------------------------

def create_staff(
    staff_name: str,
    staff_code: str,
    role: str = "",
    department: str = "",
):
    staff_code = (staff_code or "").strip()
    staff_name = (staff_name or "").strip()

    if not staff_code:
        raise ValueError("staff_code is required")

    if not staff_name:
        raise ValueError("staff_name is required")

    existing = get_staff_by_code(staff_code)
    if existing:
        raise ValueError("이미 존재하는 담당자 코드입니다.")

    doc_ref = db.collection("staff").document()

    data = {
        "id": doc_ref.id,
        "staff_name": staff_name,
        "staff_code": staff_code,
        "role": role or "",
        "department": department or "",
        "is_active": True,
        "created_at": server_timestamp(),
        "created_at_kst": now_kst_iso(),
    }

    doc_ref.set(data)
    return data


def get_staff_list(active_only: bool = True):
    query = db.collection("staff")

    if active_only:
        query = query.where(filter=FieldFilter("is_active", "==", True))

    docs = query.stream()

    staff_list = []
    for doc in docs:
        data = doc.to_dict() or {}
        data["id"] = doc.id
        staff_list.append({
            "id": data.get("id", doc.id),
            "staff_name": data.get("staff_name", ""),
            "staff_code": data.get("staff_code", ""),
            "role": data.get("role", ""),
            "department": data.get("department", ""),
            "is_active": data.get("is_active", True),
            "created_at_kst": data.get("created_at_kst", ""),
        })

    staff_list.sort(
        key=lambda x: (
            x.get("department", ""),
            x.get("staff_name", ""),
            x.get("staff_code", ""),
        )
    )
    return staff_list


def get_staff_by_code(staff_code: str):
    staff_code = (staff_code or "").strip()

    if not staff_code:
        return None

    docs = (
        db.collection("staff")
        .where(filter=FieldFilter("staff_code", "==", staff_code))
        .limit(1)
        .stream()
    )

    for doc in docs:
        data = doc.to_dict() or {}
        data["id"] = doc.id
        return data

    return None


def get_staff(staff_id: str):
    doc = db.collection("staff").document(staff_id).get()

    if not doc.exists:
        return None

    data = doc.to_dict() or {}
    data["id"] = doc.id
    return data


def delete_staff(staff_id: str):
    db.collection("staff").document(staff_id).update({
        "is_active": False,
        "updated_at": server_timestamp(),
        "updated_at_kst": now_kst_iso(),
    })
    return True