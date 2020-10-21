from keras import backend as K
from keras.models import Sequential
from keras.layers import Bidirectional
from keras.layers.recurrent import LSTM
from keras.layers.core import Dense, Activation, Dropout
import numpy as np
import DataIO
import glob
import sys

print(sys.argv[1])