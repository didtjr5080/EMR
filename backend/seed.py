from database import SessionLocal
from models import Patient, Task
from datetime import datetime, timedelta

db = SessionLocal()

# 샘플 환자
p1 = Patient(patient_name="홍길동", room_number="101", patient_code="P001")
p2 = Patient(patient_name="김영희", room_number="102", patient_code="P002")
db.add_all([p1, p2])
db.commit()
db.refresh(p1)
db.refresh(p2)

# 샘플 업무
now = datetime.utcnow()
t1 = Task(patient_id=p1.id, task_name="혈압 측정", description="혈압을 측정하세요", scheduled_time=now+timedelta(hours=1))
t2 = Task(patient_id=p2.id, task_name="혈당 체크", description="식전 혈당 체크", scheduled_time=now+timedelta(hours=2))
db.add_all([t1, t2])
db.commit()
db.close()
