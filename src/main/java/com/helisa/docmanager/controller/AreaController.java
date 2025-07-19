package com.helisa.docmanager.controller;

import com.helisa.docmanager.model.Area;
import com.helisa.docmanager.service.AreaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/areas")
@CrossOrigin(origins = "*")
public class AreaController {

    @Autowired
    private AreaService areaService;

    // Crear una nueva área
    @PostMapping
    public ResponseEntity<?> crearArea(@Valid @RequestBody Area area) {
        try {
            Area nuevaArea = areaService.crearArea(area);
            return new ResponseEntity<>(nuevaArea, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Obtener todas las áreas
    @GetMapping
    public ResponseEntity<List<Area>> obtenerTodasAreas() {
        List<Area> areas = areaService.obtenerTodasAreas();
        return new ResponseEntity<>(areas, HttpStatus.OK);
    }

    // Obtener área por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerAreaPorId(@PathVariable Integer id) {
        Optional<Area> area = areaService.obtenerAreaPorId(id);
        if (area.isPresent()) {
            return new ResponseEntity<>(area.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Área no encontrada", HttpStatus.NOT_FOUND);
        }
    }

    // Buscar áreas por descripción
    @GetMapping("/buscar")
    public ResponseEntity<List<Area>> buscarAreasPorDescripcion(
            @RequestParam String descripcion) {
        List<Area> areas = areaService.buscarAreasPorDescripcion(descripcion);
        return new ResponseEntity<>(areas, HttpStatus.OK);
    }

    // Obtener área por descripción exacta
    @GetMapping("/descripcion/{descripcion}")
    public ResponseEntity<?> obtenerAreaPorDescripcion(@PathVariable String descripcion) {
        Optional<Area> area = areaService.obtenerAreaPorDescripcion(descripcion);
        if (area.isPresent()) {
            return new ResponseEntity<>(area.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Área no encontrada", HttpStatus.NOT_FOUND);
        }
    }

    // Obtener áreas por departamento
    @GetMapping("/departamento/{idDepartamento}")
    public ResponseEntity<List<Area>> obtenerAreasPorDepartamento(@PathVariable Integer idDepartamento) {
        List<Area> areas = areaService.obtenerAreasPorDepartamento(idDepartamento);
        return new ResponseEntity<>(areas, HttpStatus.OK);
    }

    // Actualizar área
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarArea(
            @PathVariable Integer id,
            @Valid @RequestBody Area area) {
        try {
            Area areaActualizada = areaService.actualizarArea(id, area);
            return new ResponseEntity<>(areaActualizada, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Eliminar área
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarArea(@PathVariable Integer id) {
        try {
            areaService.eliminarArea(id);
            return new ResponseEntity<>("Área eliminada correctamente", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Verificar si existe un área
    @GetMapping("/{id}/existe")
    public ResponseEntity<Boolean> existeArea(@PathVariable Integer id) {
        boolean existe = areaService.existeArea(id);
        return new ResponseEntity<>(existe, HttpStatus.OK);
    }

    // Contar áreas por departamento
    @GetMapping("/departamento/{idDepartamento}/contar")
    public ResponseEntity<Long> contarAreasPorDepartamento(@PathVariable Integer idDepartamento) {
        long cantidad = areaService.contarAreasPorDepartamento(idDepartamento);
        return new ResponseEntity<>(cantidad, HttpStatus.OK);
    }
}
