from multiprocessing import Process
import requests
import math
import os

# from 10 to 17 zoom
UPDATE = True
PROC_NUM = 1

map_folder = "C:/PropertyHeatMap/osm_map_full_moscow_test2/"

zoom_start = 10
zoom_end = 17


def mercator(lat, lon, z):
    return (2**(z-1)/math.pi)*(math.radians(lat)+math.pi),\
           (2**(z-1)/math.pi)*(math.pi-math.log(math.tan(math.radians(lon)/2+math.pi/4)))


def tiles_loader(ind, server, bounds, exclude):
    # url = "http://" + server + ".maps.yandex.net/tiles?l=map&v=17.10.01-0&x={}&y={}&z={}&scale=1&lang=ru_RU"
    # url = "http://" + server + ".tile.openstreetmap.org/{z}/{x}/{y}.png"
    #url = "https://" + server + ".tiles.mapbox.com/v4/mapquest.streets-mb/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFwcXVlc3QiLCJhIjoiY2Q2N2RlMmNhY2NiZTRkMzlmZjJmZDk0NWU0ZGJlNTMifQ.mPRiEubbajc6a5y9ISgydg"
    url = "https://" + server + ".tiles.mapbox.com/v3/foursquare.qhb8olxr/{z}/{x}/{y}.png"
    tmp = mercator(bounds[0], bounds[1], zoom_start)
    offset_tile_x, offset_tile_y = round(tmp[0]), round(tmp[1])
    tmp = mercator(bounds[2], bounds[3], zoom_start)
    tiles_x, tiles_y = round(tmp[0])-offset_tile_x, round(tmp[1])-offset_tile_y

    for z in range(zoom_start, zoom_end+1):
        if z not in exclude:
            excl2 = set()
            for i in os.listdir(map_folder + "z" + str(z)):
                x, y = map(int, i.split(".")[:2])
                excl2.add((x, y))

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
                    if (x, y) not in excl2:
                        try:
                            r = requests.get(url.format(x=m_offset_tile_x+x, y=m_offset_tile_y+y, z=z), headers={
                                "Connection": "keep-alive",
                                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.59 Safari/537.36",
                                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
                                "DNT": "1",
                                "Referer": "http://www.openstreetmap.org/",
                                "Accept-Language": "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7"})
                            open(map_folder + zoom_folder + "{}.{}.png".format(x, y), "wb").write(r.content)
                            break
                        except Exception as e:
                            print(e)
                    else:
                        break

                # print("Process {}: done".format(ind), "{}.{}.png".format(x, y))


if __name__ == '__main__':
    import time

    start_time = time.clock()

    '''start_x = 36.9141
    start_y = 55.9737
    end_x = 38.322
    end_y = 55.379'''
    start_x = 36.9250000
    start_y = 56.1027000
    end_x = 38.3286000
    end_y = 55.3729000
    bounds = [start_x, start_y, end_x, end_y]

    # servers = ["vec01", "vec02", "vec03", "vec04"]*max((PROC_NUM//4), 1)
    servers = ["a", "b", "c", "d"]
    servers = servers*max((PROC_NUM//len(servers)), 1)
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




