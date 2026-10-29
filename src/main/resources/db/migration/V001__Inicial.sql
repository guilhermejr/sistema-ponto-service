-- Um espelho por mês. Horas guardadas em minutos, com sinal, exatamente como vêm no PDF:
-- o serviço não recalcula nada do que o sistema de ponto da empresa já calculou.
create table espelhos (
    id bigserial not null,
    ano integer not null,
    mes integer not null,
    inicio date not null,
    fim date not null,
    horario_contratual varchar(50) not null,
    folha_pagamento varchar(7),
    cargo varchar(255),
    lotacao varchar(255),
    emitido date,
    assinado timestamp without time zone,
    arquivo varchar(255),
    criado timestamp without time zone,
    atualizado timestamp without time zone,
    usuario uuid not null,
    primary key (id),
    constraint ck_espelhos_mes check (mes between 1 and 12)
);

create unique index uk_espelhos_usuario_ano_mes on espelhos (usuario, ano, mes);

create table dias (
    id bigserial not null,
    espelho_id bigint not null,
    data date not null,
    marcacoes varchar(255),
    ocorrencia varchar(255),
    abono integer,
    trabalhadas integer,
    compensaveis integer,
    acumulo integer,
    nao_compensaveis integer,
    primary key (id),
    constraint fk_dias_espelho foreign key (espelho_id) references espelhos (id) on delete cascade
);

create unique index uk_dias_espelho_data on dias (espelho_id, data);

-- Marcações Incluídas (I) e Descartadas (D) do rodapé do espelho.
create table ajustes (
    id bigserial not null,
    espelho_id bigint not null,
    tipo varchar(1) not null,
    data_hora timestamp without time zone not null,
    motivo varchar(255),
    primary key (id),
    constraint fk_ajustes_espelho foreign key (espelho_id) references espelhos (id) on delete cascade,
    constraint ck_ajustes_tipo check (tipo in ('I', 'D'))
);

create index ix_ajustes_espelho on ajustes (espelho_id);
