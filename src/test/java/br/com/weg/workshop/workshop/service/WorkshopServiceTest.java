package br.com.weg.workshop.workshop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.weg.workshop.preference.domain.*;
import br.com.weg.workshop.preference.repository.*;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.user.domain.*;
import br.com.weg.workshop.user.repository.UserRepository;
import br.com.weg.workshop.workshop.domain.*;
import br.com.weg.workshop.workshop.dto.*;
import br.com.weg.workshop.workshop.repository.WorkshopRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkshopServiceTest {
 @Mock WorkshopRepository workshops; @Mock ThemeRepository themes; @Mock CategoryRepository categories; @Mock UserRepository users; @InjectMocks WorkshopService service;
 @Test void createsDraftWithValidatedReferences(){UserEntity creator=user(); Theme theme=Theme.create("Java",null); Category category=Category.create("Technology",null); when(users.findById(creator.getId())).thenReturn(Optional.of(creator));when(themes.findById(theme.getId())).thenReturn(Optional.of(theme));when(categories.findById(category.getId())).thenReturn(Optional.of(category));when(workshops.save(any())).thenAnswer(i->i.getArgument(0));var response=service.create(creator.getId(),request(theme,category));assertThat(response.status()).isEqualTo("DRAFT");assertThat(response.createdBy()).isEqualTo(creator.getId());}
 @Test void allowsOnlyDocumentedTransitions(){Workshop workshop=created();when(workshops.findById(workshop.getId())).thenReturn(Optional.of(workshop));service.publish(workshop.getCreatedBy().getId(),false,workshop.getId());assertThat(workshop.getStatus()).isEqualTo(WorkshopStatus.PUBLISHED);service.close(workshop.getCreatedBy().getId(),false,workshop.getId());assertThat(workshop.getStatus()).isEqualTo(WorkshopStatus.CLOSED);service.archive(workshop.getCreatedBy().getId(),false,workshop.getId());assertThat(workshop.getStatus()).isEqualTo(WorkshopStatus.ARCHIVED);}
 @Test void rejectsInvalidTransition(){Workshop workshop=created();when(workshops.findById(workshop.getId())).thenReturn(Optional.of(workshop));assertThatThrownBy(()->service.close(workshop.getCreatedBy().getId(),false,workshop.getId())).isInstanceOf(ConflictException.class);}
 @Test void publishesScheduledWorkshopWithSameLifecycleRule(){Workshop workshop=created();workshop.schedule(Instant.now().minusSeconds(1));when(workshops.findByStatusAndScheduledPublishAtLessThanEqual(org.mockito.ArgumentMatchers.eq(WorkshopStatus.SCHEDULED),org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of(workshop));service.publishScheduled();assertThat(workshop.getStatus()).isEqualTo(WorkshopStatus.PUBLISHED);}
 private Workshop created(){UserEntity creator=user();Theme theme=Theme.create("Java",null);Category category=Category.create("Technology",null);return Workshop.create(data(theme,category),theme,category,creator);}
 private WorkshopRequest request(Theme theme,Category category){return new WorkshopRequest("Spring Boot", "Workshop", null, theme.getId(),category.getId(),LocalDate.now().plusDays(5),LocalDate.now().plusDays(5),LocalTime.of(9,0),LocalTime.of(10,0),"Room 1",WorkshopModality.IN_PERSON,BigDecimal.ZERO,Instant.now().plus(Duration.ofDays(1)),Instant.now().plus(Duration.ofDays(2)),10,PaymentMethod.FREE,null);}
 private WorkshopData data(Theme theme,Category category){WorkshopRequest request=request(theme,category);return new WorkshopData(request.title(),request.description(),request.image(),request.startDate(),request.endDate(),request.startTime(),request.endTime(),request.location(),request.modality(),request.price(),request.registrationStart(),request.registrationEnd(),request.maximumParticipants(),request.paymentMethod(),request.additionalInformation());}
 private UserEntity user(){return UserEntity.create("Arweg","arweg","arweg@example.com",null,null,null,"hash",Role.ARWEG);}
}
