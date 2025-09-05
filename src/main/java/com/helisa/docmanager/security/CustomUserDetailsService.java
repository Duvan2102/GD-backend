package com.helisa.docmanager.security;

import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        boolean enabled = usuario.getEstado() == null ||
                (usuario.getEstado().getDescripcion() != null && !usuario.getEstado().getDescripcion().equalsIgnoreCase("INACTIVO"));

        Collection<? extends GrantedAuthority> authorities = mapAuthorities(usuario);

        return User.builder()
                .username(usuario.getUsuario())
                .password(usuario.getPassword())
                .authorities(authorities)
                .disabled(!enabled)
                .build();
    }

    private Collection<? extends GrantedAuthority> mapAuthorities(Usuario usuario) {
        if (usuario.getRol() != null && usuario.getRol().getDescripcion() != null) {
            String roleName = usuario.getRol().getDescripcion().toUpperCase().replaceAll("\\s+", "_");
            return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
        }
        return List.of();
    }
}

