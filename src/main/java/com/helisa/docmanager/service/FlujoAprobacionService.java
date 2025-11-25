package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Solicitud;
import com.helisa.docmanager.model.SolicitudDestinatario;
import com.helisa.docmanager.repository.SolicitudDestinatarioRepository;
import com.helisa.docmanager.repository.SolicitudRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class FlujoAprobacionService {

    @Autowired
    private SolicitudDestinatarioRepository destinatarioRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    
    @Transactional(readOnly = true)
    public boolean puedeAprobar(Integer solicitudId, Integer usuarioId, boolean ordenFirma) {
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return false;
        }
        
        Solicitud solicitud = solicitudOpt.get();
        boolean esPrimeraRonda = solicitud.estaPendiente();
        boolean esSegundaRonda = solicitud.estaAprobadoProceso();
        
        if (!esPrimeraRonda && !esSegundaRonda) {
            log.debug("Solicitud {} no está en estado válido para aprobar", solicitudId);
            return false;
        }
        
        Boolean esProcesador = esSegundaRonda;
        
        Optional<SolicitudDestinatario> destinatario =
                destinatarioRepository.findBySolicitudIdAndUsuarioId(solicitudId, usuarioId);

        if (destinatario.isEmpty() ||
                destinatario.get().getDecision() != SolicitudDestinatario.DecisionEnum.PENDIENTE) {
            log.debug("Usuario {} no es destinatario o ya decidió en solicitud {}",
                    usuarioId, solicitudId);
            return false;
        }
        
        if (!destinatario.get().getEsProcesador().equals(esProcesador)) {
            log.debug("Usuario {} no es del tipo correcto para esta ronda (esProcesador: {}, ronda: {})",
                    usuarioId, destinatario.get().getEsProcesador(), esSegundaRonda ? "segunda" : "primera");
            return false;
        }

        boolean requiereOrden = esSegundaRonda || ordenFirma;
        
        if (!requiereOrden) {
            log.debug("Solicitud {} no requiere orden, usuario {} puede aprobar",
                    solicitudId, usuarioId);
            return true;
        }

        List<SolicitudDestinatario> pendientes =
                destinatarioRepository.findPendientesBySolicitudId(solicitudId, esProcesador);

        boolean puedeAprobar = !pendientes.isEmpty() &&
                pendientes.get(0).getUsuarioId().equals(usuarioId);

        log.debug("Solicitud {} requiere orden. Usuario {} {} aprobar (es siguiente: {})",
                solicitudId, usuarioId, puedeAprobar ? "puede" : "no puede", puedeAprobar);

        return puedeAprobar;
    }

    @Transactional(readOnly = true)
    public boolean todosAprobaron(Integer solicitudId) {
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return false;
        }
        
        Solicitud solicitud = solicitudOpt.get();
        boolean esPrimeraRonda = solicitud.estaPendiente();
        boolean esSegundaRonda = solicitud.estaAprobadoProceso();
        
        if (!esPrimeraRonda && !esSegundaRonda) {
            return false;
        }
        
        Boolean esProcesador = esSegundaRonda;
        
        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitudId, esProcesador);
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitudId, esProcesador);

        boolean todoAprobado = aprobados.equals(total);

        log.debug("Solicitud {} - Aprobados: {}/{}, Todos aprobaron: {} (ronda: {})",
                solicitudId, aprobados, total, todoAprobado, esSegundaRonda ? "segunda" : "primera");

        return todoAprobado;
    }

    @Transactional(readOnly = true)
    public SolicitudDestinatario obtenerSiguienteAprobador(Integer solicitudId) {
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return null;
        }
        
        Solicitud solicitud = solicitudOpt.get();
        boolean esPrimeraRonda = solicitud.estaPendiente();
        boolean esSegundaRonda = solicitud.estaAprobadoProceso();
        
        if (!esPrimeraRonda && !esSegundaRonda) {
            return null;
        }
        
        Boolean esProcesador = esSegundaRonda;
        
        List<SolicitudDestinatario> pendientes =
                destinatarioRepository.findPendientesBySolicitudId(solicitudId, esProcesador);

        if (pendientes.isEmpty()) {
            log.debug("No hay {} pendientes para solicitud {}",
                    esSegundaRonda ? "procesadores" : "aprobadores", solicitudId);
            return null;
        }

        SolicitudDestinatario siguiente = pendientes.get(0);
        log.debug("Siguiente {} para solicitud {}: usuario {}",
                esSegundaRonda ? "procesador" : "aprobador", solicitudId, siguiente.getUsuarioId());

        return siguiente;
    }

    @Transactional(readOnly = true)
    public boolean puedeRechazar(Integer solicitudId, Integer usuarioId) {
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return false;
        }
        
        Solicitud solicitud = solicitudOpt.get();
        if (!solicitud.estaPendiente()) {
            log.debug("Solicitud {} no está en estado PENDIENTE, no se puede rechazar", solicitudId);
            return false;
        }
        
        Optional<SolicitudDestinatario> destinatario =
                destinatarioRepository.findBySolicitudIdAndUsuarioId(solicitudId, usuarioId);

        boolean puedeRechazar = destinatario.isPresent() &&
                destinatario.get().getDecision() == SolicitudDestinatario.DecisionEnum.PENDIENTE &&
                !destinatario.get().getEsProcesador();

        log.debug("Usuario {} {} rechazar solicitud {}",
                usuarioId, puedeRechazar ? "puede" : "no puede", solicitudId);

        return puedeRechazar;
    }

    @Transactional(readOnly = true)
    public boolean puedeCancelar(Integer solicitudId, Integer usuarioId) {
        return solicitudRepository.findById(solicitudId)
                .map(solicitud -> {
                    if (solicitud.getIdSolicitante().equals(usuarioId)) {
                        log.debug("Usuario {} es creador, puede cancelar solicitud {}",
                                usuarioId, solicitudId);
                        return true;
                    }

                    boolean esDestinatario = destinatarioRepository
                            .findBySolicitudIdAndUsuarioId(solicitudId, usuarioId)
                            .isPresent();

                    log.debug("Usuario {} {} destinatario, {} cancelar solicitud {}",
                            usuarioId, esDestinatario ? "es" : "no es",
                            esDestinatario ? "puede" : "no puede", solicitudId);

                    return esDestinatario;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public ProgresoAprobacion obtenerProgreso(Integer solicitudId) {
        Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return null;
        }
        
        Solicitud solicitud = solicitudOpt.get();
        boolean esPrimeraRonda = solicitud.estaPendiente();
        boolean esSegundaRonda = solicitud.estaAprobadoProceso();
        
        if (!esPrimeraRonda && !esSegundaRonda) {
            return null;
        }
        
        Boolean esProcesador = esSegundaRonda;
        
        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitudId, esProcesador);
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitudId, esProcesador);
        SolicitudDestinatario siguiente = obtenerSiguienteAprobador(solicitudId);

        return ProgresoAprobacion.builder()
                .solicitudId(solicitudId)
                .totalDestinatarios(total.intValue())
                .aprobados(aprobados.intValue())
                .pendientes((int) (total - aprobados))
                .porcentajeCompletado(total > 0 ? (aprobados * 100.0 / total) : 0)
                .siguienteAprobadorId(siguiente != null ? siguiente.getUsuarioId() : null)
                .completado(aprobados.equals(total))
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class ProgresoAprobacion {
        private Integer solicitudId;
        private Integer totalDestinatarios;
        private Integer aprobados;
        private Integer pendientes;
        private Double porcentajeCompletado;
        private Integer siguienteAprobadorId;
        private boolean completado;
    }
}
