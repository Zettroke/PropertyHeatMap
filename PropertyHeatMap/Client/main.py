from tkinter import Tk, Canvas, NW, ALL
from PIL import Image, ImageTk
import time
from threading import Thread
from math import copysign, ceil


class MapApp(Canvas):

    def __init__(self, root, **kwargs):
        self.root = root
        self.miss_photo = ImageTk.PhotoImage(Image.open("miss.png"))

        self.map_folder = "map/"

        self.map_x = 0
        self.map_y = 0

        self.image_list = []

        self.kinetic_thread_running = True
        self.kinetic_slow = 0.2  # px per 0.001sec
        self.kinetic_run = True
        self.mouse_pos_x = 0
        self.mouse_pos_y = 0
        self.movement = []
        self.move_event = None
        self.prev_win_x_size, self.prev_win_y_size = 0, 0
        self.viewport_x = 0
        self.viewport_y = 0
        self.prev_move_pos_x = 0
        self.prev_move_pos_y = 0
        super().__init__(root, **kwargs)
        self.place(x=0, y=0)
        canv_x_size = ((self.root.winfo_width() - 1) // 256 + 7) * 256
        canv_y_size = ((self.root.winfo_height() - 1) // 256 + 7) * 256
        self.total_canv_size = (canv_x_size, canv_y_size)
        self.configure(scrollregion=(0, 0, canv_x_size, canv_y_size))
        self.root.after(10, self.move_viewport, 256, 256)
        self.root.after(15, self.load_tiles)
        self.viewport_x = 256
        self.viewport_y = 256
        self.bind("<Button-1>", self.mouse_press)
        self.bind("<B1-Motion>", self.mouse_move)
        self.bind("<ButtonRelease-1>", self.mouse_release)

    def load_tiles(self, event=None):
        self.delete(ALL)
        self.image_list.clear()
        res = tuple(map(int, open(self.map_folder+"z11/config", "r").readline().split()))
        offx = self.map_x // 256
        offy = self.map_y // 256
        for x in range(-1, self.total_canv_size[0]//256):
            for y in range(-1, self.total_canv_size[1]//256):
                if 0 <= offx+x < res[0] and 0 <= offy+y < res[1]:
                    img = ImageTk.PhotoImage(Image.open(self.map_folder + "z11/" + "{}.{}.png".format(offx+x, offy+y)))
                    self.image_list.append(img)
                    self.create_image((1+x)*256, (1+y)*256, image=img, anchor=NW)
                else:
                    self.create_image((1+x)*256, (1+y)*256, image=self.miss_photo, anchor=NW)

        '''for x in range(0, self.total_canv_size[0], 256):
            self.create_line(x, 0, x, self.total_canv_size[1])
        for y in range(0, self.total_canv_size[1], 256):
            self.create_line(0, y, self.total_canv_size[0], y)'''

    def check(self, event):
        print(self.map_x // 256, self.map_y // 256)

    def c_on_resize(self, event):
        if root.winfo_width() != self.prev_win_x_size or root.winfo_height() != self.prev_win_y_size:
            print("resize!")
            self.prev_win_x_size = self.root.winfo_width()
            self.prev_win_y_size = self.root.winfo_height()
            canv.configure(width=self.root.winfo_width(), height=self.root.winfo_height())
            canv_x_size = ((self.root.winfo_width()-1) // 256+7)*256
            canv_y_size = ((self.root.winfo_height()-1) // 256+7)*256
            canv.configure(scrollregion=(0, 0, canv_x_size, canv_y_size))
            self.total_canv_size = (canv_x_size, canv_y_size)
            # print((self.root.winfo_width(), self.root.winfo_height()))
            # print(self.total_canv_size)
    
    def kinetic(self):
        if self.kinetic_run:
            self.movement.append((self.move_event.x, self.move_event.y))
            if len(self.movement) > 1000:
                for i in range(999, 49, -1):
                    del self.movement[i]
            root.after(10, self.kinetic)
    
    def kinetic_move(self):
        pos_start = self.movement[min(49, len(self.movement)-1)]
        pos_end = self.movement[0]
        x_speed = (pos_end[0]-pos_start[0])/min(50, len(self.movement)) #px per sec
        y_speed = (pos_end[1]-pos_start[1])/min(50, len(self.movement))
        for i in range(round(max(abs(x_speed), abs(y_speed))/self.kinetic_slow+0.5)):
            if not self.kinetic_thread_running:
                break
            viewport_x = round(self.viewport_x + x_speed)
            viewport_y = round(self.viewport_y + y_speed)
            x_speed = copysign(max(0, abs(x_speed)-self.kinetic_slow), x_speed)
            y_speed = copysign(max(0, abs(y_speed)-self.kinetic_slow), y_speed)
            self.move_viewport(viewport_x, viewport_y)
            time.sleep(0.001)
    
    def mouse_press(self, event):
        self.kinetic_thread_running = False
        self.mouse_pos_x = event.x
        self.mouse_pos_y = event.y
        self.prev_move_pos_x = self.viewport_x
        self.prev_move_pos_y = self.viewport_y
        self.move_event = event
        self.kinetic_run = True
        self.kinetic()
        self.movement.clear()

    def mouse_move(self, event):
        self.move_event = event
        self.move_viewport((self.prev_move_pos_x - event.x + self.mouse_pos_x), (self.prev_move_pos_y - event.y + self.mouse_pos_y))
    
    def mouse_release(self, event):
        '''self.viewport_x -= event.x - self.mouse_pos_x
        if self.viewport_x < 0: self.viewport_x = 0
        if self.viewport_x > self.total_canv_size[0]-self.winfo_width(): self.viewport_x = self.total_canv_size[0] - self.winfo_width()
        self.viewport_y -= event.y - self.mouse_pos_y
        if self.viewport_y < 0: self.viewport_y = 0
        if self.viewport_y > self.total_canv_size[1]-self.winfo_height(): self.viewport_y = self.total_canv_size[1] - self.winfo_height()'''
        self.kinetic_run = False
        self.kinetic_thread_running = True
        Thread(target=self.kinetic_move).start()

    def move_viewport(self, x, y):
        self.xview_moveto(min(max(x / self.total_canv_size[0], 0), 1))
        self.yview_moveto(min(max(y / self.total_canv_size[1], 0), 1))
        self.map_x += x-self.viewport_x
        self.map_y += y-self.viewport_y

        # print("Map:", self.map_x, self.map_y)
        self.viewport_x = x
        self.viewport_y = y
        if self.viewport_x < 0: self.viewport_x = 0
        if self.viewport_x > self.total_canv_size[0] - self.winfo_width(): self.viewport_x = self.total_canv_size[0] - self.winfo_width()
        if self.viewport_y < 0: self.viewport_y = 0
        if self.viewport_y > self.total_canv_size[1] - self.winfo_height(): self.viewport_y = self.total_canv_size[1] - self.winfo_height()

        # if x < 256: move all tiles 1 pos left and scroll viewport to 512 also load new tile




root = Tk()
root.geometry("1024x768")
canv = MapApp(root, width=1024, height=768, bg="blue")

root.bind("<Configure>", canv.c_on_resize)
root.bind("<Return>", canv.load_tiles)
root.bind("+", canv.check)

root.mainloop()