import os
import sys
import openpyxl

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'backend')))

from app.core.database import SessionLocal
from app.models.domain import User
from app.api.reports import export_reports_to_excel

def export_and_inspect_all_reports():
    db = SessionLocal()
    try:
        admin_user = db.query(User).filter(User.username == 'admin').first()
        assert admin_user is not None, "Admin user must exist"

        export_dir = os.path.abspath("scratch/report_exports")
        os.makedirs(export_dir, exist_ok=True)

        report_types = ["sales", "purchases", "inventory", "pnl", "expenses"]

        print(f"--- EXPORTING ALL 5 REPORT TYPES TO {export_dir} ---")

        for r_type in report_types:
            response = export_reports_to_excel(report_type=r_type, db=db, current_user=admin_user)
            file_path = os.path.join(export_dir, f"StockPilot_{r_type.title()}_Report.xlsx")
            
            with open(file_path, "wb") as f:
                f.write(response.body)

            print(f"\n==========================================")
            print(f"REPORT: {r_type.upper()} ({os.path.getsize(file_path)} bytes)")
            print(f"File Path: {file_path}")
            print(f"==========================================")

            # Inspect generated workbook using openpyxl
            wb = openpyxl.load_workbook(file_path)
            ws = wb.active
            print(f"Sheet Name: '{ws.title}' | Total Rows: {ws.max_row} | Total Columns: {ws.max_column}")
            print("-" * 50)
            
            # Print first 12 rows preview
            for r in range(1, min(15, ws.max_row + 1)):
                row_vals = [ws.cell(r, c).value for c in range(1, ws.max_column + 1)]
                if any(row_vals):
                    print(f"Row {r:2d}: {row_vals[:6]}") # Show first 6 columns

            if ws.max_row > 15:
                print(f"... and {ws.max_row - 15} more rows.")

    finally:
        db.close()

if __name__ == '__main__':
    export_and_inspect_all_reports()
