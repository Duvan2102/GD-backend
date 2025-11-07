package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Cargo;
import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.model.Estado;
import com.helisa.docmanager.repository.CargoRepository;
import com.helisa.docmanager.repository.UsuarioRepository;
import com.helisa.docmanager.repository.EstadoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.security.SecureRandom;

@Service
@Transactional
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public Usuario crearUsuario(Usuario usuario) {
        if (usuarioRepository.existsByUsuario(usuario.getUsuario())) {
            throw new RuntimeException("El usuario ya existe: " + usuario.getUsuario());
        }

        if (usuarioRepository.existsByIdentificacion(usuario.getIdentificacion())) {
            throw new RuntimeException("Ya existe un usuario con esta identificación: " + usuario.getIdentificacion());
        }

        if (usuario.getCorreoEmpresarial() != null && !usuario.getCorreoEmpresarial().trim().isEmpty()) {
            if (usuarioRepository.existsByCorreoEmpresarial(usuario.getCorreoEmpresarial())) {
                throw new RuntimeException("Ya existe un usuario con este correo corporativo: " + usuario.getCorreoEmpresarial());
            }
        }

        if (usuario.getCargo() != null && usuario.getCargo().getIdCargo() != null) {
            Optional<Cargo> cargoOpt = cargoRepository.findById(usuario.getCargo().getIdCargo());
            if (cargoOpt.isPresent()) {
                usuario.setCargo(cargoOpt.get());
            } else {
                throw new RuntimeException("El cargo especificado no existe: " + usuario.getCargo().getIdCargo());
            }
        } else {
            throw new RuntimeException("Debe especificar un cargo válido");
        }

        // Validar que se proporcionen vistas disponibles
        if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
            throw new RuntimeException("Debe especificar al menos una vista disponible para el usuario");
        }

        if (usuario.getPassword() == null || usuario.getPassword().trim().isEmpty()) {
            usuario.setPassword(generarContrasenaAleatoria(12));
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        // Establecer método 2FA predeterminado: Google Auth
        if (usuario.getTokenQr() == null) {
            usuario.setTokenQr(true);
        }
        if (usuario.getTokenCorreo() == null) {
            usuario.setTokenCorreo(false);
        }

        return usuarioRepository.save(usuario);
    }

    private String generarContrasenaAleatoria(int longitud) {
        final String mayus = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String minus = "abcdefghijklmnopqrstuvwxyz";
        final String digitos = "0123456789";
        final String especiales = "!@#$%^&*()-_=+[]{}";
        final String todos = mayus + minus + digitos + especiales;

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(longitud);

        sb.append(mayus.charAt(random.nextInt(mayus.length())));
        sb.append(minus.charAt(random.nextInt(minus.length())));
        sb.append(digitos.charAt(random.nextInt(digitos.length())));
        sb.append(especiales.charAt(random.nextInt(especiales.length())));

        for (int i = sb.length(); i < longitud; i++) {
            sb.append(todos.charAt(random.nextInt(todos.length())));
        }

        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    @Transactional(readOnly = true)
    public List<Usuario> obtenerTodosUsuarios() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Usuario> obtenerTodosUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerUsuarioPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerUsuarioPorNombreUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerUsuarioPorIdentificacion(String identificacion) {
        return usuarioRepository.findByIdentificacion(identificacion);
    }

    @Transactional
    public Usuario actualizarUsuario(Integer id, Usuario usuarioActualizado) {
        Optional<Usuario> usuarioExistente = usuarioRepository.findById(id);

        if (usuarioExistente.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado con ID: " + id);
        }

        Usuario usuario = usuarioExistente.get();

        if (!usuario.getUsuario().equals(usuarioActualizado.getUsuario()) &&
                usuarioRepository.existsByUsuario(usuarioActualizado.getUsuario())) {
            throw new RuntimeException("El usuario ya existe: " + usuarioActualizado.getUsuario());
        }

        if (!usuario.getIdentificacion().equals(usuarioActualizado.getIdentificacion()) &&
                usuarioRepository.existsByIdentificacion(usuarioActualizado.getIdentificacion())) {
            throw new RuntimeException("Ya existe un usuario con esta identificación: " + usuarioActualizado.getIdentificacion());
        }

        if (usuarioActualizado.getCorreoEmpresarial() != null && !usuarioActualizado.getCorreoEmpresarial().trim().isEmpty()) {
            boolean correoActualDiferente = usuario.getCorreoEmpresarial() == null || 
                    !usuario.getCorreoEmpresarial().equals(usuarioActualizado.getCorreoEmpresarial());
            
            if (correoActualDiferente && usuarioRepository.existsByCorreoEmpresarial(usuarioActualizado.getCorreoEmpresarial())) {
                throw new RuntimeException("Ya existe un usuario con este correo corporativo: " + usuarioActualizado.getCorreoEmpresarial());
            }
        }

        usuario.setIdentificacion(usuarioActualizado.getIdentificacion());
        usuario.setNombres(usuarioActualizado.getNombres());
        usuario.setApellidos(usuarioActualizado.getApellidos());
        usuario.setUsuario(usuarioActualizado.getUsuario());

        if (usuarioActualizado.getCargo() != null && usuarioActualizado.getCargo().getIdCargo() != null) {
            Optional<Cargo> cargoOpt = cargoRepository.findById(usuarioActualizado.getCargo().getIdCargo());
            if (cargoOpt.isPresent()) {
                usuario.setCargo(cargoOpt.get());
            } else {
                throw new RuntimeException("El cargo especificado no existe: " + usuarioActualizado.getCargo().getIdCargo());
            }
        }

        // Actualizar vistas disponibles si se proporcionan
        if (usuarioActualizado.getRol() != null && !usuarioActualizado.getRol().isEmpty()) {
            usuario.setRol(usuarioActualizado.getRol());
        }

        usuario.setCorreoEmpresarial(usuarioActualizado.getCorreoEmpresarial());
        usuario.setCorreoPersonal(usuarioActualizado.getCorreoPersonal());
        usuario.setTelefono1(usuarioActualizado.getTelefono1());
        usuario.setTelefono2(usuarioActualizado.getTelefono2());
        usuario.setDireccion(usuarioActualizado.getDireccion());
        // Actualizar método 2FA si se proporciona
        if (usuarioActualizado.getTokenQr() != null) {
            usuario.setTokenQr(usuarioActualizado.getTokenQr());
        }
        if (usuarioActualizado.getTokenCorreo() != null) {
            usuario.setTokenCorreo(usuarioActualizado.getTokenCorreo());
        }

        return usuarioRepository.save(usuario);
    }

    public void eliminarUsuario(Integer id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado con ID: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existeUsuario(Integer id) {
        return usuarioRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public List<Usuario> buscarUsuariosPorNombreOApellido(String termino) {
        return usuarioRepository.findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(termino, termino);
    }

    @Transactional(readOnly = true)
    public List<Usuario> obtenerUsuariosPorCargo(Integer cargoId) {
        return usuarioRepository.findByCargo_IdCargo(cargoId);
    }

    @Transactional
    public Usuario activarUsuario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        Estado activo = estadoRepository.findByDescripcion("ACTIVO")
                .orElseThrow(() -> new RuntimeException("Estado ACTIVO no encontrado en BD"));
        usuario.setEstado(activo);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario desactivarUsuario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        Estado inactivo = estadoRepository.findByDescripcion("INACTIVO")
                .orElseThrow(() -> new RuntimeException("Estado INACTIVO no encontrado en BD"));
        usuario.setEstado(inactivo);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario cambiarPassword(Integer id, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("La nueva contraseña es obligatoria");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        return usuarioRepository.save(usuario);
    }

    public CargoRepository getCargoRepository() {
        return cargoRepository;
    }

    public EstadoRepository getEstadoRepository() {
        return estadoRepository;
    }
}
