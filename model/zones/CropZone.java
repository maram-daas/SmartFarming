package model.zones;

import model.crops.Crop;
import java.util.ArrayList;
import java.util.List;

public class CropZone extends Zone {
    private List<Crop> crops;

    public CropZone(String code, String name) {
        super(code, name);
        this.crops = new ArrayList<>();
    }

    public void addCrop(Crop crop) { crops.add(crop); }
    public List<Crop> getCrops() { return crops; }
    @Override public int getEntityCount() { return crops.size(); }
}
