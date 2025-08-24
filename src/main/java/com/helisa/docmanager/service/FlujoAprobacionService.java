package com.helisa.docmanager.service;

import com.helisa.docmanager.model.SolicitudDestinatario;
import com.helisa.docmanager.repository.SolicitudDestinatarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class FlujoAprobacionService {

    @Autowired
    private SolicitudDestinatarioRepository destinatarioRepository;

    public boolean puedeAprobar(Long solicitudId, Long usuarioId, boolean ordenFirma) {
        Optional<SolicitudDestinatario> destinatario =
                destinatarioRepository.findBySolicitudIdAndUsuarioId(solicitudId, usuarioId);

        if (destinatario.isEmpty() ||
                destinatario.get().getDecision() != SolicitudDestinatario.DecisionEnum.PENDIENTE) {
            return false;
        }

        if (!ordenFirma) {
            return true; // Sin orden, cualquier pendiente puede aprobar
        }

        // Con orden secuencial, verificar que sea el siguiente
        List<SolicitudDestinatario> pendientes =
                destinatarioRepository.findPendientesBySolicitudId(solicitudId);

        return !pendientes.isEmpty() &&
                pendientes.get(0).getUsuarioId().equals(usuarioId);
    }

    public boolean todosAprobaron(Long solicitudId) {
        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitudId);
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitudId);
        return aprobados.equals(total);
    }

    public SolicitudDestinatario obtenerSiguienteAprobador(Long solicitudId) {
        List<SolicitudDestinatario> pendientes =
                destinatarioRepository.findPendientesBySolicitudId(solicitudId);
        return pendientes.isEmpty() ? null : pendientes.get(0);
    }
}
