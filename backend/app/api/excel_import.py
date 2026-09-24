from fastapi import APIRouter, Depends, UploadFile, File, Response, HTTPException
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.domain import User
from app.services.excel_engine import generate_standard_excel_template, process_excel_import

router = APIRouter(prefix="/excel", tags=["Excel Importer & Template"])

@router.get("/template")
def download_excel_template():
    excel_bytes = generate_standard_excel_template()
    return Response(
        content=excel_bytes,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": "attachment; filename=StockPilot_Inventory_Import_Template.xlsx"}
    )

@router.post("/import")
async def import_excel_file(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if not file.filename.endswith(('.xlsx', '.xls')):
        raise HTTPException(status_code=400, detail="Only Excel files (.xlsx, .xls) are allowed")
        
    file_bytes = await file.read()
    try:
        summary = process_excel_import(
            file_bytes=file_bytes,
            db=db,
            user_id=current_user.id,
            org_id=current_user.organization_id
        )
        return {
            "message": "Excel file imported successfully",
            "summary": summary
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error parsing Excel file: {str(e)}")
