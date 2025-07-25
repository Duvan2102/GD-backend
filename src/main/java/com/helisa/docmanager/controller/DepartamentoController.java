package com.helisa.docmanager.controller;

import com.helisa.docmanager.model.Departamento;
import com.helisa.docmanager.service.DepartamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/departamentos")
@CrossOrigin(origins = "*")
public class DepartamentoController {

    @Autowired
    private DepartamentoService departamentoService;

    @PostMapping
    public ResponseEntity<?> crearDepartamento(@Valid @RequestBody Departamento departamento) {
        try {
            Departamento nuevoDepartamento = departamentoService.crearDepartamento(departamento);
            return new ResponseEntity<>(nuevoDepartamento, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<Departamento>> obtenerTodosDepartamentos() {
        List<Departamento> departamentos = departamentoService.obtenerTodosDepartamentos();
        return new ResponseEntity<>(departamentos, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerDepartamentoPorId(@PathVariable Integer id) {
        Optional<Departamento> departamento = departamentoService.obtenerDepartamentoPorId(id);
        if (departamento.isPresent()) {
            return new ResponseEntity<>(departamento.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Departamento no encontrado", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Departamento>> buscarDepartamentosPorDescripcion(
            @RequestParam String descripcion) {
        List<Departamento> departamentos = departamentoService.buscarDepartamentosPorDescripcion(descripcion);
        return new ResponseEntity<>(departamentos, HttpStatus.OK);
    }

    @GetMapping("/descripcion/{descripcion}")
    public ResponseEntity<?> obtenerDepartamentoPorDescripcion(@PathVariable String descripcion) {
        Optional<Departamento> departamento = departamentoService.obtenerDepartamentoPorDescripcion(descripcion);
        if (departamento.isPresent()) {
            return new ResponseEntity<>(departamento.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Departamento no encontrado", HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<?> actualizarDepartamento(
            @PathVariable Integer id,
            @Valid @RequestBody Departamento departamento) {
        try {
            Departamento departamentoActualizado = departamentoService.actualizarDepartamento(id, departamento);
            return new ResponseEntity<>(departamentoActualizado, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarDepartamento(@PathVariable Integer id) {
        try {
            departamentoService.eliminarDepartamento(id);
            return new ResponseEntity<>("Departamento eliminado correctamente", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}/existe")
    public ResponseEntity<Boolean> existeDepartamento(@PathVariable Integer id) {
        boolean existe = departamentoService.existeDepartamento(id);
        return new ResponseEntity<>(existe, HttpStatus.OK);
    }
}
