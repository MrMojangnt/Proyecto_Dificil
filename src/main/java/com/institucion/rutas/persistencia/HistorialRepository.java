package com.institucion.rutas.persistencia;

import java.util.List;

/** Almacenamiento de los cálculos realizados (entradas y resultados). */
public interface HistorialRepository {

    void registrar(RegistroCalculo registro);

    /** @return los registros en el orden en que se realizaron */
    List<RegistroCalculo> cargar();
}
