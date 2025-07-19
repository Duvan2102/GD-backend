package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Departamento;
import com.helisa.docmanager.repository.DepartamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartamentoService {

    @Autowired
    private DepartamentoRepository departamentoRepository;

    public Departamento crearDepartamento(Departamento departamento) {
        if (departamentoRepository.existsByDescripcion(departamento.getDescripcion())) {
            throw new RuntimeException("Ya existe un departamento con la descripción: " + departamento.getDescripcion());
        }
        return departamentoRepository.save(departamento);
    }

    public List<Departamento> obtenerTodosDepartamentos() {
        return departamentoRepository.findAllByOrderByDescripcionAsc();
    }

    public Optional<Departamento> obtenerDepartamentoPorId(Integer id) {
        return departamentoRepository.findById(id);
    }

    public Optional<Departamento> obtenerDepartamentoPorDescripcion(String descripcion) {
        return departamentoRepository.findByDescripcion(descripcion);
    }

    public List<Departamento> buscarDepartamentosPorDescripcion(String descripcion) {
        return departamentoRepository.findByDescripcionContainingIgnoreCase(descripcion);
    }

    public Departamento actualizarDepartamento(Integer id, Departamento departamentoActualizado) {
        return departamentoRepository.findById(id)
                .map(departamento -> {
                    if (!departamento.getDescripcion().equals(departamentoActualizado.getDescripcion()) &&
                            departamentoRepository.existsByDescripcion(departamentoActualizado.getDescripcion())) {
                        throw new RuntimeException("Ya existe un departamento con la descripción: " + departamentoActualizado.getDescripcion());
                    }

                    departamento.setDescripcion(departamentoActualizado.getDescripcion());
                    return departamentoRepository.save(departamento);
                })
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado con id: " + id));
    }

    public void eliminarDepartamento(Integer id) {
        if (!departamentoRepository.existsById(id)) {
            throw new RuntimeException("Departamento no encontrado con id: " + id);
        }

        Optional<Departamento> departamento = departamentoRepository.findById(id);
        if (departamento.isPresent() && departamento.get().getAreas() != null && !departamento.get().getAreas().isEmpty()) {
            throw new RuntimeException("No se puede eliminar el departamento porque tiene áreas asociadas");
        }

        departamentoRepository.deleteById(id);
    }

    public boolean existeDepartamento(Integer id) {
        return departamentoRepository.existsById(id);
    }
}
