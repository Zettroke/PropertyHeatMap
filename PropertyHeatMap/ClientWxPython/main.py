import wx, wx.lib.newevent
from PIL import Image, ImageDraw
import queue
import io
import requests
from threading import Thread, Lock, current_thread
import json
import time


# 2k18 yaaaay

EVT_REFRESH = wx.NewId()
mx_dist = 18000


# ShowApartmentsEvent, EVT_SHOW_APARTMENTS = wx.lib.newevent.NewEvent()


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

    ip = "127.0.0.1"

    base_server_url = "http://" + ip + "/"
    map_tiles_url = "http://" + ip + "/image/z{z}/{x}.{y}.png"
    price_tiles_url = "http://" + ip + "/api/tile/price?z={z}&x={x}&y={y}&price={price}&range={range}"
    road_tiles_url = "http://" + ip + "/api/tile/road?z={z}&x={x}&y={y}&start_id={start_id}&max_dist={max_dist}&foot={foot}"
    point_search_url = "http://" + ip + "/api/search/point?z={z}&x={x}&y={y}"

    def __init__(self, *args, **kwargs):
        self.parent = kwargs["parent"]
        del kwargs["parent"]
        super(Map, self).__init__(*args, **kwargs)
        
        self.tiles_dict = {}

        self.shapes_dict = {}
        self.server_zoom = 19
        self.shapes_bitmaps = {}
        self.shapes_images = {}

        # price stuff
        self.price_tiles_dict = {}
        self.price_turn_on = False

        self.price = 150000
        self.p_range = 0.5

        self.road_tiles_dict = {}
        self.road_turn_on = False
        self.foot = True

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
        # Thread(target=self.tile_loader, daemon=True, name="loader 3").start()

    def my_refresh(self, event):
        # print(current_thread().getName())
        self.Refresh()

    def request_location(self, x, y):
        print("request")
        ans = json.loads(requests.get(self.point_search_url.format(x=x, y=y, z=self.zoom)).text, encoding="utf-8")
        if ans["status"] == "success":
            
            if ans["objects"][0]["id"] not in self.shapes_dict:
                print(ans["objects"][0]["id"])
                # print(json.dumps(ans, ensure_ascii=False, indent=2))
                max_x, max_y = 0, 0
                min_x, min_y = 2 ** 32 - 1, 2 ** 32 - 1
                for p in ans["objects"][0]["points"]:
                    max_x, max_y = max(max_x, p[0]), max(max_y, p[1])
                    min_x, min_y = min(min_x, p[0]), min(min_y, p[1])

                self.shapes_dict.clear()
                self.shapes_dict[ans["objects"][0]["id"]] = ([min_x, min_y, max_x, max_y], ans["objects"][0]["points"])
                self.render_shapes_to_bitmaps()
                self.road_turn_on = True
                self.price_turn_on = False
                self.update_all_bitmaps()
                self.current_id = ans["objects"][0]["id"]

                w, h = self.GetClientSize()
                for x in range(self.map_x // 256 * 256, ((self.map_x + w) // 256 + 1) * 256, 256):
                    for y in range(self.map_y // 256 * 256, ((self.map_y + h) // 256 + 1) * 256, 256):
                        x2, y2 = x // 256, y // 256
                        dist = (self.map_x + self.GetSize()[0] // 2 - x) ** 2 + (self.map_y + self.GetSize()[1] // 2 - y) ** 2
                        self.tile_queue.put(
                            LoadTask(self.road_tiles_url, dist + 20000, (x2, y2), self.road_tiles_dict, x=x2, y=y2, z=self.zoom, start_id=self.current_id, max_dist=mx_dist, foot=self.foot))
                data = ans["objects"][0]["data"]
                self.parent.show_address(data["addr:street"] + ", " + data["addr:housenumber"])
                wx.CallAfter(self.parent.price_turn_off)

                if "apartments" in data.keys():
                    wx.CallAfter(self.parent.show_apartments, data["apartments"])
                    # self.parent.show_apartments(data["apartments"])
                    # self.parent.show_apartments(data["apartments"])
                    # self.data.show_apartments()
            else:
                del self.shapes_dict[ans["objects"][0]["id"]]
                # del self.shapes_bitmaps[ans["objects"][0]["id"]]

                self.road_turn_on = False
                self.road_tiles_dict.clear()
                self.shapes_images.clear()
                self.update_all_bitmaps()
                self.parent.show_address("")

            self.Refresh()

    def render_shapes_to_bitmaps(self):
        self.shapes_images.clear()
        for k, v in self.shapes_dict.items():
            AAmult = 4
            mult = 2 ** (self.server_zoom - self.zoom) // AAmult
            real_mult = 2 ** (self.server_zoom - self.zoom)
            bounds = v[0]
            bounds[0] -= AAmult*2
            bounds[1] -= AAmult*2
            bounds[2] += AAmult*2
            bounds[3] += AAmult*2
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
            draw.polygon(poly, fill=0x550000FF)
            shape = shape.resize((shape.size[0] // AAmult, shape.size[1] // AAmult), Image.ANTIALIAS)
            # self.shapes_bitmaps[k] = wx.Bitmap.FromBufferRGBA(shape.size[0], shape.size[1], shape.tobytes())
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
        self.Refresh()

    def on_paint(self, event):
        w, h = self.GetClientSize()
        dc = wx.AutoBufferedPaintDC(self)

        dc.Clear()
        # dc.DestroyClippingRegion()
        for x in range(self.map_x//256*256, ((self.map_x+w)//256+1)*256, 256):
            for y in range(self.map_y//256*256, ((self.map_y+h)//256+1)*256, 256):
                x2, y2 = x//256, y//256
                if 0 <= x2 < self.bounds[0] and 0 <= y2 < self.bounds[1]:
                    if (x2, y2) in self.bitmaps.keys():
                        if (x2, y2) in self.bitmaps_to_update:
                            if (x2, y2) in self.tiles_dict.keys():
                                image_base = self.tiles_dict[(x2, y2)].copy()
                                if self.price_turn_on:
                                    if (x2, y2) in self.price_tiles_dict.keys():
                                        image_base = Image.alpha_composite(image_base, self.price_tiles_dict[(x2, y2)])
                                if self.road_turn_on:
                                    if (x2, y2) in self.road_tiles_dict.keys():
                                        image_base = Image.alpha_composite(image_base, self.road_tiles_dict[(x2, y2)])
                                if (x2, y2) in self.shapes_images.keys():
                                    x3 = self.shapes_images[(x2, y2)][0][0] - x2*256
                                    y3 = self.shapes_images[(x2, y2)][0][1] - y2*256
                                    img = self.shapes_images[(x2, y2)][1]
                                    wi = img.size[0]
                                    hi = img.size[1]
                                    if x3 > 0 and y3 > 0:
                                        image_base.alpha_composite(img, (x3, y3))
                                    else:
                                        to_compose = img.crop((x2*256-self.shapes_images[(x2, y2)][0][0], y2*256-self.shapes_images[(x2, y2)][0][1], wi, hi))
                                        image_base.alpha_composite(to_compose, (0, 0))

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
                                LoadTask(self.price_tiles_url, dist + 1, (x2, y2), self.price_tiles_dict, x=x2, y=y2, z=self.zoom, price=self.price,
                                         range=self.p_range))
                        if self.road_turn_on:
                            self.tile_queue.put(
                                LoadTask(self.road_tiles_url, dist + 20000, (x2, y2), self.road_tiles_dict, x=x2, y=y2, z=self.zoom, start_id=self.current_id,
                                         max_dist=mx_dist, foot=self.foot))
                else:
                    self.bitmaps[(x // 256, y // 256)] = self.missing_image

                dc.DrawBitmap(self.bitmaps[(x2, y2)], x - self.map_x, y - self.map_y)

    def tile_loader(self):
        while True:
            self.loader_lock.acquire()
            to_load = self.tile_queue.get()
            self.loader_lock.release()
            image = Image.open(io.BytesIO(requests.get(to_load.url.format(**to_load.params), stream=False).content))
            image = image.convert("RGBA")
            self.loader_lock.acquire()
            to_load.place[to_load.key] = image

            self.bitmaps_to_update.add(to_load.key)

            self.Refresh()

            self.loader_lock.release()
            
    def center_on(self, obj):
        mult = 2**(self.server_zoom - self.zoom)
        print("from:", self.map_x, self.map_y)
        print("to:", round(obj["result"]["center"][0] / mult),round(obj["result"]["center"][1] / mult))
        self.map_x = round(obj["result"]["center"][0] / mult) - self.GetSize()[0]//2
        self.map_y = round(obj["result"]["center"][1] / mult) - self.GetSize()[1]//2
        self.Refresh()

    def turn_on_price(self):
        self.price_turn_on = True
        self.bitmaps.clear()
        self.Refresh()

    def turn_off_price(self):
        self.price_turn_on = False
        self.update_all_bitmaps()
        self.price_tiles_dict.clear()
        self.Refresh()


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
            return self.s[self.ind - 1]
        else:
            return ""


class PropertyHeatMap(wx.Frame):
    def __init__(self):
        super().__init__(None)
        self.panel = wx.Panel(self)
        self.box_sizer = wx.BoxSizer(wx.HORIZONTAL)
        self.SetTitle('PropertyHeatMap')

        self.Center()
        self.map = Map(self.panel, parent=self)

        self.sc = wx.ScrolledWindow(self.panel, size=(300, 100))

        self.sc.SetScrollbars(1, 1, 1, 2000)
        self.sc.SetScrollRate(5, 5)
        self.p = wx.Panel(self.sc, pos=(0, 0), size=(1, 2000))
        pbx = wx.BoxSizer()
        pbx.Add(self.p, 1, wx.ALL|wx.EXPAND)
        self.sc.SetSizer(pbx)
        self.p.SetBackgroundColour(wx.Colour(235, 235, 235))

        line = wx.StaticLine(self.p, size=(1, 5000))

        self.box_sizer.Add(self.map, 9, wx.ALL | wx.EXPAND)
        self.box_sizer.Add(self.sc, 2, wx.ALL | wx.EXPAND)

        self.Bind(wx.EVT_CLOSE, self.on_close)
        another_sizer = wx.BoxSizer(wx.VERTICAL)
        search_sizer = wx.BoxSizer()
        another_sizer.AddSpacer(5)
        another_sizer.Add(search_sizer, 0, wx.ALL|wx.EXPAND)

        self.search_entry = wx.TextCtrl(self.p, pos=(0, 0), size=(100, 25), style=wx.TE_PROCESS_ENTER)

        
        # self.tx.Bind(wx.EVT_TEXT, text_update)
        self.search_entry.AutoComplete(Completer(self))

        self.search_entry.Bind(wx.EVT_KEY_DOWN, self.enter)
        
        search_sizer.Add(self.search_entry, 1, wx.ALIGN_RIGHT|wx.LEFT, 5)

        line1 = wx.StaticLine(self.p, size=(300, 2), style=wx.LI_VERTICAL)
        another_sizer.Add(line1, 0, wx.TOP | wx.BOTTOM | wx.EXPAND, 5)
        search_sizer.Add(wx.Button(self.p, size=(50, 25), label="Поиск"), 0, wx.ALIGN_RIGHT|wx.LEFT, 2)
        self.search_entry.SetFont(wx.Font(12, wx.FONTFAMILY_DEFAULT, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_BOLD, False))
        # another_sizer.AddSpacer(5)

        price_choose_sizer = wx.BoxSizer(wx.HORIZONTAL)

        self.price = wx.TextCtrl(self.p)
        self.price.SetLabel(str(self.map.price))
        self.p_range = wx.TextCtrl(self.p, size=(50, 25))
        self.p_range.SetLabel(str(round(self.map.p_range * 100)) + "%")
        price_choose_sizer.Add(wx.StaticText(self.p, label="Цена "), 0, wx.EXPAND | wx.TOP, 5)
        price_choose_sizer.Add(self.price, 10, wx.EXPAND | wx.RIGHT, 2)

        price_choose_sizer.Add(wx.StaticText(self.p, label="Диапозон "), 0, wx.EXPAND | wx.TOP, 5)
        price_choose_sizer.Add(self.p_range, 1, wx.EXPAND | wx.ALL)

        another_sizer.Add(price_choose_sizer, 0, wx.LEFT | wx.BOTTOM, 4)

        self.button = wx.Button(self.p, label="Включить")
        self.button.Bind(wx.EVT_BUTTON, self.price_turn_on)

        another_sizer.Add(self.button, 0, wx.ALIGN_CENTER)
        rbox = wx.RadioBox(self.p, style=wx.RA_SPECIFY_COLS, majorDimension=2, choices=["Пешком", "Машина"])
        rbox.Bind(wx.EVT_RADIOBOX, self.foot_change)
        another_sizer.Add(rbox, 0, wx.ALIGN_CENTER)

        self.choose_text = wx.StaticText(self.p, label="Выбрано: ", pos=(1, 1))
        self.choose_text.SetFont(wx.Font(10, wx.FONTFAMILY_DECORATIVE, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_BOLD, False))
        line2 = wx.StaticLine(self.p, size=(350, 2), style=wx.LI_VERTICAL)
        another_sizer.Add(line2, 0, wx.TOP|wx.BOTTOM|wx.EXPAND, 5)
        another_sizer.Add(self.choose_text, 0, wx.LEFT|wx.EXPAND, 5)
        another_sizer.AddSpacer(4)
        self.apartments_panel = wx.Panel(self.p)
        self.apartments_panel.SetBackgroundColour(wx.Colour(255, 255, 255))
        self.apart_sizer = wx.BoxSizer(wx.VERTICAL)
        self.apartments_panel.SetSizer(self.apart_sizer)
        # tx = wx.StaticText(self.apartments_panel, label="dsa;lkdlas", pos=(0, 0))

        another_sizer.Add(self.apartments_panel, 1, wx.EXPAND|wx.LEFT, 1)

        self.p.SetSizer(another_sizer)
        self.panel.SetSizer(self.box_sizer)
        self.SetClientSize((900, 500))
        self.to_remove = []

    def on_close(self, event):
        self.Destroy()
        
    def find(self):
        print("finding")
        s = self.search_entry.GetValue()
        ans = json.loads(requests.get("http://127.0.0.1/api/search/string?text={}".format(s)).text)
        if ans["status"] == "found":
            self.map.center_on(ans)

    def enter(self, event):
        if event.KeyCode == 13:
            Thread(target=self.find, daemon=True).start()
            self.search_entry.SelectNone()
            self.search_entry.SetInsertionPointEnd()
            self.map.SetFocus()
        event.Skip()

    def show_address(self, addr):
        self.choose_text.SetLabelText("Выбрано: " + addr)

    def show_apartments(self, aparts):
        # self.apart_sizer = wx.BoxSizer(wx.VERTICAL)
        # self.apartments_panel.SetSizer(self.apart_sizer)
        for i in range(self.apart_sizer.GetItemCount()):
            self.apart_sizer.Remove(0)

        for w in self.to_remove:
            try:
                w.Destroy()
            except Exception:
                pass
        self.to_remove.clear()
        for ind in range(len(aparts)):
            data = aparts[ind]["full data"]
            curr_sizer = wx.BoxSizer(wx.HORIZONTAL)
            column1_sizer = wx.BoxSizer(wx.VERTICAL)
            column2_sizer = wx.BoxSizer(wx.VERTICAL)
            for w in ("Адрес", "url", "coords"):
                if w in data.keys():
                    del data[w]
            for k, v in data.items():

                tx1 = wx.StaticText(self.apartments_panel, pos=(0, 0), label=str(k) + ":")
                tx1.SetFont(wx.Font(wx.Font(10, wx.FONTFAMILY_DEFAULT, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_NORMAL, False)))
                column1_sizer.Add(tx1, 1, wx.EXPAND|wx.ALIGN_LEFT|wx.LEFT, 5)
                self.to_remove.append(tx1)

                if not v:
                    v = "-"
                tx2 = wx.StaticText(self.apartments_panel, pos=(0, 0), label=str(v))
                tx2.SetFont(wx.Font(wx.Font(10, wx.FONTFAMILY_DEFAULT, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_NORMAL, False)))
                column2_sizer.Add(tx2, 1, wx.EXPAND | wx.ALIGN_RIGHT | wx.LEFT, 5)
                self.to_remove.append(tx2)

            curr_sizer.Add(column1_sizer, 0)
            curr_sizer.Add(column2_sizer, 0)

            self.apart_sizer.Add(curr_sizer, 0)
            line = wx.StaticLine(self.apartments_panel, size=(300, 5))
            self.apart_sizer.Add(line, 0, wx.TOP|wx.BOTTOM|wx.EXPAND, 5)
            self.to_remove.append(line)
            # self.apart_sizer.Layout()
            # self.Refresh()
        size = self.apart_sizer.GetMinSize()
        self.p.SetSize((300, size.Height+200))
        self.sc.SetScrollbars(1, 1, 1, size.Height+200)
        self.sc.SetScrollRate(10, 10)
        self.sc.Layout()
        self.Refresh()

    def price_turn_on(self, event=None):
        self.button.SetLabel("Отключить")
        self.button.Bind(wx.EVT_BUTTON, self.price_turn_off)
        self.map.price = int(self.price.GetValue())
        self.map.p_range = int(self.p_range.GetValue()[:-1])/100
        self.map.turn_on_price()

    def price_turn_off(self, event=None):
        self.button.SetLabel("Включить")
        self.button.Bind(wx.EVT_BUTTON, self.price_turn_on)
        self.map.turn_off_price()

    def foot_change(self, event):
        if event.String == "Пешком":
            self.map.foot = True
        else:
            self.map.foot = False



def main():
    app = wx.App(False)
    frame = PropertyHeatMap()
    frame.Show()

    app.MainLoop()


if __name__ == '__main__':
    main()
