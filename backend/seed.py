from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from backend.database import SessionLocal, Base, engine
from backend.models import Patient, Task


def now_kst():
    return datetime.now(ZoneInfo("Asia/Seoul"))


def run_seed():
    Base.metadata.create_all(bind=engine)

    db = SessionLocal()

    try:
        existing = db.query(Patient).first()
        if existing:
            print("이미 샘플 데이터가 있습니다. seed 작업을 중단합니다.")
            return

        p1 = Patient(
            patient_name="김환자",
            room_number="101",
            patient_code="P001",
            created_at=now_kst()
        )
        p2 = Patient(
            patient_name="이환자",
            room_number="102",
            patient_code="P002",
            created_at=now_kst()
        )
        p3 = Patient(
            patient_name="박환자",
            room_number="103",
            patient_code="P003",
            created_at=now_kst()
        )

        db.add_all([p1, p2, p3])
        db.commit()

        db.refresh(p1)
        db.refresh(p2)
        db.refresh(p3)

        t1 = Task(
            patient_id=p1.id,
            task_name="활력징후 측정",
            description="혈압, 맥박, 호흡수, 체온을 측정합니다.",
            scheduled_time=now_kst() + timedelta(minutes=30),
            priority="high",
            status="pending",
            assigned_to="간호사A",
            created_at=now_kst(),
            updated_at=now_kst()
        )

        t2 = Task(
            patient_id=p1.id,
            task_name="항생제 투여",
            description="처방된 항생제를 정해진 시간에 투여합니다.",
            scheduled_time=now_kst() + timedelta(hours=1),
            priority="urgent",
            status="pending",
            assigned_to="간호사A",
            created_at=now_kst(),
            updated_at=now_kst()
        )

        t3 = Task(
            patient_id=p2.id,
            task_name="수액 라인 확인",
            description="수액 속도와 라인 폐색 여부를 확인합니다.",
            scheduled_time=now_kst() + timedelta(hours=2),
            priority="normal",
            status="pending",
            assigned_to="간호사B",
            created_at=now_kst(),
            updated_at=now_kst()
        )

        t4 = Task(
            patient_id=p3.id,
            task_name="격리 폐기물 정리",
            description="격리 병실 내 의료 폐기물 상태를 확인합니다.",
            scheduled_time=now_kst() - timedelta(minutes=10),
            priority="high",
            status="pending",
            assigned_to="간호사C",
            created_at=now_kst(),
            updated_at=now_kst()
        )

        db.add_all([t1, t2, t3, t4])
        db.commit()

        print("샘플 데이터 생성 완료")

    finally:
        db.close()


if __name__ == "__main__":
    run_seed()