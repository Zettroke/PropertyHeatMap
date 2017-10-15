from tkinter import Tk, Canvas, NW, ALL, Button, Label
from PIL import Image, ImageTk
import time
from threading import Thread, Lock
import math
import queue
import requests
import io
import getpass


class MapApp(Canvas):

    def __init__(self, root, **kwargs):
        self.root = root
        self.miss_photo = ImageTk.PhotoImage(Image.new("RGB", (256, 256), 0xC3C3C3))

        self.map_server = "http://178.140.109.241:25565/"

        try:
            requests.get(self.map_server, timeout=2)
        except Exception:

            l = Label(root, text="Server is down. Unable to run.", font="Arial 16")
            b = Button(root, text="ok", font="Arial 16")
            b.bind("<Button-1>", lambda event: root.destroy())
            l.pack()
            b.pack()
            root.geometry("350x75")

            return

        self.min_zoom, self.max_zoom = map(int, requests.get(self.map_server + "zoom_levels").text.split())
        self.zoom = self.min_zoom

        self.map_x = 0
        self.map_y = 0
        self.res = tuple(map(int, requests.get(self.map_server+"z{}/config".format(self.zoom), "r").text.split()))

        self.image_dict = {}
        self.image_load_queue = queue.PriorityQueue()
        self.loaded_image_offset = (0, 0)
        self.tiles_updating = False

        self.kinetic_thread_running = True
        self.kinetic_slow = 0.5  # px per 0.001sec
        self.kinetic_run = True
        self.x_speed = 0
        self.y_speed = 0

        self.mouse_pos_x = 0
        self.mouse_pos_y = 0

        self.movement = []
        self.move_event = None
        self.prev_win_x_size, self.prev_win_y_size = 0, 0
        super().__init__(root, **kwargs)
        self.place(x=0, y=0)
        canv_x_size = (self.root.winfo_width() // 256 + 3) * 256
        canv_y_size = (self.root.winfo_height() // 256 + 3) * 256
        self.total_canv_size = (canv_x_size, canv_y_size)
        self.configure(scrollregion=(0, 0, canv_x_size, canv_y_size))
        self.root.after(10, self.move_viewport, 0, 0)
        self.root.after(15, self.load_tiles)
        self.bind("<Button-1>", self.mouse_press)
        self.bind("<B1-Motion>", self.mouse_move)
        self.bind("<ButtonRelease-1>", self.mouse_release)
        self.bind("<MouseWheel>", self.zoom_map)

        root.bind("<Configure>", self.c_on_resize)
        root.bind("<Return>", self.load_tiles)

        self.tile_loader_lock = Lock()
        Thread(target=self.tile_loader, daemon=True).start()

    # (frag_x, frag_y) : ((x, y), canv_image, photo_image)
    def load_tiles(self, event=None):
        # print(len(self.image_dict))
        while not self.image_load_queue.empty():
            self.image_load_queue.get_nowait()
        self.delete(ALL)
        self.image_dict.clear()
        offx, offy = self.map_x//256, self.map_y//256
        for x in range(-1, self.total_canv_size[0]//256):
            for y in range(-1, self.total_canv_size[1]//256):
                canv = self.create_image((1 + x) * 256, (1 + y) * 256, image=self.miss_photo, anchor=NW)
                self.image_dict[(offx + x, offy + y)] = [(1 + x, 1 + y), canv, self.miss_photo]
                if 0 <= offx+x < self.res[0] and 0 <= offy+y < self.res[1]:
                    self.image_load_queue.put_nowait(((self.total_canv_size[0]//512 - (x+1))**2 + (self.total_canv_size[1]//512 - (y+1))**2, (canv, offx + x, offy + y)))

    def update_tiles(self):
        # print(len(self.image_dict))
        start = time.clock()
        offx, offy = self.map_x // 256, self.map_y // 256

        for x in range(-1, self.total_canv_size[0] // 256):
            for y in range(-1, self.total_canv_size[1] // 256):
                if (offx + x, offy + y) in self.image_dict:
                    # print((offx + x, offy + y))
                    o = self.image_dict[(offx + x, offy + y)]
                    # self.lift(o[1])
                    self.move(o[1], (1+x-o[0][0])*256, (1+y-o[0][1])*256)

                    o[0] = (1+x, 1+y)
                else:
                    canv = self.create_image((1 + x) * 256, (1 + y) * 256, image=self.miss_photo, anchor=NW)
                    self.image_dict[(offx + x, offy + y)] = [(1 + x, 1 + y), canv, self.miss_photo]
                    if 0 <= offx + x < self.res[0] and 0 <= offy + y < self.res[1]:
                        self.image_load_queue.put_nowait(((self.total_canv_size[0]//512 - (x+1))**2 + (self.total_canv_size[1]//512 - (y+1))**2, (canv, offx + x, offy + y)))

        k = set(self.image_dict.keys())
        for i in k:
            if not offx-5 <= i[0] <= offx + self.total_canv_size[0] // 256+3 or not offy -5 <= i[1] <= offy + self.total_canv_size[1] // 256+3:
                self.delete(self.image_dict[i][1])
                del self.image_dict[i]

    def tile_loader(self):
        while True:
            o = self.image_load_queue.get()[1]

            if (o[1], o[2]) in self.image_dict.keys():
                f = io.BytesIO(requests.get("http://178.140.109.241:25565/z{}/{}.{}.png".format(self.zoom, o[1], o[2]), headers={"UserName": getpass.getuser()}).content)
                img = ImageTk.PhotoImage(Image.open(f))
                self.itemconfigure(o[0], image=img)
                try:
                    self.image_dict[(o[1], o[2])][2] = img
                except Exception:
                    del f

    def draw_lines(self):
        for x in range(0, self.total_canv_size[0], 256):
            self.create_line(x, 0, x, self.total_canv_size[1])
        for y in range(0, self.total_canv_size[1], 256):
            self.create_line(0, y, self.total_canv_size[0], y)
    '''def update_tiles(self, prev_pos, new_pos):

        if prev_pos[0] < new_pos[0]:
            new_list = []
            for x in range(1, self.total_canv_size[0]//256):
                new_list.append([])
                for y in range(self.total_canv_size[1]//256):
                    self.move(self.image_list[x][y][0], -256, 0)
                    new_list[-1].append(self.image_list[x][y])
            new_list.append([])
            for y in range(self.total_canv_size[1]//256):
                self.move(self.image_list[0][y][0], self.total_canv_size[0]-256, 0)
                if 0 <= self.total_canv_size[0]//256+new_pos[0]-2 < self.res[0] and 0 <= new_pos[1]+y-1 < self.res[1]:
                    self.image_list[0][y][1] = ImageTk.PhotoImage(Image.open(
                        self.map_folder + "z{}/{}.{}.png".format(self.zoom,
                                                                 self.total_canv_size[0]//256+new_pos[0]-2,
                                                                 new_pos[1]+y-1)))
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
                if 0 <= new_pos[0] - 1 < self.res[0] and 0 <= new_pos[1] + y - 1 < self.res[1]:
                    self.image_list[-1][y][1] = ImageTk.PhotoImage(Image.open(
                        self.map_folder + "z{}/{}.{}.png".format(self.zoom,
                                                                 new_pos[0]-1,
                                                                 new_pos[1] + y - 1)))
                else:
                    self.image_list[-1][y][1] = self.miss_photo
                self.itemconfigure(self.image_list[-1][y][0], image=self.image_list[-1][y][1])
                new_list[0].append(self.image_list[-1][y])
            self.image_list = new_list

        if prev_pos[1] < new_pos[1]:
            new_list = []
            for x in range(self.total_canv_size[0] // 256):
                new_list.append([])
                for y in range(1, self.total_canv_size[1] // 256):
                    self.move(self.image_list[x][y][0], 0, -256)
                    new_list[-1].append(self.image_list[x][y])

            for x in range(self.total_canv_size[0] // 256):
                self.move(self.image_list[x][0][0], 0, self.total_canv_size[1] - 256)
                if 0 <= x + new_pos[0] - 1 < self.res[0] and 0 <= self.total_canv_size[1] // 256 + new_pos[1] - 2 < self.res[1]:

                    self.image_list[x][0][1] = ImageTk.PhotoImage(Image.open(
                        self.map_folder + "z{}/{}.{}.png".format(self.zoom,
                                                                 x + new_pos[0] - 1,
                                                                 self.total_canv_size[1] // 256 + new_pos[1] - 2)))
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
                if 0 <= x + new_pos[0] - 1 < self.res[0] and 0 <= new_pos[1] - 1 < self.res[1]:

                    self.image_list[x][-1][1] = ImageTk.PhotoImage(Image.open(
                        self.map_folder + "z{}/{}.{}.png".format(self.zoom,
                                                                 x + new_pos[0] - 1,
                                                                 new_pos[1] - 1)))
                else:
                    self.image_list[x][-1][1] = self.miss_photo
                self.itemconfigure(self.image_list[x][-1][0], image=self.image_list[x][-1][1])
                new_list[x][0] = self.image_list[x][-1]
            self.image_list = new_list
'''

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
        pos_start = self.movement[min(49, len(self.movement) - 1)]
        pos_end = self.movement[0]
        x_speed = (pos_end[0] - pos_start[0]) / min(50, len(self.movement))  # px per sec
        y_speed = (pos_end[1] - pos_start[1]) / min(50, len(self.movement))
        for i in range(round(max(abs(x_speed), abs(y_speed)) / self.kinetic_slow + 0.5)):
            if not self.kinetic_thread_running:
                break
            new_x = round(self.map_x + x_speed)
            new_y = round(self.map_y + y_speed)
            x_speed = math.copysign(max(0, abs(x_speed) - self.kinetic_slow), x_speed)
            y_speed = math.copysign(max(0, abs(y_speed) - self.kinetic_slow), y_speed)

            self.after_idle(self.move_viewport, new_x - self.map_x, new_y - self.map_y)
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
        self.kinetic_run = False
        self.kinetic_thread_running = True
        pos_start = self.movement[min(99, len(self.movement) - 1)]
        pos_end = self.movement[0]
        self.x_speed = (pos_end[0] - pos_start[0]) / min(100, len(self.movement))  # px per sec
        self.y_speed = (pos_end[1] - pos_start[1]) / min(100, len(self.movement))

        Thread(target=self.kinetic_move).start()

    def move_viewport(self, x, y):
        pos_x = 256+(self.map_x+x) % 256
        pos_y = 256+(self.map_y+y) % 256
        self.xview_moveto(pos_x / self.total_canv_size[0])
        self.yview_moveto(pos_y / self.total_canv_size[1])
        self.map_x += x
        self.map_y += y
        if (self.map_x-x)//256 != self.map_x // 256 or (self.map_y-y)//256 != self.map_y // 256:
            self.update_tiles()

    def zoom_map(self, event):
        if not (self.zoom == self.max_zoom and event.delta > 0) and not (self.zoom == self.min_zoom and event.delta < 0):
            self.kinetic_thread_running = False
            self.zoom += event.delta//abs(event.delta)
            if event.delta // abs(event.delta) > 0:
                self.map_x = self.map_x*2+event.x
                self.map_y = self.map_y*2+event.y
            else:
                self.map_x = self.map_x//2-event.x//2
                self.map_y = self.map_y//2-event.y//2
            pos_x = 256 + self.map_x % 256
            pos_y = 256 + self.map_y % 256
            self.xview_moveto(pos_x / self.total_canv_size[0])
            self.yview_moveto(pos_y / self.total_canv_size[1])
            self.res = tuple(map(int, requests.get(self.map_server + "z{}/config".format(self.zoom), "r").text.split()))
            self.load_tiles()


if __name__ == "__main__":
    root = Tk()
    root.geometry("1024x768")
    canv = MapApp(root, width=1024, height=768, bg="blue")

    root.mainloop()
