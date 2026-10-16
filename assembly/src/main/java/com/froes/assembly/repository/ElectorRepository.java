package com.froes.assembly.repository;


import com.froes.assembly.entity.Elector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElectorRepository extends JpaRepository<Elector, Long>, JpaSpecificationExecutor<Elector> {
    List<Elector> findAllByCpf(String cpf);
    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, Long id);
}
