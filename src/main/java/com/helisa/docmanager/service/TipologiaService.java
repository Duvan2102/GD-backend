package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Cargo;
import com.helisa.docmanager.model.Tipologia;
import com.helisa.docmanager.repository.CargoRepository;
import com.helisa.docmanager.repository.TipologiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TipologiaService {

    @Autowired
    private TipologiaRepository tipologiaRepository;

    @Autowired
    private CargoRepository cargoRepository;

    public Tipologia crearTipologia(Tipologia tipologia) {
        if (tipologia.getCargo() == null || tipologia.getCargo().getIdCargo() == null) {
            throw new RuntimeException("El cargo es requerido para crear una tipología");
        }

        Optional<Cargo> cargo = cargoRepository.findById(tipologia.getCargo().getIdCargo());
        if (!cargo.isPresent()) {
            throw new RuntimeException("El cargo especificado no existe");
        }

        if (tipologiaRepository.existsByDescripcionAndCargo(tipologia.getDescripcion(), cargo.get())) {
            throw new RuntimeException("Ya existe una tipología con la descripción '" + tipologia.getDescripcion() + "' en este cargo");
        }

        tipologia.setCargo(cargo.get());
        return tipologiaRepository.save(tipologia);
    }

    public List<Tipologia> obtenerTodasTipologias() {
        return tipologiaRepository.findAllByOrderByDescripcionAsc();
    }

    public List<Tipologia> obtenerTodasTipologiasCompletas() {
        return tipologiaRepository.findAllWithCargoAreaAndDepartamento();
    }

    public Optional<Tipologia> obtenerTipologiaPorId(Integer id) {
        return tipologiaRepository.findById(id);
    }

    public Optional<Tipologia> obtenerTipologiaPorIdCompleta(Integer id) {
        return tipologiaRepository.findByIdWithCargoAreaAndDepartamento(id);
    }

    public Optional<Tipologia> obtenerTipologiaPorDescripcion(String descripcion) {
        return tipologiaRepository.findByDescripcion(descripcion);
    }

    public List<Tipologia> buscarTipologiasPorDescripcion(String descripcion) {
        return tipologiaRepository.findByDescripcionContainingIgnoreCase(descripcion);
    }

    public List<Tipologia> obtenerTipologiasPorCargo(Integer idCargo) {
        return tipologiaRepository.findByCargoIdCargoOrderByDescripcionAsc(idCargo);
    }

    public List<Tipologia> obtenerTipologiasPorCargoCompletas(Integer idCargo) {
        return tipologiaRepository.findByCargoIdWithCompleteInfo(idCargo);
    }

    public List<Tipologia> obtenerTipologiasPorCargo(Cargo cargo) {
        return tipologiaRepository.findByCargoOrderByDescripcionAsc(cargo);
    }

    public List<Tipologia> obtenerTipologiasPorArea(Integer idArea) {
        return tipologiaRepository.findByAreaIdOrderByDescripcionAsc(idArea);
    }

    public List<Tipologia> obtenerTipologiasPorDepartamento(Integer idDepartamento) {
        return tipologiaRepository.findByDepartamentoIdOrderByDescripcionAsc(idDepartamento);
    }

    public Tipologia actualizarTipologia(Integer id, Tipologia tipologiaActualizada) {
        return tipologiaRepository.findById(id)
                .map(tipologia -> {
                    if (tipologiaActualizada.getCargo() != null && tipologiaActualizada.getCargo().getIdCargo() != null) {
                        Optional<Cargo> cargo = cargoRepository.findById(tipologiaActualizada.getCargo().getIdCargo());
                        if (!cargo.isPresent()) {
                            throw new RuntimeException("El cargo especificado no existe");
                        }

                        if (!tipologia.getDescripcion().equals(tipologiaActualizada.getDescripcion()) ||
                                !tipologia.getCargo().getIdCargo().equals(tipologiaActualizada.getCargo().getIdCargo())) {

                            if (tipologiaRepository.existsByDescripcionAndCargo(tipologiaActualizada.getDescripcion(), cargo.get())) {
                                throw new RuntimeException("Ya existe una tipología con la descripción '" + tipologiaActualizada.getDescripcion() + "' en este cargo");
                            }
                        }

                        tipologia.setCargo(cargo.get());
                    }

                    tipologia.setDescripcion(tipologiaActualizada.getDescripcion());
                    return tipologiaRepository.save(tipologia);
                })
                .orElseThrow(() -> new RuntimeException("Tipología no encontrada con id: " + id));
    }

    public void eliminarTipologia(Integer id) {
        if (!tipologiaRepository.existsById(id)) {
            throw new RuntimeException("Tipología no encontrada con id: " + id);
        }

        Optional<Tipologia> tipologia = tipologiaRepository.findById(id);
        if (tipologia.isPresent() && tipologia.get().getSolicitudes() != null && !tipologia.get().getSolicitudes().isEmpty()) {
            throw new RuntimeException("No se puede eliminar la tipología porque tiene solicitudes asociadas");
        }

        tipologiaRepository.deleteById(id);
    }

    public boolean existeTipologia(Integer id) {
        return tipologiaRepository.existsById(id);
    }

    public long contarTipologiasPorCargo(Integer idCargo) {
        return tipologiaRepository.countByCargoId(idCargo);
    }

    public long contarTipologiasPorArea(Integer idArea) {
        return tipologiaRepository.countByAreaId(idArea);
    }

    public long contarTipologiasPorDepartamento(Integer idDepartamento) {
        return tipologiaRepository.countByDepartamentoId(idDepartamento);
    }
}
