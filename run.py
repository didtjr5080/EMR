

import uvicorn
import sys
import os
from backend.main import app

if __name__ == "__main__":
    # PyInstaller로 빌드된 exe에서는 reload=False
    is_frozen = getattr(sys, 'frozen', False)
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        reload=not is_frozen
    )
    os.system("pause")