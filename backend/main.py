from typing import Optional
from io import BytesIO
from pathlib import Path

import qrcode
from fastapi import FastAPI, Request, Form, HTTPException
from fastapi.responses import RedirectResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
import sys
import os
from fastapi.templating import Jinja2Templates
from fastapi.middleware.cors import CORSMiddleware
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

from backend import firestore_service as fs
from backend.schemas import CompleteTaskRequest


app = FastAPI(
    title="Firebase EMR Treatment Task System",
    description="Firebase Firestore 기반 처치완료 시간 기록 시스템",
    version="0.3.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def resource_path(relative_path):
    if hasattr(sys, '_MEIPASS'):
        return os.path.join(sys._MEIPASS, relative_path)
    return os.path.abspath(relative_path)

templates = Jinja2Templates(directory=resource_path("templates"))
app.mount("/static", StaticFiles(directory=resource_path("static")), name="static")


# -------------------------
# Web Pages
# -------------------------

@app.get("/")
def dashboard(request: Request):
    tasks = fs.get_tasks() or []

    total_count = len(tasks)
    pending_count = len([t for t in tasks if t.get("status") == "pending"])
    completed_count = len([t for t in tasks if t.get("status") == "completed"])

    return templates.TemplateResponse(
        request=request,
        name="dashboard.html",
        context={
            "now": fs.now_kst_iso(),
            "tasks": tasks,
            "total_count": total_count,
            "pending_count": pending_count,
            "completed_count": completed_count,
        }
    )


@app.get("/patients")
def patients_page(request: Request):
    patients = fs.get_patients() or []

    return templates.TemplateResponse(
        request=request,
        name="patients.html",
        context={
            "now": fs.now_kst_iso(),
            "patients": patients,
        }
    )


@app.get("/tasks")
def tasks_page(request: Request):
    patients = fs.get_patients() or []
    tasks = fs.get_tasks() or []
    presets = fs.get_task_presets() or []
    staff_list = fs.get_staff_list() or []

    return templates.TemplateResponse(
        request=request,
        name="tasks.html",
        context={
            "now": fs.now_kst_iso(),
            "patients": patients,
            "tasks": tasks,
            "presets": presets,
            "staff_list": staff_list,
        }
    )


@app.get("/task-presets")
def task_presets_page(request: Request):
    presets = fs.get_task_presets() or []

    return templates.TemplateResponse(
        request=request,
        name="task_presets.html",
        context={
            "now": fs.now_kst_iso(),
            "presets": presets,
        }
    )


@app.get("/staff")
def staff_page(request: Request):
    staff_list = fs.get_staff_list() or []

    return templates.TemplateResponse(
        request=request,
        name="staff.html",
        context={
            "now": fs.now_kst_iso(),
            "staff_list": staff_list,
        }
    )


@app.get("/logs")
def logs_page(request: Request):
    logs = fs.get_task_logs() or []

    return templates.TemplateResponse(
        request=request,
        name="logs.html",
        context={
            "now": fs.now_kst_iso(),
            "logs": logs,
        }
    )


# -------------------------
# Web Form Routes
# -------------------------

@app.post("/api/patients")
def create_patient_form(
    patient_name: str = Form(...),
    room_number: str = Form(...),
    patient_code: str = Form(""),
):
    fs.create_patient(
        patient_name=patient_name,
        room_number=room_number,
        patient_code=patient_code,
    )
    return RedirectResponse(url="/patients", status_code=303)


@app.post("/api/tasks")
def create_task_form(
    patient_id: str = Form(...),
    task_name: str = Form(...),
    description: str = Form(""),
    scheduled_time: str = Form(""),
    priority: str = Form("1"),
    assigned_to: str = Form(""),
    assigned_to_code: str = Form(""),
):
    try:
        fs.create_task(
            patient_id=patient_id,
            task_name=task_name,
            description=description,
            scheduled_time=scheduled_time,
            priority=priority,
            assigned_to=assigned_to,
            assigned_to_code=assigned_to_code,
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

    return RedirectResponse(url="/tasks", status_code=303)


@app.post("/api/tasks/{task_id}/delete")
def delete_task_form(task_id: str):
    fs.delete_task(task_id)
    return RedirectResponse(url="/tasks", status_code=303)


@app.post("/api/task-presets")
def create_task_preset_form(
    task_name: str = Form(...),
    description: str = Form(""),
    priority: str = Form("1"),
    assigned_to: str = Form(""),
):
    fs.create_task_preset(
        task_name=task_name,
        description=description,
        priority=priority,
        assigned_to=assigned_to,
    )
    return RedirectResponse(url="/task-presets", status_code=303)


@app.post("/api/task-presets/seed")
def seed_task_presets():
    fs.seed_default_task_presets()
    return RedirectResponse(url="/task-presets", status_code=303)


@app.post("/api/task-presets/{preset_id}/delete")
def delete_task_preset_form(preset_id: str):
    fs.delete_task_preset(preset_id)
    return RedirectResponse(url="/task-presets", status_code=303)


@app.post("/api/staff")
def create_staff_form(
    staff_name: str = Form(...),
    staff_code: str = Form(...),
    role: str = Form(""),
    department: str = Form(""),
):
    try:
        fs.create_staff(
            staff_name=staff_name,
            staff_code=staff_code,
            role=role,
            department=department,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    return RedirectResponse(url="/staff", status_code=303)


@app.post("/api/staff/{staff_id}/delete")
def delete_staff_form(staff_id: str):
    fs.delete_staff(staff_id)
    return RedirectResponse(url="/staff", status_code=303)


# -------------------------
# Staff QR PDF
# -------------------------

def get_korean_font_name():
    font_path = Path("C:/Windows/Fonts/malgun.ttf")

    if font_path.exists():
        try:
            pdfmetrics.registerFont(TTFont("MalgunGothic", str(font_path)))
            return "MalgunGothic"
        except Exception:
            return "Helvetica"

    return "Helvetica"


def make_staff_qr_pdf(staff: dict) -> bytes:
    staff_name = staff.get("staff_name", "")
    staff_code = staff.get("staff_code", "")
    role = staff.get("role", "")
    department = staff.get("department", "")

    qr_value = staff_code
    qr_img = qrcode.make(qr_value)

    buffer = BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=A4)
    width, height = A4

    font_name = get_korean_font_name()

    pdf.setFont(font_name, 20)
    pdf.drawString(25 * mm, height - 30 * mm, "EMR 담당자 QR 코드")

    pdf.setFont(font_name, 11)
    pdf.drawString(
        25 * mm,
        height - 40 * mm,
        "이 QR 코드는 Android 앱에서 담당자 업무 목록을 조회할 때 사용합니다."
    )

    qr_buffer = BytesIO()
    qr_img.save(qr_buffer, format="PNG")
    qr_buffer.seek(0)

    qr_size = 70 * mm
    qr_x = 25 * mm
    qr_y = height - 125 * mm

    pdf.drawImage(
        ImageReader(qr_buffer),
        qr_x,
        qr_y,
        width=qr_size,
        height=qr_size
    )

    info_x = 110 * mm
    info_y = height - 58 * mm

    pdf.setFont(font_name, 13)
    pdf.drawString(info_x, info_y, f"담당자명: {staff_name}")
    pdf.drawString(info_x, info_y - 10 * mm, f"담당자 코드: {staff_code}")
    pdf.drawString(info_x, info_y - 20 * mm, f"역할: {role or '-'}")
    pdf.drawString(info_x, info_y - 30 * mm, f"부서/병동: {department or '-'}")

    pdf.setFont(font_name, 10)
    pdf.drawString(
        25 * mm,
        35 * mm,
        "사용 방법: Android 앱에서 담당자 코드 입력란에 이 QR/바코드 값을 입력하거나 스캔합니다."
    )
    pdf.drawString(25 * mm, 28 * mm, f"QR Value: {qr_value}")

    pdf.showPage()
    pdf.save()

    buffer.seek(0)
    return buffer.getvalue()


@app.get("/api/staff/{staff_id}/qr-pdf")
def download_staff_qr_pdf(staff_id: str):
    staff = fs.get_staff(staff_id)

    if not staff:
        raise HTTPException(status_code=404, detail="Staff not found")

    pdf_bytes = make_staff_qr_pdf(staff)
    filename = f"staff_qr_{staff.get('staff_code', staff_id)}.pdf"

    return StreamingResponse(
        BytesIO(pdf_bytes),
        media_type="application/pdf",
        headers={
            "Content-Disposition": f'attachment; filename="{filename}"'
        }
    )


# -------------------------
# JSON API
# -------------------------

@app.get("/api/tasks")
def api_get_tasks(status: Optional[str] = None):
    return fs.get_tasks(status=status) or []


@app.get("/api/patients")
def api_get_patients():
    return fs.get_patients() or []


@app.get("/api/task-logs")
def api_get_logs():
    return fs.get_task_logs() or []


@app.get("/api/task-presets")
def api_get_task_presets():
    return fs.get_task_presets() or []


@app.get("/api/staff")
def api_get_staff():
    return fs.get_staff_list() or []


@app.post("/api/tasks/{task_id}/complete")
def api_complete_task(task_id: str, payload: CompleteTaskRequest):
    result = fs.complete_task(
        task_id=task_id,
        completed_by=payload.completed_by or "",
        device_id=payload.device_id or "",
        note=payload.note or "",
    )

    if not result.get("success"):
        raise HTTPException(status_code=404, detail=result.get("message"))

    return result


@app.get("/health")
def health():
    return {
        "status": "ok",
        "server_time_kst": fs.now_kst_iso(),
        "database": "Firebase Firestore",
    }