package ru.itmo.is.lab.dto;

public class CarWithOwnerDto extends CarDto {

    private OwnerDto owner;

    public CarWithOwnerDto() {
    }

    public CarWithOwnerDto(CarDto car, OwnerDto owner) {
        super(car.getId(), car.getName(), car.getCool());
        this.owner = owner;
    }

    public OwnerDto getOwner() { return owner; }
    public void setOwner(OwnerDto owner) { this.owner = owner; }
}
