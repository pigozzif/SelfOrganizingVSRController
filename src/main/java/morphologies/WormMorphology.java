package morphologies;

import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.SerializationUtils;


public class WormMorphology extends Morphology {

    public WormMorphology(int w, int h, String sensorConfig) {
        super(w, h, "worm-5x1", sensorConfig);
        this.fillBody();
    }

    @Override
    public void fillBody() {
        /*SensingVoxel sensingVoxel = new SensingVoxel(this.sensors);
        for (int j=0; j < this.body.getH(); ++j) {
            for (int i = 0; i < this.body.getW(); ++i) {
                this.body.set(i, j, SerializationUtils.clone(sensingVoxel));
                this.allowedMorph.add(new Pair(i, j));
            }
        }*/
    }

}
