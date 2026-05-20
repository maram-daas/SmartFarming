package model.zones;

import model.crops.Crop;
import model.enums.CropFamily;
import java.util.ArrayList;
import java.util.List;

public class CropZone extends Zone {
    private List<Crop> crops;
    private CropFamily zoneType;

    public CropZone(String code, String name) {
        super(code, name);
        this.crops = new ArrayList<>();
        this.zoneType = null;
    }

    public CropZone(String code, String name, CropFamily zoneType) {
        super(code, name);
        this.crops = new ArrayList<>();
        this.zoneType = zoneType;
    }

    public CropFamily getZoneType() { return zoneType; }
    public void setZoneType(CropFamily zoneType) { this.zoneType = zoneType; }
    
    public void addCrop(Crop crop) {
        if (zoneType != null && !crop.getFamily().equals(zoneType)) {
            throw new IllegalArgumentException("Crop family " + crop.getFamily() + " does not match zone type " + zoneType);
        }
        crops.add(crop);
    }
    
    public List<Crop> getCrops() { return crops; }
    @Override public int getEntityCount() { return crops.size(); }
}
