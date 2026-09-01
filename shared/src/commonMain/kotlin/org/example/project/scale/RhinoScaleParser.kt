package com.abtsplazita.posplazita.scale

object RhinoScaleParser {
    private val regex = Regex("""([+-]?\d+\.?\d*)""")

    /**
     * Parsea la cadena recibida de una báscula Rhino (ej. "ST,GS, 1.234kg").
     * Retorna el peso como Double o null si no es válido.
     */
    fun parse(data: String): Double? {
        return regex.find(data)?.value?.toDoubleOrNull()
    }
}
