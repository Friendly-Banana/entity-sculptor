import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
from sys import argv
import numpy as np

name = argv[1]

fig = plt.figure()
ax = fig.add_subplot(111, projection="3d")


class Vertex:
    def __init__(self, p, n):
        self.position = [float(x) for x in p.split(", ")]
        self.normals = [float(x) for x in n.split(", ")]
        self.p = self.position
        self.n = self.normals
        self.x, self.y, self.z = self.position

    def __getitem__(self, i):
        return self.position[i]


vertices = []
with open(name) as f:
    for line in f.readlines():
        vertices.append(Vertex(*line.split("|")))
"""
for v in vertices:
    ax.plot(
        *v.position,
        marker="o",
        markersize=5,
    )"""
"""
for i in range(0, len(vertices), 2):
    s, e = vertices[i], vertices[(i + 1) % len(vertices)]
    ax.plot(*[(s.p[j], e.p[j]) for j in range(3)])
    ax.text(*s.p, i * " " + str(i))
    ax.text(*e.p, (1 + i) * " " + str(i + 1))
"""


def r(s, e, step):
    while s < e:
        yield s
        s += step
    yield e

def m(a,b):
    return min(a,b),max(a,b)

for i in range(0, len(vertices), 4):

    def line(a, b):
        ax.plot(*[(a.p[g], b.p[g]) for g in range(3)])

    line(vertices[i], vertices[i + 1])
    line(vertices[i + 1], vertices[i + 2])
    line(vertices[i + 2], vertices[i + 3])
    line(vertices[i + 3], vertices[i])
    for j in range(i, i + 4):
        ax.text(vertices[j][0] + len(str(j)) * j * 0.01, vertices[j][1], vertices[j][2], str(j))
    """ch = 0.2
    a, b, c, d = vertices[i : i + 4]

    for x in r(*m(a.x, c.x), ch):
        for y in r(*m(a.y, c.y), ch):
            for z in r(*m(a.z, c.z), ch):
                ax.plot(x, y, z, marker="o")
    # ax.add_collection3d(Poly3DCollection([vertices[i : i + 4]])))"""


"""
for i in range(0, len(vertices), 4):
    ax.plot_trisurf(
        vertices[i + 0].position, vertices[i + 2].position, vertices[i + 1].position
    )
    ax.plot_trisurf(
        vertices[i + 1].position, vertices[i + 2].position, vertices[i + 3].position
    )
"""
plt.show()