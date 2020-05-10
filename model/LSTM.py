import keras
from keras import backend as K

import tensorflow as tf
import numpy as np

epochs = 10
num_neurons = 100
seq_len = 10    # 10Hz의 센서 데이터 이용
sample_len = 10 # 센서데이터 9개 + 사용자(0, 1)
pred_len = 1

def load_data(filename, seq_len, pred_len):
    f = open(filename, 'r').read()
    data_string = f.split('\n')
    d = list()

    # TODO: Normalization
    for i in data_string:
        numbers = i.split(' ')
        d.append(numbers)
        #for j in number:
            #d.append(float(j))

    result = []  # same as list()
    for i in range(int(len(d) / seq_len)):
        result.append(d[i * seq_len : i * seq_len + seq_len])

    result = np.array(result)
    row = int(round(0.8 * result.shape[0]))
    train = result[ : row, : , : ]
    test = result[row : , : , : ]

    np.random.shuffle(train)

    X_train = train[ : , : , : -pred_len]
    X_test = test[ : , : , : -pred_len]

    y_train = train[ : , -pred_len : ]
    y_test = train[ : , -pred_len : ]

    X_train = np.reshape(X_train, (X_train.shape[0], X_train.shape[1], X_train.shape[2], 1))
    X_test =  np.reshape(X_test, (X_test.shape[0], X_test.shape[1], X_test.shape[2], 1))

    return [X_train, y_train, X_test, y_test]

load_data('sample.txt', seq_len, pred_len)