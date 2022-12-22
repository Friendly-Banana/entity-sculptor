package me.banana.entity_builder.client;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

import java.awt.*;

final class Vertex {
    private Vec3d position;
    private Vec3f normal;
    private float textureU, textureV;
    private int overlayU, overlayV;
    private int lightU, lightV;
    private Color color;

    /**
     * <table>
     * <tr> <td> u v </td> <td> side </td> <td> normal </td> <td> unmapped axis </td> </tr>
     * <tr> <td> x z </td> <td> top  </td> <td> 0 1 0 </td> <td> Direction.Axis.Y </td> </tr>
     * <tr> <td> x y </td> <td> front </td> <td> 0 0 1 </td> <td> Direction.Axis.Z </td> </tr>
     * <tr> <td> z y </td> <td> right </td> <td> 1 0 0 </td> <td> Direction.Axis.X </td> </tr>
     * </table>
     * u v  side    normal
     * x z	top     0 1 0
     * x y	front   0 0 1
     * z y	right   1 0 0
     */
    public Direction getUnmappedAxis() {
        float x = Math.abs(this.normal.getX()), y = Math.abs(this.normal.getY()), z = Math.abs(this.normal.getZ());
        if (y > x && y > z) {
            return this.normal.getY() > 0 ? Direction.UP : Direction.DOWN;
        } else if (z > x && z > y) {
            return this.normal.getZ() > 0 ? Direction.SOUTH : Direction.NORTH;
        } else if (x > y && x > z) return this.normal.getX() > 0 ? Direction.EAST : Direction.WEST;
        return Direction.UP;
    }

    public Vec3d getPosition() {
        return position;
    }

    public void setPosition(Vec3d position) {
        this.position = position;
    }

    public Vec3f getNormal() {
        return normal;
    }

    public void setNormal(Vec3f normal) {
        this.normal = normal;
    }


    public void setLight(int u, int v) {
        lightU = u;
        lightV = v;
    }

    public void setOverlay(int u, int v) {
        overlayU = u;
        overlayV = v;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(int red, int green, int blue, int alpha) {
        color = new Color(red, green, blue, alpha);
    }

    public void setTexture(float u, float v) {
        this.textureU = u;
        this.textureV = v;
    }

    public float getTextureU() {
        return textureU;
    }

    public float getTextureV() {
        return textureV;
    }

    public int getOverlayU() {
        return overlayU;
    }

    public int getOverlayV() {
        return overlayV;
    }

    public int getLightU() {
        return lightU;
    }

    public int getLightV() {
        return lightV;
    }
}
