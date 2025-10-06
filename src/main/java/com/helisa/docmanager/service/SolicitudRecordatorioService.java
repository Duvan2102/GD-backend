package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Solicitud;
import com.helisa.docmanager.model.SolicitudDestinatario;
import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.repository.SolicitudDestinatarioRepository;
import com.helisa.docmanager.repository.SolicitudRepository;
import com.helisa.docmanager.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SolicitudRecordatorioService {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private SolicitudDestinatarioRepository destinatarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    // Ejecutar todos los días a las 12:00 del medio día hora de Bogotá
    @Scheduled(cron = "0 0 12 * * *", zone = "America/Bogota")
    public void procesarRecordatoriosDiarios() {
        // Verificación adicional de franja horaria (7:00 a 19:00)
        LocalTime ahoraBogota = LocalTime.now(ZONA_BOGOTA);
        if (ahoraBogota.isBefore(LocalTime.of(7, 0)) || ahoraBogota.isAfter(LocalTime.of(19, 0))) {
            return; // fuera de franja horaria
        }

        List<Solicitud> pendientes = solicitudRepository
                .findByEstado_IdEstadoAndEnviarRecordatorioGreaterThan(Solicitud.ESTADO_PENDIENTE_ID, 0);

        LocalDate hoyBogota = LocalDate.now(ZONA_BOGOTA);

        for (Solicitud solicitud : pendientes) {
            Integer periodoDias = solicitud.getEnviarRecordatorio();
            if (periodoDias == null || periodoDias <= 0) continue;

            LocalDate fechaBase = solicitud.getFechaRegistro() != null
                    ? solicitud.getFechaRegistro().atZone(ZONA_BOGOTA).toLocalDate()
                    : (solicitud.getCreatedAt() != null
                        ? solicitud.getCreatedAt().atZone(ZONA_BOGOTA).toLocalDate()
                        : null);

            if (fechaBase == null) continue;

            long diasTranscurridos = java.time.temporal.ChronoUnit.DAYS.between(fechaBase, hoyBogota);
            if (diasTranscurridos <= 0) {
                continue; // aún no cumple ni un día
            }

            if (diasTranscurridos % periodoDias != 0) {
                continue; // hoy no es un múltiplo exacto del periodo
            }

            if (!solicitud.estaPendiente() || solicitud.estaCancelado()) {
                continue; // sólo enviar si sigue pendiente y no cancelada
            }

            // Determinar destinatarios pendientes
            List<SolicitudDestinatario> pendientesDest = destinatarioRepository
                    .findPendientesBySolicitudId(solicitud.getId());

            if (pendientesDest.isEmpty()) {
                continue; // nada que notificar
            }

            if (solicitud.esOrdenSecuencial()) {
                // Enviar solo al menor ordenIndex pendiente
                int menorOrden = pendientesDest.get(0).getOrdenIndex();
                List<SolicitudDestinatario> objetivo = pendientesDest.stream()
                        .filter(d -> d.getOrdenIndex() == menorOrden)
                        .collect(Collectors.toList());
                enviarCorreosRecordatorio(solicitud, objetivo);
            } else {
                // Enviar a todos los pendientes
                enviarCorreosRecordatorio(solicitud, pendientesDest);
            }
        }
    }

    private void enviarCorreosRecordatorio(Solicitud solicitud, List<SolicitudDestinatario> destinatarios) {
        if (destinatarios == null || destinatarios.isEmpty()) return;

        List<Integer> idsUsuarios = destinatarios.stream()
                .map(SolicitudDestinatario::getUsuarioId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, Usuario> idToUsuario = usuarioRepository.findByIdUsuarioIn(idsUsuarios)
                .stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, u -> u));

        for (Integer idUsuario : idsUsuarios) {
            Usuario usuario = idToUsuario.get(idUsuario);
            if (usuario == null) continue;
            String email = usuario.getCorreoEmpresarial();
            if (email == null || email.trim().isEmpty()) continue;
            emailService.enviarRecordatorioSolicitud(email, solicitud.getId());
        }
    }
}


