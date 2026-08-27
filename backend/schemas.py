from typing import Optional
from pydantic import BaseModel


class PatientCreate(BaseModel):
    patient_name: str
    room_number: str
    patient_code: Optional[str] = None


class TaskCreate(BaseModel):
    patient_id: str
    task_name: str
    description: Optional[str] = None
    scheduled_time: Optional[str] = None
    priority: str = "normal"
    assigned_to: Optional[str] = None


class CompleteTaskRequest(BaseModel):
    completed_by: Optional[str] = None
    device_id: Optional[str] = None
    note: Optional[str] = None