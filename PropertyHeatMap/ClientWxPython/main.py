import wx
from PIL import Image
import queue
import io
import requests
from threading import Thread


class Map(wx.Panel):

    def __init__(self, *args, **kwargs):
        super(Map, self).__init__(*args, **kwargs)
        self.map_folder = "C:/PropertyHeatMap/osm_map_small"
        self.map_tiles_url = "http://178.140.109.241:25565/image/z{z}/{x}.{y}.png"
        self.tiles_dict = {}
        self.loaded_tiles_set = set()
        self.SetBackgroundStyle(wx.BG_STYLE_CUSTOM)
        self.Bind(wx.EVT_PAINT, self.on_paint)
        self.Bind(wx.EVT_CHAR_HOOK, self.key)
        self.Bind(wx.EVT_LEFT_DOWN, self.left_down)
        self.Bind(wx.EVT_LEFT_UP, self.left_up)
        self.Bind(wx.EVT_MOTION, self.on_move)
        self.Bind(wx.EVT_LEAVE_WINDOW, self.left_up)
        self.Bind(wx.EVT_MOUSEWHEEL, self.zoom)
        self.Show()
        self.sx, self.sy = 0, 0
        self.last_pos = (0, 0)
        self.bitmaps = {}
        self.pressed = False
        self.missing_image = wx.Bitmap.FromBuffer(256, 256, Image.new("RGB", (256, 256), 0xCCCCCC).tobytes())
        self.zoom = 16
        self.available_zoom_levels = tuple(map(int, open(self.map_folder+"/zoom_levels", "r").read().split()))
        self.bounds = (9, 9)
        self.tile_queue = queue.PriorityQueue()
        Thread(target=self.tile_loader_process, daemon=True).start()

    def left_down(self, event):
        # print("CLICK")
        self.pressed = True
        self.last_pos = event.GetPosition()

    def left_up(self, event):
        # print("UNCLICK")
        self.pressed = False

    def on_move(self, event):
        if self.pressed:
            self.sx -= event.GetPosition()[0]-self.last_pos[0]
            self.sy -= event.GetPosition()[1]-self.last_pos[1]
            self.last_pos = event.GetPosition()
            self.Refresh(False)
            # print(self.sx, self.sy)

    def key(self, event):
        speed = 25
        if event.KeyCode == 316:
            # right
            self.sx += speed
        elif event.KeyCode == 315:
            # up
            self.sy -= speed
        elif event.KeyCode == 314:
            # left
            self.sx -= speed
        elif event.KeyCode == 317:
            # down
            self.sy += speed
        self.Refresh()

    def zoom(self, event=None):
        z = abs(event.WheelRotation)//event.WheelRotation
        if self.available_zoom_levels[0] <= z+self.zoom <= self.available_zoom_levels[1]:
            if z > 0:
                self.zoom += 1
                self.sx = self.sx * 2 + event.GetPosition()[0]
                self.sy = self.sy * 2 + event.GetPosition()[1]
            else:
                self.zoom -= 1
                self.sx = self.sx // 2 - event.GetPosition()[0] // 2
                self.sy = self.sy // 2 - event.GetPosition()[1] // 2
            self.bounds = tuple(map(int, open(self.map_folder+"/z" + str(self.zoom) + "/config", "r").read().split()))

            self.bitmaps.clear()
            while not self.tile_queue.empty():
                self.tile_queue.get()
            self.tiles_dict.clear()
            self.loaded_tiles_set.clear()
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

        for x in range(self.sx//256*256, ((self.sx+w)//256+1)*256, 256):
            for y in range(self.sy//256*256, ((self.sy+h)//256+1)*256, 256):

                if 0 <= x // 256 < self.bounds[0] and 0 <= y // 256 < self.bounds[1]:
                    if (x//256, y//256) in self.tiles_dict.keys():
                        self.bitmaps[(x//256, y//256)] = wx.Bitmap.FromBuffer(256, 256, self.tiles_dict[(x//256, y//256)].tobytes())
                    else:
                        self.bitmaps[(x // 256, y // 256)] = self.missing_image
                        self.tile_queue.put(((self.sx+self.GetSize()[0]//2-x)**2+(self.sy+self.GetSize()[1]//2-y)**2, (x//256, y//256, self.zoom)))
                else:
                    self.bitmaps[(x // 256, y // 256)] = self.missing_image
                dc.DrawBitmap(self.bitmaps[(x//256, y//256)], x-self.sx, y-self.sy)

    def tile_loader_process(self):
        while True:
            to_load = self.tile_queue.get()[1]
            if to_load not in self.loaded_tiles_set:
                try:
                    image = Image.open(io.BytesIO(requests.get(self.map_tiles_url.format(x=to_load[0], y=to_load[1], z=to_load[2])).content))
                except Exception:
                    print("PLEASE FUCKING DEBUG")
                image = image.convert("RGB")

                self.tiles_dict[(to_load[0], to_load[1])] = image
                self.loaded_tiles_set.add(to_load)
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
