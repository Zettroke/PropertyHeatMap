from multiprocessing import Process
import requests
import math

# from 10 to 16 zoom
UPDATE = False
PROC_NUM = 16

map_folder = "map/"

zoom_start = 10
zoom_end = 17


def mercator(lat, lon, z):
    return (2**(z-1)/math.pi)*(math.radians(lat)+math.pi),\
           (2**(z-1)/math.pi)*(math.pi-math.log(math.tan(math.radians(lon)/2+math.pi/4)))


def tiles_loader(ind, server, bounds, exclude):
    url = "http://" + server + ".maps.yandex.net/tiles?l=map&v=17.10.01-0&x={}&y={}&z={}&scale=1&lang=ru_RU"
    tmp = mercator(bounds[0], bounds[1], zoom_start)
    offset_tile_x, offset_tile_y = int(tmp[0]), int(tmp[1])
    tmp = mercator(bounds[2], bounds[3], zoom_start)
    tiles_x, tiles_y = int(tmp[0])-offset_tile_x, math.ceil(tmp[1])-offset_tile_y

    for z in range(zoom_start, zoom_end+1):
        if z not in exclude:
            m = 2**(z-zoom_start)
            m_tiles_x = tiles_x*m
            m_tiles_y = tiles_y*m
            m_offset_tile_x = offset_tile_x*m
            m_offset_tile_y = offset_tile_y*m
            
            zoom_folder = "z" + str(z) + "/"
            '''offset_tile_x = round((180 + bounds[0]) / (360 / (2 ** z)))  # true
            offset_tile_y = round((bounds[1]) / (180 / (2 ** z)))  # - round(0.0047*2**z)
            tiles_x = round((180 + bounds[2]) / (360 / (2 ** z))) - offset_tile_x
            tiles_y = -(round((bounds[3]) / (180 / (2 ** z))) - offset_tile_y)  # - round(0.0047*2**z)'''

            num_to_process = m_tiles_x*m_tiles_y//PROC_NUM + (m_tiles_x*m_tiles_y%PROC_NUM > ind)

            start_from = ind*(m_tiles_x*m_tiles_y//PROC_NUM) + min(ind, (m_tiles_x*m_tiles_y) % PROC_NUM)

            for i in range(start_from, start_from + num_to_process):
                x = i // m_tiles_y
                y = i % m_tiles_y
                while True:
                    try:
                        r = requests.get(url.format(m_offset_tile_x+x, m_offset_tile_y+y, z))
                        open(map_folder + zoom_folder +"{}.{}.png".format(x, y), "wb").write(r.content)
                        break
                    except Exception:
                        pass

                # print("Process {}: done".format(ind), "{}.{}.png".format(x, y))


if __name__ == '__main__':
    import time

    start_time = time.clock()

    start_x = 36.9141
    start_y = 55.9737
    end_x = 38.322
    end_y = 55.379
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

    '''import assemble
    assemble.assemble()'''




