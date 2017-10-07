from multiprocessing import Process
import requests
import math

# from 10 to 16 zoom
UPDATE = False
PROC_NUM = 8

map_folder = "map/"

zoom_start = 10
zoom_end = 15


def tiles_loader(ind, server, bounds, exclude):
    url = "https://" + server + ".maps.yandex.net/tiles?l=map&v=17.10.01-0&x={}&y={}&z={}&scale=1&lang=ru_RU"
    for z in range(zoom_start, zoom_end+1):
        if z not in exclude:
            zoom_folder = "z" + str(z) + "/"
            offset_tile_x = round((180 + bounds[0]) / (360 / (2 ** z)))  # true
            offset_tile_y = round(bounds[1] / (180 / (2 ** z)))  # - round(0.0047*2**z)
            tiles_x = round((180 + bounds[2]) / (360 / (2 ** z))) - offset_tile_x
            tiles_y = -(round(bounds[3] / (180 / (2 ** z))) - offset_tile_y)  # - round(0.0047*2**z)

            num_to_process = tiles_x*tiles_y//PROC_NUM + (tiles_x*tiles_y%PROC_NUM > ind)

            start_from = ind*(tiles_x*tiles_y//PROC_NUM) + min(ind, (tiles_x*tiles_y) % PROC_NUM)

            for i in range(start_from, start_from + num_to_process):
                x = i // tiles_y
                y = i % tiles_y

                r = requests.get(url.format(offset_tile_x+x, offset_tile_y+y, z))
                open(map_folder + zoom_folder +"{}.{}.png".format(x, y), "wb").write(r.content)
                # print("Process {}: done".format(ind), "{}.{}.png".format(x, y))


if __name__ == '__main__':
    import time

    start_time = time.clock()
    start_x = 36.9223
    start_y = 56.1364
    end_x = 38.5071
    end_y = 55.3900
    bounds = [start_x, start_y, end_x, end_y]

    servers = ["vec01", "vec02", "vec03", "vec04"]*(PROC_NUM//4)
    import os
    exclude = []
    for i in range(zoom_start, zoom_end+1):
        if not os.path.exists(os.path.join(map_folder, "z" + str(i))):
            os.makedirs(os.path.join(map_folder, "z" + str(i)))
        elif not UPDATE:
            exclude.append(i)

    processes = []
    for i in range(PROC_NUM):
        p = Process(target=tiles_loader, args=(i, servers[i], bounds, exclude))
        p.start()
        processes.append(p)
    for i in processes:
        i.join()
    print("done in {}sec.".format(time.clock()-start_time))




