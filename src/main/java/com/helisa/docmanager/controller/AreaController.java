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

    @PostMapping
    public ResponseEntity<?> crearArea(@Valid @RequestBody Area area) {
        try {
            Area nuevaArea = areaService.crearArea(area);
            return new ResponseEntity<>(nuevaArea, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<Area>> obtenerTodasAreas() {
        List<Area> areas = areaService.obtenerTodasAreas();
        return new ResponseEntity<>(areas, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerAreaPorId(@PathVariable Integer id) {
        Optional<Area> area = areaService.obtenerAreaPorId(id);
        if (area.isPresent()) {
            return new ResponseEntity<>(area.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Área no encontrada", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Area>> buscarAreasPorDescripcion(
            @RequestParam String descripcion) {
        List<Area> areas = areaService.buscarAreasPorDescripcion(descripcion);
        return new ResponseEntity<>(areas, HttpStatus.OK);
    }

    @GetMapping("/descripcion/{descripcion}")
    public ResponseEntity<?> obtenerAreaPorDescripcion(@PathVariable String descripcion) {
        Optional<Area> area = areaService.obtenerAreaPorDescripcion(descripcion);
        if (area.isPresent()) {
            return new ResponseEntity<>(area.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Área no encontrada", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/departamento/{idDepartamento}")
    public ResponseEntity<List<Area>> obtenerAreasPorDepartamento(@PathVariable Integer idDepartamento) {
        List<Area> areas = areaService.obtenerAreasPorDepartamento(idDepartamento);
        return new ResponseEntity<>(areas, HttpStatus.OK);
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarArea(@PathVariable Integer id) {
        try {
            areaService.eliminarArea(id);
            return new ResponseEntity<>("Área eliminada correctamente", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}/existe")
    public ResponseEntity<Boolean> existeArea(@PathVariable Integer id) {
        boolean existe = areaService.existeArea(id);
        return new ResponseEntity<>(existe, HttpStatus.OK);
    }

    @GetMapping("/departamento/{idDepartamento}/contar")
    public ResponseEntity<Long> contarAreasPorDepartamento(@PathVariable Integer idDepartamento) {
        long cantidad = areaService.contarAreasPorDepartamento(idDepartamento);
        return new ResponseEntity<>(cantidad, HttpStatus.OK);
    }
}
