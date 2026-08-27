from datetime import datetime
from zoneinfo import ZoneInfo
from typing import Optional

from sqlalchemy.orm import Session, joinedload

from backend.models import Patient, Task, TaskLog
from backend.schemas import PatientCreate, TaskCreate, TaskUpdate, CompleteTaskRequest


def now_kst():
    return datetime.now(ZoneInfo("Asia/Seoul"))


def create_patient(db: Session, payload: PatientCreate) -> Patient:
    patient = Patient(
        patient_name=payload.patient_name,
        room_number=payload.room_number,
        patient_code=payload.patient_code,
        created_at=now_kst()
    )
    db.add(patient)
    db.commit()
    db.refresh(patient)
    return patient


def get_patients(db: Session):
    return db.query(Patient).order_by(Patient.room_number.asc(), Patient.id.asc()).all()


def create_task(db: Session, payload: TaskCreate) -> Task:
    task = Task(
        patient_id=payload.patient_id,
        task_name=payload.task_name,
        description=payload.description,
        scheduled_time=payload.scheduled_time,
        priority=payload.priority or "normal",
        status="pending",
        assigned_to=payload.assigned_to,
        created_at=now_kst(),
        updated_at=now_kst()
    )
    db.add(task)
    db.commit()
    db.refresh(task)
    return task


def get_tasks(db: Session, status: Optional[str] = None):
    query = (
        db.query(Task)
        .options(joinedload(Task.patient))
        .order_by(Task.status.asc(), Task.scheduled_time.asc().nullslast(), Task.id.desc())
    )

    if status:
        query = query.filter(Task.status == status)

    return query.all()


def get_task_by_id(db: Session, task_id: int) -> Optional[Task]:
    return (
        db.query(Task)
        .options(joinedload(Task.patient))
        .filter(Task.id == task_id)
        .first()
    )


def update_task(db: Session, task_id: int, payload: TaskUpdate) -> Optional[Task]:
    task = get_task_by_id(db, task_id)
    if not task:
        return None

    update_data = payload.model_dump(exclude_unset=True)

    for key, value in update_data.items():
        setattr(task, key, value)

    task.updated_at = now_kst()
    db.commit()
    db.refresh(task)
    return task


def delete_task(db: Session, task_id: int) -> bool:
    task = get_task_by_id(db, task_id)
    if not task:
        return False

    db.delete(task)
    db.commit()
    return True


def complete_task(db: Session, task_id: int, payload: CompleteTaskRequest):
    task = get_task_by_id(db, task_id)
    if not task:
        return None, None, "not_found"

    # 중복 완료 방지
    if task.status == "completed":
        existing_log = (
            db.query(TaskLog)
            .filter(TaskLog.task_id == task.id)
            .order_by(TaskLog.completed_at.desc())
            .first()
        )
        return task, existing_log, "already_completed"

    completed_time = now_kst()

    task.status = "completed"
    task.updated_at = completed_time

    log = TaskLog(
        task_id=task.id,
        patient_id=task.patient_id,
        action="complete",
        completed_at=completed_time,
        completed_by=payload.completed_by,
        device_id=payload.device_id,
        note=payload.note
    )

    db.add(log)
    db.commit()
    db.refresh(task)
    db.refresh(log)

    return task, log, "completed"


def get_task_logs(db: Session):
    return (
        db.query(TaskLog)
        .options(joinedload(TaskLog.task).joinedload(Task.patient))
        .order_by(TaskLog.completed_at.desc())
        .all()
    )