from tkinter import Tk, Canvas, NW, ALL
from PIL import Image, ImageTk
import time
from threading import Thread
from math import copysign, ceil


class MapApp(Canvas):

    def __init__(self, root, **kwargs):
        self.root = root
        self.miss_photo = ImageTk.PhotoImage(Image.open("miss.png"))

        self.zoom = 13
        self.map_folder = "map/"

        self.map_x = 0
        self.map_y = 0
        self.res = (0, 0)

        self.image_list = []

        self.kinetic_thread_running = True
        self.kinetic_slow = 0.2  # px per 0.001sec
        self.kinetic_run = True

        self.mouse_pos_x = 0
        self.mouse_pos_y = 0

        self.movement = []
        self.move_event = None
        self.prev_win_x_size, self.prev_win_y_size = 0, 0
        super().__init__(root, **kwargs)
        self.place(x=0, y=0)
        canv_x_size = ((self.root.winfo_width() - 1) // 256 + 7) * 256
        canv_y_size = ((self.root.winfo_height() - 1) // 256 + 7) * 256
        self.total_canv_size = (canv_x_size, canv_y_size)
        self.configure(scrollregion=(0, 0, canv_x_size, canv_y_size))
        self.root.after(10, self.move_viewport, 0, 0)
        self.root.after(15, self.load_tiles)
        self.bind("<Button-1>", self.mouse_press)
        self.bind("<B1-Motion>", self.mouse_move)
        self.bind("<ButtonRelease-1>", self.mouse_release)

    def load_tiles(self, event=None):
        self.delete(ALL)
        self.image_list.clear()
        self.res = tuple(map(int, open(self.map_folder+"z{}/config".format(self.zoom), "r").readline().split()))
        offx = self.map_x // 256
        offy = self.map_y // 256
        for x in range(-1, self.total_canv_size[0]//256):
            self.image_list.append([])
            for y in range(-1, self.total_canv_size[1]//256):
                if 0 <= offx+x < self.res[0] and 0 <= offy+y < self.res[1]:
                    img = ImageTk.PhotoImage(Image.open(self.map_folder + "z{}/{}.{}.png".format(self.zoom, offx+x, offy+y)))

                    canv_img = self.create_image((1+x)*256, (1+y)*256, image=img, anchor=NW)
                    self.image_list[-1].append([canv_img, img])
                else:
                    canv_img = self.create_image((1+x)*256, (1+y)*256, image=self.miss_photo, anchor=NW)
                    self.image_list[-1].append([canv_img, self.miss_photo])

    def update_tiles(self, prev_pos):
        new_pos = (self.map_x//256, self.map_y//256)
        if prev_pos[0] < new_pos[0]:
            print(":dsadas")
            new_list = []
            for x in range(1, self.total_canv_size[0]//256):
                new_list.append([])
                for y in range(self.total_canv_size[1]//256):
                    self.move(self.image_list[x][y][0], -256, 0)
                    new_list[-1].append(self.image_list[x][y])
            new_list.append([])
            for y in range(self.total_canv_size[1]//256):
                self.move(self.image_list[0][y][0], self.total_canv_size[0]-256, 0)
                if 0 <= self.total_canv_size[0]//256+self.map_x//256-2 < self.res[0] and 0 <= self.map_y//256+y-1 < self.res[1]:
                    self.image_list[0][y][1] = ImageTk.PhotoImage(Image.open(
                        self.map_folder + "z{}/{}.{}.png".format(self.zoom,
                                                                 self.total_canv_size[0]//256+self.map_x//256-2,
                                                                 self.map_y//256+y-1)))
                else:
                    self.image_list[0][y][1] = self.miss_photo
                self.itemconfigure(self.image_list[0][y][0], image=self.image_list[0][y][1])
                new_list[-1].append(self.image_list[0][y])
            self.image_list = new_list

        elif prev_pos[0] > new_pos[0]:
            new_list = [[]]
            for x in range(self.total_canv_size[0] // 256-1):
                new_list.append([])
                for y in range(self.total_canv_size[1] // 256):
                    self.move(self.image_list[x][y][0], 256, 0)
                    new_list[-1].append(self.image_list[x][y])
            for y in range(self.total_canv_size[1] // 256):
                self.move(self.image_list[-1][y][0], -(self.total_canv_size[0] - 256), 0)
                if 0 <= self.map_x // 256 - 1 < self.res[0] and 0 <= self.map_y // 256 + y - 1 < self.res[1]:
                    self.image_list[-1][y][1] = ImageTk.PhotoImage(Image.open(
                        self.map_folder + "z{}/{}.{}.png".format(self.zoom,
                                                                 self.map_x // 256-1,
                                                                 self.map_y // 256 + y - 1)))
                else:
                    self.image_list[-1][y][1] = self.miss_photo
                self.itemconfigure(self.image_list[-1][y][0], image=self.image_list[-1][y][1])
                new_list[0].append(self.image_list[-1][y])
            self.image_list = new_list

        if prev_pos[1] < new_pos[1]:
            print(":xzxczx")
            new_list = []
            for x in range(self.total_canv_size[0] // 256):
                new_list.append([])
                for y in range(1, self.total_canv_size[1] // 256):
                    self.move(self.image_list[x][y][0], 0, -256)
                    new_list[-1].append(self.image_list[x][y])

            for x in range(self.total_canv_size[0] // 256):
                self.move(self.image_list[x][0][0], 0, self.total_canv_size[1] - 256)
                if 0 <= x + self.map_x // 256 - 1 < self.res[0] and 0 <= self.total_canv_size[1] // 256 + self.map_y // 256 - 2 < self.res[1]:

                    self.image_list[x][0][1] = ImageTk.PhotoImage(Image.open(
                        self.map_folder + "z{}/{}.{}.png".format(self.zoom,
                                                                 x + self.map_x // 256 - 1,
                                                                 self.total_canv_size[1] // 256 + self.map_y // 256 - 2)))
                else:
                    self.image_list[x][0][1] = self.miss_photo
                self.itemconfigure(self.image_list[x][0][0], image=self.image_list[x][0][1])
                new_list[x].append(self.image_list[x][0])
            self.image_list = new_list
        elif prev_pos[1] > new_pos[1]:
            new_list = []
            for x in range(self.total_canv_size[0] // 256):
                new_list.append([0])
                for y in range(self.total_canv_size[1] // 256-1):
                    self.move(self.image_list[x][y][0], 0, 256)
                    new_list[-1].append(self.image_list[x][y])

            for x in range(self.total_canv_size[0] // 256):
                self.move(self.image_list[x][-1][0], 0, -(self.total_canv_size[1] - 256))
                if 0 <= x + self.map_x // 256 - 1 < self.res[0] and 0 <= self.map_y // 256 - 1 < self.res[1]:

                    self.image_list[x][-1][1] = ImageTk.PhotoImage(Image.open(
                        self.map_folder + "z{}/{}.{}.png".format(self.zoom,
                                                                 x + self.map_x // 256 - 1,
                                                                 self.map_y // 256 - 1)))
                else:
                    self.image_list[x][-1][1] = self.miss_photo
                self.itemconfigure(self.image_list[x][-1][0], image=self.image_list[x][-1][1])
                new_list[x][0] = self.image_list[x][-1]
            self.image_list = new_list

    def c_on_resize(self, event):
        if root.winfo_width() != self.prev_win_x_size or root.winfo_height() != self.prev_win_y_size:
            print("resize!")
            self.prev_win_x_size = self.root.winfo_width()
            self.prev_win_y_size = self.root.winfo_height()
            canv.configure(width=self.root.winfo_width(), height=self.root.winfo_height())
            canv_x_size = ((self.root.winfo_width()-1) // 256+3)*256
            canv_y_size = ((self.root.winfo_height()-1) // 256+3)*256
            canv.configure(scrollregion=(0, 0, canv_x_size, canv_y_size))
            self.total_canv_size = (canv_x_size, canv_y_size)
            self.load_tiles()
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
            new_x = round(self.map_x + x_speed)
            new_y = round(self.map_y + y_speed)
            x_speed = copysign(max(0, abs(x_speed)-self.kinetic_slow), x_speed)
            y_speed = copysign(max(0, abs(y_speed)-self.kinetic_slow), y_speed)

            self.after_idle(self.move_viewport, new_x-self.map_x, new_y-self.map_y)
            # self.move_viewport(new_x-self.map_x, new_y-self.map_y)
            # self.event_generate("<kinetic_movement>", when="tail")
            time.sleep(0.001)
    
    def mouse_press(self, event):
        self.kinetic_thread_running = False
        self.mouse_pos_x = event.x
        self.mouse_pos_y = event.y
        self.move_event = event
        self.kinetic_run = True
        self.kinetic()
        self.movement.clear()

    def mouse_move(self, event):
        self.move_event = event
        self.move_viewport(self.mouse_pos_x-event.x, self.mouse_pos_y-event.y)
        self.mouse_pos_x, self.mouse_pos_y = event.x, event.y
    
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
        # relative
        # print(self.map_x, self.map_y)
        pos_x = 256+(self.map_x+x) % 256
        pos_y = 256+(self.map_y+y) % 256
        self.xview_moveto(min(max(pos_x / self.total_canv_size[0], 0), 1))
        self.yview_moveto(min(max(pos_y / self.total_canv_size[1], 0), 1))
        self.map_x += x
        self.map_y += y
        if (self.map_x-x)//256 != self.map_x // 256 or (self.map_y-y)//256 != self.map_y // 256:
            self.update_tiles(((self.map_x-x)//256, (self.map_y-y)//256))
            print("Update")


        # viewport pos is map_pos % 256




root = Tk()
root.geometry("1024x768")
canv = MapApp(root, width=1024, height=768, bg="blue")

root.bind("<Configure>", canv.c_on_resize)
root.bind("<Return>", canv.load_tiles)

root.mainloop()