package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.CommentWithEventDto;
import ru.practicum.dto.comment.CommentWithUserDto;
import ru.practicum.model.Comment;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentMapper {

    @Mapping(target = "event", source = "eventId")
    @Mapping(target = "author", source = "authorId")
    CommentDto toDto(Comment comment);

    @Mapping(target = "event", source = "eventId")
    CommentWithUserDto toWithUserDto(Comment comment);

    @Mapping(target = "author", source = "authorId")
    CommentWithEventDto toWithEventDto(Comment comment);
}