from tkinter import Tk, Canvas, BOTH, YES
from PIL import Image, ImageTk

prev_win_x_size, prev_win_y_size = 0, 0


def c_on_resize(event):
    global prev_win_x_size, prev_win_y_size
    if root.winfo_width() != prev_win_x_size or root.winfo_height() != prev_win_y_size:
        print("resize!")
        prev_win_x_size = root.winfo_width()
        prev_win_y_size = root.winfo_height()
        canv.configure(width=root.winfo_width(), height=root.winfo_height())


mouse_pos_x = 0
mouse_pos_y = 0


def mouse_press(event):
    global mouse_pos_x, mouse_pos_y
    mouse_pos_x = event.x
    mouse_pos_y = event.y


def mouse_move(event):
    canv.xview_moveto((canv.xpos - event.x + mouse_pos_x) / 2000)
    canv.yview_moveto((canv.ypos - event.y + mouse_pos_y) / 2000)


def mouse_release(event):
    canv.xpos -= event.x - mouse_pos_x
    if canv.xpos < 0: canv.xpos = 0
    if canv.xpos > 2000-canv.winfo_width(): canv.xpos = 2000-canv.winfo_width()
    canv.ypos -= event.y - mouse_pos_y
    if canv.ypos < 0: canv.ypos = 0
    if canv.ypos > 2000-canv.winfo_height(): canv.ypos = 2000-canv.winfo_height()


def arrow(event):

    if event.keysym == "Down":
        canv.ypos += 100
        canv.yview_moveto(canv.ypos/2000)
    elif event.keysym == "Up":
        canv.ypos -= 100
        canv.yview_moveto(canv.ypos / 2000)


root = Tk()

root.geometry("1024x768")
canv = Canvas(root, width=500, height=500, bg="blue", scrollregion=(0, 0, 2000, 2000))
for y in range(0, 2000, 256):
    canv.create_line(0, y, 2000, y)
for x in range(0, 2000, 256):
    canv.create_line(x, 0, x, 2000)


# canv.create_rectangle(0, 0, 750, 750, fill="green")
canv.place(x=0, y=0)
canv.xpos = 0
canv.ypos = 0
canv.bind("<Button-1>", mouse_press)
canv.bind("<B1-Motion>", mouse_move)
canv.bind("<ButtonRelease-1>", mouse_release)
root.bind("<Configure>", c_on_resize)
root.bind("<Left>", arrow)
root.bind("<Right>", arrow)
root.bind("<Up>", arrow)
root.bind("<Down>", arrow)

root.mainloop()