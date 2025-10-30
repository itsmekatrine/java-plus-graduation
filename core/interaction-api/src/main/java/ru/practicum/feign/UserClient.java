package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/admin/users")
    List<UserDto> getUsers(@RequestParam(required = false, name = "ids") List<Long> ids,
                           @RequestParam(defaultValue = "0",  name = "from") Integer from,
                           @RequestParam(defaultValue = "1000", name = "size") Integer size);

    default UserDto getUserById(Long userId) {
        List<UserDto> res = getUsers(List.of(userId), 0, 1);
        return res.isEmpty() ? null : res.get(0);
    }
}
