package ru.practicum.ewm.main.services;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.practicum.ewm.main.dto.newRequests.NewComment;
import ru.practicum.ewm.main.dto.responses.CommentDto;
import ru.practicum.ewm.main.dto.responses.EventFullDto;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentAdminRequest;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentUserRequest;
import ru.practicum.ewm.main.enums.CommentSort;
import ru.practicum.ewm.main.enums.CommentState;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.exceptions.ConflictException;
import ru.practicum.ewm.main.exceptions.NotFoundException;
import ru.practicum.ewm.main.exceptions.ValidationException;
import ru.practicum.ewm.main.model.*;
import ru.practicum.ewm.main.repository.CategoryRepository;
import ru.practicum.ewm.main.repository.CommentRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.UserRepository;
import ru.practicum.ewm.stats.client.StatClient;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class CommentServiceTest {
    @Autowired
    private CommentService commentService;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventService eventService;
    @MockBean
    private StatClient statClient;

    //User
    private User user;
    private long userId;

    //Category
    private Category category;

    //Event
    private Event event;
    private long eventId;

    @BeforeEach
    void setup() {
        user = userRepository.save(new User(0L, "email@email.com", "userName"));
        userId = user.getId();

        category = categoryRepository.save(new Category(0, "catName"));

        Event eventForSave = Event.builder()
                .id(0L)
                .category(category)
                .title("eventTitle")
                .annotation("eventAnnotation")
                .description("eventDescription")
                .createdOn(LocalDateTime.now().minusMinutes(1L))
                .eventDate(LocalDateTime.now().plusDays(1))
                .publishedOn(LocalDateTime.now())
                .initiator(user)
                .location(new Location(45, 45))
                .state(EventsState.PUBLISHED)
                .allowComments(true)
                .build();
        event = eventRepository.save(eventForSave);
        eventId = event.getId();
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createComment() {
        final String newCommentText = "newCommentText";
        NewComment newComment = new NewComment(newCommentText);
        CommentDto commentDto = commentService.createComment(userId, eventId, newComment);

        //проверить, что новый объект корректно сохранился
        assertNotNull(commentDto);
        assertTrue(commentDto.getId() > 0);
        assertThat(commentDto.getEventId()).isEqualTo(eventId);
        assertThat(commentDto.getAuthorId()).isEqualTo(userId);
        assertThat(commentDto.getText()).isEqualTo(newCommentText);
        assertNotNull(commentDto.getCreated());
        assertThat(commentDto.getState()).isEqualTo(CommentState.PENDING);
        assertThat(commentDto.getUseful()).isEqualTo(0L);

        //проверить, что event.countOfComments = 1
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(List.of());
        commentService.updateCommentByAdmin(commentDto.getId(),
                UpdateCommentAdminRequest
                        .builder()
                        .state(CommentState.PUBLISHED)
                        .build());
        EventFullDto eventWithComments = eventService.findEventByIdAndUserId(eventId, userId);
        assertThat(eventWithComments.getCountOfComments()).isEqualTo(1);

        //проверить выбрасываемые исключения
        //выбрасывает NotFoundException, если пользователь не найден
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.createComment(notExistUserId, eventId, newComment));

        //выбрасывает NotFoundException, если событие не найдено
        long notExistEventId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.createComment(userId, notExistEventId, newComment));

        //выбрасывает ConflictException, если событие не опубликовано
        event.setState(EventsState.PENDING);
        eventRepository.save(event);
        assertThrows(ConflictException.class, () -> commentService.createComment(userId, eventId, newComment));
        event.setState(EventsState.PUBLISHED);
        eventRepository.save(event);

        //выбрасывает ConflictException, если событие нельзя комментировать
        event.setAllowComments(false);
        eventRepository.save(event);
        assertThrows(ConflictException.class, () -> commentService.createComment(userId, eventId, newComment));
    }

    @Test
    void updateCommentByUser() {
        final String oldText = "text";
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment(oldText));
        final String newText = "updated text";
        CommentDto updatedCommentDto = commentService.updateCommentByUser(userId, commentDto.getId(), new UpdateCommentUserRequest(newText));

        assertNotNull(updatedCommentDto);
        assertThat(updatedCommentDto.getText()).isEqualTo(newText);
        assertThat(updatedCommentDto).usingRecursiveComparison().ignoringFields("text").isEqualTo(commentDto);

        //проверить выбрасываемые исключения
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.updateCommentByUser(notExistUserId, commentDto.getId(),
                new UpdateCommentUserRequest(newText)));
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.updateCommentByUser(userId, notExistComId,
                new UpdateCommentUserRequest(newText)));
    }

    @Test
    void updateCommentByAdmin() {
        final String oldText = "text";
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment(oldText));
        final String newText = "updated text";
        CommentDto updatedCommentDto = commentService.updateCommentByAdmin(commentDto.getId(),
                new UpdateCommentAdminRequest(newText, CommentState.PUBLISHED));

        assertNotNull(updatedCommentDto);
        assertThat(updatedCommentDto.getText()).isEqualTo(newText);
        assertThat(updatedCommentDto.getState()).isEqualTo(CommentState.PUBLISHED);
        assertThat(updatedCommentDto).usingRecursiveComparison().ignoringFields("text", "state")
                .isEqualTo(commentDto);

        //проверить выбрасываемые исключения
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.updateCommentByAdmin(notExistComId,
                new UpdateCommentAdminRequest(newText, CommentState.PUBLISHED)));
    }

    @Test
    void deleteComment() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        commentService.updateCommentByAdmin(commentDto.getId(),
                UpdateCommentAdminRequest
                        .builder()
                        .state(CommentState.PUBLISHED)
                        .build());
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(List.of());
        EventFullDto eventWithComments = eventService.findEventByIdAndUserId(eventId, userId);
        assertThat(eventWithComments.getCountOfComments()).isEqualTo(1);

        commentService.deleteComment(userId, commentDto.getId());
        assertTrue(commentRepository.findById(commentDto.getId()).isEmpty());
        eventWithComments = eventService.findEventByIdAndUserId(eventId, userId);
        assertThat(eventWithComments.getCountOfComments()).isEqualTo(0);

        //проверить выбрасываемые исключения
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.deleteComment(notExistUserId, commentDto.getId()));
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.deleteComment(userId, notExistComId));
    }

    @Test
    void findCommentByIdUser() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        CommentDto searchedComment = commentService.findCommentByIdUser(userId, commentDto.getId());
        assertThat(commentDto).usingRecursiveComparison().isEqualTo(searchedComment);

        //проверить выбрасываемые исключения
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.findCommentByIdUser(commentDto.getId(), notExistUserId));
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.findCommentByIdUser(notExistComId, userId));
    }

    @Test
    void findCommentByIdAdmin() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        CommentDto searchedComment = commentService.findCommentByIdAdmin(commentDto.getId());
        assertThat(commentDto).usingRecursiveComparison().isEqualTo(searchedComment);

        //проверить выбрасываемые исключения
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.findCommentByIdAdmin(notExistComId));
    }

    @Test
    void findCommentByIdPublic() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        long comId = commentDto.getId();
        commentService.updateCommentByAdmin(comId, UpdateCommentAdminRequest.builder().state(CommentState.PUBLISHED).build());
        CommentDto searchedComment = commentService.findCommentByIdPublic(eventId, comId);
        assertThat(commentDto)
                .usingRecursiveComparison()
                .ignoringFields("state")
                .isEqualTo(searchedComment);
        assertThat(searchedComment.getState()).isEqualTo(CommentState.PUBLISHED);

        //проверить выбрасываемые исключения
        long notExistEventId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.findCommentByIdPublic(notExistEventId, comId));
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.findCommentByIdPublic(eventId, notExistComId));
        commentService.updateCommentByAdmin(comId, UpdateCommentAdminRequest.builder().state(CommentState.PENDING).build());
        assertThrows(ConflictException.class, () -> commentService.findCommentByIdPublic(eventId, comId));
    }

    @Test
    void findCommentsByAuthor() {
        CommentDto commentDto1 = commentService.createComment(userId, eventId, new NewComment("text1"));
        List<CommentDto> comments1 = commentService.findCommentsByAuthor(userId, 0, 10);

        assertNotNull(commentDto1);
        assertFalse(comments1.isEmpty());
        assertThat(comments1.size()).isEqualTo(1);
        assertThat(comments1.getFirst()).usingRecursiveComparison().isEqualTo(commentDto1);

        //проверить выбрасываемые исключения
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.findCommentsByAuthor(notExistUserId, 0, 10));

        //проверить параметры from, size
        CommentDto commentDto2 = commentService.createComment(userId, eventId, new NewComment("text2"));
        CommentDto commentDto3 = commentService.createComment(userId, eventId, new NewComment("text3"));
        CommentDto commentDto4 = commentService.createComment(userId, eventId, new NewComment("text4"));

        List<CommentDto> comments2 = commentService.findCommentsByAuthor(userId, 2, 2);
        assertFalse(comments2.isEmpty());
        assertThat(comments2.size()).isEqualTo(2);
        assertThat(comments2.getFirst()).usingRecursiveComparison().isEqualTo(commentDto2);
        assertThat(comments2.getLast()).usingRecursiveComparison().isEqualTo(commentDto1);

        List<CommentDto> comments3 = commentService.findCommentsByAuthor(userId, 0, 2);
        assertFalse(comments3.isEmpty());
        assertThat(comments3.size()).isEqualTo(2);
        assertThat(comments3.getFirst()).usingRecursiveComparison().isEqualTo(commentDto4);
        assertThat(comments3.getLast()).usingRecursiveComparison().isEqualTo(commentDto3);
    }

    @Test
    void findCommentsByAdmin() {
        //создать новые объекты для проверки фильтрации комментариев
        long otherUserId = userRepository.save(new User(0L, "otherUser@email.com", "otherName")).getId();
        long otherEventId = eventRepository.save(Event.builder()
                        .id(0L)
                        .category(category)
                        .title("eventTitle")
                        .annotation("eventAnnotation")
                        .description("eventDescription")
                        .createdOn(LocalDateTime.now().minusMinutes(1L))
                        .eventDate(LocalDateTime.now().plusDays(1))
                        .publishedOn(LocalDateTime.now())
                        .initiator(user)
                        .location(new Location(45, 45))
                        .state(EventsState.PUBLISHED)
                        .allowComments(true)
                        .build())
                .getId();
        //создать комментарии с разными authorId, eventId
        CommentDto commentDto1 = commentService.createComment(userId, eventId, new NewComment("text1"));
        CommentDto commentDto2 = commentService.createComment(otherUserId, eventId, new NewComment("text2"));
        CommentDto commentDto3 = commentService.createComment(userId, otherEventId, new NewComment("text3"));
        CommentDto commentDto4 = commentService.createComment(otherUserId, otherEventId, new NewComment("text4"));

        //обновить комментарии, чтобы были разные states
        commentDto2 = commentService.updateCommentByAdmin(commentDto2.getId(), UpdateCommentAdminRequest.builder().state(CommentState.PUBLISHED).build());
        commentDto3 = commentService.updateCommentByAdmin(commentDto3.getId(), UpdateCommentAdminRequest.builder().state(CommentState.PUBLISHED).build());

        //проверить поиск при отсутствии параметров
        List<CommentDto> comments1 = commentService.findCommentsByAdmin(
                null,
                null,
                null,
                null,
                null,
                0, 10);
        assertNotNull(comments1);
        assertFalse(comments1.isEmpty());
        assertThat(comments1.size()).isEqualTo(4);
        assertThat(comments1.getFirst()).usingRecursiveComparison().isEqualTo(commentDto4);
        assertThat(comments1.get(1)).usingRecursiveComparison().isEqualTo(commentDto3);
        assertThat(comments1.get(2)).usingRecursiveComparison().isEqualTo(commentDto2);
        assertThat(comments1.getLast()).usingRecursiveComparison().isEqualTo(commentDto1);

        //проверить поиск по параметру users = userId. Должен вернуть commentDto3, commentDto1
        List<CommentDto> comments2 = commentService.findCommentsByAdmin(
                List.of(userId),
                null,
                null,
                null,
                null,
                0, 10);
        assertNotNull(comments2);
        assertFalse(comments2.isEmpty());
        assertThat(comments2.size()).isEqualTo(2);
        assertThat(comments2.getFirst()).usingRecursiveComparison().isEqualTo(commentDto3);
        assertThat(comments2.getLast()).usingRecursiveComparison().isEqualTo(commentDto1);

        //проверить поиск по параметру users = notExistUserId. Должен вернуть пустой список
        long notExistUserId = 1000000L;
        List<CommentDto> comments3 = commentService.findCommentsByAdmin(
                List.of(notExistUserId),
                null,
                null,
                null,
                null,
                0, 10);
        assertNotNull(comments3);
        assertTrue(comments3.isEmpty());

        //проверить поиск по параметру states = CommentState.PUBLISHED. Должен вернуть commentDto3, commentDto2
        List<CommentDto> comments4 = commentService.findCommentsByAdmin(
                null,
                List.of(CommentState.PUBLISHED),
                null,
                null,
                null,
                0, 10);
        assertNotNull(comments4);
        assertFalse(comments4.isEmpty());
        assertThat(comments4.size()).isEqualTo(2);
        assertThat(comments4.getFirst()).usingRecursiveComparison().isEqualTo(commentDto3);
        assertThat(comments4.getLast()).usingRecursiveComparison().isEqualTo(commentDto2);

        //проверить поиск по параметру states = CommentState.CANCELED. Должен вернуть пустой список
        List<CommentDto> comments5 = commentService.findCommentsByAdmin(
                null,
                List.of(CommentState.CANCELED),
                null,
                null,
                null,
                0, 10);
        assertNotNull(comments5);
        assertTrue(comments5.isEmpty());

        //проверить поиск по параметру events = eventId. Должен вернуть commentDto2, commentDto1
        List<CommentDto> comments6 = commentService.findCommentsByAdmin(
                null,
                null,
                List.of(eventId),
                null,
                null,
                0, 10);
        assertNotNull(comments6);
        assertFalse(comments6.isEmpty());
        assertThat(comments6.size()).isEqualTo(2);
        assertThat(comments6.getFirst()).usingRecursiveComparison().isEqualTo(commentDto2);
        assertThat(comments6.getLast()).usingRecursiveComparison().isEqualTo(commentDto1);

        //проверить поиск по параметру events = notExistEventId. Должен вернуть пустой  список
        long notExistEventId = 1000000L;
        List<CommentDto> comments7 = commentService.findCommentsByAdmin(
                null,
                null,
                List.of(notExistEventId),
                null,
                null,
                0, 10);
        assertNotNull(comments7);
        assertTrue(comments7.isEmpty());

        //проверить поиск по параметру rangeStart и rangeEnd. Должен вернуть все 4 commentDto
        List<CommentDto> comments8 = commentService.findCommentsByAdmin(
                null,
                null,
                null,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                0, 10);
        assertNotNull(comments8);
        assertFalse(comments8.isEmpty());
        assertThat(comments8.size()).isEqualTo(4);
        assertThat(comments8.getFirst()).usingRecursiveComparison().isEqualTo(commentDto4);
        assertThat(comments8.get(1)).usingRecursiveComparison().isEqualTo(commentDto3);
        assertThat(comments8.get(2)).usingRecursiveComparison().isEqualTo(commentDto2);
        assertThat(comments8.getLast()).usingRecursiveComparison().isEqualTo(commentDto1);

        //проверить поиск по параметру rangeStart и rangeEnd в прошлом. Должен вернуть пустой список
        List<CommentDto> comments9 = commentService.findCommentsByAdmin(
                null,
                null,
                null,
                LocalDateTime.now().minusYears(2),
                LocalDateTime.now().minusYears(1),
                0, 10);
        assertNotNull(comments9);
        assertTrue(comments9.isEmpty());

        //проверить поиск по параметру rangeStart и rangeEnd в будущем. Должен вернуть пустой список
        List<CommentDto> comments10 = commentService.findCommentsByAdmin(
                null,
                null,
                null,
                LocalDateTime.now().plusYears(1),
                LocalDateTime.now().plusYears(2),
                0, 10);
        assertNotNull(comments10);
        assertTrue(comments10.isEmpty());

        //проверить параметры from = 2, size = 2. Должен вернуть commentDto2, commentDto1
        List<CommentDto> comments11 = commentService.findCommentsByAdmin(
                null,
                null,
                null,
                null,
                null,
                2, 2);
        assertNotNull(comments11);
        assertFalse(comments11.isEmpty());
        assertThat(comments11.size()).isEqualTo(2);
        assertThat(comments11.getFirst()).usingRecursiveComparison().isEqualTo(commentDto2);
        assertThat(comments11.getLast()).usingRecursiveComparison().isEqualTo(commentDto1);

        //проверить параметры from = 0, size = 2. Должен вернуть commentDto4, commentDto3
        List<CommentDto> comments12 = commentService.findCommentsByAdmin(
                null,
                null,
                null,
                null,
                null,
                0, 2);
        assertNotNull(comments12);
        assertFalse(comments12.isEmpty());
        assertThat(comments12.size()).isEqualTo(2);
        assertThat(comments12.getFirst()).usingRecursiveComparison().isEqualTo(commentDto4);
        assertThat(comments12.getLast()).usingRecursiveComparison().isEqualTo(commentDto3);

        //проверить поиск при указании всех параметров from = 0, size = 2. Должен вернуть commentDto4
        List<CommentDto> comments13 = commentService.findCommentsByAdmin(
                List.of(otherUserId),
                List.of(CommentState.PENDING),
                List.of(otherEventId),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                0, 10);
        assertNotNull(comments13);
        assertFalse(comments13.isEmpty());
        assertThat(comments13.size()).isEqualTo(1);
        assertThat(comments13.getFirst()).usingRecursiveComparison().isEqualTo(commentDto4);

        //проверить выбрасываемые исключения
        //ValidationException, если rangeStart позднее rangeEnd
        assertThrows(ValidationException.class, () -> commentService.findCommentsByAdmin(
                null,
                null,
                null,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().minusHours(1),
                0, 10));
    }

    @Test
    void findCommentsPublic() {
        //создать новые объекты для проверки фильтрации комментариев
        long otherEventId = eventRepository.save(Event.builder()
                        .id(0L)
                        .category(category)
                        .title("eventTitle")
                        .annotation("eventAnnotation")
                        .description("eventDescription")
                        .createdOn(LocalDateTime.now().minusMinutes(1L))
                        .eventDate(LocalDateTime.now().plusDays(1))
                        .publishedOn(LocalDateTime.now())
                        .initiator(user)
                        .location(new Location(45, 45))
                        .state(EventsState.PUBLISHED)
                        .allowComments(true)
                        .build())
                .getId();
        //создать комментарии с разными eventId
        CommentDto commentDto1 = commentService.createComment(userId, eventId, new NewComment("text1"));
        CommentDto commentDto2 = commentService.createComment(userId, eventId, new NewComment("text2"));
        CommentDto commentDto3 = commentService.createComment(userId, otherEventId, new NewComment("text3"));
        CommentDto commentDto4 = commentService.createComment(userId, otherEventId, new NewComment("text4"));

        //опубликовать только commentDto1, commentDto2, commentDto3
        commentDto1 = commentService.updateCommentByAdmin(commentDto1.getId(), UpdateCommentAdminRequest.builder().state(CommentState.PUBLISHED).build());
        commentDto2 = commentService.updateCommentByAdmin(commentDto2.getId(), UpdateCommentAdminRequest.builder().state(CommentState.PUBLISHED).build());
        commentDto3 = commentService.updateCommentByAdmin(commentDto3.getId(), UpdateCommentAdminRequest.builder().state(CommentState.PUBLISHED).build());

        //проверка поиска по параметру eventId = eventId. Должен вернуть commentDto2, commentDto1
        List<CommentDto> comments1 = commentService.findCommentsPublic(eventId, 0, 10, null);
        assertNotNull(comments1);
        assertFalse(comments1.isEmpty());
        assertThat(comments1.size()).isEqualTo(2);
        assertThat(comments1.getFirst()).usingRecursiveComparison().isEqualTo(commentDto2);
        assertThat(comments1.getLast()).usingRecursiveComparison().isEqualTo(commentDto1);

        //проверка поиска по параметру eventId = otherEventId. Должен вернуть только commentDto3, т.к. commentDto4 не опубликован
        List<CommentDto> comments2 = commentService.findCommentsPublic(otherEventId, 0, 10, null);
        assertNotNull(comments2);
        assertFalse(comments2.isEmpty());
        assertThat(comments2.size()).isEqualTo(1);
        assertThat(comments2.getFirst()).usingRecursiveComparison().isEqualTo(commentDto3);

        //проверка параметров from, size
        List<CommentDto> comments3 = commentService.findCommentsPublic(eventId, 0, 1, null);
        assertNotNull(comments3);
        assertFalse(comments3.isEmpty());
        assertThat(comments3.size()).isEqualTo(1);
        assertThat(comments3.getFirst()).usingRecursiveComparison().isEqualTo(commentDto2);

        List<CommentDto> comments4 = commentService.findCommentsPublic(eventId, 1, 1, null);
        assertNotNull(comments4);
        assertFalse(comments4.isEmpty());
        assertThat(comments4.size()).isEqualTo(1);
        assertThat(comments4.getFirst()).usingRecursiveComparison().isEqualTo(commentDto1);

        //проверка параметра sort = CommentSort.COMMENT_DATE. Первым элементом должен быть commentDto2
        List<CommentDto> comments5 = commentService.findCommentsPublic(eventId, 0, 10, CommentSort.COMMENT_DATE);
        assertNotNull(comments5);
        assertFalse(comments5.isEmpty());
        assertThat(comments5.size()).isEqualTo(2);
        assertThat(comments5.getFirst()).usingRecursiveComparison().isEqualTo(commentDto2);

        //проверка параметра sort = CommentSort.USEFUL. Первым элементом должен быть commentDto1
        long newUserId = userRepository.save(new User(0L, "newUser@mail.com", "newUserName")).getId();
        commentDto1 = commentService.addLikeToComment(newUserId, commentDto1.getId());
        List<CommentDto> comments6 = commentService.findCommentsPublic(eventId, 0, 10, CommentSort.USEFUL);
        assertNotNull(comments6);
        assertFalse(comments6.isEmpty());
        assertThat(comments6.size()).isEqualTo(2);
        assertThat(comments6.getFirst()).usingRecursiveComparison().isEqualTo(commentDto1);

        //проверить выбрасываемые исключения
        long notExistId = 10000000;
        assertThrows(NotFoundException.class, () -> commentService.findCommentsPublic(notExistId, 0, 10, null));
    }

    @Test
    void addLikeToComment() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        final long comId = commentDto.getId();
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PUBLISHED));
        User otherUser = userRepository.save(new User(0L, "otherUser@email.com", "otherUserName"));
        long otherUserId = otherUser.getId();
        commentDto = commentService.addLikeToComment(otherUserId, comId);

        assertNotNull(commentDto);
        assertFalse(commentDto.getLikes().isEmpty());
        assertThat(commentDto.getLikes().size()).isEqualTo(1);
        List<User> likes = commentDto.getLikes().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
        assertThat(likes.getFirst()).usingRecursiveComparison().isEqualTo(otherUser);
        assertThat(commentDto.getUseful()).isEqualTo(1L);

        User otherUser2 = userRepository.save(new User(0L, "otherUser2@email.com", "otherUser2Name"));
        long otherUserId2 = otherUser2.getId();
        commentDto = commentService.addLikeToComment(otherUserId2, comId);

        assertNotNull(commentDto);
        assertFalse(commentDto.getLikes().isEmpty());
        assertThat(commentDto.getLikes().size()).isEqualTo(2);
        likes = commentDto.getLikes().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
        assertThat(likes.getFirst()).usingRecursiveComparison().isEqualTo(otherUser);
        assertThat(likes.getLast()).usingRecursiveComparison().isEqualTo(otherUser2);
        assertThat(commentDto.getUseful()).isEqualTo(2L);

        //проверить изменение поля useful, если пользователь, поставивший лайк, потом ставит дизлайк
        commentDto = commentService.addDislikeToComment(otherUserId2, comId);
        assertThat(commentDto.getUseful()).isEqualTo(0L);

        //проверить выбрасываемые исключения
        //NotFoundException, если пользователь не найден
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.addLikeToComment(notExistUserId, comId));

        //NotFoundException, если комментарий не найден
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.addLikeToComment(otherUserId, notExistComId));

        //ConflictException, если автор комментария хочет поставить своему комментарию лайк
        assertThrows(ConflictException.class, () -> commentService.addLikeToComment(userId, comId));

        //ConflictException, если комментарий не опубликован
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PENDING));
        assertThrows(ConflictException.class, () -> commentService.addLikeToComment(userId, comId));
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PUBLISHED));

        //ConflictException, если пользователь уже поставил лайк
        assertThrows(ConflictException.class, () -> commentService.addLikeToComment(otherUserId, comId));
    }

    @Test
    void deleteLikeFromComment() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        final long comId = commentDto.getId();
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PUBLISHED));
        User otherUser = userRepository.save(new User(0L, "otherUser@email.com", "otherUserName"));
        long otherUserId = otherUser.getId();
        commentService.addLikeToComment(otherUserId, comId);
        commentDto = commentService.deleteLikeFromComment(otherUserId, comId);

        assertNotNull(commentDto);
        assertTrue(commentDto.getLikes().isEmpty());
        assertThat(commentDto.getUseful()).isEqualTo(0L);

        //проверить выбрасываемые исключения
        //NotFoundException, если пользователь не найден
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.deleteLikeFromComment(notExistUserId, comId));

        //NotFoundException, если комментарий не найден
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.deleteLikeFromComment(otherUserId, notExistComId));

        //ConflictException, если автор комментария хочет удалить лайк у своего комментария
        assertThrows(ConflictException.class, () -> commentService.deleteLikeFromComment(userId, comId));

        //ConflictException, если комментарий не опубликован
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PENDING));
        assertThrows(ConflictException.class, () -> commentService.deleteLikeFromComment(userId, comId));
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PUBLISHED));

        //ConflictException, если лайк пользователя  не существует
        assertThrows(ConflictException.class, () -> commentService.deleteLikeFromComment(otherUserId, comId));
    }

    @Test
    void addDislikeToComment() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        final long comId = commentDto.getId();
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PUBLISHED));
        User otherUser = userRepository.save(new User(0L, "otherUser@email.com", "otherUserName"));
        long otherUserId = otherUser.getId();
        commentDto = commentService.addDislikeToComment(otherUserId, comId);

        assertNotNull(commentDto);
        assertFalse(commentDto.getDislikes().isEmpty());
        assertThat(commentDto.getDislikes().size()).isEqualTo(1);
        List<User> dislikes = commentDto.getDislikes().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
        assertThat(dislikes.getFirst()).usingRecursiveComparison().isEqualTo(otherUser);
        assertThat(commentDto.getUseful()).isEqualTo(-1L);

        User otherUser2 = userRepository.save(new User(0L, "otherUser2@email.com", "otherUser2Name"));
        long otherUserId2 = otherUser2.getId();
        commentDto = commentService.addDislikeToComment(otherUserId2, comId);

        assertNotNull(commentDto);
        assertFalse(commentDto.getDislikes().isEmpty());
        assertThat(commentDto.getDislikes().size()).isEqualTo(2);
        dislikes = commentDto.getDislikes().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
        assertThat(dislikes.getFirst()).usingRecursiveComparison().isEqualTo(otherUser);
        assertThat(dislikes.getLast()).usingRecursiveComparison().isEqualTo(otherUser2);
        assertThat(commentDto.getUseful()).isEqualTo(-2L);

        //проверить изменение поля useful, если пользователь, поставивший дизлайк, потом ставит лайк
        commentDto = commentService.addLikeToComment(otherUserId2, comId);
        assertThat(commentDto.getUseful()).isEqualTo(0L);

        //проверить выбрасываемые исключения
        //NotFoundException, если пользователь не найден
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.addDislikeToComment(notExistUserId, comId));

        //NotFoundException, если комментарий не найден
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.addDislikeToComment(otherUserId, notExistComId));

        //ConflictException, если автор комментария хочет поставить своему комментарию дислайк
        assertThrows(ConflictException.class, () -> commentService.addDislikeToComment(userId, comId));

        //ConflictException, если комментарий не опубликован
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PENDING));
        assertThrows(ConflictException.class, () -> commentService.addDislikeToComment(userId, comId));
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PUBLISHED));

        //ConflictException, если пользователь уже поставил дислайк
        assertThrows(ConflictException.class, () -> commentService.addDislikeToComment(otherUserId, comId));
    }

    @Test
    void deleteDislikeFromComment() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        final long comId = commentDto.getId();
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PUBLISHED));
        User otherUser = userRepository.save(new User(0L, "otherUser@email.com", "otherUserName"));
        long otherUserId = otherUser.getId();
        commentService.addDislikeToComment(otherUserId, comId);
        commentDto = commentService.deleteDislikeFromComment(otherUserId, comId);

        assertNotNull(commentDto);
        assertTrue(commentDto.getLikes().isEmpty());
        assertThat(commentDto.getUseful()).isEqualTo(0L);

        //проверить выбрасываемые исключения
        //NotFoundException, если пользователь не найден
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.deleteDislikeFromComment(notExistUserId, comId));

        //NotFoundException, если комментарий не найден
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.deleteDislikeFromComment(otherUserId, notExistComId));

        //ConflictException, если автор комментария хочет удалить лайк у своего комментария
        assertThrows(ConflictException.class, () -> commentService.deleteDislikeFromComment(userId, comId));

        //ConflictException, если комментарий не опубликован
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PENDING));
        assertThrows(ConflictException.class, () -> commentService.deleteDislikeFromComment(userId, comId));
        commentService.updateCommentByAdmin(comId, new UpdateCommentAdminRequest(null, CommentState.PUBLISHED));

        //ConflictException, если лайк пользователя  не существует
        assertThrows(ConflictException.class, () -> commentService.deleteDislikeFromComment(otherUserId, comId));
    }
}