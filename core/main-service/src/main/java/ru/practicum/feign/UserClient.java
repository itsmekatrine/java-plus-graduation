package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users")
public interface UserClient {

    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable Long userId);

    @GetMapping
    List<UserDto> getUsers(@RequestParam(required = false, name = "ids") List<Long> ids,
                           @RequestParam(defaultValue = "0",  name = "from") Integer from,
                           @RequestParam(defaultValue = "1000", name = "size") Integer size);
}
