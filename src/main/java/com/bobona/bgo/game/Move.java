package com.bobona.bgo.game;

import com.bobona.bgo.web.data.RawMove;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Move {

    private Date timestamp;
    private List<Integer> squares;
    private List<Object> inputs;

    public Move(Date timestamp, List<Integer> squares, List<Object> inputs) {
        this.timestamp = timestamp;
        this.squares = squares;
        this.inputs = inputs;
    }

    public Move(RawMove rawMove) {
        this.timestamp = rawMove.getTimestamp();
        this.squares = rawMove.getSquares();
        inputs = new ArrayList<>();
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public List<Integer> getSquares() {
        return squares;
    }

    public List<Object> getInputs() {
        return inputs;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setSquares(List<Integer> squares) {
        this.squares = squares;
    }

    public void setInputs(List<Object> inputs) {
        this.inputs = inputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return squares.equals(move.squares);
    }

    @Override
    public int hashCode() {
        return Objects.hash(squares);
    }
}
