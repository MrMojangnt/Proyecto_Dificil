package com.institucion.rutas.dominio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapaSedesTest {

    private MapaSedes mapa;

    @BeforeEach
    void preparar() {
        mapa = new MapaSedes();
        mapa.agregarSede(new Sede("A", "Sede Norte"));
        mapa.agregarSede(new Sede("B", "Sede Sur"));
    }

    @Test
    @DisplayName("Criterio 3: un peso -1 se rechaza sin añadir la conexión")
    void pesoNegativoSeRechazaSinAgregar() {
        assertThrows(PesoInvalidoException.class, () -> mapa.agregarConexion("A", "B", -1));
        assertEquals(0, mapa.cantidadConexiones());
        assertEquals(Optional.empty(), mapa.costoConexion("A", "B"));
    }

    @Test
    void pesoNegativoNoAlteraUnaConexionExistente() {
        mapa.agregarConexion("A", "B", 5);
        assertThrows(PesoInvalidoException.class, () -> mapa.agregarConexion("A", "B", -1));
        assertEquals(Optional.of(5), mapa.costoConexion("A", "B"));
    }

    @Test
    void conexionConSedeInexistenteSeRechaza() {
        SedeInexistenteException e = assertThrows(SedeInexistenteException.class,
                () -> mapa.agregarConexion("A", "Z", 3));
        assertEquals("Z", e.getIdSede());
        assertEquals(0, mapa.cantidadConexiones());
    }

    @Test
    void costoCeroEsValido() {
        mapa.agregarConexion("A", "B", 0);
        assertEquals(Optional.of(0), mapa.costoConexion("A", "B"));
    }

    @Test
    void conexionesSonDirigidas() {
        mapa.agregarConexion("A", "B", 4);
        assertEquals(Optional.empty(), mapa.costoConexion("B", "A"));
    }

    @Test
    void reagregarConexionReemplazaElCosto() {
        mapa.agregarConexion("A", "B", 4);
        mapa.agregarConexion("A", "B", 9);
        assertEquals(1, mapa.cantidadConexiones());
        assertEquals(Optional.of(9), mapa.costoConexion("A", "B"));
    }

    @Test
    void sedeDuplicadaSeRechaza() {
        assertThrows(MapaException.class, () -> mapa.agregarSede(new Sede("A")));
        assertEquals(2, mapa.cantidadSedes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "A;B", "con espacio", "ñandú"})
    void identificadoresInvalidosSeRechazan(String id) {
        assertThrows(MapaException.class, () -> new Sede(id));
    }

    @Test
    void eliminarSedeQuitaSusConexionesEntrantesYSalientes() {
        mapa.agregarSede(new Sede("C"));
        mapa.agregarConexion("A", "C", 1);
        mapa.agregarConexion("C", "B", 1);
        mapa.agregarConexion("A", "B", 1);
        mapa.eliminarSede("C");
        assertFalse(mapa.contieneSede("C"));
        assertEquals(List.of(new Conexion("A", "B", 1)), mapa.conexiones());
    }

    @Test
    void copiaEsIndependiente() {
        mapa.agregarConexion("A", "B", 1);
        MapaSedes copia = mapa.copia();
        copia.agregarConexion("B", "A", 2);
        assertEquals(1, mapa.cantidadConexiones());
        assertEquals(2, copia.cantidadConexiones());
        assertTrue(copia.contieneSede("A"));
    }
}
