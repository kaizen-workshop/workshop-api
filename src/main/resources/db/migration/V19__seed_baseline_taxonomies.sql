INSERT INTO workshop.theme (id, name, description, active, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Tecnologia e Inovação', 'Tecnologia, transformação digital e novas soluções.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000002', 'Qualidade', 'Qualidade, excelência e melhoria contínua.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000003', 'Lean Manufacturing', 'Eficiência operacional e eliminação de desperdícios.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000004', 'Segurança', 'Saúde, segurança e prevenção no ambiente de trabalho.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000005', 'Sustentabilidade', 'Práticas sustentáveis e responsabilidade ambiental.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000006', 'Liderança e Gestão', 'Desenvolvimento de pessoas, equipes e liderança.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO workshop.category (id, name, description, active, created_at, updated_at)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'Treinamento', 'Capacitação técnica ou comportamental.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000002', 'Palestra', 'Apresentação conduzida por especialistas.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000003', 'Workshop Prático', 'Atividade prática com participação dos inscritos.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000004', 'Integração', 'Encontro de integração e colaboração entre equipes.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
