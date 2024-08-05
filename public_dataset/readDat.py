import struct
import os
import fnmatch
import pandas as pd
import json


def read_dat_files_write_csv(input_pathname, output_pathname, label):
    # 检查文件是否为 .dat 文件
    if fnmatch.fnmatch(input_pathname, '*.dat'):
        # 假设.dat文件中的数据是4字节整数
        content = ''
        with open(input_pathname, 'rb') as file:  # 初始化 content
            # print(input_pathname, end=' ')
            # content += str(input_pathname) + ','
            index = 1
            while True:
                # 读取4字节
                bytes = file.read(4)
                if not bytes:
                    break
                # 解析为整数
                number, = struct.unpack('i', bytes)
                # print(number, end=' ')
                content += str(number) + ','
                if index % 1000 == 0:
                    content += str(label) + '\n'
                index += 1
            # print()
            # 提取最后一个 '/' 之前的目录部分
            directory = os.path.dirname(output_pathname)
            if not os.path.exists(directory):
                # 创建目录
                os.makedirs(directory)
            with open(output_pathname, 'a') as writefile:
                writefile.write(content)


def read_scp_statements_ptbxl(statements_path, ptbxl_path):
    df = pd.read_csv(statements_path)
    # 选择第一列
    first_column = df.iloc[:, 0]
    # 显示第一列的数据
    # print('statements', first_column)

    df = pd.read_csv(ptbxl_path)
    # 选择特定列
    # selected_columns = df[['filename_hr', 'scp_codes']] #record500
    selected_columns = df[['filename_lr', 'scp_codes']]  # record100
    # 显示选定的列
    # print('ptbxl_path', selected_columns)

    index = 0
    paths = []
    labels = []
    for code in selected_columns['scp_codes']:
        # print(eval(code))
        # 使用 max 函数和 lambda 表达式找到值最大的键
        max_key = max(eval(code), key=lambda k: eval(code)[k])
        # print(max_key, end=' ')
        # 查找值对应的索引
        key = first_column.index[first_column == max_key][0]
        labels.append(key+1)
        # print(key, end=' ')
        # path = selected_columns['filename_hr'][index]  #record500
        path = selected_columns['filename_lr'][index]  # record100
        paths.append(path)
        # print(path, end=' ')
        # print()
        index += 1
    return paths, labels


# 使用函数读取指定目录下的所有 .dat 文件
# read_dat_files_write_csv(directory_to_search)

statements_path = './scp_statements.csv'
ptbxl_path = './ptbxl_database.csv'
paths, labels = read_scp_statements_ptbxl(statements_path, ptbxl_path)
index = 0
for path in paths:
    input_pathname = '/Users/leeway/ptb-xl-a-large-publicly-available-electrocardiography-dataset-1.0.3/' + path + '.dat'  # 替换为实际的目录路径
    # output_pathname = '/Users/leeway/PTB-XL/'+path[:-5]+'.csv' #record500
    output_pathname = '/Users/leeway/PTB-XL/' + path[:-12] + '.csv'  # record100
    label = labels[index]
    read_dat_files_write_csv(input_pathname, output_pathname, label)
    # if (index+1) % 100 ==0: #record500
    if (index + 1) % 1000 == 0:  # record100
        print(output_pathname)
    index += 1
