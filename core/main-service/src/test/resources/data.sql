INSERT INTO users (id, name, email)
VALUES  (1, 'name1', 'email1'),
        (2, 'name2', 'email2'),
        (3, 'name3', 'email3'),
        (4, 'name4', 'email4'),
        (5, 'name5', 'email5'),
        (6, 'name6', 'email6'),
        (7, 'name7', 'email7'),
        (8, 'name8', 'email8'),
        (9, 'name9', 'email9'),
        (10, 'name10', 'email10');
SELECT SETVAL('users_id_seq', (SELECT MAX(id) FROM users));

INSERT INTO category (id, name)
VALUES  (1, 'name1'),
        (2, 'name2'),
        (3, 'name3');
SELECT SETVAL('category_id_seq', (SELECT MAX(id) FROM category));


INSERT INTO event (id, title, annotation, state, event_date, category_id, initiator_id, lat, lon)
VALUES
  (1, 'title1', 'annotation1', 'PENDING', '2099-01-01 00:00:00', 1, 1, 0.1, 0.1),
  (2, 'Событие', 'Описание события', 'PUBLISHED', '2099-01-02 12:00:00', 1, 1, 0.2, 0.2);
SELECT SETVAL('event_id_seq', (SELECT MAX(id) FROM event));

