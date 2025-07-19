package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Area;
import com.helisa.docmanager.model.Departamento;
import com.helisa.docmanager.repository.AreaRepository;
import com.helisa.docmanager.repository.DepartamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AreaService {

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    public Area crearArea(Area area) {
        if (area.getDepartamento() == null || area.getDepartamento().getIdDepartamento() == null) {
            throw new RuntimeException("El departamento es requerido para crear un área");
        }

        Optional<Departamento> departamento = departamentoRepository.findById(area.getDepartamento().getIdDepartamento());
        if (!departamento.isPresent()) {
            throw new RuntimeException("El departamento especificado no existe");
        }

        if (areaRepository.existsByDescripcionAndDepartamento(area.getDescripcion(), departamento.get())) {
            throw new RuntimeException("Ya existe un área con la descripción '" + area.getDescripcion() + "' en este departamento");
        }


        area.setDepartamento(departamento.get());
        return areaRepository.save(area);
    }

    public List<Area> obtenerTodasAreas() {
        return areaRepository.findAllByOrderByDescripcionAsc();
    }

    public Optional<Area> obtenerAreaPorId(Integer id) {
        return areaRepository.findById(id);
    }

    public Optional<Area> obtenerAreaPorDescripcion(String descripcion) {
        return areaRepository.findByDescripcion(descripcion);
    }

    public List<Area> buscarAreasPorDescripcion(String descripcion) {
        return areaRepository.findByDescripcionContainingIgnoreCase(descripcion);
    }

    public List<Area> obtenerAreasPorDepartamento(Integer idDepartamento) {
        return areaRepository.findByDepartamentoIdDepartamentoOrderByDescripcionAsc(idDepartamento);
    }

    public List<Area> obtenerAreasPorDepartamento(Departamento departamento) {
        return areaRepository.findByDepartamentoOrderByDescripcionAsc(departamento);
    }

    public Area actualizarArea(Integer id, Area areaActualizada) {
        return areaRepository.findById(id)
                .map(area -> {
                    if (areaActualizada.getDepartamento() != null && areaActualizada.getDepartamento().getIdDepartamento() != null) {
                        Optional<Departamento> departamento = departamentoRepository.findById(areaActualizada.getDepartamento().getIdDepartamento());
                        if (!departamento.isPresent()) {
                            throw new RuntimeException("El departamento especificado no existe");
                        }

                        if (!area.getDescripcion().equals(areaActualizada.getDescripcion()) ||
                                !area.getDepartamento().getIdDepartamento().equals(areaActualizada.getDepartamento().getIdDepartamento())) {

                            if (areaRepository.existsByDescripcionAndDepartamento(areaActualizada.getDescripcion(), departamento.get())) {
                                throw new RuntimeException("Ya existe un área con la descripción '" + areaActualizada.getDescripcion() + "' en este departamento");
                            }
                        }

                        area.setDepartamento(departamento.get());
                    }

                    area.setDescripcion(areaActualizada.getDescripcion());
                    return areaRepository.save(area);
                })
                .orElseThrow(() -> new RuntimeException("Área no encontrada con id: " + id));
    }

    public void eliminarArea(Integer id) {
        if (!areaRepository.existsById(id)) {
            throw new RuntimeException("Área no encontrada con id: " + id);
        }

        Optional<Area> area = areaRepository.findById(id);
        if (area.isPresent() && area.get().getCargos() != null && !area.get().getCargos().isEmpty()) {
            throw new RuntimeException("No se puede eliminar el área porque tiene cargos asociados");
        }

        areaRepository.deleteById(id);
    }

    public boolean existeArea(Integer id) {
        return areaRepository.existsById(id);
    }

    public long contarAreasPorDepartamento(Integer idDepartamento) {
        return areaRepository.countByDepartamentoId(idDepartamento);
    }
}
