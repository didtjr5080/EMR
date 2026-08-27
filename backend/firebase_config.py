
import sys
import os
import firebase_admin
from firebase_admin import credentials, firestore

def resource_path(relative_path):
    if hasattr(sys, '_MEIPASS'):
        return os.path.join(sys._MEIPASS, relative_path)
    return os.path.abspath(relative_path)

SERVICE_ACCOUNT_PATH = resource_path("serviceAccountKey.json")


def init_firebase():
    if not os.path.exists(SERVICE_ACCOUNT_PATH):
        raise FileNotFoundError(
            f"Firebase service account key not found: {SERVICE_ACCOUNT_PATH}"
        )

    if not firebase_admin._apps:
        cred = credentials.Certificate(str(SERVICE_ACCOUNT_PATH))
        firebase_admin.initialize_app(cred)

    return firestore.client()


db = init_firebase()