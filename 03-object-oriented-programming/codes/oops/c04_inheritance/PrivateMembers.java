package oops.c04_inheritance;

public class PrivateMembers {
    public static void main(String[] args) {
        SmartBulb bulb = new SmartBulb();
        bulb.setDeviceId(116);
        bulb.setPowerStatus(true);
        bulb.setBrightnessLevel(69);
        System.out.println(bulb.getBrightnessLevel());
        System.out.println(bulb.isPowerOn());
        
    }
}

/**
 * SmartApplicances
 * When we follow the Encapsulation
 * the child class should access the Fields & Methods using GettersAndSetters
 */
class SmartApplicances{
    private int deviceId;
    private boolean powerStatus = false;

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isPowerOn() {
        return powerStatus;
    }

    public void setPowerStatus(boolean powerStatus) {
        this.powerStatus = powerStatus;
    }

    
}

class SmartBulb extends SmartApplicances{
    private int brightnessLevel = 0;

    public int getBrightnessLevel() {
        if (!isPowerOn())
            throw new RuntimeException("Please turn on the device to set brightness leve");
        return brightnessLevel;
    }

    public void setBrightnessLevel(int brightnessLevel) {
        if (brightnessLevel < 0 || brightnessLevel > 100)
            throw new IllegalArgumentException("Brightness level must should be from 0-100");
        this.brightnessLevel = brightnessLevel;
    }
}