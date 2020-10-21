from tensorflow.keras.models import load_model
import numpy as np
import sys


X = []
secondDataList2D = []
#num = 0.08771935
for j in range(64):
    oneOver64HzDataList1D = []
    for k in range(9):
        oneOver64HzDataList1D.append(float(sys.argv[1 + k + j * 9]))
    secondDataList2D.append(oneOver64HzDataList1D)

X.append(secondDataList2D)
X = np.array(X)

user_id = sys.argv[577]

try:
    model = load_model("D:/Android/AndroidStudioProjects/AUToSen/model/models/" + user_id + ".h5")

    numOfPositive = 0

    for i in range(len(X)):
        input = X[i]
        y_pred = model.predict(np.array(input).reshape(1, 64, 9))  # 3차원 배열로 바꿈
        print(y_pred[0][0])
        if y_pred[0][0] >= 0.5:
            numOfPositive += 1

    if numOfPositive / len(X):
        print("true")
    else:
        print("false")
except Exception as e:
    print(e)

