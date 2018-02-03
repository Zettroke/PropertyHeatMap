from PyQt5 import Qt
from PyQt5.QtCore import *
from PyQt5.QtGui import *
from PyQt5.QtWidgets import *
import sys

from threading import Thread, RLock, current_thread
from PIL import Image, ImageQt, ImageDraw
import queue
import io
import requests
import time

ip = "127.0.0.1"

base_server_url = "http://" + ip + "/"
map_tiles_url = "http://" + ip + "/image/z{z}/{x}.{y}.png"
price_tiles_url = "http://" + ip + "/api/tile/price?z={z}&x={x}&y={y}&price={price}&range={range}"
road_tiles_url = "http://" + ip + "/api/tile/road?z={z}&x={x}&y={y}&start_id={start_id}&max_dist={max_dist}&foot={foot}"
point_search_url = "http://" + ip + "/api/search/point?z={z}&x={x}&y={y}"

'''class DownloadThread(QThread):

    data_downloaded = pyqtSignal(object)

    def __init__(self, bundle):
        QThread.__init__(self)
        self.bundle = bundle

    def run(self):

        img = Image.open(io.BytesIO(requests.get(self.bundle[2].format(**(self.bundle[3]))).content))
        img.convert("RGBA")
        imgqt = ImageQt.toqimage(img)
        self.data_downloaded.emit((self.bundle[0], self.bundle[1], imgqt))'''

class LoaderBundle:
    def __init__(self, priority, container, key, url, args):
        self.priority = priority
        self.container = container
        self.key = key
        self.url = url
        self.args = args

    def __lt__(self, other):
        return self.priority < other.priority

    def __eq__(self, other):
        return self.priority == other.priority



class Map(QWidget):
    refresh_signal = pyqtSignal()

    def __init__(self):
        super().__init__()
        self.refresh_signal.connect(self.refresh)
        print(current_thread().getName())
        self.map_x = 0
        self.map_y = 0
        self.zoom = 14

        self.prev_x = 0
        self.prev_y = 0

        self.loader_lock = RLock()
        self.loader_queue = queue.PriorityQueue()
        self.loading = set()

        self.num_tiles_x, self.num_tiles_y = map(int, requests.get(base_server_url + "image/z" + str(self.zoom) + "/config").text.split())
        self.min_zoom, self.max_zoom = map(int, requests.get(base_server_url + "image/zoom_levels").text.split())

        self.map_tiles_dict = dict()

        self.road_turn_on = True
        self.road_tiles_dict = dict()

        self.resize(500, 500)
        self.move(400, 200)
        self.setWindowTitle('Simple')
        self.show()

        Thread(target=self.tile_loader, daemon=True, name="loader 1").start()
        Thread(target=self.tile_loader, daemon=True, name="loader 2").start()

        img = Image.new('RGB', (256, 256), 0xCCCCCC)
        draw = ImageDraw.Draw(img)
        for i in range(0, 257, 32):
            draw.line((0, i, 256, i), 0xDDDDDD)
            draw.line((i, 0, i, 256), 0xDDDDDD)
        self.missing_image = ImageQt.toqimage(img)


        # Thread(target=self.updater, daemon=True).start()

    def refresh(self, event=None):
        self.repaint()

    def tile_loader(self):
        while True:
            bundle = self.loader_queue.get()

            img = Image.open(io.BytesIO(requests.get(bundle.url.format(**bundle.args)).content))
            img.convert("RGBA")
            imgqt = ImageQt.toqimage(img)
            with self.loader_lock:
                bundle.container[bundle.key] = imgqt
                # TODO: Normal tile remove
                if len(bundle.container) > 1000:
                    bundle.container.clear()
                if (bundle.key, bundle.url) in self.loading:
                    self.loading.remove((bundle.key, bundle.url))
            self.refresh_signal.emit()

    def dist(self, x1, y1, x2, y2):
        return (x1-x2)**2 + (y1-y2)**2

    def paintEvent(self, QPaintEvent):

        # self.loader_lock.acquire()
        painter = QPainter(self)
        w, h = self.size().width(), self.size().height()
        for x in range(self.map_x//256*256, ((self.map_x+w)//256+1)*256, 256):
            for y in range(self.map_y//256*256, ((self.map_y+h)//256+1)*256, 256):
                x2, y2 = x//256, y//256
                if 0 <= x2 < self.num_tiles_x and 0 <= y2 < self.num_tiles_y:
                    if (x2, y2) in self.map_tiles_dict.keys():
                        painter.drawImage(QPoint(x - self.map_x, y - self.map_y), self.map_tiles_dict[(x2, y2)])
                    elif ((x2, y2), map_tiles_url) not in self.loading:
                        bundle = LoaderBundle(self.dist(self.map_x+w/2, self.map_y+h/2, x2*256, y2*256), self.map_tiles_dict, (x2, y2), map_tiles_url, dict(x=x2, y=y2, z=self.zoom))
                        self.loading.add((bundle.key, bundle.url))
                        self.loader_queue.put(bundle)

                    if self.road_turn_on:
                        if (x2, y2) in self.road_tiles_dict.keys():
                            pass
                            painter.drawImage(QPoint(x - self.map_x, y - self.map_y), self.road_tiles_dict[(x2, y2)])
                        elif ((x2, y2), road_tiles_url) not in self.loading:
                            bundle = LoaderBundle(self.dist(self.map_x+w/2, self.map_y+h/2, x2*256, y2*256) + 1000, self.road_tiles_dict, (x2, y2), road_tiles_url, dict(x=x2, y=y2, z=self.zoom, start_id=62186199, foot=True, max_dist=12000))
                            self.loading.add((bundle.key, bundle.url))
                            self.loader_queue.put(bundle)

                else:
                    painter.drawImage(QPoint(x - self.map_x, y - self.map_y), self.missing_image)

        # self.loader_lock.release()

    def image_loaded(self, bundle):
        bundle[0][bundle[1]] = bundle[2]
        self.repaint()

    def mousePressEvent(self, event):
        self.prev_x, self.prev_y = event.x(), event.y()

    def mouseMoveEvent(self, event):
        self.map_x += self.prev_x - event.x()
        self.map_y += self.prev_y - event.y()
        self.prev_x, self.prev_y = event.x(), event.y()
        self.repaint()

    def mouseReleaseEvent(self, event):
        pass

    def wheelEvent(self, event):
        # self.loader_lock.acquire()

        z = event.angleDelta().y()//abs(event.angleDelta().y())

        if self.min_zoom <= self.zoom + z <= self.max_zoom:
            if z > 0:
                self.zoom += 1
                self.map_x = self.map_x * 2 + event.pos().x()
                self.map_y = self.map_y * 2 + event.pos().y()
            else:
                self.zoom -= 1
                self.map_x = self.map_x // 2 - event.pos().x() // 2
                self.map_y = self.map_y // 2 - event.pos().y() // 2

            self.num_tiles_x, self.num_tiles_y = map(int, requests.get(base_server_url + "image/z" + str(self.zoom) + "/config").text.split())
            self.map_tiles_dict.clear()
            self.road_tiles_dict.clear()
            while not self.loader_queue.empty():
                self.loader_queue.get()
            self.loading.clear()
        # self.loader_lock.release()
        self.repaint()


app = QApplication([])
m = Map()


sys.exit(app.exec_())