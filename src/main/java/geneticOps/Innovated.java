package geneticOps;

import buildingBlocks.MyController;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME,
        include=JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value=MyController.Edge.class, name="edge"),
        @JsonSubTypes.Type(value=MyController.Neuron.class, name="neuron"),
})
public abstract class Innovated implements Comparable<Innovated> {

    private static class InnovationEvent {

        public final int first;
        public final int second;

        public InnovationEvent(int x, int y) {
            this.first = x;
            this.second = y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }

    }

    protected InnovationEvent event;

    public Innovated() { this.event = null; }

    public Innovated(int x, int y) {
        this.event = new InnovationEvent(x, y);
    }

    public int getFirstInnovation() { return this.event.first; }

    public int getSecondInnovation() { return this.event.second; }

    public void setInnovation(int x, int y) { this.event = new InnovationEvent(x, y); }

    public void setInnovation(InnovationEvent otherEvent) { this.event = otherEvent; }

    public int getInnovation() { return this.event.hashCode(); }

    @Override
    public int compareTo(Innovated other) {
            return Integer.compare(this.getInnovation(), other.getInnovation());
        }

}
