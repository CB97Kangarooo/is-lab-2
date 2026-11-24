package ru.itmo.is.lab.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "import_operation")
public class ImportOperation {

    public enum Status {
        IN_PROGRESS, SUCCESS, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiator_id", nullable = false)
    private AppUser initiator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 11)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "added_count")
    private Integer addedCount;

    protected ImportOperation() {
    }

    public ImportOperation(AppUser initiator) {
        this.initiator = initiator;
    }

    public void succeed(int count) {
        requireInProgress();
        status = Status.SUCCESS;
        addedCount = count;
    }

    public void fail() {
        requireInProgress();
        status = Status.FAILED;
    }

    private void requireInProgress() {
        if (status != Status.IN_PROGRESS) {
            throw new IllegalStateException("Import already finished: " + id);
        }
    }

    public Long getId() { return id; }
    public AppUser getInitiator() { return initiator; }
    public Status getStatus() { return status; }
    public Integer getAddedCount() { return addedCount; }
}
