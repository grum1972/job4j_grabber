create table if not exists posts (
	id      serial primary key,
    name    text,
    text    text,
    link    text unique,
    created timestamp
);
