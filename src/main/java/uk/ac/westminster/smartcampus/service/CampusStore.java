package uk.ac.westminster.smartcampus.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;

public class CampusStore {

    private static final CampusStore INSTANCE = new CampusStore();

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private CampusStore() {
    }

    public static CampusStore getInstance() {
        return INSTANCE;
    }

    public List<Room> getAllRooms() {
        List<Room> roomList = new ArrayList<>();
        for (Room room : rooms.values()) {
            roomList.add(copyRoom(room));
        }
        return roomList;
    }

    public Optional<Room> getRoom(String roomId) {
        Room room = rooms.get(roomId);
        return room == null ? Optional.empty() : Optional.of(copyRoom(room));
    }

    public boolean roomExists(String roomId) {
        return rooms.containsKey(roomId);
    }

    public boolean addRoom(Room room) {
        return rooms.putIfAbsent(room.getId(), copyRoom(room)) == null;
    }

    public boolean deleteRoom(String roomId) {
        return rooms.remove(roomId) != null;
    }

    public boolean roomHasSensors(String roomId) {
        Room room = rooms.get(roomId);
        return room != null && !room.getSensorIds().isEmpty();
    }

    public List<Sensor> getAllSensors() {
        List<Sensor> sensorList = new ArrayList<>();
        for (Sensor sensor : sensors.values()) {
            sensorList.add(copySensor(sensor));
        }
        return sensorList;
    }

    public Optional<Sensor> getSensor(String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        return sensor == null ? Optional.empty() : Optional.of(copySensor(sensor));
    }

    public boolean sensorExists(String sensorId) {
        return sensors.containsKey(sensorId);
    }

    public boolean addSensor(Sensor sensor) {
        Sensor sensorCopy = copySensor(sensor);
        Sensor existing = sensors.putIfAbsent(sensorCopy.getId(), sensorCopy);
        if (existing != null) {
            return false;
        }

        Room room = rooms.get(sensorCopy.getRoomId());
        if (room != null) {
            synchronized (room.getSensorIds()) {
                room.getSensorIds().add(sensorCopy.getId());
            }
        }

        readings.putIfAbsent(sensorCopy.getId(), Collections.synchronizedList(new ArrayList<>()));
        return true;
    }

    public List<SensorReading> getReadings(String sensorId) {
        List<SensorReading> sensorReadings = readings.get(sensorId);
        List<SensorReading> history = new ArrayList<>();
        if (sensorReadings == null) {
            return history;
        }

        synchronized (sensorReadings) {
            for (SensorReading sensorReading : sensorReadings) {
                history.add(copyReading(sensorReading));
            }
        }
        return history;
    }

    public boolean addReading(String sensorId, SensorReading reading) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return false;
        }

        List<SensorReading> sensorReadings = readings.computeIfAbsent(sensorId,
                ignored -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (sensorReadings) {
            sensorReadings.add(copyReading(reading));
        }
        sensor.setCurrentValue(reading.getValue());
        return true;
    }

    public void clear() {
        rooms.clear();
        sensors.clear();
        readings.clear();
    }

    private Room copyRoom(Room room) {
        Room copy = new Room();
        copy.setId(room.getId());
        copy.setName(room.getName());
        copy.setCapacity(room.getCapacity());
        List<String> sensorIds = new ArrayList<>();
        if (room.getSensorIds() != null) {
            sensorIds.addAll(room.getSensorIds());
        }
        copy.setSensorIds(Collections.synchronizedList(sensorIds));
        return copy;
    }

    private Sensor copySensor(Sensor sensor) {
        Sensor copy = new Sensor();
        copy.setId(sensor.getId());
        copy.setType(sensor.getType());
        copy.setStatus(sensor.getStatus());
        copy.setCurrentValue(sensor.getCurrentValue());
        copy.setRoomId(sensor.getRoomId());
        return copy;
    }

    private SensorReading copyReading(SensorReading reading) {
        SensorReading copy = new SensorReading();
        copy.setId(reading.getId());
        copy.setTimestamp(reading.getTimestamp());
        copy.setValue(reading.getValue());
        return copy;
    }
}
