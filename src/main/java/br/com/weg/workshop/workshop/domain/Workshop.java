package br.com.weg.workshop.workshop.domain;

import br.com.weg.workshop.preference.domain.Category;
import br.com.weg.workshop.preference.domain.Theme;
import br.com.weg.workshop.user.domain.UserEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "workshop", schema = "workshop")
public class Workshop {
    @Id private UUID id;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    private String image;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "theme_id", nullable = false) private Theme theme;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false) private Category category;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(nullable = false) private LocalTime startTime;
    @Column(nullable = false) private LocalTime endTime;
    @Column(nullable = false) private String location;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WorkshopModality modality;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false) private Instant registrationStart;
    @Column(nullable = false) private Instant registrationEnd;
    @Column(nullable = false) private int maximumParticipants;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentMethod paymentMethod;
    @Column(nullable = false) private boolean championship;
    @Column(columnDefinition = "TEXT") private String additionalInformation;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WorkshopStatus status;
    private Instant scheduledPublishAt;
    private Instant publishedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = false) private UserEntity createdBy;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Workshop() { }
    public static Workshop create(WorkshopData data, Theme theme, Category category, UserEntity creator) {
        Workshop workshop = new Workshop(); workshop.id = UUID.randomUUID(); workshop.apply(data, theme, category);
        workshop.createdBy = creator; workshop.status = WorkshopStatus.DRAFT; workshop.createdAt = Instant.now(); workshop.updatedAt = workshop.createdAt; return workshop;
    }
    public void apply(WorkshopData data, Theme theme, Category category) { title=data.title(); description=data.description(); image=data.image(); this.theme=theme; this.category=category; startDate=data.startDate(); endDate=data.endDate(); startTime=data.startTime(); endTime=data.endTime(); location=data.location(); modality=data.modality(); price=data.price(); registrationStart=data.registrationStart(); registrationEnd=data.registrationEnd(); maximumParticipants=data.maximumParticipants(); paymentMethod=data.paymentMethod(); championship=data.championship(); additionalInformation=data.additionalInformation(); updatedAt=Instant.now(); }
    public void schedule(Instant when) { status=WorkshopStatus.SCHEDULED; scheduledPublishAt=when; updatedAt=Instant.now(); }
    public void publish() { status=WorkshopStatus.PUBLISHED; scheduledPublishAt=null; publishedAt=Instant.now(); updatedAt=publishedAt; }
    public void close() { status=WorkshopStatus.CLOSED; updatedAt=Instant.now(); }
    public void cancel() { status=WorkshopStatus.CANCELLED; updatedAt=Instant.now(); }
    public void archive() { status=WorkshopStatus.ARCHIVED; updatedAt=Instant.now(); }
    public void replaceImage(String storageKey) { image = storageKey; updatedAt = Instant.now(); }
    public void removeImage() { image = null; updatedAt = Instant.now(); }
    public UUID getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;} public String getImage(){return image;} public Theme getTheme(){return theme;} public Category getCategory(){return category;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public LocalTime getStartTime(){return startTime;} public LocalTime getEndTime(){return endTime;} public String getLocation(){return location;} public WorkshopModality getModality(){return modality;} public BigDecimal getPrice(){return price;} public Instant getRegistrationStart(){return registrationStart;} public Instant getRegistrationEnd(){return registrationEnd;} public int getMaximumParticipants(){return maximumParticipants;} public PaymentMethod getPaymentMethod(){return paymentMethod;} public boolean isChampionship(){return championship;} public String getAdditionalInformation(){return additionalInformation;} public WorkshopStatus getStatus(){return status;} public Instant getScheduledPublishAt(){return scheduledPublishAt;} public Instant getPublishedAt(){return publishedAt;} public UserEntity getCreatedBy(){return createdBy;}
}
