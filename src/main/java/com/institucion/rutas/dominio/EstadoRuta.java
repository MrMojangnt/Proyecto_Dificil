package com.institucion.rutas.dominio;

/** Resultado posible de un cálculo de trayecto. */
public enum EstadoRuta {
    /** Existe al menos una ruta; se reporta la de menor costo. */
    ENCONTRADA,
    /** El destino no es alcanzable desde el origen. */
    SIN_RUTA
}
