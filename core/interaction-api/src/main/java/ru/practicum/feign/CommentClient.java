package ru.practicum.feign;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.comment.CommentWithUserDto;

import java.util.List;

@FeignClient(name = "comment-service", path = "/admin/comments")
public interface CommentClient {

    @GetMapping("/{eventId}/comments")
    List<CommentWithUserDto> getCommentsByEventId(@PathVariable Long eventId,
                                                  @RequestParam Integer from,
                                                  @RequestParam Integer size) throws FeignException;
}
