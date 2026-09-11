package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserSyncService userSyncService;

    private final String keycloakId = "3a2980bc-b4cd-4cbf-bc6e-1c228d43d038";

    @Test
    void findOrCreateUser_conUsuarioExistente_retornaUsuarioSinCrearNuevo() {
        User existingUser = User.builder()
                .idUser(UUID.randomUUID())
                .keycloakId(keycloakId)
                .username("admin")
                .email("admin@camelsvsdwarfs.test")
                .build();

        when(jwt.getSubject()).thenReturn(keycloakId);
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(existingUser));

        User result = userSyncService.findOrCreateUser(jwt);

        assertThat(result).isEqualTo(existingUser);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateUser_conUsuarioNuevo_loCreaYLoGuardaConDatosDelToken() {
        when(jwt.getSubject()).thenReturn(keycloakId);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("admin");
        when(jwt.getClaimAsString("email")).thenReturn("admin@camelsvsdwarfs.test");
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userSyncService.findOrCreateUser(jwt);

        assertThat(result.getKeycloakId()).isEqualTo(keycloakId);
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getEmail()).isEqualTo("admin@camelsvsdwarfs.test");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findOrCreateUser_conUsuarioNuevoSinEmailEnToken_loCreaConEmailNulo() {
        when(jwt.getSubject()).thenReturn(keycloakId);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("viewer");
        when(jwt.getClaimAsString("email")).thenReturn(null);
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userSyncService.findOrCreateUser(jwt);

        assertThat(result.getUsername()).isEqualTo("viewer");
        assertThat(result.getEmail()).isNull();
    }

    @Test
    void findOrCreateUser_llamadoDosVecesConMismoToken_noCreaUsuarioDuplicado() {
        User existingUser = User.builder()
                .idUser(UUID.randomUUID())
                .keycloakId(keycloakId)
                .username("organizer")
                .email("organizer@camelsvsdwarfs.test")
                .build();

        when(jwt.getSubject()).thenReturn(keycloakId);
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(existingUser));

        userSyncService.findOrCreateUser(jwt);
        userSyncService.findOrCreateUser(jwt);

        verify(userRepository, never()).save(any());
        verify(userRepository, times(2)).findByKeycloakId(keycloakId);
    }
}