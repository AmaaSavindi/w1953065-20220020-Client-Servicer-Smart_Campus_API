package uk.ac.westminster.smartcampus.exception;

public class RoomNotEmptyException extends RuntimeException {

    public RoomNotEmptyException(String message) {
        super(message);
    }
}
