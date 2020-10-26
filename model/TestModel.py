from tensorflow.keras.models import load_model
import DataIO
import numpy as np
import matplotlib.pyplot as plt

user_id = "sungjae"

model = load_model("D:/Android/AndroidStudioProjects/AUToSen/model/models/" + user_id + "epochs_500_different_data_augmented_compensated_weight4_package.h5")

result = [] # 9개의 센서 데이터와 주인 여부를 1초 단위로 묶은 것 => 3차원 배열

DataIO.read_data("../data/user_data/sungjae2.txt", result, 1795, True)
DataIO.read_data("../data/user_data/chanhee.txt", result, 2550, False)

result = np.array(result)
X = result[ : , : , : -1]
y = []

for i in result:
    y_temp = []
    y_temp.append(i[0][9])
    y.append(y_temp)

X = np.array(X)
y = np.array(y)

total = len(X)
truePositive = 0
falsePositive = 0 # FP -> FAR
falseNegative = 0 #FN -> FRR
trueNegative = 0

for i in range(len(X)):
    input = X[i]
    y_pred = model.predict(np.array(input).reshape(1, 64, 9)) # 3차원 배열로 바꿈
    #print(y_pred[0][0])
    #predictions.append(y_pred)

    if (y[i][0] == 1 and y_pred[0][0] >= 0.5) or (y[i][0] == 0 and y_pred[0][0] < 0.5):
        if y_pred[0][0] >= 0.5:
            truePositive += 1
        else:
            trueNegative += 1
    else:
        if y_pred[0][0] >= 0.5:
            falsePositive += 1
        else:
            falseNegative += 1


correctRatio = (truePositive + trueNegative) * 100 / total
far = falsePositive * 100 / total
frr = falseNegative * 100 / total

ys = [correctRatio, far, frr]
label = ["Correct", "FAR", "FRR"]
plt.bar(label, ys)
plt.title("%s - epochs= %d : %d/%d= %.2f%%, FAR= %.2f%%, FRR= %.2f%%" %(user_id, 40, truePositive + trueNegative, total, correctRatio, far, frr))
plt.ylabel("Ratio")
plt.show()

print("FAR: %f, FRR: %f" %(far, frr))