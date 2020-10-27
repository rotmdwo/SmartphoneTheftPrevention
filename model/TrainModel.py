from keras import backend as K
from keras.models import Sequential
from keras.layers import Bidirectional
from keras.layers.recurrent import LSTM
from keras.layers.core import Dense, Activation, Dropout
import numpy as np
import DataIO
import glob
import sys

def load_data(user_id: str, pred_len: int):
    #num_of_other_users = 0
    num_of_user_data = 0
    num_of_other_users_data = 0

    result = []  # 9개의 센서 데이터와 주인 여부를 1초 단위로 묶은 것 => 3차원 배열

    file_list = glob.glob('../data/secs_data/*.txt')
    for path in file_list:
        temp_user = path[path.index("\\") + 1: -4]
        if user_id == temp_user:
            f = open(path, 'rt')
            num: int = int(f.read())
            num_of_user_data += num
            DataIO.read_data("../data/user_data/" + temp_user + ".txt", result, num, True)
        else:
            if num_of_other_users_data > 100000:
                continue
            f = open(path, 'rt')
            num: int = int(f.read())
            num_of_other_users_data += num
            #num_of_other_users += 1
            DataIO.read_data("../data/user_data/" + temp_user + ".txt", result, num, False)

    result = np.array(result)
    np.random.shuffle(result)

    # 인풋데이터 파싱
    X_train = result[:, :, : -pred_len]
    y_train = []

    # 아웃풋데이터 파싱
    for i in result:
        y_temp = []
        y_temp.append(i[0][sample_len])
        y_train.append(y_temp)

    return [X_train, y_train, num_of_user_data, num_of_other_users_data]


epochs = 10
num_neurons = 100
seq_len = 64  # 64Hz의 센서 데이터 이용
sample_len = 9  # 센서데이터 9개
pred_len = 1

user_id = sys.argv[1]
#user_id = "seongjeong"

X_train, y_train, num_of_user_data, num_of_other_users_data = load_data(user_id, pred_len)

# Input은 3차원 , Output은 2차원 데이터를 필요로 함
# input_shape에서 맨 앞 차원(데이터 수)는 적지 않고 2차원, 3차원 크기만 씀
model = Sequential()
model.add(Bidirectional(LSTM(num_neurons, return_sequences=True, input_shape=(None, sample_len)),
                        input_shape=(seq_len, sample_len)))
# model.add(LSTM(num_neurons, return_sequences= True, input_shape= (9,)))
model.add(Dropout(0.25))

model.add(LSTM(num_neurons, return_sequences=True))
model.add(Dropout(0.25))

model.add(LSTM(num_neurons, return_sequences=False))
model.add(Dropout(0.25))

model.add(Dense(units=pred_len))

model.add(Activation('relu'))
model.compile(loss='mse', optimizer='rmsprop')

user_data_ratio = num_of_user_data / (num_of_user_data + num_of_other_users_data)
other_users_data_ratio = 1.0 - user_data_ratio

K.set_value(model.optimizer.learning_rate, 0.0001)
#class_weight = {1: other_users_data_ratio, 0: user_data_ratio}

#model.fit(np.array(X_train), np.array(y_train), batch_size=1024, epochs=epochs, validation_split=0.2,class_weight=class_weight)
model.fit(np.array(X_train), np.array(y_train), batch_size=1024, epochs=epochs, validation_split=0.2)

model.save("D:/Android/AndroidStudioProjects/AUToSen/model/models/" + user_id + ".h5")

print("OK")