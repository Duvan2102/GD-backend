package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.model.Estado;
import com.helisa.docmanager.repository.UsuarioRepository;
import com.helisa.docmanager.repository.EstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UsuarioActivationService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Transactional
    public Usuario activarUsuario(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        String estadoActual = usuario.getEstado().getDescripcion().toUpperCase();
        
        if (!estadoActual.equals("PENDIENTE") && !estadoActual.equals("INACTIVO")) {
            throw new IllegalStateException("El usuario debe estar en estado PENDIENTE o INACTIVO para ser activado. Estado actual: " + estadoActual);
        }

        boolean esPendiente = estadoActual.equals("PENDIENTE");
        boolean esActivo = estadoActual.equals("ACTIVO");

        
        Estado estadoActivo = estadoRepository.findByDescripcion("ACTIVO")
                .orElseThrow(() -> new RuntimeException("Estado ACTIVO no encontrado en la base de datos"));
        
        usuario.setEstado(estadoActivo);
        Usuario usuarioActivado = usuarioRepository.save(usuario);

            
        if (usuario.getCorreoEmpresarial() != null && !usuario.getCorreoEmpresarial().trim().isEmpty()) {
            try {
                if (esPendiente || esActivo) {
                    
                    String token = passwordResetService.generarTokenParaActivacion(usuario.getIdUsuario());
                    emailService.enviarCorreoActivacionConToken(usuario, token);
                } else {
                    
                    emailService.enviarCorreoReactivacion(usuario);
                }
            } catch (Exception e) {
            
                System.err.println("Error al enviar correo de activación: " + e.getMessage());
            }
        }

        return usuarioActivado;
    }
    
    @Transactional(readOnly = true)
    public List<Usuario> obtenerUsuariosPendientes() {
        Estado estadoPendiente = estadoRepository.findByDescripcion("PENDIENTE")
                .orElseThrow(() -> new RuntimeException("Estado PENDIENTE no encontrado en la base de datos"));
        
        return usuarioRepository.findByEstado(estadoPendiente);
    }

    
    @Transactional(readOnly = true)
    public boolean esUsuarioPendiente(Integer idUsuario) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
        if (usuarioOpt.isEmpty()) {
            return false;
        }
        
        Usuario usuario = usuarioOpt.get();
        return usuario.getEstado() != null && 
               usuario.getEstado().getDescripcion().equalsIgnoreCase("PENDIENTE");
    }

    
    @Transactional
    public Usuario rechazarUsuario(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        if (!usuario.getEstado().getDescripcion().equalsIgnoreCase("PENDIENTE")) {
            throw new IllegalStateException("El usuario no está en estado PENDIENTE");
        }

        Estado estadoInactivo = estadoRepository.findByDescripcion("INACTIVO")
                .orElseThrow(() -> new RuntimeException("Estado INACTIVO no encontrado en la base de datos"));
        
        usuario.setEstado(estadoInactivo);
        return usuarioRepository.save(usuario);
    }
}

