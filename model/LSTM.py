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

import firebase_admin
from firebase_admin import credentials
from firebase_admin import db

db_url = 'https://autosen-7011c.firebaseio.com/'
firebase_json_location = "d:\\Android\\AndroidStudioProjects\\AUToSen\\model\\autosen-7011c-firebase-adminsdk-0fofm-ddf8fd56e1.json"

# 서비스 계정의 비공개 키 파일이름
cred = credentials.Certificate(firebase_json_location)

default_app = firebase_admin.initialize_app(cred, {'databaseURL':db_url})

epochs = 40
num_neurons = 100
seq_len = 64    # 64Hz의 센서 데이터 이용
sample_len = 9 # 센서데이터 9개
pred_len = 1

user_id = "sungjae"
num_of_other_users_data = 5

'''
def load_data(filename, seq_len, pred_len):
    # 한 줄 씩 읽어오기
    f = open(filename, 'r').read()
    data_string = f.split('\n')
    d = list() # 9개의 센서 데이터 셋의 나열 => 2차원 배열

    # TODO: Normalization
    # 줄 마다 숫자들로 파싱
    for i in data_string:
        numbers = i.split(' ')
        d.append(numbers)
        #for j in number:
            #d.append(float(j))

    # seq_len 만큼의 데이터 마다 합침 --> 1초 간의 데이터들을 하나로
    result = []  # 9개의 센서 데이터를 1초 단위로 묶은 것 => 3차원 배열
    for i in range(int(len(d) / seq_len)):
        result.append(d[i * seq_len : (i + 1) * seq_len])

    # 데이터를 8:2 비율로 Train:Validation 데이터로 나눔
    result = np.array(result)
    row = int(round(0.8 * result.shape[0]))
    train = result[ : row, : , : ]
    test = result[row : , : , : ]

    np.random.shuffle(train)

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
'''

def load_data(user_id, seq_len, pred_len):
    ref = db.reference("Sensor_Data")
    # 딕셔너리 형태 .. Dictionary[key]로 value에 접근
    snapshot_self = ref.child(user_id).get() # 스마트폰 주인의 데이터 reference
    #snapshot_others = ref.order_by_key().limit_to_first(num_of_other_users_data + 1).get() # 타인들의 데이터들 reference

    # requests.exceptions.HTTPError: 413 Client Error: Request Entity Too Large for url로 인한 하드코딩
    snapshot_other1 = ref.child("chettem").get()
    snapshot_other2 = ref.child("jinsol").get()
    snapshot_other3 = ref.child("seongjeong").get()
    snapshot_other4 = ref.child("wiu").get()
    snapshot_other5 = ref.child("youngoh").get()
    snapshot_others = {"chettem": snapshot_other1, "jinsol": snapshot_other2, "seongjeong": snapshot_other3, "wiu": snapshot_other4, "youngoh": snapshot_other5}


    result = [] # 9개의 센서 데이터와 주인 여부를 1초 단위로 묶은 것 => 3차원 배열

    # 스마트폰 주인의 데이터 불러오기
    for sec in snapshot_self:
        secondDataList2D = []
        secondDataDictionary2D = snapshot_self[sec]

        for dataSet in secondDataDictionary2D:
            oneOver64HzDataList1D = []
            oneOver64HzDataDictionary1D = secondDataDictionary2D[dataSet]

            for data in oneOver64HzDataDictionary1D:
                oneOver64HzDataList1D.append(oneOver64HzDataDictionary1D[data])

            oneOver64HzDataList1D.append(1) # 스마트폰의 주인이라는 걸 의미하는 class 추가
            secondDataList2D.append(oneOver64HzDataList1D)

        result.append(secondDataList2D)

    # 타인들의 데이터 불러오기
    for user in snapshot_others:
        if user == user_id:
            continue

        snapshot = snapshot_others[user]

        for sec in snapshot:
            secondDataList2D = []
            secondDataDictionary2D = snapshot[sec]

            for dataSet in secondDataDictionary2D:
                oneOver64HzDataList1D = []
                oneOver64HzDataDictionary1D = secondDataDictionary2D[dataSet]

                for data in oneOver64HzDataDictionary1D:
                    oneOver64HzDataList1D.append(oneOver64HzDataDictionary1D[data])

                oneOver64HzDataList1D.append(0)  # 스마트폰의 주인이 아니라는 걸 의미하는 class 추가
                secondDataList2D.append(oneOver64HzDataList1D)

            result.append(secondDataList2D)

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

#X_train, y_train, X_test, y_test = load_data('sample.txt', seq_len, pred_len)
X_train, y_train, X_test, y_test = load_data(user_id, seq_len, pred_len)

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

model.fit(np.array(X_train), np.array(y_train), batch_size= 32, epochs=epochs, validation_split= 0.05)

print(model.output.op.name)
print(model.input.op.name)

#model.save(tf.compat.v1.Session(), '/tmp/keras_' + user_id + '.ckpt')
#saver = tf.compat.v1.train.Saver()
#saver.save(K.get_session(), '/tmp/keras_' + user_id + '.ckpt')

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

model.save('d:\\Android\\AndroidStudioProjects\\AUToSen\\model\\keras_' + user_id + '_' + str(epochs) +'_' + str(far) + '_' + str(frr) + '.ckpt')

ys = [correctRatio, far, frr]
label = ["Correct", "FAR", "FRR"]
plt.bar(label, ys)
plt.title("%s - epochs = %d, seq_len = %d, sample_len = %d : %d/%d = %.2f%%" %(user_id, epochs, seq_len, sample_len, truePositive + trueNegative, total, correctRatio))
plt.ylabel("Ratio")
plt.show()

print("FAR: %f, FRR: %f" %(far, frr))