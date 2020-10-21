from tensorflow.keras.models import load_model
import numpy as np
import sys


def read_data(path: str, result: list, secs: int):
    f = open(path, 'rb')

    for i in range(secs):
        secondDataList2D = []
        for j in range(64):
            oneOver64HzDataList1D = []
            for k in range(9):
                float_data_in_bytes = f.read(4)
                oneOver64HzDataList1D.append(np.fromstring(float_data_in_bytes, dtype='>f')[0])
            secondDataList2D.append(oneOver64HzDataList1D)
        result.append(secondDataList2D)


X = []
read_data("../model/temp.txt", X, 5)
X = np.array(X)
user_id = sys.argv[1]

try:
    model = load_model("D:/Android/AndroidStudioProjects/AUToSen/model/models/" + user_id + ".h5")

    numOfPositive = 0

    for i in range(len(X)):
        input = X[i]
        y_pred = model.predict(np.array(input).reshape(1, 64, 9))  # 3차원 배열로 바꿈
        #print(y_pred[0][0])
        if y_pred[0][0] >= 0.5:
            numOfPositive += 1

    if numOfPositive / len(X) >= 0.5:
        print("true")
    else:
        print("false")
except Exception as e:
    print(e)

