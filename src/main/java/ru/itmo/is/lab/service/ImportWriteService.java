package ru.itmo.is.lab.service;

import ru.itmo.is.lab.entity.Car;
import ru.itmo.is.lab.event.ChangeType;
import ru.itmo.is.lab.event.DataChangedEvent;
import ru.itmo.is.lab.exception.HeroNameTakenException;
import ru.itmo.is.lab.mapper.CarMapper;
import ru.itmo.is.lab.repository.CarRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;

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
    @Inject
    private ImportHistoryService history;
    @Inject
    private HeroNamePolicy names;
    @Inject
    private UniqueNameWrites uniqueNameWrites;

    public int save(long operationId, List<ImportFileReader.Entry> entries) {
        return uniqueNameWrites.run(() -> saveInTransaction(operationId, entries));
    }

    private int saveInTransaction(long operationId, List<ImportFileReader.Entry> entries) {
        boolean carsCreated = false;
        Set<String> occupied = names.occupiedNames();
        for (int index = 0; index < entries.size(); index++) {
            ImportFileReader.Entry entry = entries.get(index);
            if (!occupied.add(HeroNamePolicy.normalize(entry.human.getName()))) {
                throw new HeroNameTakenException(entry.human.getName(), index + 1);
            }
            Car car = entry.car == null ? null : carMapper.toEntity(entry.car);
            if (car != null) {
                cars.persist(car);
                carsCreated = true;
            }
            humans.persistNew(entry.human, car);
        }
        history.succeed(operationId, entries.size());
        changes.fire(new DataChangedEvent("HumanBeing", null, ChangeType.BULK));
        if (carsCreated) {
            changes.fire(new DataChangedEvent("Car", null, ChangeType.BULK));
        }
        return entries.size();
    }
}
