INSERT INTO public.genres (genre_id, "name")
VALUES (1, 'Комедия'),
       (2, 'Драма'),
       (3, 'Мультфильм'),
       (4, 'Триллер'),
       (5, 'Документальный'),
       (6, 'Боевик')
ON CONFLICT (genre_id) DO UPDATE
    SET "name" = EXCLUDED."name";


INSERT INTO public.mpa_ratings (rating_id, rating_name, description)
VALUES
    (1, 'G', 'У фильма нет возрастных ограничений'),
    (2, 'PG', 'Детям рекомендуется смотреть фильм с родителями'),
    (3, 'PG-13', 'Детям до 13 лет просмотр не желателен'),
    (4, 'R', 'Лицам до 17 лет просматривать фильм можно только в присутствии взрослого'),
    (5, 'NC-17', 'Лицам до 18 лет просмотр запрещён')
ON CONFLICT (rating_id) DO UPDATE
    SET rating_name = EXCLUDED.rating_name,
        description = EXCLUDED.description;