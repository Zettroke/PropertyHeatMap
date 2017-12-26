import wx
from PIL import Image
import queue
import io
import requests
from threading import Thread
import json


class LoadTask:

    def __init__(self, url, priority, key, place, **kwargs):
        self.params = kwargs
        self.priority = priority
        self.key = key
        self.place = place
        self.url = url

    def __lt__(self, other):
        return self.priority < other.priority

    def __eq__(self, other):
        return self.priority == other.priority

    def __hash__(self):
        return id(self.place) * hash(self.key)


class Map(wx.Panel):

    def __init__(self, *args, **kwargs):
        super(Map, self).__init__(*args, **kwargs)
        self.map_folder = "C:/PropertyHeatMap/osm_map_small/image"
        self.map_tiles_url = "http://127.0.0.1:25565/image/z{z}/{x}.{y}.png"
        self.price_tiles_url = "http://127.0.0.1:25565/api/tile/price?z={z}&x={x}&y={y}&price={price}&range={range}"
        self.road_tiles_url = "http://127.0.0.1:25565/api/tile/road?z={z}&x={x}&y={y}&start_id={start_id}&max_dist={max_dist}"
        self.point_search_url = "http://127.0.0.1:25565/api/search/point?z={z}&x={x}&y={y}"
        self.tiles_dict = {}
        self.shapes_dict = {}
        self.server_zoom = 19

        # price stuff
        self.price_tiles_dict = {}
        self.price_turn_on = False

        self.road_tiles_dict = {}
        self.road_turn_on = True

        self.loaded_tiles_set = set()
        self.already_updated_bitmaps = set()
        self.SetBackgroundStyle(wx.BG_STYLE_CUSTOM)
        self.Bind(wx.EVT_PAINT, self.on_paint)
        self.Bind(wx.EVT_CHAR_HOOK, self.key)
        self.Bind(wx.EVT_LEFT_DOWN, self.left_down)
        self.Bind(wx.EVT_LEFT_UP, self.left_up)
        self.Bind(wx.EVT_MOTION, self.on_move)
        self.Bind(wx.EVT_LEAVE_WINDOW, self.left_up)
        self.Bind(wx.EVT_MOUSEWHEEL, self.zoom)
        self.moved = False
        self.Show()
        self.map_x, self.map_y = 0, 0
        self.last_pos = (0, 0)
        self.bitmaps = {}
        self.pressed = False
        self.missing_image = wx.Bitmap.FromBuffer(256, 256, Image.new("RGB", (256, 256), 0xCCCCCC).tobytes())
        self.zoom = 16
        self.available_zoom_levels = tuple(map(int, open(self.map_folder+"/zoom_levels", "r").read().split()))
        self.bounds = (9, 9)
        self.tile_queue = queue.PriorityQueue()

        Thread(target=self.tile_loader, daemon=True, name="loader 1").start()
        Thread(target=self.tile_loader, daemon=True, name="loader 2").start()

    def request_location(self, x, y):
        ans = json.loads(requests.get(self.point_search_url.format(x=x, y=y, z=self.zoom)).text, encoding="utf-8")
        if ans["status"] == "success":

            print(json.dumps(ans, ensure_ascii=False, indent=2))
            self.shapes_dict[ans["objects"][0]["id"]] = ans["objects"][0]["points"]

            self.Refresh(False)

    def left_down(self, event):
        # print("CLICK")
        self.pressed = True
        self.moved = False

        self.last_pos = event.GetPosition()

    def left_up(self, event):
        # print("UNCLICK")
        self.pressed = False
        if not self.moved:
            Thread(target=self.request_location, args=(self.map_x+event.Position[0], self.map_y+event.Position[1]),
                   daemon=True).start()

    def on_move(self, event):
        if self.pressed:
            self.moved = True
            self.map_x -= event.GetPosition()[0] - self.last_pos[0]
            self.map_y -= event.GetPosition()[1] - self.last_pos[1]
            self.last_pos = event.GetPosition()
            self.Refresh(False)
            # print(self.sx, self.sy)

    def key(self, event):
        speed = 25
        if event.KeyCode == 316:
            # right
            self.map_x += speed
        elif event.KeyCode == 315:
            # up
            self.map_y -= speed
        elif event.KeyCode == 314:
            # left
            self.map_x -= speed
        elif event.KeyCode == 317:
            # down
            self.map_y += speed
        self.Refresh()

    def zoom(self, event=None):
        z = abs(event.WheelRotation)//event.WheelRotation
        if self.available_zoom_levels[0] <= z+self.zoom <= self.available_zoom_levels[1]:
            if z > 0:
                self.zoom += 1
                self.map_x = self.map_x * 2 + event.GetPosition()[0]
                self.map_y = self.map_y * 2 + event.GetPosition()[1]
            else:
                self.zoom -= 1
                self.map_x = self.map_x // 2 - event.GetPosition()[0] // 2
                self.map_y = self.map_y // 2 - event.GetPosition()[1] // 2
            self.bounds = tuple(map(int, open(self.map_folder+"/z" + str(self.zoom) + "/config", "r").read().split()))

            self.bitmaps.clear()
            while not self.tile_queue.empty():
                self.tile_queue.get()
            self.tiles_dict.clear()
            self.loaded_tiles_set.clear()
            self.price_tiles_dict.clear()
            self.road_tiles_dict.clear()
            self.already_updated_bitmaps.clear()
            self.Refresh()

    def on_size(self, event):
        # event.Skip()
        # self.Refresh()
        self.on_paint(None)

    def on_paint(self, event):
        w, h = self.GetClientSize()
        dc = wx.AutoBufferedPaintDC(self)
        dc.Clear()
        dc.DestroyClippingRegion()

        for x in range(self.map_x//256*256, ((self.map_x+w)//256+1)*256, 256):
            for y in range(self.map_y//256*256, ((self.map_y+h)//256+1)*256, 256):
                x2, y2 = x//256, y//256
                if (x2, y2) not in self.already_updated_bitmaps:
                    if 0 <= x2 < self.bounds[0] and 0 <= y2 < self.bounds[1]:
                        if (x2, y2) in self.tiles_dict.keys():
                            image_base = self.tiles_dict[(x2, y2)]
                            # Image.alpha_composite(image_base, self)
                            if self.price_turn_on:
                                if (x2, y2) in self.price_tiles_dict.keys():
                                    image_base = Image.alpha_composite(image_base, self.price_tiles_dict[(x2, y2)])
                            if self.road_turn_on:
                                if (x2, y2) in self.road_tiles_dict.keys():
                                    image_base = Image.alpha_composite(image_base, self.road_tiles_dict[(x2, y2)])

                            self.bitmaps[(x2, y2)] = wx.Bitmap.FromBufferRGBA(256, 256, image_base.tobytes())
                            self.already_updated_bitmaps.add((x2, y2))
                        else:
                            self.bitmaps[(x2, y2)] = self.missing_image
                            dist = (self.map_x + self.GetSize()[0] // 2 - x) ** 2 + (self.map_y + self.GetSize()[1] // 2 - y) ** 2
                            self.tile_queue.put(LoadTask(self.map_tiles_url, dist, (x2, y2), self.tiles_dict, x=x2, y=y2, z=self.zoom))
                            self.already_updated_bitmaps.add((x2, y2))
                            if self.price_turn_on:
                                self.tile_queue.put(LoadTask(self.price_tiles_url, dist + 1, (x2, y2), self.price_tiles_dict, x=x2, y=y2, z=self.zoom, price=150000, range=0.5))
                            if self.road_turn_on:
                                self.tile_queue.put(LoadTask(self.road_tiles_url, dist + 2, (x2, y2), self.road_tiles_dict, x=x2, y=y2, z=self.zoom, start_id=97660596, max_dist=4500))

                    else:
                        self.bitmaps[(x // 256, y // 256)] = self.missing_image
                dc.DrawBitmap(self.bitmaps[(x2, y2)], x - self.map_x, y - self.map_y)

        for v in self.shapes_dict.values():
            l = []
            for i in v:
                l.append((round(i[0]/2**(self.server_zoom-self.zoom))-self.map_x, round(i[1]/2**(self.server_zoom-self.zoom))-self.map_y))
            dc.DrawPolygon(l)


    def tile_loader(self):
        while True:
            to_load = self.tile_queue.get()
            if to_load not in self.loaded_tiles_set:
                try:
                    image = Image.open(io.BytesIO(requests.get(to_load.url.format(**to_load.params)).content))
                except Exception:
                    print("PLEASE FUCKING DEBUG")
                image = image.convert("RGBA")

                to_load.place[to_load.key] = image
                self.loaded_tiles_set.add(to_load)
                if to_load.key in self.already_updated_bitmaps:
                    self.already_updated_bitmaps.remove(to_load.key)

                self.Refresh(False)


class DataView(wx.ScrolledWindow):
    def __init__(self, parent):
        super().__init__(parent)

    def view_info(self, json_string):
        pass

    def on_resize(self, event):
        pass


class PropertyHeatMap(wx.Frame):
    def __init__(self):
        super().__init__(None)
        self.panel = wx.Panel(self)
        self.box_sizer = wx.BoxSizer(wx.HORIZONTAL)
        self.SetTitle('PropertyHeatMap')

        self.Center()
        self.map = Map(self.panel)
        self.box_sizer.Add(self.map, 4, wx.ALL | wx.EXPAND)
        sc = wx.ScrolledWindow(self.panel)

        sc.SetScrollbars(1, 1, 1, 2000)
        sc.SetScrollRate(5, 5)
        sc.Bind(wx.EVT_SIZE, self.size)
        p = wx.Panel(sc, pos=(0, 0), size=(100, 2000))

        line = wx.StaticLine(p, size=(1, 2000))

        self.box_sizer.Add(sc, 1, wx.ALL | wx.EXPAND)

        self.panel.SetSizer(self.box_sizer)
        self.Bind(wx.EVT_CLOSE, self.on_close)

        self.SetClientSize((500, 500))
        # self.view.SetScrollbars(1, 1, 1000, 1000)

    def on_close(self, event):
        self.Destroy()

    def size(self, event):
        print("sizing")


def main():
    app = wx.App(False)
    frame = PropertyHeatMap()
    frame.Show()

    app.MainLoop()


if __name__ == '__main__':
    main()
