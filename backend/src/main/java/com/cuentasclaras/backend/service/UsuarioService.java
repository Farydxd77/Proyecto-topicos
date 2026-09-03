package com.cuentasclaras.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.cuentasclaras.backend.dto.response.UsuarioDto;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar(String username) {
        List<Usuario> usuarios = StringUtils.hasText(username)
                ? usuarioRepository.findByUsernameContainingIgnoreCase(username)
                : usuarioRepository.findAll();
        return usuarios.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        return toDto(usuario);
    }

    private UsuarioDto toDto(Usuario usuario) {
        return new UsuarioDto(usuario.getId(), usuario.getUsername());
    }
}
