package com.helisa.docmanager.controller;

import com.helisa.docmanager.model.Cargo;
import com.helisa.docmanager.service.CargoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cargos")
@CrossOrigin(origins = "*")
public class CargoController {

    @Autowired
    private CargoService cargoService;

    @PostMapping
    public ResponseEntity<?> crearCargo(@Valid @RequestBody Cargo cargo) {
        try {
            Cargo nuevoCargo = cargoService.crearCargo(cargo);
            return new ResponseEntity<>(nuevoCargo, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<Cargo>> obtenerTodosCargos() {
        List<Cargo> cargos = cargoService.obtenerTodosCargos();
        return new ResponseEntity<>(cargos, HttpStatus.OK);
    }

    @GetMapping("/completos")
    public ResponseEntity<List<Cargo>> obtenerTodosCargosCompletos() {
        List<Cargo> cargos = cargoService.obtenerTodosCargosCompletos();
        return new ResponseEntity<>(cargos, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCargoPorId(@PathVariable Integer id) {
        Optional<Cargo> cargo = cargoService.obtenerCargoPorId(id);
        if (cargo.isPresent()) {
            return new ResponseEntity<>(cargo.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Cargo no encontrado", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}/completo")
    public ResponseEntity<?> obtenerCargoPorIdCompleto(@PathVariable Integer id) {
        Optional<Cargo> cargo = cargoService.obtenerCargoPorIdCompleto(id);
        if (cargo.isPresent()) {
            return new ResponseEntity<>(cargo.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Cargo no encontrado", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Cargo>> buscarCargosPorDescripcion(
            @RequestParam String descripcion) {
        List<Cargo> cargos = cargoService.buscarCargosPorDescripcion(descripcion);
        return new ResponseEntity<>(cargos, HttpStatus.OK);
    }

    @GetMapping("/descripcion/{descripcion}")
    public ResponseEntity<?> obtenerCargoPorDescripcion(@PathVariable String descripcion) {
        Optional<Cargo> cargo = cargoService.obtenerCargoPorDescripcion(descripcion);
        if (cargo.isPresent()) {
            return new ResponseEntity<>(cargo.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Cargo no encontrado", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/area/{idArea}")
    public ResponseEntity<List<Cargo>> obtenerCargosPorArea(@PathVariable Integer idArea) {
        List<Cargo> cargos = cargoService.obtenerCargosPorArea(idArea);
        return new ResponseEntity<>(cargos, HttpStatus.OK);
    }

    @GetMapping("/departamento/{idDepartamento}")
    public ResponseEntity<List<Cargo>> obtenerCargosPorDepartamento(@PathVariable Integer idDepartamento) {
        List<Cargo> cargos = cargoService.obtenerCargosPorDepartamento(idDepartamento);
        return new ResponseEntity<>(cargos, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCargo(
            @PathVariable Integer id,
            @Valid @RequestBody Cargo cargo) {
        try {
            Cargo cargoActualizado = cargoService.actualizarCargo(id, cargo);
            return new ResponseEntity<>(cargoActualizado, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCargo(@PathVariable Integer id) {
        try {
            cargoService.eliminarCargo(id);
            return new ResponseEntity<>("Cargo eliminado correctamente", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}/existe")
    public ResponseEntity<Boolean> existeCargo(@PathVariable Integer id) {
        boolean existe = cargoService.existeCargo(id);
        return new ResponseEntity<>(existe, HttpStatus.OK);
    }

    @GetMapping("/area/{idArea}/contar")
    public ResponseEntity<Long> contarCargosPorArea(@PathVariable Integer idArea) {
        long cantidad = cargoService.contarCargosPorArea(idArea);
        return new ResponseEntity<>(cantidad, HttpStatus.OK);
    }

    @GetMapping("/departamento/{idDepartamento}/contar")
    public ResponseEntity<Long> contarCargosPorDepartamento(@PathVariable Integer idDepartamento) {
        long cantidad = cargoService.contarCargosPorDepartamento(idDepartamento);
        return new ResponseEntity<>(cantidad, HttpStatus.OK);
    }
}
