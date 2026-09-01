package com.cuentasclaras.backend.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cuentasclaras.backend.dto.request.LoginRequest;
import com.cuentasclaras.backend.dto.request.RegisterRequest;
import com.cuentasclaras.backend.dto.response.LoginResponse;
import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.dto.response.RegisterResponse;
import com.cuentasclaras.backend.dto.response.UsuarioDto;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.exception.UsernameAlreadyExistsException;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;
import com.cuentasclaras.backend.security.JwtUtil;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ParticipanteRepository participanteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UsuarioRepository usuarioRepository,
            ParticipanteRepository participanteRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.participanteRepository = participanteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (usuarioRepository.findByUsername(req.username()).isPresent()) {
            throw new UsernameAlreadyExistsException(
                    "El username ya está en uso: " + req.username());
        }

        Usuario usuario = Usuario.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .build();
        usuarioRepository.save(usuario);

        Participante participante = Participante.builder()
                .usuario(usuario)
                .nombre(req.nombre())
                .apellido(req.apellido())
                .ci(req.ci())
                .build();
        participanteRepository.save(participante);

        String token = jwtUtil.generateToken(usuario.getUsername());
        return new RegisterResponse(token, new ParticipanteDto(
                participante.getId(),
                participante.getNombre(),
                participante.getApellido(),
                participante.getCi(),
                usuario.getUsername()));
    }

    public LoginResponse login(LoginRequest req) {
        return usuarioRepository.findByUsername(req.username())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .map(u -> new LoginResponse(
                        jwtUtil.generateToken(u.getUsername()),
                        new UsuarioDto(u.getId(), u.getUsername())))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
    }
}
