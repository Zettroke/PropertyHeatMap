from tkinter import Tk, Canvas, BOTH, YES
from PIL import Image, ImageTk
import time
from threading import Thread
from math import copysign

prev_win_x_size, prev_win_y_size = 0, 0


def c_on_resize(event):
    global prev_win_x_size, prev_win_y_size
    if root.winfo_width() != prev_win_x_size or root.winfo_height() != prev_win_y_size:
        print("resize!")
        prev_win_x_size = root.winfo_width()
        prev_win_y_size = root.winfo_height()
        canv.configure(width=root.winfo_width(), height=root.winfo_height())


kinetic_thread_running = True
kinetic_slow = 0.2 #px per 0.1sec
kinetic_run = True
mouse_pos_x = 0
mouse_pos_y = 0
movement = []
move_event = None


def kinetic():
    if kinetic_run:
        movement.append((move_event.x, move_event.y))
        if len(movement) > 1000:
            for i in range(50, 1000):
                del movement[i]

        root.after(10, kinetic)


def kinetic_move():
    pos_start = movement[min(49, len(movement)-1)]
    pos_end = movement[0]
    x_speed = (pos_end[0]-pos_start[0])/min(50, len(movement)) #px per sec
    y_speed = (pos_end[1]-pos_start[1])/min(50, len(movement))
    for i in range(round(max(abs(x_speed), abs(y_speed))/kinetic_slow+0.5)):
        if not kinetic_thread_running:
            break
        canv.xpos = round(canv.xpos + x_speed)
        canv.ypos = round(canv.ypos + y_speed)
        if canv.xpos < 0: canv.xpos = 0
        if canv.xpos > 2000 - canv.winfo_width(): canv.xpos = 2000 - canv.winfo_width()
        if canv.ypos < 0: canv.ypos = 0
        if canv.ypos > 2000 - canv.winfo_height(): canv.ypos = 2000 - canv.winfo_height()
        x_speed = copysign(max(0, abs(x_speed)-kinetic_slow), x_speed)
        y_speed = copysign(max(0, abs(y_speed)-kinetic_slow), y_speed)
        canv.xview_moveto(canv.xpos/2000)
        canv.yview_moveto(canv.ypos/2000)
        time.sleep(0.001)


def mouse_press(event):
    global mouse_pos_x, mouse_pos_y, prev_move_pos_x, prev_move_pos_y, move_event, kinetic_run, kinetic_thread_running
    kinetic_thread_running = False
    mouse_pos_x = event.x
    mouse_pos_y = event.y
    prev_move_pos_x = event.x
    prev_move_pos_y = event.y
    move_event = event
    kinetic_run = True
    kinetic()
    movement.clear()


def mouse_move(event):
    global move_event
    move_event = event
    canv.xview_moveto((canv.xpos - event.x + mouse_pos_x) / 2000)
    canv.yview_moveto((canv.ypos - event.y + mouse_pos_y) / 2000)


def mouse_release(event):
    global kinetic_run, kinetic_thread, kinetic_thread_running
    canv.xpos -= event.x - mouse_pos_x
    if canv.xpos < 0: canv.xpos = 0
    if canv.xpos > 2000-canv.winfo_width(): canv.xpos = 2000-canv.winfo_width()
    canv.ypos -= event.y - mouse_pos_y
    if canv.ypos < 0: canv.ypos = 0
    if canv.ypos > 2000-canv.winfo_height(): canv.ypos = 2000-canv.winfo_height()
    kinetic_run = False
    kinetic_thread_running = True
    Thread(target=kinetic_move).start()


root = Tk()

root.geometry("1024x768")
canv = Canvas(root, width=500, height=500, bg="blue", scrollregion=(0, 0, 2000, 2000))
for y in range(0, 2000, 256):
    canv.create_line(0, y, 2000, y)
for x in range(0, 2000, 256):
    canv.create_line(x, 0, x, 2000)
for y in range(0, 2000, 256):
    for x in range(0, 2000, 256):
        canv.create_text(x+10, y+10, text=str((y//256)*2000//256+(x//256)))


# canv.create_rectangle(0, 0, 750, 750, fill="green")
canv.place(x=0, y=0)
canv.xpos = 0
canv.ypos = 0
canv.bind("<Button-1>", mouse_press)
canv.bind("<B1-Motion>", mouse_move)
canv.bind("<ButtonRelease-1>", mouse_release)
root.bind("<Configure>", c_on_resize)

root.mainloop()