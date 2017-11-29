import wx
from PIL import Image


app = wx.App()
frame = wx.Frame(None, -1, "dslkdls")
window = wx.ScrolledWindow(frame)
window.SetScrollbars(1, 1, 256*9, 256*9)
window.SetScrollRate(5, 5)
# window.ShowScrollbars(wx.SHOW_SB_NEVER, wx.SHOW_SB_NEVER)
for x in range(9):
    for y in range(9):
        wx.StaticBitmap(window, -1, wx.Bitmap("C:/PropertyHeatMap/osm_map_small/z16/{}.{}.png".format(x, y), wx.BITMAP_TYPE_ANY), pos=(x*256, y*256))

frame.Show()

app.MainLoop()
