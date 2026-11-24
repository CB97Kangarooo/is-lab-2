package ru.itmo.is.lab.service;

import ru.itmo.is.lab.exception.HeroNameTakenException;
import ru.itmo.is.lab.repository.HumanBeingRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class HeroNamePolicy {

    @Inject
    private HumanBeingRepository humans;

    public void requireAvailable(String name, Long excludedId) {
        String normalized = normalize(name);
        if (humans.findNames(excludedId).stream().map(HeroNamePolicy::normalize).anyMatch(normalized::equals)) {
            throw new HeroNameTakenException(name, null);
        }
    }

    public Set<String> occupiedNames() {
        return humans.findNames(null).stream().map(HeroNamePolicy::normalize).collect(Collectors.toSet());
    }

    static String normalize(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
