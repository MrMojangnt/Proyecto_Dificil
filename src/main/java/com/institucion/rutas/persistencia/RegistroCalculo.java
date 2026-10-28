package com.institucion.rutas.persistencia;

import com.institucion.rutas.dominio.ResultadoRuta;

import java.time.Instant;
import java.util.Objects;

/** Cálculo realizado: momento, entradas (origen/destino) y resultado. */
public record RegistroCalculo(Instant fecha, ResultadoRuta resultado) {

    public RegistroCalculo {
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(resultado, "resultado");
    }
}
