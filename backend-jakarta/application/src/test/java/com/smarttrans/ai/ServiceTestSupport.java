package com.smarttrans.ai;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mockito.ArgumentMatchers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ServiceTestSupport {

    private ServiceTestSupport() {
    }

    public static void inject(Object target, Object... values) {
        boolean[] used = new boolean[values.length];
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                for (int index = 0; index < values.length; index++) {
                    Object value = values[index];
                    if (used[index] || value == null || !field.getType().isAssignableFrom(value.getClass())) {
                        continue;
                    }
                    field.setAccessible(true);
                    try {
                        field.set(target, value);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException(exception);
                    }
                    used[index] = true;
                    break;
                }
            }
            type = type.getSuperclass();
        }
        for (int index = 0; index < values.length; index++) {
            if (!used[index]) {
                throw new IllegalStateException("No field accepts " + values[index].getClass().getName());
            }
        }
    }

    public static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
                return;
            } catch (NoSuchFieldException exception) {
                type = type.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }
        throw new IllegalStateException("No id field on " + entity.getClass().getName());
    }

    public static void stubQueries(EntityManager entityManager, Map<String, Long> counts) {
        lenient().when(entityManager.createQuery(anyString(), ArgumentMatchers.<Class<?>>any()))
                .thenAnswer(invocation -> {
                    String jpql = invocation.getArgument(0);
                    long count = 0L;
                    for (Map.Entry<String, Long> entry : counts.entrySet()) {
                        if (jpql.contains(entry.getKey())) {
                            count = entry.getValue();
                            break;
                        }
                    }
                    return query(count, List.of());
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TypedQuery<?> query(long count, List<?> rows) {
        TypedQuery query = mock(TypedQuery.class);
        lenient().when(query.setParameter(anyString(), nullable(Object.class))).thenReturn(query);
        lenient().when(query.getResultList()).thenReturn(rows);
        lenient().when(query.getSingleResult()).thenReturn(count);
        return query;
    }
}
