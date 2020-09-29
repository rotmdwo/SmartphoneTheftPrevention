import numpy as np

def read_data(path: str, result: list, secs: int, is_owner: bool):
    f = open(path, 'rb')

    for i in range(secs):
        secondDataList2D = []
        for j in range(64):
            oneOver64HzDataList1D = []
            for k in range(9):
                float_data_in_bytes = f.read(4)
                oneOver64HzDataList1D.append(np.fromstring(float_data_in_bytes, dtype='>f')[0])
            if is_owner:
                oneOver64HzDataList1D.append(1)
            else:
                oneOver64HzDataList1D.append(0)
            secondDataList2D.append(oneOver64HzDataList1D)
        result.append(secondDataList2D)

def save_data(path: str, data, secs: int):
    f = open(path, 'ab')

    for i in range(secs):
        for j in range(64):
            for k in range(9):
                f.write(bytes(np.array([data[i][j][k]]).astype(dtype='>f')))

    f.close()


