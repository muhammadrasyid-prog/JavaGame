from PIL import Image
import sys

def ascii_art(path, width=80):
    img = Image.open(path).convert('L')
    aspect_ratio = img.height / img.width
    height = int(aspect_ratio * width * 0.5)
    img = img.resize((width, height))
    pixels = img.getdata()
    chars = ["B","S","#","&","@","$","%","*","!",":","."," "]
    new_pixels = [chars[pixel//25] for pixel in pixels]
    new_pixels = ''.join(new_pixels)
    new_pixels_count = len(new_pixels)
    ascii_image = [new_pixels[index:index + width] for index in range(0, new_pixels_count, width)]
    ascii_image = "\n".join(ascii_image)
    print(f"--- {path} ---")
    print(ascii_image)

ascii_art("d:/jekjek/assets/MC/idle.png", 120)
ascii_art("d:/jekjek/assets/MC/run.png", 120)
