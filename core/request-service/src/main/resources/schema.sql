CREATE TABLE IF NOT EXISTS public.participation_request (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT participation_request_event_fk
        FOREIGN KEY (event_id)
        REFERENCES public.event(id)
        ON DELETE CASCADE,  -- Удаление запросов при удалении события
    CONSTRAINT participation_status_check
        CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELED')),
    CONSTRAINT unique_request UNIQUE (requester_id, event_id)
);