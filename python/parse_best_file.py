import sys


path = sys.argv[1]
data = []
with open(path, "r") as file:
    lines = file.read().splitlines()
    last = lines[-1].split(";")
    data.append(",".join(["0", "0", last[-1]]))


with open(sys.argv[2], "w") as file:
    file.write("x,y,serialized\n")
    for line in data:
        file.write(line)