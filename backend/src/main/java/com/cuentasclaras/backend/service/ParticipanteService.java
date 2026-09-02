package com.cuentasclaras.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.exception.ResourceNotFoundException;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

@Service
public class ParticipanteService {

    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    public ParticipanteService(
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository) {
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ParticipanteDto> listar(String ci, String nombre, String apellido) {
        List<Participante> participantes;
        if (StringUtils.hasText(ci)) {
            participantes = participanteRepository.findByCi(ci)
                    .map(List::of)
                    .orElseGet(List::of);
        } else if (StringUtils.hasText(nombre)) {
            participantes = participanteRepository.findByNombreContainingIgnoreCase(nombre);
        } else if (StringUtils.hasText(apellido)) {
            participantes = participanteRepository.findByApellidoContainingIgnoreCase(apellido);
        } else {
            participantes = participanteRepository.findAll();
        }
        return participantes.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ParticipanteDto obtenerPorId(Long id) {
        Participante participante = participanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado: " + id));
        return toDto(participante);
    }

    @Transactional(readOnly = true)
    public ParticipanteDto obtenerPorUsuarioId(Long usuarioId) {
        if (usuarioRepository.findById(usuarioId).isEmpty()) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + usuarioId);
        }
        Participante participante = participanteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no tiene un participante vinculado"));
        return toDto(participante);
    }

    private ParticipanteDto toDto(Participante p) {
        return new ParticipanteDto(
                p.getId(),
                p.getNombre(),
                p.getApellido(),
                p.getCi(),
                p.getUsuario().getUsername());
    }
}
