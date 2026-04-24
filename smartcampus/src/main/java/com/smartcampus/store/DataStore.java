package com.smartcampus.store;

import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {}

    public static DataStore getInstance() {
        return INSTANCE;
    }

    //  Rooms 

    public Map<String, Room> getRooms() { return rooms; }

    public Room getRoom(String id) { return rooms.get(id); }

    public void putRoom(Room room) { rooms.put(room.getId(), room); }

    public boolean deleteRoom(String id) {
        return rooms.remove(id) != null;
    }

    public boolean roomExists(String id) { return rooms.containsKey(id); }

    // Sensors

    public Map<String, Sensor> getSensors() { return sensors; }

    public Sensor getSensor(String id) { return sensors.get(id); }

    public void putSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public boolean sensorExists(String id) { return sensors.containsKey(id); }

    public boolean deleteSensor(String id) {
        readings.remove(id);
        return sensors.remove(id) != null;
    }

    //Readings

    public List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
}
