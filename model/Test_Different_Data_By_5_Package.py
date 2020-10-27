from keras import backend as K
from keras.models import Sequential
from keras.layers import Bidirectional
from keras.layers.recurrent import LSTM
from keras.layers.core import Dense, Activation, Dropout
import numpy as np
import matplotlib.pyplot as plt
import DataIO
import glob


def load_data(user_id: str, pred_len: int, is_augmented: bool):
    num_of_user_data = 0
    num_of_other_users_data = 0

    temp_result_list = []  # 9개의 센서 데이터와 주인 여부를 1초 단위로 묶은 것 => 3차원 배열
    file_list = glob.glob('../data/secs_data/*.txt')

    for path in file_list:
        temp_user = path[path.index("\\") + 1: -4]
        #temp_user = path[path.index("secs_data/") + 10: -4] # Mac

        # 테스트에는 사용되지만 학습에는 사용되지 않는 데이터
        if temp_user == "sungjae2" or temp_user == "chanhee":
            continue

        if user_id == temp_user:
            f = open(path, 'rt')
            num: int = int(f.read())
            num_of_user_data += num
            DataIO.read_data("../data/user_data/" + temp_user + ".txt", temp_result_list, num, True)
        else:
            if num_of_other_users_data > 100000:
                continue
            f = open(path, 'rt')
            num: int = int(f.read())
            num_of_other_users_data += num
            # num_of_other_users += 1
            DataIO.read_data("../data/user_data/" + temp_user + ".txt", temp_result_list, num, False)


    temp_result_array = np.array(temp_result_list)

    if is_augmented:
        result = np.zeros(shape=(temp_result_array.shape[0] * 5, temp_result_array.shape[1], temp_result_array.shape[2]))

        for i in range(5):
            noise = np.random.normal(scale=0.000001, size=temp_result_array.shape)
            noise[:, :, -1:] = 0
            result[i*temp_result_array.shape[0]:(i+1)*temp_result_array.shape[0],:,:] = (temp_result_array + noise)
    else:
        result = temp_result_array

    np.random.shuffle(result)

    # 인풋데이터 파싱
    X_train = result[:, :, : -pred_len]
    y_train = []

    # 아웃풋데이터 파싱
    for i in result:
        y_temp = []
        y_temp.append(int(i[0][sample_len])) # int로 변환
        y_train.append(y_temp)

    return [X_train, y_train, num_of_user_data, num_of_other_users_data]


epochs = 10
num_neurons = 100
seq_len = 64    # 64Hz의 센서 데이터 이용
sample_len = 9 # 센서데이터 9개
pred_len = 1

user_id = "sungjae"

X_train, y_train, num_of_user_data, num_of_other_users_data = load_data(user_id, pred_len, is_augmented=False)

# Input은 3차원 , Output은 2차원 데이터를 필요로 함
# input_shape에서 맨 앞 차원(데이터 수)는 적지 않고 2차원, 3차원 크기만 씀
# 학습
model = Sequential()
model.add(Bidirectional(LSTM(num_neurons, return_sequences= True, input_shape= (None, sample_len)), input_shape= (seq_len, sample_len)))
model.add(Dropout(0.25))

model.add(LSTM(num_neurons, return_sequences=True))
model.add(Dropout(0.25))

model.add(LSTM(num_neurons, return_sequences=False))
model.add(Dropout(0.25))

model.add(Dense(units= pred_len))

model.add(Activation('relu'))
model.compile(loss= 'mse', optimizer= 'rmsprop')

user_data_ratio = num_of_user_data / (num_of_user_data + num_of_other_users_data)
other_users_data_ratio = 1.0 - user_data_ratio
#compensation = np.abs(user_data_ratio - other_users_data_ratio) / 2
#compensation = np.abs(user_data_ratio - other_users_data_ratio) * 3 / 5
#compensation = np.abs(user_data_ratio - other_users_data_ratio) * 2 / 3
#compensation = np.abs(user_data_ratio - other_users_data_ratio) * 3 / 4
#compensation = np.abs(user_data_ratio - other_users_data_ratio) * 4 / 5

K.set_value(model.optimizer.learning_rate, 0.0001)
class_weight = {1: other_users_data_ratio, 0: user_data_ratio}
'''
if other_users_data_ratio > user_data_ratio:
    class_weight = {1: other_users_data_ratio - compensation, 0: user_data_ratio + compensation}
else:
    class_weight = {1: other_users_data_ratio + compensation, 0: user_data_ratio - compensation}
'''
#class_weight = {1: 0.5, 0: 0.5}
#model.fit(np.array(X_train), np.array(y_train), batch_size= 1024, epochs=epochs, validation_split= 0.2, class_weight=class_weight)
model.fit(np.array(X_train), np.array(y_train), batch_size= 1024, epochs=epochs, validation_split= 0.2)

model.save("D:/Android/AndroidStudioProjects/AUToSen/model/models/" + user_id + "epochs10_different_data_not_augmented_no_weight_package.h5")

# 테스트
result = []
DataIO.read_data("../data/user_data/sungjae2.txt", result, 1795, True)
DataIO.read_data("../data/user_data/chanhee.txt", result, 2550, False)

result = np.array(result)
X = result[ : , : , : -1]
y = []

for i in result:
    y_temp = []
    y_temp.append(int(i[0][9]))
    y.append(y_temp)

X = np.array(X)
y = np.array(y)

total = int(len(X) / 5)
truePositive = 0
falsePositive = 0 # FP -> FAR
falseNegative = 0 #FN -> FRR
trueNegative = 0

for i in range(total):
    numOfPositive = 0

    for j in range(5):
        input = X[i * 5 + j]
        y_pred = model.predict(np.array(input).reshape(1, 64, 9))  # 3차원 배열로 바꿈

        if y_pred[0][0] >= 0.5:
            numOfPositive += 1

    if numOfPositive >= 3:
        if y[i * 5][0] == 1:
            truePositive += 1
        else:
            falsePositive += 1
    else:
        if y[i * 5][0] == 1:
            falseNegative += 1
        else:
            trueNegative += 1


correctRatio = (truePositive + trueNegative) * 100 / total
far = falsePositive * 100 / total
frr = falseNegative * 100 / total

ys = [correctRatio, far, frr]
label = ["Correct", "FAR", "FRR"]
plt.bar(label, ys)
plt.title("%s - epochs= %d : %d/%d= %.2f%%, FAR= %.2f%%, FRR= %.2f%%" %(user_id, epochs, truePositive + trueNegative, total, correctRatio, far, frr))
plt.ylabel("Ratio")
plt.show()

print("FAR: %f, FRR: %f" %(far, frr))