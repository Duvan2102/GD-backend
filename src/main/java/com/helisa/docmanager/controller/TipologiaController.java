package com.helisa.docmanager.controller;

import com.helisa.docmanager.model.Tipologia;
import com.helisa.docmanager.service.TipologiaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tipologias")
@CrossOrigin(origins = "*")
public class TipologiaController {

    @Autowired
    private TipologiaService tipologiaService;

    @PostMapping
    public ResponseEntity<?> crearTipologia(@Valid @RequestBody Tipologia tipologia) {
        try {
            Tipologia nuevaTipologia = tipologiaService.crearTipologia(tipologia);
            return new ResponseEntity<>(nuevaTipologia, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<Tipologia>> obtenerTodasTipologias() {
        List<Tipologia> tipologias = tipologiaService.obtenerTodasTipologias();
        return new ResponseEntity<>(tipologias, HttpStatus.OK);
    }

    @GetMapping("/completas")
    public ResponseEntity<List<Tipologia>> obtenerTodasTipologiasCompletas() {
        List<Tipologia> tipologias = tipologiaService.obtenerTodasTipologiasCompletas();
        return new ResponseEntity<>(tipologias, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerTipologiaPorId(@PathVariable Integer id) {
        Optional<Tipologia> tipologia = tipologiaService.obtenerTipologiaPorId(id);
        if (tipologia.isPresent()) {
            return new ResponseEntity<>(tipologia.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Tipología no encontrada", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}/completa")
    public ResponseEntity<?> obtenerTipologiaPorIdCompleta(@PathVariable Integer id) {
        Optional<Tipologia> tipologia = tipologiaService.obtenerTipologiaPorIdCompleta(id);
        if (tipologia.isPresent()) {
            return new ResponseEntity<>(tipologia.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Tipología no encontrada", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Tipologia>> buscarTipologiasPorDescripcion(
            @RequestParam String descripcion) {
        List<Tipologia> tipologias = tipologiaService.buscarTipologiasPorDescripcion(descripcion);
        return new ResponseEntity<>(tipologias, HttpStatus.OK);
    }

    @GetMapping("/descripcion/{descripcion}")
    public ResponseEntity<?> obtenerTipologiaPorDescripcion(@PathVariable String descripcion) {
        Optional<Tipologia> tipologia = tipologiaService.obtenerTipologiaPorDescripcion(descripcion);
        if (tipologia.isPresent()) {
            return new ResponseEntity<>(tipologia.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Tipología no encontrada", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/cargo/{idCargo}")
    public ResponseEntity<List<Tipologia>> obtenerTipologiasPorCargo(@PathVariable Integer idCargo) {
        List<Tipologia> tipologias = tipologiaService.obtenerTipologiasPorCargo(idCargo);
        return new ResponseEntity<>(tipologias, HttpStatus.OK);
    }

    @GetMapping("/cargo/{idCargo}/completas")
    public ResponseEntity<List<Tipologia>> obtenerTipologiasPorCargoCompletas(@PathVariable Integer idCargo) {
        List<Tipologia> tipologias = tipologiaService.obtenerTipologiasPorCargoCompletas(idCargo);
        return new ResponseEntity<>(tipologias, HttpStatus.OK);
    }

    @GetMapping("/area/{idArea}")
    public ResponseEntity<List<Tipologia>> obtenerTipologiasPorArea(@PathVariable Integer idArea) {
        List<Tipologia> tipologias = tipologiaService.obtenerTipologiasPorArea(idArea);
        return new ResponseEntity<>(tipologias, HttpStatus.OK);
    }

    @GetMapping("/departamento/{idDepartamento}")
    public ResponseEntity<List<Tipologia>> obtenerTipologiasPorDepartamento(@PathVariable Integer idDepartamento) {
        List<Tipologia> tipologias = tipologiaService.obtenerTipologiasPorDepartamento(idDepartamento);
        return new ResponseEntity<>(tipologias, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarTipologia(
            @PathVariable Integer id,
            @Valid @RequestBody Tipologia tipologia) {
        try {
            Tipologia tipologiaActualizada = tipologiaService.actualizarTipologia(id, tipologia);
            return new ResponseEntity<>(tipologiaActualizada, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarTipologia(@PathVariable Integer id) {
        try {
            tipologiaService.eliminarTipologia(id);
            return new ResponseEntity<>("Tipología eliminada correctamente", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}/existe")
    public ResponseEntity<Boolean> existeTipologia(@PathVariable Integer id) {
        boolean existe = tipologiaService.existeTipologia(id);
        return new ResponseEntity<>(existe, HttpStatus.OK);
    }

    @GetMapping("/cargo/{idCargo}/contar")
    public ResponseEntity<Long> contarTipologiasPorCargo(@PathVariable Integer idCargo) {
        long cantidad = tipologiaService.contarTipologiasPorCargo(idCargo);
        return new ResponseEntity<>(cantidad, HttpStatus.OK);
    }

    @GetMapping("/area/{idArea}/contar")
    public ResponseEntity<Long> contarTipologiasPorArea(@PathVariable Integer idArea) {
        long cantidad = tipologiaService.contarTipologiasPorArea(idArea);
        return new ResponseEntity<>(cantidad, HttpStatus.OK);
    }

    @GetMapping("/departamento/{idDepartamento}/contar")
    public ResponseEntity<Long> contarTipologiasPorDepartamento(@PathVariable Integer idDepartamento) {
        long cantidad = tipologiaService.contarTipologiasPorDepartamento(idDepartamento);
        return new ResponseEntity<>(cantidad, HttpStatus.OK);
    }
}
