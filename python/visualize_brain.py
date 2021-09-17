from PIL import Image, ImageDraw, ImageFont
import lhsmdu
import math
from collections import namedtuple
import sys
import os
import re
import csv


class Drawer(object):

    def __init__(self, voxels, voxel_size, node_size, pad, blank_size, shape):
        self.voxels = voxels
        self.voxel_size = voxel_size
        self.node_size = node_size
        self.pad = pad
        self.blank_size = blank_size
        self.image = Image.new("RGB", ((pad * 2) + (voxel_size * (max(voxels, key=lambda x: x[0])[0] + 1)),
                                       (pad * 2) + (voxel_size * (max(voxels, key=lambda x: x[1])[1] + 1))), "white")
        self.draw = ImageDraw.Draw(self.image)
        self.shape = shape
        self.module = set([])

    def plot_edges(self, edges, nodes_positions, nodes):
        for (u, v), w in edges.items():
            self.plot_arrow(nodes_positions[v], nodes_positions[u],
                            color="black" if is_not_crossing_edge(nodes[u]["x"], nodes[u]["y"],
                                                                  nodes[v]["x"], nodes[v]["y"], self.shape) else "black", width=1)

    def plot_rectangles(self):
        for x, voxel in enumerate(self.voxels):
            x, y = voxel
            self.draw.rectangle([(self.pad + self.voxel_size * (x + 1) + self.blank_size * x,
                                  self.pad + self.voxel_size * y + self.blank_size * y),
                                 (self.pad + self.voxel_size * x + self.blank_size * x,
                                  self.pad + self.voxel_size * (y + 1) + self.blank_size * y)],
                                outline="blue" if (x, y) not in self.module else "red", width=10, fill="white")

    def plot_arrow(self, pt_a, pt_b, width, color=(0, 0, 0, int(0.75 * 255))):
        self.draw.line((pt_a, pt_b), width=int(width * 5), fill=color)
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
        self.draw.polygon([vtx0, vtx1, pt_b], fill=color)

    def get_fixed_position(self, attr_dict, sensors):
        x, y = attr_dict["x"], attr_dict["y"]
        num_sensors = sensors[(x, y)]
        if attr_dict["type"].startswith("SENSING"):
            return (self.pad + self.voxel_size * x + self.blank_size * x + (
                    (int(re.findall(r'\d+', attr_dict["type"])[0]) + 0.5) * (self.voxel_size / (num_sensors + 1))),
                    self.pad + self.voxel_size * y + self.blank_size * y)
        return (self.pad + (self.voxel_size * x + (self.voxel_size / 2) + self.blank_size * x),
                self.voxel_size * y + self.voxel_size + self.pad + self.blank_size * y)

    def plot_nodes(self, nodes, font, func_to_color, voxel_to_num_sensors):
        hidden_nodes = {}
        nodes_positions = {}
        for idx, attrs in nodes.items():
            if attrs["type"] != "HIDDEN":
                x, y = self.get_fixed_position(attrs, voxel_to_num_sensors)
                nodes_positions[idx] = x, y
                self.draw.ellipse([(x - self.node_size, y - self.node_size), (x + self.node_size, y + self.node_size)],
                                  outline="black", width=3, fill=self.get_node_color(attrs["type"]))
            else:
                pos = attrs["x"], attrs["y"]
                if pos not in hidden_nodes:
                    hidden_nodes[pos] = [idx]
                else:
                    hidden_nodes[pos].append(idx)

        for pos, n in hidden_nodes.items():
            positions = lhsmdu.sample(2, len(n))
            for i in range(positions.shape[1]):
                x, y = positions[:, i]
                x = (x * self.voxel_size) + (self.pad + self.voxel_size * pos[0] + self.blank_size * pos[0])
                y = (y * self.voxel_size) + (self.pad + self.voxel_size * pos[1] + self.blank_size * pos[1])
                nodes_positions[n[i]] = x, y
                self.draw.ellipse([(x - self.node_size, y - self.node_size), (x + self.node_size, y + self.node_size)],
                                  outline="black", width=3, fill=self.get_node_color(nodes[n[i]]["type"]))
        return nodes_positions

    def get_node_color(self, t):
        if t.startswith("SENSING"):
            return 55, 126, 184
        elif t == "ACTUATOR":
            return 228, 26, 28
        return 77, 175, 74

    def save_image(self, file_name):
        self.image.save(file_name)

    def init_module(self, path):
        with open(path, "r") as file:
            reader = csv.reader(file)
            next(reader)
            for line in reader:
                self.module.add((int(line[0]), (2 if "biped" in line[2] else 1) - int(line[1])))


def read_file(file_name):
    Row = namedtuple("row", ["index", "x", "y", "function", "edges", "type"])
    nodes = {}
    edges = {}
    voxel_to_num_sensors = {}
    with open(file_name, "r") as file:
        for line in file:
            if line.startswith("index"):
                continue
            line = line.split(",")
            row = Row(int(line[0]), int(line[1]), int(line[2]), line[3], line[4], line[5].strip("\n"))
            if row.edges:
                for e in row.edges.split("&"):
                    edges[(row.index, int(e.split("/")[0]))] = abs(float(e.split("/")[1])) + abs(float(e.split("/")[2]))
            entry = {"x": row.x, "y": row.y, "function": row.function, "type": row.type}
            nodes[row.index] = entry
            if entry["type"].startswith("SENSING"):
                coord = entry["x"], entry["y"]
                if coord not in voxel_to_num_sensors:
                    voxel_to_num_sensors[coord] = 1
                else:
                    voxel_to_num_sensors[coord] += 1
    return nodes, edges, voxel_to_num_sensors


def flip_coordinates(nodes, voxel_to_num_sensors):
    max_y = max([attrs["y"] for attrs in nodes.values()])
    temp = {}
    for attrs in nodes.values():
        value = max_y - attrs["y"]
        temp[attrs["y"]] = value
        attrs["y"] = value
    new_voxel_to_num_sensors = {}
    for (x, y), num in voxel_to_num_sensors.items():
        new_voxel_to_num_sensors[(x, temp[y])] = num
    return nodes, new_voxel_to_num_sensors

def is_not_crossing_edge(x1, y1, x2, y2, shape):
    if shape == "worm":
        return (x1 <= 2 and x2 <= 2) or (x1 > 2 and x2 > 2)
    elif shape == "biped":
        return (x1 == 0 and x2 == 0) or (x1 == 3 and x2 == 3) or (0 < x1 < 3 and 0 < x2 < 3)
    raise Exception("Unknown shape: " + shape)


def main(input_file, output_file):
    nodes, edges, voxel_to_num_sensors = read_file(input_file)
    nodes, voxel_to_num_sensors = flip_coordinates(nodes, voxel_to_num_sensors)
    voxels = set([(attrs["x"], attrs["y"]) for attrs in nodes.values()])
    pad = 400
    voxel_size = 400
    node_size = 20
    blank_size = node_size * 2
    func_to_color = {"SIGMOID": "yellow", "RELU": "cyan", "TANH": "green", "SIN": "orange"}
    font = ImageFont.truetype("/Library/Fonts/times_new_roman.ttf", 100)
    drawer = Drawer(voxels, voxel_size, node_size, pad, blank_size, "biped" if "biped" in input_file else "worm")
    module_path = "/Users/federicopigozzi/Desktop/PhD/SelfOrganizingController/best_module.txt"
    if os.path.exists(module_path):
        drawer.init_module(module_path)
    drawer.plot_rectangles()
    nodes_positions = drawer.plot_nodes(nodes, font, func_to_color, voxel_to_num_sensors)
    drawer.plot_edges(edges, nodes_positions, nodes)
    drawer.save_image(output_file)
    os.remove(input_file)
    if os.path.exists(module_path):
        os.remove(module_path)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
