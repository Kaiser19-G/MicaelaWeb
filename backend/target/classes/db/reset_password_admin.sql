-- ============================================================
-- ACTUALIZAR CONTRASEÑA ADMIN – I.E. Micaela Bastidas
-- Ejecutar en: Supabase Dashboard → SQL Editor
-- ============================================================

-- OPCIÓN 1: Actualizar con el hash que ya tienes en la BD
-- (si ese hash fue generado con BCrypt de 'admin123', debería funcionar)
-- No necesitas hacer nada si el hash es correcto.

-- OPCIÓN 2 (RECOMENDADA): Actualizar con un hash nuevo generado ahora
-- Hash BCrypt de 'admin123' con strength=12:
UPDATE usuarios 
SET password_hash = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TiGVT.VjnxfNB8xNq7h5j7pJ0eDy'
WHERE username = 'admin';

-- Verificar que la actualización fue exitosa
SELECT id, username, rol, activo, primer_login 
FROM usuarios 
WHERE username = 'admin';

-- ============================================================
-- TAMBIÉN: Asegúrate de que el usuario está activo
-- ============================================================
UPDATE usuarios SET activo = true WHERE username = 'admin';
