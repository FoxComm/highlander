alter table customers add constraint non_guest_must_have_credentials check
    (email is not null and name is not null and is_guest = false or is_guest = true);
