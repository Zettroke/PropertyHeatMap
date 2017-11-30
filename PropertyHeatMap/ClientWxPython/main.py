import wx
from PIL import Image


class Map(wx.Panel):
    def __init__(self, parent):
        super(Map, self).__init__(parent)
        self.SetBackgroundStyle(wx.BG_STYLE_CUSTOM)
        #self.Bind(wx.EVT_SIZE, self.on_size)
        self.Bind(wx.EVT_PAINT, self.on_paint)
        self.Bind(wx.EVT_CHAR_HOOK, self.key)
        self.Bind(wx.EVT_LEFT_DOWN, self.left_down)
        self.Bind(wx.EVT_LEFT_UP, self.left_up)
        self.Bind(wx.EVT_MOTION, self.on_move)
        self.Bind(wx.EVT_LEAVE_WINDOW, self.left_up)
        self.Show()
        self.sx, self.sy = 0, 0
        self.last_pos = (0, 0)
        self.bitmaps = {}
        self.pressed = False
        self.missing_image = wx.Bitmap.FromBuffer(256, 256, Image.new("RGB", (256, 256), 0xCCCCCC).tobytes())
        self.bounds = (18, 18)
        #self.on_paint(None)

    def left_down(self, event):
        print("CLICK")
        self.pressed = True
        self.last_pos = event.GetPosition()

    def left_up(self, event):
        print("UNCLICK")
        self.pressed = False

    def on_move(self, event):
        if self.pressed:
            self.sx -= event.GetPosition()[0]-self.last_pos[0]
            self.sy -= event.GetPosition()[1]-self.last_pos[1]
            self.last_pos = event.GetPosition()
            self.Refresh()
            print(self.sx, self.sy)

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



    def on_size(self, event):
        # event.Skip()
        # self.Refresh()
        self.on_paint(None)

    def on_paint(self, event):
        w, h = self.GetClientSize()
        # w = round(w/256+0.5)*256
        # h = round(h/256+0.5)*256
        dc = wx.AutoBufferedPaintDC(self)
        dc.Clear()
        dc.DestroyClippingRegion()
        #dc.DrawLine(0, 0, w, h)
        #dc.SetPen(wx.Pen(wx.BLACK, 5))
        #dc.DrawCircle(w / 2, h / 2, 100)
        # self.sx, self.sy = 0, 0

        for x in range(self.sx//256*256, ((self.sx+w)//256+1)*256, 256):
            for y in range(self.sy//256*256, ((self.sy+h)//256+1)*256, 256):

                if (x//256, y//256) not in self.bitmaps.keys():
                    if 0 <= x//256 < self.bounds[0] and 0 <= y//256 < self.bounds[1]:
                        self.bitmaps[(x//256, y//256)] = wx.Bitmap("C:/PropertyHeatMap/osm_map_small/z17/{}.{}.png".format(x//256, y//256), wx.BITMAP_TYPE_ANY)
                    else:
                        self.bitmaps[(x // 256, y // 256)] = self.missing_image
                dc.DrawBitmap(self.bitmaps[(x//256, y//256)], x-self.sx, y-self.sy)
        # print("Painted")


class Frame(wx.Frame):
    def __init__(self):
        super(Frame, self).__init__(None)
        self.SetTitle('Lelelel')
        self.SetClientSize((500, 500))
        self.Center()
        self.map = Map(self)
        self.Bind(wx.EVT_CLOSE, self.on_close)
        # self.view.SetScrollbars(1, 1, 1000, 1000)

    def on_close(self, event):
        self.Destroy()

def main():
    app = wx.App(False)
    frame = Frame()
    frame.Show()

    app.MainLoop()


if __name__ == '__main__':
    main()
