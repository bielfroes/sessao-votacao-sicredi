package com.froes.assembly.repository.spec;

import com.froes.assembly.entity.Elector;
import org.springframework.data.jpa.domain.Specification;

public final class ElectorSpecification {

    private ElectorSpecification() {
    }

    public static Specification<Elector> withCpf(String cpf) {
        return (root, query, criteriaBuilder) ->
                cpf == null || cpf.isBlank()
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("cpf"), cpf);
    }
}
