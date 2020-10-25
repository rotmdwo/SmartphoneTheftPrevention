import numpy as np
import DataIO
import glob

temp_result_list = []

DataIO.read_data("../data/user_data/" + "sungjae" + ".txt", temp_result_list, 2, True)
temp_result_array = np.array(temp_result_list)
print(temp_result_array.shape)
result = np.zeros(shape=(temp_result_array.shape[0] * 5, temp_result_array.shape[1], temp_result_array.shape[2]))

for i in range(5):
    noise = np.random.normal(scale=0.000001, size=temp_result_array.shape)
    noise[:, :, -1:] = 0

    result[i*temp_result_array.shape[0]:(i+1)*temp_result_array.shape[0],:,:] = (temp_result_array + noise)

print(result)

file_list = glob.glob('../data/secs_data/*.txt')
for path in file_list:
    temp_user = path[path.index("secs_data/") + 10: -4]
    print(temp_user)