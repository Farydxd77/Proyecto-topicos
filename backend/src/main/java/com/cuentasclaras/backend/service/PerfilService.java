package com.cuentasclaras.backend.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cuentasclaras.backend.dto.request.ActualizarPerfilRequest;
import com.cuentasclaras.backend.dto.request.CambiarPasswordRequest;
import com.cuentasclaras.backend.dto.request.CambiarUsernameRequest;
import com.cuentasclaras.backend.dto.response.PerfilResponse;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.exception.UsernameAlreadyExistsException;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final ParticipanteRepository participanteRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilService(
            UsuarioRepository usuarioRepository,
            ParticipanteRepository participanteRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.participanteRepository = participanteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtenerMiPerfil() {
        Usuario usuario = usuarioActual();
        return toResponse(usuario, participanteDe(usuario));
    }

    @Transactional
    public PerfilResponse actualizarMiPerfil(ActualizarPerfilRequest req) {
        Usuario usuario = usuarioActual();
        Participante participante = participanteDe(usuario);
        participante.setNombre(req.nombre());
        participante.setApellido(req.apellido());
        // El CI no es editable: se conserva el valor registrado.
        participanteRepository.save(participante);
        return toResponse(usuario, participante);
    }

    @Transactional
    public PerfilResponse cambiarUsername(CambiarUsernameRequest req) {
        Usuario usuario = usuarioActual();
        String nuevoUsername = req.username();

        if (!nuevoUsername.equals(usuario.getUsername())) {
            usuarioRepository.findByUsername(nuevoUsername)
                    .filter(otro -> !otro.getId().equals(usuario.getId()))
                    .ifPresent(otro -> {
                        throw new UsernameAlreadyExistsException(
                                "El username ya está en uso: " + nuevoUsername);
                    });
            usuario.setUsername(nuevoUsername);
            usuarioRepository.save(usuario);
        }

        return toResponse(usuario, participanteDe(usuario));
    }

    @Transactional
    public void cambiarPassword(CambiarPasswordRequest req) {
        Usuario usuario = usuarioActual();
        usuario.setPassword(passwordEncoder.encode(req.password()));
        usuarioRepository.save(usuario);
    }

    private Usuario usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("No hay un usuario autenticado");
        }
        String username = authentication.getName();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado: " + username));
    }

    private Participante participanteDe(Usuario usuario) {
        return participanteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no tiene un participante vinculado"));
    }

    private PerfilResponse toResponse(Usuario usuario, Participante participante) {
        return new PerfilResponse(
                participante.getId(),
                usuario.getId(),
                usuario.getUsername(),
                participante.getNombre(),
                participante.getApellido(),
                participante.getCi(),
                participante.getCreatedAt());
    }
}
