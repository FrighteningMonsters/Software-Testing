package uk.ac.ed.acp.cw2.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Objects;


@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {
    @JsonProperty("lng")
    public Double lng;

    @JsonProperty("lat")
    public Double lat;

    public Position(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    @Override
    public String toString() {
        return String.format("Position{lng=%s, lat=%s}", lng, lat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Math.round(lng / 0.00015), Math.round(lat / 0.00015));
    }
}

