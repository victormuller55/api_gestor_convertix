-- Campo de mensagem nos formulários de landing page já existentes.
-- Novas landings já nascem com esse campo no seed de LandingPageService.

INSERT INTO landing_page_campos (
    formulario_id, nome_interno, label, tipo, placeholder, obrigatorio, ordem, ativo, created_at, updated_at
)
SELECT
    f.id,
    'mensagem',
    'Mensagem',
    'TEXTAREA',
    'Como podemos ajudar?',
    0,
    4,
    1,
    NOW(6),
    NOW(6)
FROM landing_page_formularios f
WHERE NOT EXISTS (
    SELECT 1
    FROM landing_page_campos c
    WHERE c.formulario_id = f.id
      AND c.nome_interno = 'mensagem'
);
