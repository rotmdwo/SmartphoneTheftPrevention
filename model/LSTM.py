import keras
from keras import backend as K
from keras.models import Sequential
from keras.layers import Bidirectional
from keras.layers.recurrent import LSTM
from keras.layers.core import Dense, Activation, Dropout
#from keras.utils.np_utils import to_categorical
import tensorflow as tf
import numpy as np

import firebase_admin
from firebase_admin import credentials
from firebase_admin import db

db_url = 'https://autosen-7011c.firebaseio.com/'
firebase_json_location = "d:\\Android\\AndroidStudioProjects\\AUToSen\\model\\autosen-7011c-firebase-adminsdk-0fofm-ddf8fd56e1.json"

# 서비스 계정의 비공개 키 파일이름
cred = credentials.Certificate(firebase_json_location)

default_app = firebase_admin.initialize_app(cred, {'databaseURL':db_url})

epochs = 10
num_neurons = 100
seq_len = 64    # 64Hz의 센서 데이터 이용
sample_len = 9 # 센서데이터 9개
pred_len = 1

user_id = "rotmdwo1"
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
    snapshot_others = ref.order_by_key().limit_to_first(num_of_other_users_data + 1).get() # 타인들의 데이터들 reference

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

            oneOver64HzDataList1D.append(0) # 스마트폰의 주인이라는 의미하는 class 추가
            secondDataList2D.append(oneOver64HzDataList1D)

        result.append(secondDataList2D)

    '''
    # 한 줄 씩 읽어오기
   
    f = open(filename, 'r').read()
    data_string = f.split('\n')
    d = list() # 9개의 센서 데이터 셋의 나열 => 2차원 배열

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
    '''
    # 데이터를 8:2 비율로 Train:Validation 데이터로 나눔
    # 유저의 데이터의 개수가 달라지면 이 부분에서 오류가 남. 리스트의 차원이 정의되지 않음.
    # 그러므로 안드로이드에서 데이터 수집할 때 정확히 5분 동안 같은 양의 데이터가 전송되게 해야 함!
    result = np.array(result)
    row = int(round(0.8 * result.shape[0]))
    train = result[ : row, : , :]
    test = result[row : , : , :]

    # TODO: 셔플을 Train 데이터와 Test 데이터로 나누기 전에 해야 함. 셔플 전에 하면 Train 데이터에 스마트폰 주인 데이터가 쏠림.
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

#X_train, y_train, X_test, y_test = load_data('sample.txt', seq_len, pred_len)
X_train, y_train, X_test, y_test = load_data(user_id, seq_len, pred_len)
'''
ref = db.reference('1').child('Accelerometer').child('1')
row = ref.get()
print(row)


# Input은 3차원 , Output은 2차원 데이터를 필요로 함
# input_shape에서 맨 앞 차원(데이터 수)는 적지 않고 2차원, 3차원 크기만 씀
model = Sequential()
model.add(Bidirectional(LSTM(num_neurons, return_sequences= True, input_shape= (None, sample_len)), input_shape= (seq_len, sample_len)))
model.add(Dropout(0.25))
#model.add(LSTM(num_neurons, return_sequences= True, input_shape= (9,)))
#model.add(Dropout(0.25))

model.add(LSTM(num_neurons, return_sequences=True))
model.add(Dropout(0.25))

model.add(LSTM(num_neurons, return_sequences=False))
model.add(Dropout(0.25))

model.add(Dense(units= pred_len))

model.add(Activation('linear'))
model.compile(loss= 'mse', optimizer= 'rmsprop')

model.fit(np.array(X_train), np.array(y_train), batch_size= 32, epochs=epochs, validation_split= 0.05)

print(model.output.op.name)
print(model.input.op.name)
'''