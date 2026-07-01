-- Ícones de itens do BioLink passam a usar o enum BioLinkItemIcone (string).
-- Valores legados (ex.: emoji) são limpos na subida via DatabaseSchemaMigrator.
UPDATE biolink_items
SET icone = NULL
WHERE icone IS NOT NULL
  AND icone NOT IN (
    'WHATSAPP', 'INSTAGRAM', 'TIKTOK', 'YOUTUBE', 'FACEBOOK', 'LINKEDIN', 'X',
    'TELEGRAM', 'DISCORD', 'SPOTIFY', 'PINTEREST', 'THREADS', 'SNAPCHAT', 'TWITCH',
    'GITHUB', 'BEHANCE', 'DRIBBBLE', 'MEDIUM', 'SUBSTACK', 'GOOGLE_MAPS', 'OUTROS'
  );
