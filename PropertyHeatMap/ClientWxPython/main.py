import wx, wx.lib.newevent
from PIL import Image, ImageDraw
import queue
import io
import requests
from threading import Thread, Lock, current_thread, Condition
import json
import time
import sys

server_address = "127.0.0.1"
ip_set_manually = False
if len(sys.argv) > 1:
    ind = sys.argv.index("-ip")
    if ind != -1:
        ip_set_manually = True
        server_address = sys.argv[ind+1]
        print("ip set manually to " + server_address)

# 2k18 yaaaay

EVT_REFRESH = wx.NewId()
mx_dist = 18000
mx_dist_stuff = 6000

if not ip_set_manually:
    try:
        data = requests.get("https://pastebin.com/raw/jkUmzJZ0", timeout=2).text
        ip, message = data.split("\n", 1)
        server_address = ip.replace("\r", "")

        print("ip set to " + server_address)
        print(len(server_address))
        print("message:", message)
    except Exception:
        print("Failed to get ip!")

base_server_url = "http://" + server_address + "/"
map_tiles_url = "http://" + server_address + "/image/z{z}/{x}.{y}.png"
price_tiles_url = "http://" + server_address + "/api/tile/price?z={z}&x={x}&y={y}&price={price}&range={range}"
road_tiles_url = "http://" + server_address + "/api/tile/road?z={z}&x={x}&y={y}&start_id={start_id}&max_dist={max_dist}&foot={foot}"
point_search_url = "http://" + server_address + "/api/search/point?z={z}&x={x}&y={y}"
suggestion_url = "http://" + server_address + "/api/search/predict?text={}&suggestions=10"
close_things_url = "http://" + server_address + "/api/search/close_objects?id={id}&max_dist={max_dist}&foot={foot}&max_num={max_num}"


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
        self.parent = kwargs["parent"]
        del kwargs["parent"]
        super(Map, self).__init__(*args, **kwargs)

        self.update_cond = Condition()
        
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
        self.available_zoom_levels = tuple(map(int, requests.get(base_server_url + "image/zoom_levels").text.split()))
        self.bounds = tuple(map(int, requests.get(base_server_url + "image/z" + str(self.zoom) + "/config", "r").text.split()))
        self.tile_queue = queue.PriorityQueue()
        self.Connect(-1, -1, EVT_REFRESH, self.my_refresh)
        self.loader_lock = Lock()
        self.need_refresh = True

        Thread(target=self.tile_loader, daemon=True, name="loader 1").start()
        Thread(target=self.tile_loader, daemon=True, name="loader 2").start()
        # Thread(target=self.tile_loader, daemon=True, name="loader 3").start()

    def my_refresh(self, event):

        self.Refresh()

    def request_location(self, x, y):
        print("request")
        ans = json.loads(requests.get(point_search_url.format(x=x, y=y, z=self.zoom)).text, encoding="utf-8")
        if ans["status"] == "success":
            
            if ans["objects"][0]["id"] not in self.shapes_dict:
                print(ans["objects"][0]["id"])
                stuff_close = json.loads(requests.get(close_things_url.format(id=ans["objects"][0]["id"], max_dist=mx_dist_stuff, max_num=300, foot=True)).text, encoding="utf-8")
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
                            LoadTask(road_tiles_url, dist + 20000, (x2, y2), self.road_tiles_dict, x=x2, y=y2, z=self.zoom, start_id=self.current_id, max_dist=mx_dist, foot=self.foot))
                data = ans["objects"][0]["data"]
                self.parent.show_address(data["addr:street"] + ", " + data["addr:housenumber"])
                wx.CallAfter(self.parent.price_turn_off)

                if "apartments" in data.keys():
                    wx.CallAfter(self.parent.show_apartments, data["apartments"])
                wx.CallAfter(self.parent.show_close_things, stuff_close["objects"])
            else:
                del self.shapes_dict[ans["objects"][0]["id"]]
                # del self.shapes_bitmaps[ans["objects"][0]["id"]]
                wx.CallAfter(self.parent.clear_aparts)
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
        event.Skip()

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
            self.bounds = tuple(map(int, requests.get(base_server_url + "image/z" + str(self.zoom) + "/config", "r").text.split()))

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
        dc.DestroyClippingRegion()
        self.loader_lock.acquire()
        for x in range(self.map_x//256*256, ((self.map_x+w)//256+1)*256+1, 256):
            for y in range(self.map_y//256*256, ((self.map_y+h)//256+1)*256+1, 256):
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
                                image_base = image_base.convert("RGB")
                                self.bitmaps[(x2, y2)] = wx.Bitmap.FromBuffer(256, 256, image_base.tobytes())
                                self.bitmaps_to_update.remove((x2, y2))
                    else:
                        dist = (self.map_x + self.GetSize()[0] // 2 - x) ** 2 + (self.map_y + self.GetSize()[1] // 2 - y) ** 2
                        self.bitmaps[(x2, y2)] = self.missing_image
                        self.tile_queue.put(LoadTask(map_tiles_url, dist, (x2, y2), self.tiles_dict, x=x2, y=y2, z=self.zoom))
                        self.already_updated_bitmaps.add((x2, y2))
                        if self.price_turn_on:
                            self.tile_queue.put(
                                LoadTask(price_tiles_url, dist + 1, (x2, y2), self.price_tiles_dict, x=x2, y=y2, z=self.zoom, price=self.price,
                                         range=self.p_range))
                        if self.road_turn_on:

                            self.tile_queue.put(
                                LoadTask(road_tiles_url, dist + 20000, (x2, y2), self.road_tiles_dict, x=x2, y=y2, z=self.zoom, start_id=self.current_id,
                                         max_dist=mx_dist, foot=self.foot))
                else:
                    self.bitmaps[(x // 256, y // 256)] = self.missing_image

                dc.DrawBitmap(self.bitmaps[(x2, y2)], x - self.map_x, y - self.map_y)
        self.loader_lock.release()

    def tile_loader(self):
        while True:

            to_load = self.tile_queue.get()
            image = Image.open(io.BytesIO(requests.get(to_load.url.format(**to_load.params), stream=False).content))
            image = image.convert("RGBA")
            self.loader_lock.acquire()
            to_load.place[to_load.key] = image
            self.bitmaps_to_update.add(to_load.key)
            wx.CallAfter(self.Refresh)

            self.loader_lock.release()
            
    def center_on(self, obj):
        mult = 2**(self.server_zoom - self.zoom)
        # print("from:", self.map_x, self.map_y)
        # print("to:", round(obj["result"]["center"][0] / mult),round(obj["result"]["center"][1] / mult))
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
        pass

    def Start(self, prefix):
        self.ind = 0
        s = self.app.search_entry.GetValue()
        resp = requests.get(suggestion_url.format(s)).text
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
        self.map = Map(self.panel, parent=self)
        self.sc = wx.ScrolledWindow(self.panel, size=(300, 100))
        self.p = wx.Panel(self.sc, pos=(0, 0), size=(1, 2000))

        self.box_sizer = wx.BoxSizer(wx.HORIZONTAL)
        self.p_range = wx.TextCtrl(self.p, size=(50, 25))
        self.search_entry = wx.TextCtrl(self.p, pos=(0, 0), size=(100, 25), style=wx.TE_PROCESS_ENTER)
        self.price = wx.TextCtrl(self.p)

        self.Center()
        self.to_remove = []

        self.test_bool = True
        self.current_show = "apart"
        self.data_panel_size = wx.BoxSizer(wx.VERTICAL)

        self.apart_sizer = wx.BoxSizer(wx.VERTICAL)
        self.close_things_sizer = wx.BoxSizer(wx.HORIZONTAL)

        self.apartments_panel = wx.Panel(self.p)
        self.close_things_panel = wx.Panel(self.p)

        self.turn_things_and_aparts = wx.Button(self.p, label="Жилье")
        self.turn_things_and_aparts.Bind(wx.EVT_BUTTON, self.switch_views)
        # self.close_things_panel.Hide()

        self.UiInit()


    def UiInit(self):
        self.SetTitle('PropertyHeatMap')
        self.sc.SetScrollbars(1, 1, 1, 2000)
        self.sc.SetScrollRate(5, 5)

        pbx = wx.BoxSizer()
        pbx.Add(self.p, 1, wx.ALL | wx.EXPAND)
        self.sc.SetSizer(pbx)
        self.p.SetBackgroundColour(wx.Colour(235, 235, 235))

        self.box_sizer.Add(self.map, 9, wx.ALL | wx.EXPAND)
        self.box_sizer.Add(self.sc, 2, wx.ALL | wx.EXPAND)
        self.Bind(wx.EVT_CLOSE, self.on_close)

        search_sizer = wx.BoxSizer()
        self.data_panel_size.AddSpacer(5)
        self.data_panel_size.Add(search_sizer, 0, wx.ALL | wx.EXPAND)

        self.search_entry.AutoComplete(Completer(self))

        self.search_entry.Bind(wx.EVT_KEY_DOWN, self.enter)

        search_sizer.Add(self.search_entry, 1, wx.ALIGN_RIGHT | wx.LEFT, 5)

        line1 = wx.StaticLine(self.p, size=(300, 2), style=wx.LI_VERTICAL)
        self.data_panel_size.Add(line1, 0, wx.TOP | wx.BOTTOM | wx.EXPAND, 5)
        search_sizer.Add(wx.Button(self.p, size=(50, 25), label="Поиск"), 0, wx.ALIGN_RIGHT | wx.LEFT, 2)
        self.search_entry.SetFont(wx.Font(12, wx.FONTFAMILY_DEFAULT, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_BOLD, False))
        # self.another_sizer.AddSpacer(5)

        price_choose_sizer = wx.BoxSizer(wx.HORIZONTAL)

        self.price.SetLabel(str(self.map.price))

        self.p_range.SetLabel(str(round(self.map.p_range * 100)) + "%")
        price_choose_sizer.Add(wx.StaticText(self.p, label="Цена "), 0, wx.EXPAND | wx.TOP, 5)
        price_choose_sizer.Add(self.price, 10, wx.EXPAND | wx.RIGHT, 2)

        price_choose_sizer.Add(wx.StaticText(self.p, label="Диапозон "), 0, wx.EXPAND | wx.TOP, 5)
        price_choose_sizer.Add(self.p_range, 1, wx.EXPAND | wx.ALL)

        self.data_panel_size.Add(price_choose_sizer, 0, wx.LEFT | wx.BOTTOM, 4)

        self.button = wx.Button(self.p, label="Включить")
        self.button.Bind(wx.EVT_BUTTON, self.price_turn_on)

        self.data_panel_size.Add(self.button, 0, wx.ALIGN_CENTER)
        rbox = wx.RadioBox(self.p, style=wx.RA_SPECIFY_COLS, majorDimension=2, choices=["Пешком", "Машина"])
        rbox.Bind(wx.EVT_RADIOBOX, self.foot_change)
        self.data_panel_size.Add(rbox, 0, wx.ALIGN_CENTER)

        self.choose_text = wx.StaticText(self.p, label="Выбрано: ", pos=(1, 1))
        self.choose_text.SetFont(wx.Font(10, wx.FONTFAMILY_DECORATIVE, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_BOLD, False))
        line2 = wx.StaticLine(self.p, size=(350, 2), style=wx.LI_VERTICAL)

        self.data_panel_size.Add(self.turn_things_and_aparts, 0, wx.ALIGN_RIGHT)

        self.data_panel_size.Add(line2, 0, wx.TOP | wx.BOTTOM | wx.EXPAND, 5)
        self.data_panel_size.Add(self.choose_text, 0, wx.LEFT | wx.EXPAND, 5)
        self.data_panel_size.AddSpacer(4)

        self.apartments_panel.SetBackgroundColour(wx.Colour(255, 255, 255))
        self.apartments_panel.SetSizer(self.apart_sizer)

        self.data_panel_size.Add(self.apartments_panel, 1, wx.EXPAND | wx.LEFT, 2)
        self.data_panel_size.Add(self.close_things_panel, 1, wx.EXPAND | wx.LEFT, 2)
        self.close_things_panel.Hide()

        self.p.SetSizer(self.data_panel_size)
        self.panel.SetSizer(self.box_sizer)
        self.SetClientSize((900, 500))

    def on_close(self, event):
        self.Destroy()
        
    def find(self):
        s = self.search_entry.GetValue()
        ans = json.loads(requests.get("http://"+server_address+"/api/search/string?text={}".format(s)).text)
        if ans["status"] == "found":
            print("found")
            self.map.center_on(ans)

    def switch_views(self, event):
        if self.test_bool:
            self.turn_things_and_aparts.SetLabelText("Инфраструктура")
            self.apartments_panel.Hide()
            self.close_things_panel.Show()
            self.data_panel_size.Layout()
            self.apart_sizer.Layout()
            self.p.SetSize(self.p.GetSize()[0], self.close_things_sizer.GetMinSize()[1] + 175)
            self.sc.SetScrollbars(1, 1, 1, self.close_things_sizer.GetMinSize()[1] + 175)
        else:
            self.turn_things_and_aparts.SetLabelText("Жилье")
            self.apartments_panel.Show()
            self.close_things_panel.Hide()
            self.data_panel_size.Layout()
            self.apart_sizer.Layout()
            self.p.SetSize((300, self.apart_sizer.GetMinSize().Height+170))
            self.sc.SetScrollbars(1, 1, 1, self.apart_sizer.GetMinSize().Height+170)
        self.test_bool = not self.test_bool

        event.Skip()

    def enter(self, event):
        if event.KeyCode == 13:
            Thread(target=self.find, daemon=True).start()
            self.search_entry.SelectNone()
            self.search_entry.SetInsertionPointEnd()
            self.map.SetFocus()
        event.Skip()

    def show_address(self, addr):
        self.choose_text.SetLabelText("Выбрано: " + addr)

    def clear_aparts(self):
        for i in range(self.apart_sizer.GetItemCount()):
            self.apart_sizer.Remove(0)

        for w in self.to_remove:
            try:
                w.Destroy()
            except Exception:
                pass
        self.to_remove.clear()

    def clear_close_things(self):
        for i in range(self.close_things_sizer.GetItemCount()):
            self.close_things_sizer.Remove(0)

    def show_apartments(self, aparts):

        self.clear_aparts()
        self.clear_close_things()
        for ind in range(len(aparts)):
            data = aparts[ind]["full data"]

            curr_sizer = wx.BoxSizer(wx.HORIZONTAL)
            column1_sizer = wx.BoxSizer(wx.VERTICAL)
            column2_sizer = wx.BoxSizer(wx.VERTICAL)
            for w in ("Адрес", "url", "coords"):
                if w in data.keys():
                    del data[w]
            st1 = ""
            st2 = ""
            for k, v in data.items():
                st1 += str(k) + ":\n"

                if not v:
                    v = "-"
                st2 += str(v) + "\n"
            tx1 = wx.StaticText(self.apartments_panel, pos=(0, 0), label=st1)
            tx2 = wx.StaticText(self.apartments_panel, pos=(0, 0), label=st2)
            column1_sizer.Add(tx1, 1, wx.EXPAND | wx.ALIGN_LEFT | wx.LEFT, 5)
            column2_sizer.Add(tx2, 1, wx.EXPAND | wx.ALIGN_RIGHT | wx.LEFT, 5)
            curr_sizer.Add(column1_sizer, 1, wx.EXPAND)
            curr_sizer.Add(column2_sizer, 1, wx.EXPAND)

            self.apart_sizer.Add(curr_sizer, 0, wx.EXPAND)
            line = wx.StaticLine(self.apartments_panel, size=(300, 5))
            self.apart_sizer.Add(line, 0, wx.TOP | wx.BOTTOM | wx.EXPAND, 5)
            self.to_remove.append(tx1)
            self.to_remove.append(tx2)
            self.to_remove.append(line)
        self.apart_sizer.Layout()
        self.p.SetSize((300, self.apart_sizer.GetMinSize().Height+170))
        self.sc.SetScrollbars(1, 1, 1, self.apart_sizer.GetMinSize().Height+170)
        self.sc.SetScrollRate(10, 10)

        self.Refresh()

    def show_close_things(self, data):
        self.clear_close_things()
        names = wx.StaticText(self.close_things_panel)
        times = wx.StaticText(self.close_things_panel)
        st1 = ""
        st2 = ""
        for i in data:
            if "name" in i["data"].keys():
                st1 += i["data"]["name"] + "\n"
                st2 += str(round(i["dist"]/600, 1)) + "мин\n"
        names.SetLabelText(st1)
        times.SetLabelText(st2)
        

        # names.SetMaxSize((self.p.GetSize()[0]-50, 10000))
        names.SetMinSize((100, 100))
        self.close_things_sizer.Add(names, 10, wx.EXPAND|wx.LEFT, 2)
        self.close_things_sizer.Add(times, 1, wx.ALIGN_RIGHT|wx.RIGHT, 2)
        self.close_things_panel.SetSizer(self.close_things_sizer)
        self.close_things_sizer.Layout()

        self.close_things_panel.Refresh()
        print()

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


if __name__ == '__main__':
    app = wx.App(False)
    frame = PropertyHeatMap()
    frame.Show()

    app.MainLoop()