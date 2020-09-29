import numpy as np
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db

db_url = 'https://autosen-7011c.firebaseio.com/'
firebase_json_location = "d:\\Android\\AndroidStudioProjects\\AUToSen\\model\\autosen-7011c-firebase-adminsdk-0fofm-ddf8fd56e1.json"

# 서비스 계정의 비공개 키 파일이름
cred = credentials.Certificate(firebase_json_location)

default_app = firebase_admin.initialize_app(cred, {'databaseURL':db_url})


def read_data(path: str, result: list, secs: int, is_owner: bool):
    f = open(path, 'rb')

    for i in range(secs):
        secondDataList2D = []
        for j in range(64):
            oneOver64HzDataList1D = []
            for k in range(9):
                float_data_in_bytes = f.read(4)
                oneOver64HzDataList1D.append(np.fromstring(float_data_in_bytes, dtype='>f')[0])
            if is_owner:
                oneOver64HzDataList1D.append(1)
            else:
                oneOver64HzDataList1D.append(0)
            secondDataList2D.append(oneOver64HzDataList1D)
        result.append(secondDataList2D)

def save_data(path: str, result: list, secs: int):
    f = open(path, 'ab')

    for i in range(secs):
        for j in range(64):
            for k in range(9):
                f.write(bytes(np.array([result[i][j][k]]).astype(dtype='>f')))

    f.close()


def load_data_from_firebase(user_id: str, result: list):
    ref = db.reference("Sensor_Data")
    snapshot = ref.child(user_id).get()

    for sec in snapshot:
        secondDataList2D = []
        secondDataDictionary2D = snapshot[sec]

        for dataSet in secondDataDictionary2D:
            oneOver64HzDataList1D = []
            oneOver64HzDataDictionary1D = secondDataDictionary2D[dataSet]

            for data in oneOver64HzDataDictionary1D:
                oneOver64HzDataList1D.append(oneOver64HzDataDictionary1D[data])

            secondDataList2D.append(oneOver64HzDataList1D)
        result.append(secondDataList2D)

    return len(result)


