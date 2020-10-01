import keras
from keras import backend as K
from keras.models import Sequential
from keras.layers import Bidirectional
from keras.layers.recurrent import LSTM
from keras.layers.core import Dense, Activation, Dropout
#from keras.utils.np_utils import to_categorical
import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt
import DataIO
from tensorflow.python.client import device_lib

epochs = 40
num_neurons = 100
seq_len = 64    # 64Hz의 센서 데이터 이용
sample_len = 9 # 센서데이터 9개
pred_len = 1

user_id = "sungjae"
num_of_other_users_data = 5


def load_data(user_id: str, pred_len: int):

    result = [] # 9개의 센서 데이터와 주인 여부를 1초 단위로 묶은 것 => 3차원 배열

    DataIO.read_data("../data/user_data/sungjae.txt", result, 19045, True)
    DataIO.read_data("../data/user_data/chettem.txt", result, 5055, False)
    DataIO.read_data("../data/user_data/hanjun.txt", result, 10800, False)
    DataIO.read_data("../data/user_data/jinsol.txt", result, 2825, False)
    DataIO.read_data("../data/user_data/seongjeong.txt", result, 8000, False)
    DataIO.read_data("../data/user_data/wiu.txt", result, 4410, False)
    DataIO.read_data("../data/user_data/youngoh.txt", result, 6980, False)

    # 데이터를 8:2 비율로 Train:Validation 데이터로 나눔
    # 유저의 데이터의 개수가 달라지면 이 부분에서 오류가 남. 리스트의 차원이 정의되지 않음.
    # 그러므로 안드로이드에서 데이터 수집할 때 정확히 5분 동안 같은 양의 데이터가 전송되게 해야 함!
    result = np.array(result)
    np.random.shuffle(result)

    row = int(round(0.9 * result.shape[0]))
    train = result[ : row, : , :]
    test = result[row : , : , :]

    # 인풋데이터 파싱
    X_train = train[ : , : , : -pred_len]
    X_test = test[ : , : , : -pred_len]


    y_train = []
    y_test = []
    #y_test = test[ : , : , -pred_len : ]

    # 아웃풋데이터 파싱
    for i in train:
        y_temp = []
        y_temp.append(i[0][sample_len])
        y_train.append(y_temp)

    for i in test:
        y_temp = []
        y_temp.append(i[0][sample_len])
        y_test.append(y_temp)

    #X_train = np.reshape(X_train, (X_train.shape[0], X_train.shape[1], X_train.shape[2], 1))
    #X_test =  np.reshape(X_test, (X_test.shape[0], X_test.shape[1], X_test.shape[2], 1))

    #y_train = np.reshape(y_train, (y_train.shape[0], y_train.shape[1]))
    #y_test = np.reshape(y_test, (y_test.shape[0], y_train.shape[1]))
    #y_train = to_categorical(y_train, 2)
    #y_test = to_categorical(y_test, 2)

    return [X_train, y_train, X_test, y_test]
#print(device_lib.list_local_devices())

X_train, y_train, X_test, y_test = load_data(user_id, pred_len)

# Input은 3차원 , Output은 2차원 데이터를 필요로 함
# input_shape에서 맨 앞 차원(데이터 수)는 적지 않고 2차원, 3차원 크기만 씀
model = Sequential()
model.add(Bidirectional(LSTM(num_neurons, return_sequences= True, input_shape= (None, sample_len)), input_shape= (seq_len, sample_len)))
model.add(Dropout(0.2))
#model.add(LSTM(num_neurons, return_sequences= True, input_shape= (9,)))
#model.add(Dropout(0.25))

model.add(LSTM(num_neurons, return_sequences=True))
model.add(Dropout(0.2))

model.add(LSTM(num_neurons, return_sequences=False))
model.add(Dropout(0.2))

model.add(Dense(units= pred_len))

model.add(Activation('linear'))
model.compile(loss= 'mse', optimizer= 'rmsprop')

class_weight = {1: 0.67, 0: 0.33}
#with tf.device('/device:CPU:0'):
model.fit(np.array(X_train), np.array(y_train), batch_size= 1024, epochs=epochs, validation_split= 0.05, class_weight=class_weight)

model.save(user_id + ".h5")
#print(model.output.op.name)
#print(model.input.op.name)

#model.save(tf.compat.v1.Session(), '/tmp/keras_' + user_id + '.ckpt')
#saver = tf.compat.v1.train.Saver()
#saver.save(K.get_session(), '/tmp/keras_' + user_id + '.ckpt')

#model.evaluate(np.array(X_test), np.array(y_test), batch_size=1024)

# 테스트 값 예측
predictions = []
total = len(X_test)
truePositive = 0
falsePositive = 0 # FP -> FAR
falseNegative = 0 #FN -> FRR
trueNegative = 0

for i in range(len(X_test)):
    input = X_test[i]
    y_pred = model.predict(np.array(input).reshape(1, seq_len, sample_len)) # 3차원 배열로 바꿈
    #predictions.append(y_pred)

    if (y_test[i][0] == 1 and y_pred[0][0] >= 0.5) or (y_test[i][0] == 0 and y_pred[0][0] < 0.5):
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

#model.save('d:\\Android\\AndroidStudioProjects\\AUToSen\\model\\keras_' + user_id + '_' + str(epochs) +'_' + str(far) + '_' + str(frr) + '.ckpt')

ys = [correctRatio, far, frr]
label = ["Correct", "FAR", "FRR"]
plt.bar(label, ys)
plt.title("%s - epochs= %d : %d/%d= %.2f%%, FAR= %.2f%%, FRR= %.2f%%" %(user_id, epochs, truePositive + trueNegative, total, correctRatio, far, frr))
plt.ylabel("Ratio")
plt.show()

print("FAR: %f, FRR: %f" %(far, frr))
