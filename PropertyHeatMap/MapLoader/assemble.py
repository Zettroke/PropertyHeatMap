from PIL import Image
import os
from os.path import join

map_dir = "map/"

for p in os.listdir(map_dir):
    if os.path.isdir(join(map_dir, p)):
        pth = join(map_dir, p)

        max_x, max_y = 0, 0
        for i in os.listdir(pth):
            l = i.split(".")
            if l[0].isdecimal() and l[1].isdecimal():
                max_x = max(int(l[0]), max_x)
                max_y = max(int(l[1]), max_y)

        size = [max_x+1, max_y+1]

        print("found max size", size)

        image_size = Image.open(join(pth, "0.0.png")).size

        image = Image.new("RGB", (size[0]*image_size[0], size[1]*image_size[1]))

        for x in range(size[0]):
            for y in range(size[1]):
                image.paste(Image.open(join(pth, "{0}.{1}.png".format(x, y))), (x*image_size[0], y*image_size[1]))
        image.save(join(map_dir, p+".png"))


