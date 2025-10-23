package ru.practicum.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.IntegrationTestBase;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.entity.User;
import ru.practicum.exception.NotFoundException;
import ru.practicum.parameters.UserAdminSearchParam;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserServiceIntegrationTest extends IntegrationTestBase {

    private final UserService service;
    private final EntityManager entityManager;

    @Test
    void create_whenOK() {
        String email = "abc@mail.com";
        String name = "name";
        UserDto savedDto = service.create(NewUserRequest.builder()
                .name(name)
                .email(email)
                .build());

        TypedQuery<User> query = entityManager.createQuery("select u from User u where u.id = :id", User.class);
        User saved = query.setParameter("id", savedDto.getId())
                .getSingleResult();

        assertThat(saved)
                .hasFieldOrPropertyWithValue("name", name)
                .hasFieldOrPropertyWithValue("email", email)
                .hasFieldOrPropertyWithValue("id", savedDto.getId());
    }

    @Test
    void create_whenEmailIsBusy() {
        String email = "email1";
        String name = "name";

        assertThatThrownBy(() -> service.create(NewUserRequest.builder()
                .name(name)
                .email(email)
                .build())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void delete_whenUserExists() {
        long userId = 1L;
        service.delete(userId);

        User user = entityManager.find(User.class, 1L);

        assertThat(user).isNull();
    }

    @Test
    void delete_whenUserNotExists() {
        long userId = 999L;
        assertThatThrownBy(() -> service.delete(userId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getUsers_withoutIdsAndFromParam() {
        List<UserDto> users = service.getUsers(UserAdminSearchParam.builder().size(10).from(0).build());

        assertThat(users).hasSize(10);
    }

    @Test
    void getUsers_withoutIdsAndWithFromParam() {
        List<UserDto> users = service.getUsers(UserAdminSearchParam.builder().size(10).from(0).build());

        assertThat(users).hasSize(10);
    }

    @Test
    void getUsers_withIds() {
        List<UserDto> users = service.getUsers(UserAdminSearchParam.builder().ids(List.of(3L, 7L)).size(10).from(0).build());

        assertThat(users).hasSize(2)
                .first()
                .hasFieldOrPropertyWithValue("id", 3L)
                .hasFieldOrPropertyWithValue("name", "name3")
                .hasFieldOrPropertyWithValue("email", "email3");

        assertThat(users).last()
                .hasFieldOrPropertyWithValue("id", 7L)
                .hasFieldOrPropertyWithValue("name", "name7")
                .hasFieldOrPropertyWithValue("email", "email7");
    }
}