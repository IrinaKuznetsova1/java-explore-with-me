package ru.practicum.ewm.main.services;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.ewm.main.dto.newRequests.NewComment;
import ru.practicum.ewm.main.dto.responses.CommentDto;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentAdminRequest;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentUserRequest;
import ru.practicum.ewm.main.enums.CommentState;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.exceptions.ConflictException;
import ru.practicum.ewm.main.exceptions.NotFoundException;
import ru.practicum.ewm.main.mapper.CommentMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.Location;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.CategoryRepository;
import ru.practicum.ewm.main.repository.CommentRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
    private CommentMapper commentMapper;

    //переменные для создания сущностей
    //User
    private User user;
    private long userId;
    private final String userEmail = "email@email.com";
    private final String userName = "userName";

    //Category
    private Category category;
    private int catId;
    private final String catName = "catName";

    //Event
    private Event event;
    private long eventId;
    private final String eventTitle = "eventTitle";
    private final String eventAnnotation = "eventAnnotation";
    private final String eventDescription = "eventDescription";
    private final LocalDateTime eventDate = LocalDateTime.now().plusDays(1);
    private final Location eventLocation = new Location(45, 45);
    private final EventsState eventsState = EventsState.PUBLISHED;
    private final boolean eventAllowComments = true;


    @BeforeEach
    void setup() {
        user = userRepository.save(new User(0L, userEmail, userName));
        userId = user.getId();

        category = categoryRepository.save(new Category(0, catName));
        catId = category.getId();

        Event eventForSave = Event.builder()
                .id(0L)
                .category(category)
                .title(eventTitle)
                .annotation(eventAnnotation)
                .description(eventDescription)
                .createdOn(LocalDateTime.now().minusMinutes(1L))
                .eventDate(eventDate)
                .publishedOn(LocalDateTime.now())
                .initiator(user)
                .location(eventLocation)
                .state(eventsState)
                .allowComments(eventAllowComments)
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

        //проверить, что комментарий появился у event
        Event eventWithComment = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("", ""));
        assertNotNull(eventWithComment.getComments());
        assertFalse(eventWithComment.getComments().isEmpty());
        assertThat(eventWithComment.getComments().size()).isEqualTo(1);

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
        commentRepository.deleteById(commentDto.getId());
        assertTrue(commentRepository.findById(commentDto.getId()).isEmpty());

        //проверить выбрасываемые исключения
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.deleteComment(notExistUserId, commentDto.getId()));
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.deleteComment(userId, notExistComId));
    }

    @Test
    void findCommentById() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        CommentDto searchedComment = commentService.findCommentById(userId, commentDto.getId());
        assertThat(commentDto).usingRecursiveComparison().isEqualTo(searchedComment);

        //проверить выбрасываемые исключения
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.findCommentById(commentDto.getId(), notExistUserId));
        long notExistComId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.findCommentById(notExistComId, userId));
    }

    @Test
    void findCommentsByAuthor() {
        CommentDto commentDto = commentService.createComment(userId, eventId, new NewComment("text"));
        List<CommentDto> comments = commentService.findCommentsByAuthor(userId);

        assertNotNull(commentDto);
        assertFalse(comments.isEmpty());
        assertThat(comments.size()).isEqualTo(1);
        assertThat(comments.getFirst()).usingRecursiveComparison().isEqualTo(commentDto);

        //проверить выбрасываемые исключения
        long notExistUserId = 1000000L;
        assertThrows(NotFoundException.class, () -> commentService.findCommentsByAuthor(notExistUserId));
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