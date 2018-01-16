import wx
from PIL import Image, ImageDraw
import queue
import io
import requests
from threading import Thread, Lock, current_thread
import json
import time


# 2k18 yaaaay

EVT_REFRESH = wx.NewId()
mx_dist = 12000


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


class RefreshEvent(wx.PyEvent):
    def __init__(self):
        wx.PyEvent.__init__(self)
        self.SetEventType(EVT_REFRESH)


class Map(wx.Panel):

    LocalNetwork = False

    def __init__(self, *args, **kwargs):
        super(Map, self).__init__(*args, **kwargs)
        if self.LocalNetwork:
            self.base_server_url = "http://127.0.0.1/"
            self.map_tiles_url = "http://127.0.0.1/image/z{z}/{x}.{y}.png"
            self.price_tiles_url = "http://127.0.0.1/api/tile/price?z={z}&x={x}&y={y}&price={price}&range={range}"
            self.road_tiles_url = "http://127.0.0.1/api/tile/road?z={z}&x={x}&y={y}&start_id={start_id}&max_dist={max_dist}"
            self.point_search_url = "http://127.0.0.1/api/search/point?z={z}&x={x}&y={y}"

        else:
            self.base_server_url = "http://178.140.109.241/"
            self.map_tiles_url = "http://178.140.109.241/image/z{z}/{x}.{y}.png"
            self.price_tiles_url = "http://178.140.109.241/api/tile/price?z={z}&x={x}&y={y}&price={price}&range={range}"
            self.road_tiles_url = "http://178.140.109.241/api/tile/road?z={z}&x={x}&y={y}&start_id={start_id}&max_dist={max_dist}"
            self.point_search_url = "http://178.140.109.241/api/search/point?z={z}&x={x}&y={y}"
        self.tiles_dict = {}

        self.shapes_dict = {}
        self.server_zoom = 19
        self.shapes_bitmaps = {}
        self.shapes_images = {}

        # price stuff
        self.price_tiles_dict = {}
        self.price_turn_on = False

        self.road_tiles_dict = {}
        self.road_turn_on = False

        self.current_id = 0
        self.buff = True

        self.loaded_tiles_set = set()
        self.already_updated_bitmaps = set()
        self.bitmaps_to_update = set()
        self.SetBackgroundStyle(wx.BG_STYLE_CUSTOM)
        self.Bind(wx.EVT_PAINT, self.on_paint)
        self.Bind(wx.EVT_CHAR_HOOK, self.key)
        self.Bind(wx.EVT_LEFT_DOWN, self.left_down)
        self.Bind(wx.EVT_LEFT_UP, self.left_up)
        self.Bind(wx.EVT_MOTION, self.on_move)
        self.Bind(wx.EVT_LEAVE_WINDOW, self.out_of_window)
        self.Bind(wx.EVT_MOUSEWHEEL, self.zoom_method)
        self.Bind(wx.EVT_CLOSE, self.Destroy)
        self.moved = False
        self.Show()
        self.map_x, self.map_y = 0, 0
        self.last_pos = (0, 0)
        self.bitmaps = {}
        self.pressed = False
        self.missing_image = wx.Bitmap.FromBuffer(256, 256, Image.new("RGB", (256, 256), 0xCCCCCC).tobytes())
        self.zoom = 16
        self.available_zoom_levels = tuple(map(int, requests.get(self.base_server_url + "image/zoom_levels").text.split()))
        self.bounds = tuple(map(int, requests.get(self.base_server_url + "image/z" + str(self.zoom) + "/config", "r").text.split()))
        self.tile_queue = queue.PriorityQueue()
        self.Connect(-1, -1, EVT_REFRESH, self.my_refresh)
        self.loader_lock = Lock()
        self.need_refresh = True
        Thread(target=self.tile_loader, daemon=True, name="loader 1").start()
        Thread(target=self.tile_loader, daemon=True, name="loader 2").start()
        Thread(target=self.tile_loader, daemon=True, name="loader 3").start()
        # Thread(target=self.map_refresher, daemon=True, name="map_updater").start()

    def my_refresh(self, event):
        # print(current_thread().getName())
        self.Refresh()

    def request_location(self, x, y):

        ans = json.loads(requests.get(self.point_search_url.format(x=x, y=y, z=self.zoom)).text, encoding="utf-8")
        if ans["status"] == "success":

            if ans["objects"][0]["id"] not in self.shapes_dict:
                print(json.dumps(ans, ensure_ascii=False, indent=2))
                max_x, max_y = 0, 0
                min_x, min_y = 2 ** 32 - 1, 2 ** 32 - 1
                for p in ans["objects"][0]["points"]:
                    max_x, max_y = max(max_x, p[0]), max(max_y, p[1])
                    min_x, min_y = min(min_x, p[0]), min(min_y, p[1])

                self.shapes_dict[ans["objects"][0]["id"]] = ((min_x, min_y, max_x, max_y), ans["objects"][0]["points"])
                self.render_shapes_to_bitmaps()
                self.current_id = ans["objects"][0]["id"]
                self.road_turn_on = True

                w, h = self.GetClientSize()
                for x in range(self.map_x // 256 * 256, ((self.map_x + w) // 256 + 1) * 256, 256):
                    for y in range(self.map_y // 256 * 256, ((self.map_y + h) // 256 + 1) * 256, 256):
                        x2, y2 = x // 256, y // 256
                        dist = (self.map_x + self.GetSize()[0] // 2 - x) ** 2 + (self.map_y + self.GetSize()[1] // 2 - y) ** 2
                        self.tile_queue.put(
                            LoadTask(self.road_tiles_url, dist + 20000, (x2, y2), self.road_tiles_dict, x=x2, y=y2, z=self.zoom, start_id=self.current_id, max_dist=mx_dist))

            else:
                del self.shapes_dict[ans["objects"][0]["id"]]
                del self.shapes_bitmaps[ans["objects"][0]["id"]]
                self.road_turn_on = False
                self.road_tiles_dict.clear()
                self.update_all_bitmaps()
            self.Refresh()

    def render_shapes_to_bitmaps(self):
        for k, v in self.shapes_dict.items():
            AAmult = 4
            mult = 2 ** (self.server_zoom - self.zoom) // AAmult
            real_mult = 2 ** (self.server_zoom - self.zoom)
            bounds = v[0]

            shape = Image.new("RGBA", (bounds[2] - bounds[0], bounds[3] - bounds[1]), 0x00FFFFFF)
            draw = ImageDraw.ImageDraw(shape)
            x, y = v[1][0][0] // mult - bounds[0]//mult, v[1][0][1] // mult - bounds[1]//mult
            poly = [x, y]
            for p in v[1]:
                x1, y1 = round(p[0] / mult - bounds[0]/mult), round(p[1] / mult - bounds[1]/mult)
                poly.append(x1)
                poly.append(y1)
                draw.line((x, y, x1, y1), fill=0xFF0000FF, width=round(AAmult*2))
                x, y = x1, y1
            draw.polygon(poly, fill=0x100000FF)
            shape = shape.resize((shape.size[0] // AAmult, shape.size[1] // AAmult), Image.BICUBIC)
            self.shapes_bitmaps[k] = wx.Bitmap.FromBufferRGBA(shape.size[0], shape.size[1], shape.tobytes())
            for x in range(v[0][0]//real_mult//256, v[0][2]//real_mult//256+1):
                for y in range(v[0][1]//real_mult//256, v[0][3]//real_mult//256+1):
                    self.shapes_images[(x, y)] = ((v[0][0]//real_mult, v[0][1]//real_mult), shape)
                    self.bitmaps_to_update.add((x, y))

    def out_of_window(self, event):
        self.pressed = False

    def left_down(self, event):
        self.pressed = True
        self.moved = False
        self.last_pos = event.GetPosition()
        event.Skip()

    def left_up(self, event):
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
        class evt:
                def __init__(self, rotation, position):
                    self.WheelRotation = rotation
                    self.c = position

                def GetPosition(self):
                    return self.c
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
        elif event.KeyCode == 388:
            c = self.ScreenToClient(wx.GetMousePosition())
            self.zoom_method(evt(1, c))
        elif event.KeyCode == 390:
            c = self.ScreenToClient(wx.GetMousePosition())
            self.zoom_method(evt(-1, c))
        self.Refresh()

    def zoom_method(self, event=None):
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
            self.bounds = tuple(map(int, requests.get(self.base_server_url + "image/z" + str(self.zoom) + "/config", "r").text.split()))

            self.bitmaps.clear()
            while not self.tile_queue.empty():
                self.tile_queue.get()
            self.tiles_dict.clear()
            self.loaded_tiles_set.clear()
            self.price_tiles_dict.clear()
            self.road_tiles_dict.clear()
            self.already_updated_bitmaps.clear()
            self.render_shapes_to_bitmaps()
            self.Refresh()

    def update_all_bitmaps(self):
        w, h = self.GetClientSize()
        for x in range(self.map_x // 256 * 256, ((self.map_x + w) // 256 + 1) * 256, 256):
            for y in range(self.map_y // 256 * 256, ((self.map_y + h) // 256 + 1) * 256, 256):
                self.bitmaps_to_update.add((x // 256, y // 256))
        self.Refresh(False)

    def on_paint(self, event):

        # print(current_thread().getName())
        w, h = self.GetClientSize()
        dc = wx.BufferedPaintDC(self)

        dc.Clear()
        dc.DestroyClippingRegion()
        for x in range(self.map_x//256*256, ((self.map_x+w)//256+1)*256, 256):
            for y in range(self.map_y//256*256, ((self.map_y+h)//256+1)*256, 256):
                x2, y2 = x//256, y//256
                if 0 <= x2 < self.bounds[0] and 0 <= y2 < self.bounds[1]:
                    if (x2, y2) in self.bitmaps.keys():
                        if (x2, y2) in self.bitmaps_to_update:
                            if (x2, y2) in self.tiles_dict.keys():
                                image_base = self.tiles_dict[(x2, y2)]
                                if self.price_turn_on:
                                    if (x2, y2) in self.price_tiles_dict.keys():
                                        image_base = Image.alpha_composite(image_base, self.price_tiles_dict[(x2, y2)])
                                if self.road_turn_on:
                                    if (x2, y2) in self.road_tiles_dict.keys():
                                        image_base = Image.alpha_composite(image_base, self.road_tiles_dict[(x2, y2)])
                                '''if (x2, y2) in self.shapes_images.keys():
                                    x3 = self.shapes_images[(x2, y2)][0][0] - x2*256
                                    y3 = self.shapes_images[(x2, y2)][0][1] - y2*256
                                    if x3 < 0 or y3 < 0:
                                        print()
                                    image_base.alpha_composite(self.shapes_images[(x2, y2)][1], (x3, y3))'''

                                self.bitmaps[(x2, y2)] = wx.Bitmap.FromBufferRGBA(256, 256, image_base.tobytes())
                                self.bitmaps_to_update.remove((x2, y2))
                                self.buff = True
                    else:
                        dist = (self.map_x + self.GetSize()[0] // 2 - x) ** 2 + (self.map_y + self.GetSize()[1] // 2 - y) ** 2
                        self.bitmaps[(x2, y2)] = self.missing_image
                        self.tile_queue.put(LoadTask(self.map_tiles_url, dist, (x2, y2), self.tiles_dict, x=x2, y=y2, z=self.zoom))
                        self.already_updated_bitmaps.add((x2, y2))
                        if self.price_turn_on:
                            self.tile_queue.put(
                                LoadTask(self.price_tiles_url, dist + 1, (x2, y2), self.price_tiles_dict, x=x2, y=y2, z=self.zoom, price=150000,
                                         range=0.5))
                        if self.road_turn_on:
                            self.tile_queue.put(
                                LoadTask(self.road_tiles_url, dist + 20000, (x2, y2), self.road_tiles_dict, x=x2, y=y2, z=self.zoom, start_id=self.current_id,
                                         max_dist=mx_dist))
                else:
                    self.bitmaps[(x // 256, y // 256)] = self.missing_image

                dc.DrawBitmap(self.bitmaps[(x2, y2)], x - self.map_x, y - self.map_y)

        '''mult = 2**(self.server_zoom-self.zoom)
        for k, v in self.shapes_bitmaps.items():
            bounds = self.shapes_dict[k][0]
            dc.DrawBitmap(v, bounds[0]//mult-self.map_x, bounds[1]//mult-self.map_y)'''

    def map_refresher(self):
        while True:
            self.loader_lock.acquire()
            wx.PostEvent(self, RefreshEvent())
            self.loader_lock.release()
            time.sleep(0.5)

    def tile_loader(self):
        while True:

            to_load = self.tile_queue.get()
            image = Image.open(io.BytesIO(requests.get(to_load.url.format(**to_load.params), stream=False).content))
            image = image.convert("RGBA")
            self.loader_lock.acquire()
            to_load.place[to_load.key] = image
            # self.loaded_tiles_set.add(to_load)
            self.bitmaps_to_update.add(to_load.key)

            # wx.PostEvent(self, wx.EVT_PAINT())
            self.Refresh()

            # time.sleep(0.01)
            self.loader_lock.release()
            
    def center_on(self, obj):
        mult = 2**(self.server_zoom - self.zoom)
        print("from:", self.map_x, self.map_y)
        print("to:", round(obj["result"]["center"][0] / mult),round(obj["result"]["center"][1] / mult))
        self.map_x = round(obj["result"]["center"][0] / mult) - self.GetSize()[0]//2
        self.map_y = round(obj["result"]["center"][1] / mult) - self.GetSize()[1]//2
        self.Refresh()


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

        sc = wx.ScrolledWindow(self.panel, size=(300, 100))

        sc.SetScrollbars(1, 1, 1, 2000)
        sc.SetScrollRate(5, 5)
        sc.Bind(wx.EVT_SIZE, self.size)
        p = wx.Panel(sc, pos=(0, 0), size=(1, 2000))
        pbx = wx.BoxSizer()
        pbx.Add(p, 1, wx.ALL|wx.EXPAND)
        sc.SetSizer(pbx)
        p.SetBackgroundColour(wx.Colour(255, 255, 255))

        line = wx.StaticLine(p, size=(1, 2000))

        self.box_sizer.Add(self.map, 9, wx.ALL | wx.EXPAND)
        self.box_sizer.Add(sc, 2, wx.ALL | wx.EXPAND)

        self.Bind(wx.EVT_CLOSE, self.on_close)
        another_sizer = wx.BoxSizer(wx.VERTICAL)
        search_sizer = wx.BoxSizer()
        another_sizer.AddSpacer(25)
        another_sizer.Add(search_sizer, 1, wx.ALL|wx.EXPAND)

        self.search_entry = wx.TextCtrl(p, pos=(0, 0), size=(100, 25), style=wx.TE_PROCESS_ENTER)

        class Completer(wx.TextCompleterSimple):

            def __init__(self, app):
                wx.TextCompleterSimple.__init__(self)
                self.s = []
                self.ind = 0
                self.app = app

            def GetCompletions(self, prefix, res):

                print("Completion for", prefix)

            def Start(self, prefix):
                print("Completion for", prefix)
                self.ind = 0
                s = self.app.search_entry.GetValue()
                resp = requests.get("http://127.0.0.1/api/search/predict?text={}&suggestions=10".format(s)).text
                ans = json.loads(resp)
                self.s = ans["suggestions"]
                return True

            def GetNext(self):
                if self.ind < len(self.s):
                    self.ind += 1
                    return self.s[self.ind-1]
                else:
                    return ""
        # self.tx.Bind(wx.EVT_TEXT, text_update)
        self.search_entry.AutoComplete(Completer(self))

        def fuck(event):
            if event.KeyCode == 13:
                Thread(target=self.find, daemon=True).start()
                self.search_entry.SelectNone()
                self.search_entry.SetInsertionPointEnd()
                self.map.SetFocus()
            event.Skip()

        self.search_entry.Bind(wx.EVT_KEY_DOWN, fuck)

        search_sizer.Add(self.search_entry, 1, wx.ALIGN_RIGHT|wx.LEFT, 5)
        search_sizer.Add(wx.Button(p, size=(50, 25), label="Поиск"), 0, wx.ALIGN_RIGHT|wx.LEFT, 2)
        self.search_entry.SetFont(wx.Font(12, wx.FONTFAMILY_DEFAULT, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_BOLD, False))
        p.SetSizer(another_sizer)
        self.panel.SetSizer(self.box_sizer)
        self.SetClientSize((750, 500))

    def on_close(self, event):
        self.Destroy()

    def size(self, event):
        print("sizing")
        
    def find(self):
        print("finding")
        s = self.search_entry.GetValue()
        ans = json.loads(requests.get("http://127.0.0.1/api/search/string?text={}".format(s)).text)
        if ans["status"] == "found":
            self.map.center_on(ans)


def main():
    app = wx.App(False)
    frame = PropertyHeatMap()
    frame.Show()

    app.MainLoop()


if __name__ == '__main__':
    main()
