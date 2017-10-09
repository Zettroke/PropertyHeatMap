from tkinter import Tk, Canvas, BOTH, YES
from PIL import Image, ImageTk
import time
from threading import Thread
from math import copysign, ceil


class MapApp(Canvas):

    def __init__(self, root, **kwargs):
        self.root = root

        self.total_canv_size = (2000, 2000)

        self.map_x = 0
        self.map_y = 0

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
        for y in range(0, 2000, 256):
            self.create_line(0, y, 2000, y)
        for x in range(0, 2000, 256):
            self.create_line(x, 0, x, 2000)
        for y in range(0, 2000, 256):
            for x in range(0, 2000, 256):
                self.create_text(x + 10, y + 10, text=str((y // 256) * 2000 // 256 + (x // 256)))
        self.bind("<Button-1>", self.mouse_press)
        self.bind("<B1-Motion>", self.mouse_move)
        self.bind("<ButtonRelease-1>", self.mouse_release)

    def c_on_resize(self, event):
        if root.winfo_width() != self.prev_win_x_size or root.winfo_height() != self.prev_win_y_size:
            print("resize!")
            self.prev_win_x_size = self.root.winfo_width()
            self.prev_win_y_size = self.root.winfo_height()
            canv.configure(width=self.root.winfo_width(), height=self.root.winfo_height())
            canv_x_size = ceil(self.root.winfo_width() / 256)*256 + 512
            canv_y_size = ceil(self.root.winfo_height() / 256)*256 + 512
            canv.configure(scrollregion=(0, 0, canv_x_size, canv_y_size))
            self.total_canv_size = (canv_x_size, canv_y_size)
    
    def kinetic(self):
        if self.kinetic_run:
            self.movement.append((self.move_event.x, self.move_event.y))
            if len(self.movement) > 1000:
                for i in range(50, 1000):
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
            self.viewport_x = round(self.viewport_x + x_speed)
            self.viewport_y = round(self.viewport_y + y_speed)
            if self.viewport_x < 0: self.viewport_x = 0
            if self.viewport_x > self.total_canv_size[0] - self.winfo_width(): self.viewport_x = self.total_canv_size[0] - self.winfo_width()
            if self.viewport_y < 0: self.viewport_y = 0
            if self.viewport_y > self.total_canv_size[1] - self.winfo_height(): self.viewport_y = self.total_canv_size[1] - self.winfo_height()
            x_speed = copysign(max(0, abs(x_speed)-self.kinetic_slow), x_speed)
            y_speed = copysign(max(0, abs(y_speed)-self.kinetic_slow), y_speed)
            self.move_viewport(self.viewport_x, self.viewport_y)
            time.sleep(0.001)
    
    def mouse_press(self, event):
        self.kinetic_thread_running = False
        self.mouse_pos_x = event.x
        self.mouse_pos_y = event.y
        self.prev_move_pos_x = event.x
        self.prev_move_pos_y = event.y
        self.move_event = event
        self.kinetic_run = True
        self.kinetic()
        self.movement.clear()

    def mouse_move(self, event):
        self.move_event = event
        '''self.xview_moveto((self.viewport_x - event.x + mouse_pos_x) / 2000)
        self.yview_moveto((self.viewport_y - event.y + mouse_pos_y) / 2000)'''
        self.move_viewport((self.viewport_x - event.x + self.mouse_pos_x), (self.viewport_y - event.y + self.mouse_pos_y))
    
    def mouse_release(self, event):
        self.viewport_x -= event.x - self.mouse_pos_x
        if self.viewport_x < 0: self.viewport_x = 0
        if self.viewport_x > self.total_canv_size[0]-self.winfo_width(): self.viewport_x = self.total_canv_size[0] - self.winfo_width()
        self.viewport_y -= event.y - self.mouse_pos_y
        if self.viewport_y < 0: self.viewport_y = 0
        if self.viewport_y > self.total_canv_size[1]-self.winfo_height(): self.viewport_y = self.total_canv_size[1] - self.winfo_height()
        self.kinetic_run = False
        self.kinetic_thread_running = True
        Thread(target=self.kinetic_move).start()

    def move_viewport(self, x, y):
        self.xview_moveto(x / self.total_canv_size[0])
        self.yview_moveto(y / self.total_canv_size[1])


root = Tk()
root.geometry("1024x768")
canv = MapApp(root, width=1024, height=768, bg="blue")
canv.place(x=0, y=0)
root.bind("<Configure>", canv.c_on_resize)

root.mainloop()