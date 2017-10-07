from multiprocessing import Process
import requests
import math

# from 9 to 16 zoom

def request(start, len, server):
    pass

if __name__ == '__main__':

    summ = 0
    n = 578946
    ind = n%8
    for i in range(8):
        if i < ind:
            summ += int(n/8)+1
        else:
            summ += int(n/8)
    print(summ)