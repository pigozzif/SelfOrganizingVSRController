package morphologies;

import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.SerializationUtils;


public class BipedMorphology extends Morphology {

    private final int legWidth;

    public BipedMorphology(int w, int h, String sensorConfig, int l) {
        super(w, h, "test", sensorConfig);
        this.legWidth = l;
        this.fillBody();
    }

    @Override
    public void fillBody() {
        /*SensingVoxel sensingVoxel = new SensingVoxel(this.sensors);
        for (int j=0; j < this.body.getH(); ++j) {
            for (int i=0; i < this.body.getW(); ++i) {
                if ((i < this.legWidth || i >= this.body.getW() - this.legWidth) || (j > this.legWidth - 1)) {
                    this.body.set(i, j, SerializationUtils.clone(sensingVoxel));
                    this.allowedMorph.add(new Pair(i, j));
                }
            }
        }*/
    }

}
