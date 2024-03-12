package org.vagabond.engine.crud.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.vagabond.engine.auth.entity.BaseProfileEntity;
import org.vagabond.engine.auth.entity.BaseUserEntity;
import org.vagabond.engine.crud.entity.BaseEntity;
import org.vagabond.engine.crud.service.ICrudService;
import org.vagabond.engine.crud.utils.SecurityUtils;
import org.vagabond.engine.exeption.MetierException;

@RunOnVirtualThread
public abstract class BaseSecurityResource<T extends BaseEntity> implements BaseResource {

    public static final String ADMIN = "ADMIN";

    protected ICrudService<T> service;

    protected String roleModify = ADMIN;
    protected String roleRead = "";

    @Inject
    JsonWebToken jwt;

    @Inject
    EntityManager entityManager;

    @GET()
    @Path("/{id}")
    public Response findById(@Context SecurityContext contexte, Long id) {
        var userConnected = hasRole(contexte, roleRead);
        var entity = service.findById(id);
        return responseOk(doAfterFindById(userConnected, entity));
    }

    protected <U extends BaseUserEntity<P>, P extends BaseProfileEntity> Object doAfterFindById(U userConnected, T response) {
        if (SecurityUtils.hasRole(userConnected, ADMIN)) {
            return response;
        }
        return toDto(response);
    }

    public abstract Object toDto(T entity);

    @SuppressWarnings("unchecked")
    @Transactional
    protected <U extends BaseUserEntity<P>, P extends BaseProfileEntity> U hasRole(SecurityContext contexte, String roles) {
        U user = null;
        List<String> groups = new ArrayList<>();
        if (contexte != null && contexte.getUserPrincipal() != null) {
            Query query = entityManager.createQuery("FROM UserEntity u WHERE u.username = :username");
            query.setParameter("username", contexte.getUserPrincipal().getName());
            var users = query.getResultList();
            if (users != null && !users.isEmpty()) {
                user = (U) query.getResultList().get(0);
                if (user != null) {
                    groups = user.getProfiles().stream().map(profile -> profile.roles.split(",")).flatMap(Arrays::stream).toList();
                }
            }
        }
        SecurityUtils.hasRole(roles, groups);
        return user;
    }

    protected <U extends BaseUserEntity<P>, P extends BaseProfileEntity> void verifyUserConnected(U user, Long id) {
        if (!user.id.equals(id) && user.getProfiles().stream().filter(profile -> profile.roles.contains((ADMIN))).toList().isEmpty()) {
            throw new MetierException("ERRORS.NOT_ALLOWED");
        }
    }
}
