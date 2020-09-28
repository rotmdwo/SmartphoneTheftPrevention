import numpy as np

def read_data(path: str, result: list, secs: int, is_owner: bool):
    f = open(path, 'rb').read()
    data = f.split(" ".encode())

    for i in range(secs):
        secondDataList2D = []
        for j in range(64):
            oneOver64HzDataList1D = []
            for k in range(9):
                float_data_in_bytes = data[i * 64 * 9 + j * 9 + k]
                result.append(np.fromstring(float_data_in_bytes, dtype='>f')[0])
