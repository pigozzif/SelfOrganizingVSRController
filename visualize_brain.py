from PIL import Image, ImageDraw, ImageFont
import lhsmdu
import math
from collections import namedtuple
import sys
import os
import re


def read_file(file_name):
    Row = namedtuple("row", ["index", "x", "y", "function", "edges", "type"])
    nodes = {}
    edges = []
    voxel_to_num_sensors = {}
    with open(file_name, "r") as file:
        for line in file:
            if line.startswith("index"):
                continue
            line = line.split(",")
            row = Row(int(line[0]), int(line[1]), int(line[2]), line[3], line[4], line[5].strip("\n"))
            if row.edges:
                edges.extend([(row.index, int(e)) for e in row.edges.split("-")])
            entry = {"x": row.x, "y": row.y, "function": row.function, "type": row.type}
            nodes[row.index] = entry
            if entry["type"].startswith("SENSING"):
                coord = entry["x"], entry["y"]
                if coord not in voxel_to_num_sensors:
                    voxel_to_num_sensors[coord] = 1
                else:
                    voxel_to_num_sensors[coord] += 1
    return nodes, edges, voxel_to_num_sensors


def plot_rectangles(draw, voxels, voxel_size, pad, blank_size):
    for x, voxel in enumerate(voxels):
        x, y = voxel
        draw.rectangle([(pad + voxel_size * (x + 1) + blank_size * x, pad + voxel_size * y + blank_size * y),
                    (pad + voxel_size * x + blank_size * x, pad + voxel_size * (y + 1) + blank_size * y)],
                   outline="blue", width=10, fill="white")


def plot_arrow(draw, pt_a, pt_b, width=1, color="black"):
    draw.line((pt_a, pt_b), width=width, fill=color)
    x0, y0 = pt_a
    x1, y1 = pt_b
    xb = 0.95 * (x1 - x0) + x0
    yb = 0.95 * (y1 - y0) + y0
    if x0 == x1:
       vtx0 = (xb - 5, yb)
       vtx1 = (xb + 5, yb)
    elif y0 == y1:
       vtx0 = (xb, yb + 5)
       vtx1 = (xb, yb - 5)
    else:
       alpha = math.atan2(y1 - y0, x1 - x0) - 90 * math.pi / 180
       a = 8 * math.cos(alpha)
       b = 8 * math.sin(alpha)
       vtx0 = (xb + a, yb + b)
       vtx1 = (xb - a, yb - b)
    draw.polygon([vtx0, vtx1, pt_b], fill=color)


def get_fixed_position(attr_dict, voxel_size, pad, blank_size, sensors):
    x, y = attr_dict["x"], attr_dict["y"]
    num_sensors = sensors[(x, y)]
    if attr_dict["type"].startswith("SENSING"):
        return (pad + voxel_size * x + blank_size * x + ((int(re.findall(r'\d+', attr_dict["type"])[0]) + 1) * (voxel_size / (num_sensors + 1))),
                pad + voxel_size * y + blank_size * y)
    return (pad + (voxel_size * x + (voxel_size / 2) + blank_size * x),
            voxel_size * y + voxel_size + pad + blank_size * y)


def plot_nodes(nodes, draw, font, voxel_size, pad, node_size, blank_size, func_to_color, voxel_to_num_sensors):
    hidden_nodes = {}
    nodes_positions = {}
    print(nodes)
    for idx, attrs in nodes.items():
        if attrs["type"] != "HIDDEN":
            x, y = get_fixed_position(attrs, voxel_size, pad, blank_size, voxel_to_num_sensors)
        else:
            pos = attrs["x"], attrs["y"]
            if pos not in hidden_nodes:
                hidden_nodes[pos] = [idx]
            else:
                hidden_nodes[pos].append(idx)
        nodes_positions[idx] = x, y
        text = "SENS." if nodes[idx]["type"].startswith("SENSING") else "ACT."
        draw.ellipse([(x - node_size, y - node_size), (x + node_size, y + node_size)],
                 outline="black", width=3, fill="green")
        draw.text((x - font.getsize(text)[0] / 2, y - font.getsize(text)[1] / 2), text, font=font, fill="black")

    for pos, n in hidden_nodes.items():
        positions = lhsmdu.sample(2, len(n))
        for i in range(positions.shape[1]):
            x, y = positions[:, i]
            x = (x * voxel_size) + (pad + voxel_size * pos[0] + blank_size * pos[0])
            y = (y * voxel_size) + (pad + voxel_size * pos[1] + blank_size * pos[1])
            nodes_positions[n[i]] = x, y
            draw.ellipse([(x - node_size, y - node_size), (x + node_size, y + node_size)],
                     outline="black", width=3, fill=func_to_color[nodes[n[i]]["function"]])
    return nodes_positions


def plot_edges(edges, draw, nodes_positions, nodes):
    for u, v in edges:
        plot_arrow(draw, nodes_positions[v], nodes_positions[u],
                   color="black" if nodes[u]["x"] == nodes[v]["x"]
                                    and nodes[u]["y"] == nodes[v]["y"] else "red")


def main(input_file, output_file):
    nodes, edges, voxel_to_num_sensors = read_file(input_file)
    voxels = set([(attrs["x"], attrs["y"]) for _, attrs in nodes.items()])
    pad = 400
    voxel_size = 400
    node_size = 20
    blank_size = node_size * 2
    image = Image.new("RGB", ((pad * 2) + (voxel_size * (max(voxels, key=lambda x: x[0])[0] + 1)),
                            (pad * 2) + (voxel_size * (max(voxels, key=lambda x: x[1])[1] + 1))), "white")
    draw = ImageDraw.Draw(image)
    func_to_color = {"SIGMOID": "yellow", "RELU": "cyan", "TANH": "green", "SIN": "red"}
    font = ImageFont.truetype("/Library/Fonts/times_new_roman.ttf", 15)
    plot_rectangles(draw, voxels, voxel_size, pad, blank_size)
    nodes_positions = plot_nodes(nodes, draw, font, voxel_size, pad, node_size, blank_size, func_to_color, voxel_to_num_sensors)
    plot_edges(edges, draw, nodes_positions, nodes)
    os.remove(input_file)
    image.save(output_file)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
