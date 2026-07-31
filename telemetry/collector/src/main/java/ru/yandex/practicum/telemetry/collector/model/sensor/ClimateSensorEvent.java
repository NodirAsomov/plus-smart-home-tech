package ru.yandex.practicum.telemetry.collector.model.sensor;

public class ClimateSensorEvent extends SensorEvent {
    private int temperatureC;
    private int humidity;
    private int co2Level;
    public int getTemperatureC() { return temperatureC; }
    public void setTemperatureC(int temperatureC) { this.temperatureC = temperatureC; }
    public int getHumidity() { return humidity; }
    public void setHumidity(int humidity) { this.humidity = humidity; }
    public int getCo2Level() { return co2Level; }
    public void setCo2Level(int co2Level) { this.co2Level = co2Level; }
    @Override public SensorEventType getType() { return SensorEventType.CLIMATE_SENSOR_EVENT; }
}
