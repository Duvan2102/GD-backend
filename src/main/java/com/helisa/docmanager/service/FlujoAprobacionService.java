package com.helisa.docmanager.service;

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

    /**
     * Verifica si un usuario puede aprobar una solicitud en este momento
     *
     * @param solicitudId ID de la solicitud
     * @param usuarioId ID del usuario
     * @param ordenFirma true si requiere orden secuencial
     * @return true si el usuario puede aprobar
     */
    @Transactional(readOnly = true)
    public boolean puedeAprobar(Integer solicitudId, Integer usuarioId, boolean ordenFirma) {
        // Verificar que el usuario sea destinatario y esté pendiente
        Optional<SolicitudDestinatario> destinatario =
                destinatarioRepository.findBySolicitudIdAndUsuarioId(solicitudId, usuarioId);

        if (destinatario.isEmpty() ||
                destinatario.get().getDecision() != SolicitudDestinatario.DecisionEnum.PENDIENTE) {
            log.debug("Usuario {} no es destinatario o ya decidió en solicitud {}",
                    usuarioId, solicitudId);
            return false;
        }

        // Si no requiere orden, cualquier pendiente puede aprobar
        if (!ordenFirma) {
            log.debug("Solicitud {} no requiere orden, usuario {} puede aprobar",
                    solicitudId, usuarioId);
            return true;
        }

        // Con orden secuencial, verificar que sea el siguiente
        List<SolicitudDestinatario> pendientes =
                destinatarioRepository.findPendientesBySolicitudId(solicitudId);

        boolean puedeAprobar = !pendientes.isEmpty() &&
                pendientes.get(0).getUsuarioId().equals(usuarioId);

        log.debug("Solicitud {} requiere orden. Usuario {} {} aprobar (es siguiente: {})",
                solicitudId, usuarioId, puedeAprobar ? "puede" : "no puede", puedeAprobar);

        return puedeAprobar;
    }

    /**
     * Verifica si todos los destinatarios han aprobado
     *
     * @param solicitudId ID de la solicitud
     * @return true si todos aprobaron
     */
    @Transactional(readOnly = true)
    public boolean todosAprobaron(Integer solicitudId) {
        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitudId);
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitudId);

        boolean todoAprobado = aprobados.equals(total);

        log.debug("Solicitud {} - Aprobados: {}/{}, Todos aprobaron: {}",
                solicitudId, aprobados, total, todoAprobado);

        return todoAprobado;
    }

    /**
     * Obtiene el siguiente aprobador en la cadena
     *
     * @param solicitudId ID de la solicitud
     * @return Siguiente destinatario pendiente o null si no hay
     */
    @Transactional(readOnly = true)
    public SolicitudDestinatario obtenerSiguienteAprobador(Integer solicitudId) {
        List<SolicitudDestinatario> pendientes =
                destinatarioRepository.findPendientesBySolicitudId(solicitudId);

        if (pendientes.isEmpty()) {
            log.debug("No hay aprobadores pendientes para solicitud {}", solicitudId);
            return null;
        }

        SolicitudDestinatario siguiente = pendientes.get(0);
        log.debug("Siguiente aprobador para solicitud {}: usuario {}",
                solicitudId, siguiente.getUsuarioId());

        return siguiente;
    }

    /**
     * Verifica si un usuario puede rechazar una solicitud
     *
     * @param solicitudId ID de la solicitud
     * @param usuarioId ID del usuario
     * @return true si puede rechazar
     */
    @Transactional(readOnly = true)
    public boolean puedeRechazar(Integer solicitudId, Integer usuarioId) {
        // Cualquier destinatario pendiente puede rechazar
        Optional<SolicitudDestinatario> destinatario =
                destinatarioRepository.findBySolicitudIdAndUsuarioId(solicitudId, usuarioId);

        boolean puedeRechazar = destinatario.isPresent() &&
                destinatario.get().getDecision() == SolicitudDestinatario.DecisionEnum.PENDIENTE;

        log.debug("Usuario {} {} rechazar solicitud {}",
                usuarioId, puedeRechazar ? "puede" : "no puede", solicitudId);

        return puedeRechazar;
    }

    /**
     * Verifica si un usuario puede cancelar una solicitud
     *
     * @param solicitudId ID de la solicitud
     * @param usuarioId ID del usuario
     * @return true si puede cancelar
     */
    @Transactional(readOnly = true)
    public boolean puedeCancelar(Integer solicitudId, Integer usuarioId) {
        return solicitudRepository.findById(solicitudId)
                .map(solicitud -> {
                    // El creador siempre puede cancelar
                    if (solicitud.getIdSolicitante().equals(usuarioId)) {
                        log.debug("Usuario {} es creador, puede cancelar solicitud {}",
                                usuarioId, solicitudId);
                        return true;
                    }

                    // Los destinatarios también pueden cancelar
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

    /**
     * Obtiene información del progreso de aprobación
     *
     * @param solicitudId ID de la solicitud
     * @return Objeto con información del progreso
     */
    @Transactional(readOnly = true)
    public ProgresoAprobacion obtenerProgreso(Integer solicitudId) {
        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitudId);
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitudId);
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

    /**
     * Clase para representar el progreso de aprobación
     */
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
