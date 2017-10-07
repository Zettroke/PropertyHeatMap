import os

ok_size = [144, 136]

path = "C:\PropertyHeatMap\PropertyHeatMap\MapLoader\map\z15"

l = os.listdir(path)

for x in range(ok_size[0]):
    for y in range(ok_size[1]):
        if "{}.{}.png".format(x, y) not in l:
            print("AHTUNG miss" + "{}.{}.png".format(x, y))
            break