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
from keras.utils import np_utils
from keras.callbacks import ModelCheckpoint

user_id = "sungjae"

X_train = [[[1, 2, 3, 4, 5, 6, 7, 8, 9], [2, 3, 4, 5, 6, 7, 8, 9, 10]], [[11, 10, 9, 8, 7, 6, 5, 4, 3], [10, 9, 8, 7, 6, 5, 4, 3, 2]]]
y_train = [[2], [10]]
X_test = [[[3, 4, 5, 6, 7, 8, 9, 10, 11], [4, 5, 6, 7, 8, 9, 10, 11, 12]], [[12, 11, 10, 9, 8, 7, 6, 5, 4], [14, 13, 12, 11, 10, 9, 8, 7, 6]]]
y_test = [[1], [0]]

#y_train = np_utils.to_categorical(y_train)
#y_test = np_utils.to_categorical(y_test)

model = Sequential()
model.add(Bidirectional(LSTM(100, return_sequences= True, input_shape= (None, 9)), input_shape= (2, 9)))
model.add(Dropout(0.2))
#model.add(LSTM(num_neurons, return_sequences= True, input_shape= (9,)))
#model.add(Dropout(0.25))

model.add(LSTM(100, return_sequences=True))
model.add(Dropout(0.2))

model.add(LSTM(100, return_sequences=False))
model.add(Dropout(0.2))

model.add(Dense(units= 1))

model.add(Activation('linear'))
model.compile(loss= 'mse', optimizer= 'rmsprop')

#cp_callback = ModelCheckpoint('d:\\Android\\AndroidStudioProjects\\AUToSen\\model\\keras_' + user_id + '_test.ckpt', verbose=1)

model.fit(np.array(X_train), np.array(y_train), batch_size= 32, epochs=1, validation_split= 0.05)

print(model.output.op.name)
print(model.input.op.name)

predictions = []
total = len(X_test)
truePositive = 0
falsePositive = 0 # FP -> FAR
falseNegative = 0 #FN -> FRR
trueNegative = 0

for i in range(len(X_test)):
    input = X_test[i]
    y_pred = model.predict(np.array(input).reshape(1, 2, 9))
    predictions.append(y_pred)

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


#tf.train.Checkpoint()
#tf.keras.Model.save_weights('d:\\Android\\AndroidStudioProjects\\AUToSen\\model\\keras_' + user_id + '_test')
#tf.compat.v1.train.Saver([])
#model.save_weights('d:\\Android\\AndroidStudioProjects\\AUToSen\\model\\keras_' + user_id + '_test', save_format= "tf")
#save_model(model, 'd:\\Android\\AndroidStudioProjects\\AUToSen\\model\\keras_' + user_id + '_test.ckpt')
saver = tf.compat.v1.train.Saver([1,2,3])
saver.save(tf.compat.v1.Session(), '/tmp/keras_' + user_id + '_test.ckpt')
#model.save('d:\\Android\\AndroidStudioProjects\\AUToSen\\model\\keras_' + user_id + '.ckpt')

ys = [correctRatio, far, frr]
label = ["Correct", "FAR", "FRR"]
index = np.arange(len(label))
plt.bar(label, ys)
plt.title("%s - epochs = 1, lens = 2 : %d/%d = %.2f%%" %(user_id, truePositive + trueNegative, total, correctRatio))
plt.ylabel("Ratio")
plt.show()

print("FAR: %f, FRR: %f" %(far, frr))