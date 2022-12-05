package com.bobona.bgo.game.utils;

public class Position {

    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // convert to single valued position
    public int unwrap(int width) {
        return x + y * width;
    }

    public Position add(Position other) {
        return new Position(x + other.x, y + other.y);
    }

    public Position multiply(int o) {
        return new Position(x * o, y * o);
    }
}
