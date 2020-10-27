from tensorflow.keras.models import load_model
import DataIO
import numpy as np
import matplotlib.pyplot as plt

user_id = "sungjae"
packaged_secs = 5

model = load_model("D:/Android/AndroidStudioProjects/AUToSen/model/models/sungjaeepochs5_different_data_not_augmented_weight_package.h5")

result_true = [] # 9개의 센서 데이터와 주인 여부를 1초 단위로 묶은 것 => 3차원 배열
result_false = []

DataIO.read_data("../data/user_data/sungjae2.txt", result_true, 1795, True)
DataIO.read_data("../data/user_data/chanhee.txt", result_false, 2550, False)

result_true = np.array(result_true)
result_false = np.array(result_false)
np.random.shuffle(result_true)
np.random.shuffle(result_false)

X_true = result_true[ : , : , : -1]
X_false = result_false[ : , : , : -1]

X_true = np.array(X_true)
X_false = np.array(X_false)

total_true = int(len(X_true) / packaged_secs)
total_false = int(len(X_false) / packaged_secs)
total = total_true + total_false

truePositive = 0
falsePositive = 0 # FP -> FAR
falseNegative = 0 #FN -> FRR
trueNegative = 0

for i in range(total_true):
    numOfPositive = 0

    for j in range(packaged_secs):
        input = X_true[i * packaged_secs + j]
        y_pred = model.predict(np.array(input).reshape(1, 64, 9))  # 3차원 배열로 바꿈

        if y_pred[0][0] >= 0.5:
            numOfPositive += 1

    if numOfPositive >= packaged_secs // 2 + 1:
        truePositive += 1
    else:
        falseNegative += 1

for i in range(total_false):
    numOfPositive = 0

    for j in range(packaged_secs):
        input = X_false[i * packaged_secs + j]
        y_pred = model.predict(np.array(input).reshape(1, 64, 9))  # 3차원 배열로 바꿈

        if y_pred[0][0] >= 0.5:
            numOfPositive += 1

    if numOfPositive >= packaged_secs // 2 + 1:
        falsePositive += 1
    else:
        trueNegative += 1

correctRatio = (truePositive + trueNegative) * 100 / total
far = falsePositive * 100 / total
frr = falseNegative * 100 / total

ys = [correctRatio, far, frr]
label = ["Correct", "FAR", "FRR"]
plt.bar(label, ys)
plt.title("%s - epochs= %d : %d/%d= %.2f%%, FAR= %.2f%%, FRR= %.2f%%" %(user_id, 5, truePositive + trueNegative, total, correctRatio, far, frr))
plt.ylabel("Ratio")
plt.show()

print("FAR: %f, FRR: %f" %(far, frr))