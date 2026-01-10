package uk.ac.ed.acp.cw2.data;

public class MedDispatchRec {
    public int id;
    public String date;
    public String time;
    public Requirements requirements;
    public Position delivery;

    public static class Requirements {
        public Double capacity;
        public Boolean cooling;
        public Boolean heating;
        public Double maxCost;
    }
}
