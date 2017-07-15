drop table if exists project;
drop table if exists client;

create table client (
    id      integer primary key,
    name    varchar(128) not null,
    address varchar(128),
    city    varchar(128),
    state   varchar(128),
    zip     varchar(128)
);

create table project (
    id          integer primary key,
    client_id   integer not null,
    name        varchar(128),
    amount      decimal(12,2),
    invoiced    boolean,
    manager     varchar(128),
    acct_mgr    varchar(128),
    foreign key (client_id) references client(id)
);

commit;