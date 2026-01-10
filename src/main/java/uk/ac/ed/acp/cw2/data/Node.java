package uk.ac.ed.acp.cw2.data;

public class Node {
    public Position position;
    public double g = Double.POSITIVE_INFINITY; // moves from start
    public double f = Double.POSITIVE_INFINITY; // g + h
    public Node parent;

    public Node(Position position) { this.position = position; }
}
