package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Cargo;
import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.repository.CargoRepository;
import com.helisa.docmanager.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CargoRepository cargoRepository;
    /**
     * Crear un nuevo usuario
     * @param usuario Usuario a crear
     * @return Usuario creado
     */
    @Transactional
    public Usuario crearUsuario(Usuario usuario) {
        // Validar que el usuario no exista
        if (usuarioRepository.existsByUsuario(usuario.getUsuario())) {
            throw new RuntimeException("El usuario ya existe: " + usuario.getUsuario());
        }

        // Validar que la identificación no exista
        if (usuarioRepository.existsByIdentificacion(usuario.getIdentificacion())) {
            throw new RuntimeException("Ya existe un usuario con esta identificación: " + usuario.getIdentificacion());
        }

        // Cargar el cargo completo desde la base de datos
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

        return usuarioRepository.save(usuario);
    }

    /**
     * Obtener todos los usuarios
     * @return Lista de usuarios
     */
    @Transactional(readOnly = true)
    public List<Usuario> obtenerTodosUsuarios() {
        return usuarioRepository.findAll();
    }

    /**
     * Obtener usuario por ID
     * @param id ID del usuario
     * @return Usuario encontrado
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerUsuarioPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    /**
     * Obtener usuario por nombre de usuario
     * @param usuario Nombre de usuario
     * @return Usuario encontrado
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerUsuarioPorNombreUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario);
    }

    /**
     * Obtener usuario por identificación
     * @param identificacion Identificación del usuario
     * @return Usuario encontrado
     */
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

        // Validar si el nuevo nombre de usuario ya existe (excepto para el mismo usuario)
        if (!usuario.getUsuario().equals(usuarioActualizado.getUsuario()) &&
                usuarioRepository.existsByUsuario(usuarioActualizado.getUsuario())) {
            throw new RuntimeException("El usuario ya existe: " + usuarioActualizado.getUsuario());
        }

        // Validar si la nueva identificación ya existe (excepto para el mismo usuario)
        if (!usuario.getIdentificacion().equals(usuarioActualizado.getIdentificacion()) &&
                usuarioRepository.existsByIdentificacion(usuarioActualizado.getIdentificacion())) {
            throw new RuntimeException("Ya existe un usuario con esta identificación: " + usuarioActualizado.getIdentificacion());
        }

        // Actualizar campos
        usuario.setIdentificacion(usuarioActualizado.getIdentificacion());
        usuario.setNombres(usuarioActualizado.getNombres());
        usuario.setApellidos(usuarioActualizado.getApellidos());
        usuario.setUsuario(usuarioActualizado.getUsuario());

        // Cargar el cargo completo desde la base de datos
        if (usuarioActualizado.getCargo() != null && usuarioActualizado.getCargo().getIdCargo() != null) {
            Optional<Cargo> cargoOpt = cargoRepository.findById(usuarioActualizado.getCargo().getIdCargo());
            if (cargoOpt.isPresent()) {
                usuario.setCargo(cargoOpt.get());
            } else {
                throw new RuntimeException("El cargo especificado no existe: " + usuarioActualizado.getCargo().getIdCargo());
            }
        }

        usuario.setCorreoEmpresarial(usuarioActualizado.getCorreoEmpresarial());
        usuario.setCorreoPersonal(usuarioActualizado.getCorreoPersonal());
        usuario.setTelefono1(usuarioActualizado.getTelefono1());
        usuario.setTelefono2(usuarioActualizado.getTelefono2());
        usuario.setDireccion(usuarioActualizado.getDireccion());
        usuario.setDobleAutenticacion(usuarioActualizado.getDobleAutenticacion());

        return usuarioRepository.save(usuario);
    }

    /**
     * Eliminar usuario por ID
     * @param id ID del usuario a eliminar
     */
    public void eliminarUsuario(Integer id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado con ID: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    /**
     * Verificar si existe un usuario por ID
     * @param id ID del usuario
     * @return true si existe, false si no
     */
    @Transactional(readOnly = true)
    public boolean existeUsuario(Integer id) {
        return usuarioRepository.existsById(id);
    }

    /**
     * Buscar usuarios por nombre o apellido
     * @param termino Término de búsqueda
     * @return Lista de usuarios encontrados
     */
    @Transactional(readOnly = true)
    public List<Usuario> buscarUsuariosPorNombreOApellido(String termino) {
        return usuarioRepository.findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(termino, termino);
    }

    /**
     * Obtener usuarios por cargo
     * @param cargoId ID del cargo
     * @return Lista de usuarios con el cargo especificado
     */
    @Transactional(readOnly = true)
    public List<Usuario> obtenerUsuariosPorCargo(Integer cargoId) {
        return usuarioRepository.findByCargo_IdCargo(cargoId);
    }
}
