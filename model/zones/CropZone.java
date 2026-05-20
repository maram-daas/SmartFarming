package model.zones;

import model.crops.Crop;
import model.enums.CropFamily;
import java.util.ArrayList;
import java.util.List;

public class CropZone extends Zone {
    private List<Crop> crops;

    public CropZone(String code, String name) {
        super(code, name);
        this.crops = new ArrayList<>();
    }

    public CropZone(String code, String name, double north, double south, double east, double west, CropFamily allowedFamily) {
        super(code, name, north, south, east, west);
        this.crops = new ArrayList<>();
        this.setAllowedCropFamily(allowedFamily);
    }

    public void addCrop(Crop crop) {
        if (getAllowedCropFamily() == null || crop.getFamily() == getAllowedCropFamily()) {
            crops.add(crop);
        } else {
            throw new IllegalArgumentException("This zone only allows " + getAllowedCropFamily() + " crops");
        }
    }

    public List<Crop> getCrops() { return crops; }
    @Override public int getEntityCount() { return crops.size(); }
}