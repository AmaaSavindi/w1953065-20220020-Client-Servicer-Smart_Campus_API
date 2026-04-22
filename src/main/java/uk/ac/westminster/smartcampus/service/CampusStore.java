package uk.ac.westminster.smartcampus.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import uk.ac.westminster.smartcampus.model.Room;
import uk.ac.westminster.smartcampus.model.Sensor;
import uk.ac.westminster.smartcampus.model.SensorReading;
import uk.ac.westminster.smartcampus.model.SensorStatus;

public class CampusStore {

    public enum RoomRemovalResult {
        REMOVED,
        NOT_FOUND,
        BLOCKED_BY_ACTIVE_SENSORS
    }

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

    public synchronized boolean addRoom(Room room) {
        return rooms.putIfAbsent(room.getId(), copyRoom(room)) == null;
    }

    public boolean roomHasActiveSensors(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null || room.getSensorIds().isEmpty()) {
            return false;
        }

        synchronized (room.getSensorIds()) {
            for (String sensorId : room.getSensorIds()) {
                Sensor sensor = sensors.get(sensorId);
                if (sensor != null && SensorStatus.isActive(sensor.getStatus())) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized RoomRemovalResult removeRoomIfNoActiveSensors(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return RoomRemovalResult.NOT_FOUND;
        }

        if (roomHasActiveSensors(roomId)) {
            return RoomRemovalResult.BLOCKED_BY_ACTIVE_SENSORS;
        }

        synchronized (room.getSensorIds()) {
            for (String sensorId : room.getSensorIds()) {
                sensors.computeIfPresent(sensorId, (ignored, existingSensor) -> {
                    Sensor detachedSensor = copySensor(existingSensor);
                    detachedSensor.setRoomId(null);
                    return detachedSensor;
                });
            }
            room.getSensorIds().clear();
        }

        rooms.remove(roomId);
        return RoomRemovalResult.REMOVED;
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

    public synchronized boolean addSensor(Sensor sensor) {
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

    public boolean readingExists(String sensorId, String readingId) {
        List<SensorReading> sensorReadings = readings.get(sensorId);
        if (sensorReadings == null) {
            return false;
        }

        synchronized (sensorReadings) {
            for (SensorReading sensorReading : sensorReadings) {
                if (readingId.equals(sensorReading.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized boolean addReading(String sensorId, SensorReading reading) {
        if (!sensors.containsKey(sensorId)) {
            return false;
        }

        List<SensorReading> sensorReadings = readings.computeIfAbsent(sensorId,
                ignored -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (sensorReadings) {
            sensorReadings.add(copyReading(reading));
        }

        sensors.computeIfPresent(sensorId, (ignored, existingSensor) -> {
            Sensor updatedSensor = copySensor(existingSensor);
            updatedSensor.setCurrentValue(reading.getValue());
            return updatedSensor;
        });
        return true;
    }

    public synchronized void clear() {
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
