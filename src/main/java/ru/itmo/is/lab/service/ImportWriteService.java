package ru.itmo.is.lab.service;

import ru.itmo.is.lab.entity.Car;
import ru.itmo.is.lab.event.ChangeType;
import ru.itmo.is.lab.event.DataChangedEvent;
import ru.itmo.is.lab.mapper.CarMapper;
import ru.itmo.is.lab.repository.CarRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class ImportWriteService {

    @Inject
    private HumanBeingService humans;
    @Inject
    private CarRepository cars;
    @Inject
    private CarMapper carMapper;
    @Inject
    private Event<DataChangedEvent> changes;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public int save(List<ImportFileReader.Entry> entries) {
        boolean carsCreated = false;
        for (ImportFileReader.Entry entry : entries) {
            Car car = entry.car == null ? null : carMapper.toEntity(entry.car);
            if (car != null) {
                cars.persist(car);
                carsCreated = true;
            }
            humans.persistNew(entry.human, car);
        }
        changes.fire(new DataChangedEvent("HumanBeing", null, ChangeType.BULK));
        if (carsCreated) {
            changes.fire(new DataChangedEvent("Car", null, ChangeType.BULK));
        }
        return entries.size();
    }
}
