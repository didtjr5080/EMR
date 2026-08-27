from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from datetime import datetime
from zoneinfo import ZoneInfo

from backend.database import Base


def now_kst():
    return datetime.now(ZoneInfo("Asia/Seoul"))


class Patient(Base):
    __tablename__ = "patients"

    id = Column(Integer, primary_key=True, index=True)
    patient_name = Column(String(100), nullable=False)
    room_number = Column(String(50), nullable=False)
    patient_code = Column(String(100), nullable=True)
    created_at = Column(DateTime(timezone=True), default=now_kst, nullable=False)

    tasks = relationship(
        "Task",
        back_populates="patient",
        cascade="all, delete-orphan"
    )


class Task(Base):
    __tablename__ = "tasks"

    id = Column(Integer, primary_key=True, index=True)
    patient_id = Column(Integer, ForeignKey("patients.id"), nullable=False)

    task_name = Column(String(200), nullable=False)
    description = Column(Text, nullable=True)
    scheduled_time = Column(DateTime(timezone=True), nullable=True)

    priority = Column(String(30), default="normal", nullable=False)
    status = Column(String(30), default="pending", nullable=False)
    assigned_to = Column(String(100), nullable=True)

    created_at = Column(DateTime(timezone=True), default=now_kst, nullable=False)
    updated_at = Column(
        DateTime(timezone=True),
        default=now_kst,
        onupdate=now_kst,
        nullable=False
    )

    patient = relationship("Patient", back_populates="tasks")
    logs = relationship(
        "TaskLog",
        back_populates="task",
        cascade="all, delete-orphan"
    )


class TaskLog(Base):
    __tablename__ = "task_logs"

    id = Column(Integer, primary_key=True, index=True)

    task_id = Column(Integer, ForeignKey("tasks.id"), nullable=False)
    patient_id = Column(Integer, ForeignKey("patients.id"), nullable=False)

    action = Column(String(50), default="complete", nullable=False)
    completed_at = Column(DateTime(timezone=True), default=now_kst, nullable=False)

    completed_by = Column(String(100), nullable=True)
    device_id = Column(String(100), nullable=True)
    note = Column(Text, nullable=True)

    task = relationship("Task", back_populates="logs")
    patient = relationship("Patient")