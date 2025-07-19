package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Area;
import com.helisa.docmanager.model.Cargo;
import com.helisa.docmanager.repository.AreaRepository;
import com.helisa.docmanager.repository.CargoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CargoService {

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private AreaRepository areaRepository;

    public Cargo crearCargo(Cargo cargo) {
        if (cargo.getArea() == null || cargo.getArea().getIdArea() == null) {
            throw new RuntimeException("El área es requerida para crear un cargo");
        }

        Optional<Area> area = areaRepository.findById(cargo.getArea().getIdArea());
        if (!area.isPresent()) {
            throw new RuntimeException("El área especificada no existe");
        }

        if (cargoRepository.existsByDescripcionAndArea(cargo.getDescripcion(), area.get())) {
            throw new RuntimeException("Ya existe un cargo con la descripción '" + cargo.getDescripcion() + "' en esta área");
        }

        cargo.setArea(area.get());
        return cargoRepository.save(cargo);
    }

    public List<Cargo> obtenerTodosCargos() {
        return cargoRepository.findAllByOrderByDescripcionAsc();
    }

    public List<Cargo> obtenerTodosCargosCompletos() {
        return cargoRepository.findAllWithAreaAndDepartamento();
    }

    public Optional<Cargo> obtenerCargoPorId(Integer id) {
        return cargoRepository.findById(id);
    }

    public Optional<Cargo> obtenerCargoPorIdCompleto(Integer id) {
        return cargoRepository.findByIdWithAreaAndDepartamento(id);
    }

    public Optional<Cargo> obtenerCargoPorDescripcion(String descripcion) {
        return cargoRepository.findByDescripcion(descripcion);
    }

    public List<Cargo> buscarCargosPorDescripcion(String descripcion) {
        return cargoRepository.findByDescripcionContainingIgnoreCase(descripcion);
    }

    public List<Cargo> obtenerCargosPorArea(Integer idArea) {
        return cargoRepository.findByAreaIdAreaOrderByDescripcionAsc(idArea);
    }

    public List<Cargo> obtenerCargosPorArea(Area area) {
        return cargoRepository.findByAreaOrderByDescripcionAsc(area);
    }

    public List<Cargo> obtenerCargosPorDepartamento(Integer idDepartamento) {
        return cargoRepository.findByDepartamentoIdOrderByDescripcionAsc(idDepartamento);
    }

    public Cargo actualizarCargo(Integer id, Cargo cargoActualizado) {
        return cargoRepository.findById(id)
                .map(cargo -> {
                    if (cargoActualizado.getArea() != null && cargoActualizado.getArea().getIdArea() != null) {
                        Optional<Area> area = areaRepository.findById(cargoActualizado.getArea().getIdArea());
                        if (!area.isPresent()) {
                            throw new RuntimeException("El área especificada no existe");
                        }

                        if (!cargo.getDescripcion().equals(cargoActualizado.getDescripcion()) ||
                                !cargo.getArea().getIdArea().equals(cargoActualizado.getArea().getIdArea())) {

                            if (cargoRepository.existsByDescripcionAndArea(cargoActualizado.getDescripcion(), area.get())) {
                                throw new RuntimeException("Ya existe un cargo con la descripción '" + cargoActualizado.getDescripcion() + "' en esta área");
                            }
                        }

                        cargo.setArea(area.get());
                    }

                    cargo.setDescripcion(cargoActualizado.getDescripcion());
                    return cargoRepository.save(cargo);
                })
                .orElseThrow(() -> new RuntimeException("Cargo no encontrado con id: " + id));
    }

    public void eliminarCargo(Integer id) {
        if (!cargoRepository.existsById(id)) {
            throw new RuntimeException("Cargo no encontrado con id: " + id);
        }

        Optional<Cargo> cargo = cargoRepository.findById(id);
        if (cargo.isPresent()) {
            if (cargo.get().getUsuarios() != null && !cargo.get().getUsuarios().isEmpty()) {
                throw new RuntimeException("No se puede eliminar el cargo porque tiene usuarios asociados");
            }
            if (cargo.get().getTipologias() != null && !cargo.get().getTipologias().isEmpty()) {
                throw new RuntimeException("No se puede eliminar el cargo porque tiene tipologías asociadas");
            }
        }

        cargoRepository.deleteById(id);
    }

    public boolean existeCargo(Integer id) {
        return cargoRepository.existsById(id);
    }

    public long contarCargosPorArea(Integer idArea) {
        return cargoRepository.countByAreaId(idArea);
    }

    public long contarCargosPorDepartamento(Integer idDepartamento) {
        return cargoRepository.countByDepartamentoId(idDepartamento);
    }
}
