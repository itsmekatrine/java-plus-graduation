package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.exception.BadRequestException;
import ru.practicum.feign.EventClient;
import ru.practicum.model.Comment;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ForbiddenException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.RequestClient;
import ru.practicum.feign.UserClient;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final UserClient userClient;
    private final EventClient eventClient;
    private final CommentRepository commentRepository;
    private final CommentMapper mapper;
    private final RequestClient requestClient;

    @Transactional
    public CommentDto addComment(Long userId, Long eventId, CreateUpdateCommentDto dto) {
        EventFullDto event = eventClient.findEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ForbiddenException("Событие должно быть опубликовано");
        }
        userClient.getUserById(userId);

        Set<String> forbidden = event.getForbiddenWords() == null ? Collections.emptySet() : event.getForbiddenWords();

        validateContent(dto.getContent(), forbidden);

        Comment comment = new Comment();
        comment.setEventId(eventId);
        comment.setAuthorId(userId);
        comment.setContent(dto.getContent());
        comment.setCreated(LocalDateTime.now());
        comment.setUpdated(LocalDateTime.now());

        comment = commentRepository.save(comment);

        boolean isAuthorParticipant = requestClient.exists(userId, eventId, "CONFIRMED");

        CommentDto response = mapper.toDto(comment);
        response.setIsAuthorParticipant(isAuthorParticipant);
        return response;
    }


    @Transactional
    public void addPreModeration(Long userId, Long eventId, PreModerationRequest preModerationDto) {
        EventFullDto event = eventClient.getByUserIdAndEventId(userId, eventId);
        Long initiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;

        if (!Objects.equals(initiatorId, userId)) {
            throw new ForbiddenException("Только инициатор события может устанавливать премодерацию");
        }
        if (preModerationDto == null || preModerationDto.getForbiddenWords() == null || preModerationDto.getForbiddenWords().isEmpty()) {
            throw new BadRequestException("Список слов пуст");
        }

        eventClient.appendForbiddenWords(eventId, preModerationDto);
    }

    @Transactional
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, CreateUpdateCommentDto dto) {
        EventFullDto event = eventClient.getByUserIdAndEventId(userId, eventId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (!Objects.equals(eventId, comment.getEventId())) {
            throw new ConflictException("Событие в пути запроса и событие комментария не совпадают");
        }
        if (!Objects.equals(comment.getAuthorId(), userId)) {
            throw new ForbiddenException("Редактировать можно только свои комментарии");
        }

        Set<String> forbidden = event.getForbiddenWords() == null ? Collections.emptySet() : event.getForbiddenWords();

        validateContent(dto.getContent(), forbidden);

        comment.setContent(dto.getContent());
        comment.setUpdated(LocalDateTime.now());

        comment = commentRepository.saveAndFlush(comment);

        boolean isAuthorParticipant = requestClient.exists(userId, eventId, "CONFIRMED");

        CommentDto response = mapper.toDto(comment);
        response.setIsAuthorParticipant(isAuthorParticipant);
        return response;
    }

    public List<CommentWithUserDto> getCommentsByEventId(Long eventId, Pageable pageable) {
        return commentRepository.findByEventId(eventId, pageable)
                .stream()
                .map(mapper::toWithUserDto)
                .toList();
    }

    public List<CommentWithEventDto> getUsersComments(Long userId, Pageable pageable) {
        return commentRepository.findByAuthorId(userId, pageable)
                .stream()
                .map(mapper::toWithEventDto)
                .toList();
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (!comment.getAuthorId().equals(userId)) {
            throw new ForbiddenException("Удалять можно только свои комментарии");
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public void deleteCommentsByUser(Long userId) {
        commentRepository.deleteByAuthorId(userId);
    }

    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));
        commentRepository.delete(comment);
    }

    private static void validateContent(String content, Set<String> forbiddenWords) {
        boolean hasForbidden = Arrays.stream(content.split(" ")).anyMatch(forbiddenWords::contains);
        if (hasForbidden) {
            throw new ForbiddenException("Комментарий содержит запрещённые слова");
        }
    }
}