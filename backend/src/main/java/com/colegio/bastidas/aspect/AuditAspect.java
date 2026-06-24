package com.colegio.bastidas.aspect;

import com.colegio.bastidas.model.AuditoriaLog;
import com.colegio.bastidas.repository.AuditoriaLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditAspect {

    private final AuditoriaLogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public AuditAspect(AuditoriaLogRepository logRepository, ObjectMapper objectMapper, EntityManager entityManager) {
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    @Around("execution(* com.colegio.bastidas.repository.*.save(..))")
    public Object auditSave(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return joinPoint.proceed();
        }

        Object entity = args[0];
        if (!isAuditable(entity)) {
            return joinPoint.proceed();
        }

        String tablaAfectada = entity.getClass().getSimpleName();
        String usuarioId = getCurrentUser();
        Long entityId = getEntityId(entity);
        
        String valoresAnteriores = null;
        String accion = "INSERT";

        // Si la entidad ya tiene ID, intentamos obtener su estado anterior
        if (entityId != null) {
            accion = "UPDATE";
            Object oldEntity = entityManager.find(entity.getClass(), entityId);
            if (oldEntity != null) {
                // Hacemos detach para que no se modifique con la transaccion actual antes de leerlo
                entityManager.detach(oldEntity);
                valoresAnteriores = extractJson(oldEntity);
            }
        }

        // Ejecutar el guardado real
        Object savedEntity = joinPoint.proceed();

        // Obtener valores nuevos
        String valoresNuevos = extractJson(savedEntity);
        Long savedEntityId = getEntityId(savedEntity);

        // Registrar log
        AuditoriaLog log = AuditoriaLog.builder()
                .tablaAfectada(tablaAfectada)
                .registroId(savedEntityId != null ? String.valueOf(savedEntityId) : "UNKNOWN")
                .accion(accion)
                .valoresAnteriores(valoresAnteriores)
                .valoresNuevos(valoresNuevos)
                .usuarioId(usuarioId)
                .build();
                
        logRepository.save(log);

        return savedEntity;
    }

    @Around("execution(* com.colegio.bastidas.repository.*.delete(..))")
    public Object auditDelete(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return joinPoint.proceed();
        }

        Object entity = args[0];
        if (!isAuditable(entity)) {
            return joinPoint.proceed();
        }

        String tablaAfectada = entity.getClass().getSimpleName();
        String usuarioId = getCurrentUser();
        Long entityId = getEntityId(entity);
        
        String valoresAnteriores = extractJson(entity);
        
        Object result = joinPoint.proceed();

        AuditoriaLog log = AuditoriaLog.builder()
                .tablaAfectada(tablaAfectada)
                .registroId(entityId != null ? String.valueOf(entityId) : "UNKNOWN")
                .accion("DELETE")
                .valoresAnteriores(valoresAnteriores)
                .valoresNuevos(null)
                .usuarioId(usuarioId)
                .build();
                
        logRepository.save(log);

        return result;
    }

    private boolean isAuditable(Object entity) {
        if (entity == null) return false;
        String name = entity.getClass().getSimpleName();
        return name.equals("Asistencia") || name.equals("Nota") || name.equals("Matricula");
    }

    private Long getEntityId(Object entity) {
        try {
            var method = entity.getClass().getMethod("getId");
            Object id = method.invoke(entity);
            if (id instanceof Long) return (Long) id;
        } catch (Exception e) {
            // Ignorar
        }
        return null;
    }

    /**
     * Extrae las propiedades escalares de la entidad para evitar infinite recursion
     * (LazyInitializationException) con Jackson.
     */
    private String extractJson(Object entity) {
        if (entity == null) return null;
        try {
            Map<String, Object> map = new HashMap<>();
            for (Field field : entity.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value == null) {
                    map.put(field.getName(), null);
                } else if (value.getClass().isPrimitive() || value instanceof String || value instanceof Number || value instanceof Boolean || value.getClass().isEnum() || value instanceof java.time.temporal.Temporal) {
                    map.put(field.getName(), value.toString());
                } else if (value.getClass().getPackage() != null && value.getClass().getPackage().getName().startsWith("com.colegio.bastidas.model")) {
                    try {
                        var idMethod = value.getClass().getMethod("getId");
                        Object idObj = idMethod.invoke(value);
                        map.put(field.getName() + "Id", idObj != null ? idObj.toString() : null);
                    } catch(Exception ignored) {}
                }
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"error\": \"Unserializable\"}";
        }
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}
