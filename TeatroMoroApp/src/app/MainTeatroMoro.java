/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ===============================================================
 *  AUTOR: SR. JHANS 
 *  PROYECTO: TEATRO MORO – Sistema de Ventas en Consola (ASCII)
 * ---------------------------------------------------------------
 *  DESCRIPCIÓN:
 *  Sistema de gestión de un teatro (eventos, asientos, clientes,
 *  ventas y reportes) con visualización ASCII, validaciones
 *  robustas, descuentos por tipo de cliente y límite de asientos
 *  por venta. Operación por consola, enfoque educativo.
 *
 *  CARACTERÍSTICAS:
 *    - Arreglos: Cliente[], Venta[] (crecimiento dinámico).
 *    - Lista: Eventos (cada evento contiene sus ventas).
 *    - Asientos con etiquetas "A1", "B3", etc. y también por ID.
 *    - Descuentos: 10% (ESTUDIANTE) y 15% (TERCERA_EDAD).
 *    - Límite por venta: 6 asientos; control de stock y
 *      verificación de invariante (no duplicidad de ocupación).
 *    - Visual ASCII puro. Colores ANSI opcionales 
 *
 *  NOTAS DE USO:
 *    - Menú principal muestra opciones 1..6 y 0. La tecla "7"
 *      (no visible) alterna colores ANSI ON/OFF.
 *    - Si tu consola no soporta acentos, compila/ejecuta con
 *      -Dfile.encoding=UTF-8 o mantén solo ASCII en mensajes.
 *
 *  ESTRUCTURA:
 *    - MainTeatroMoro: punto de entrada + menús (UI consola).
 *    - TeatroMoroCore: lógica de negocio (eventos/ventas/clientes).
 *    - Modelos: Cliente, Asiento, Venta, Evento.
 *    - Servicios: DescuentoService (descuentos), Check (validaciones).
 * ===============================================================
 */
public class MainTeatroMoro {

    /* ================== CONFIGURACIÓN GLOBAL ================== */

    /** Activa/desactiva colores ANSI (toggled con tecla oculta "7"). */
    private static boolean USE_COLORS = true;

    /** Límite de asientos que se pueden vender en una sola operación. */
    private static final int MAX_ASIENTOS_POR_VENTA = 6;

    /** Lector estándar para todas las entradas de usuario. */
    private static final Scanner SC = new Scanner(System.in);


    /* ================== ENUMERACIONES Y MODELOS ================== */

    /** Tipos de cliente válidos (determinan el descuento aplicado). */
    enum TipoCliente { GENERAL, ESTUDIANTE, TERCERA_EDAD }

    /**
     * Representa un cliente con identificación, nombre y tipo.
     * Usado en ventas para aplicar descuentos y trazabilidad.
     */
    static class Cliente {
        private final int id;
        private String nombre;
        private TipoCliente tipo;

        Cliente(int id, String nombre, TipoCliente tipo) {
            this.id = id;
            this.nombre = nombre;
            this.tipo = tipo;
        }

        int getId() { return id; }
        String getNombre() { return nombre; }
        void setNombre(String n) { this.nombre = n; }
        TipoCliente getTipo() { return tipo; }
        void setTipo(TipoCliente t) { this.tipo = t; }

        @Override
        public String toString() {
            return "Cliente{id=" + id + ", nombre='" + nombre + "', tipo=" + tipo + "}";
        }
    }

    /**
     * Representa un asiento físico del teatro base: ID secuencial,
     * posición (fila/columna, base cero) y etiqueta amigable (A1...).
     * Los eventos usan un subplano (<= al plano base).
     */
    static class Asiento {
        private final int id, fila, columna;
        private final String etiqueta;

        Asiento(int id, int fila, int columna, String etiqueta) {
            this.id = id;
            this.fila = fila;
            this.columna = columna;
            this.etiqueta = etiqueta;
        }

        int getId() { return id; }
        int getFila() { return fila; }
        int getColumna() { return columna; }
        String getEtiqueta() { return etiqueta; }
    }

    /**
     * Transacción de compra: asocia cliente y evento con un conjunto
     * de asientos, incluyendo montos (bruto, descuento, neto) y fecha.
     */
    static class Venta {
        private final int id, eventoId, clienteId;
        private final int[] asientosIds;
        private final LocalDateTime fecha;
        private final double bruto, desc, neto;

        Venta(int id, int eventoId, int clienteId, int[] asientosIds,
              LocalDateTime fecha, double bruto, double desc, double neto) {
            this.id = id;
            this.eventoId = eventoId;
            this.clienteId = clienteId;
            this.asientosIds = asientosIds;
            this.fecha = fecha;
            this.bruto = bruto;
            this.desc = desc;
            this.neto = neto;
        }

        int getId() { return id; }
        int getEventoId() { return eventoId; }
        int getClienteId() { return clienteId; }
        int[] getAsientosIds() { return asientosIds; }
        double getNeto() { return neto; }

        @Override
        public String toString() {
            return "Venta{id=" + id + ", evento=" + eventoId + ", cliente=" + clienteId +
                    ", asientos=" + Arrays.toString(asientosIds) + ", fecha=" + fecha +
                    ", bruto=" + bruto + ", desc=" + desc + ", neto=" + neto + "}";
        }
    }

    /**
     * Evento programado en el teatro, con un subplano (filas/columnas)
     * no mayor que el plano base, precio base y lista de ventas.
     */
    static class Evento {
        private final int id;
        private String nombre;
        private final int filas, columnas;
        private double precioBase;
        private final List<Venta> ventas = new ArrayList<>();

        Evento(int id, String nombre, int filas, int columnas, double precioBase) {
            this.id = id;
            this.nombre = nombre;
            this.filas = filas;
            this.columnas = columnas;
            this.precioBase = precioBase;
        }

        int getId() { return id; }
        String getNombre() { return nombre; }
        void setNombre(String n) { this.nombre = n; }
        int getFilas() { return filas; }
        int getColumnas() { return columnas; }
        double getPrecioBase() { return precioBase; }
        void setPrecioBase(double p) { this.precioBase = p; }
        List<Venta> getVentas() { return ventas; }

        /** Capacidad total del subplano del evento. */
        int capacidad() { return filas * columnas; }

        @Override
        public String toString() {
            return "Evento{id=" + id + ", nombre='" + nombre + "', sala=" + capacidad() +
                    ", precioBase=" + precioBase + ", ventas=" + ventas.size() + "}";
        }
    }


    /* ================== SERVICIOS AUXILIARES ================== */

    /** Servicio de cálculo de descuentos según el tipo de cliente. */
    static class DescuentoService {
        /**
         * @return factor de descuento (0.10, 0.15 o 0.0)
         */
        static double factor(TipoCliente t) {
            return switch (t) {
                case ESTUDIANTE -> 0.10;
                case TERCERA_EDAD -> 0.15;
                default -> 0.0;
            };
        }
    }

    /**
     * Utilidades de validación para mantener la integridad del
     * estado (inputs, existencia de entidades y ocupación).
     */
    static class Check {
        /** Lanza IllegalArgumentException si la condición no se cumple. */
        static void require(boolean c, String m) {
            if (!c) throw new IllegalArgumentException(m);
        }

        /** Valida que una cadena no esté vacía o nula. */
        static boolean texto(String s) {
            return s != null && !s.trim().isEmpty();
        }

        /** Verifica si un cliente de ID dado existe en el arreglo. */
        static boolean existeCliente(Cliente[] cs, int id) {
            for (Cliente c : cs) if (c != null && c.getId() == id) return true;
            return false;
        }

        /** Busca un asiento por ID dentro del plano base. */
        static Asiento buscar(Asiento[] as, int id) {
            for (Asiento a : as) if (a != null && a.getId() == id) return a;
            return null;
        }

        /** Indica si un asiento está ocupado en un evento. */
        static boolean ocupado(Evento e, int asientoId) {
            for (Venta v : e.getVentas())
                for (int x : v.getAsientosIds())
                    if (x == asientoId) return true;
            return false;
        }

        /** Indica si todos los asientos pasados están libres en el evento. */
        static boolean libres(Evento e, int[] ids) {
            for (int id : ids)
                if (ocupado(e, id)) return false;
            return true;
        }

        /**
         * Invariante de ocupación: el total de asientos ocupados
         * (sumado por ventas) no debe contener duplicados.
         */
        static boolean invariante(Evento e) {
            Set<Integer> set = new HashSet<>();
            int suma = 0;
            for (Venta v : e.getVentas()) {
                for (int id : v.getAsientosIds()) set.add(id);
                suma += v.getAsientosIds().length;
            }
            return set.size() == suma;
        }
    }


    /* ================== NÚCLEO DE NEGOCIO ================== */

    /**
     * Núcleo lógico del sistema:
     * - Mantiene los arreglos de clientes y ventas.
     * - Mantiene lista de eventos.
     * - Genera el plano base de asientos y permite renderizar vistas.
     */
    static class TeatroMoroCore {
        // Estructuras principales (arreglos dinámicos + lista)
        private Cliente[] clientes;
        private Venta[] ventas;
        private final Asiento[] asientosBase;
        private final List<Evento> eventos = new ArrayList<>();

        // Autoincrementales
        private int nextClienteId = 1, nextVentaId = 1, nextEventoId = 1;

        // Dimensiones del plano base
        private final int baseFilas, baseColumnas;

        /**
         * @param capClientes capacidad inicial del arreglo de clientes
         * @param filas       filas del plano base del teatro
         * @param columnas    columnas del plano base del teatro
         * @param capVentas   capacidad inicial del arreglo de ventas
         * @param precioInicial precio base para el "Evento Inicial"
         */
        TeatroMoroCore(int capClientes, int filas, int columnas, int capVentas, double precioInicial) {
            this.clientes = new Cliente[capClientes];
            this.ventas = new Venta[capVentas];
            this.baseFilas = filas;
            this.baseColumnas = columnas;
            this.asientosBase = generarAsientos(filas, columnas);
            // Evento inicial para tener datos listos para operar
            eventos.add(new Evento(nextEventoId++, "Evento Inicial", filas, columnas, precioInicial));
        }

        /**
         * Genera el plano base de asientos etiquetado (A1..), con IDs secuenciales.
         */
        private Asiento[] generarAsientos(int filas, int columnas) {
            Asiento[] base = new Asiento[filas * columnas];
            int id = 1;
            for (int f = 0; f < filas; f++) {
                for (int c = 0; c < columnas; c++) {
                    String etiqueta = ((char) ('A' + f)) + String.valueOf(c + 1);
                    base[(f * columnas) + c] = new Asiento(id++, f, c, etiqueta);
                }
            }
            return base;
        }

        /* ====== CLIENTES (CRUD con arreglo dinámico) ====== */

        /**
         * Alta de cliente; expande el arreglo si no hay espacio.
         */
        Cliente altaCliente(String nombre, TipoCliente tipo) {
            Check.require(Check.texto(nombre), "Nombre invalido");
            Cliente n = new Cliente(nextClienteId++, nombre.trim(), tipo);
            insertarCliente(n);
            return n;
        }

        /** Inserta cliente, ampliando el arreglo si está lleno. */
        private void insertarCliente(Cliente c) {
            for (int i = 0; i < clientes.length; i++)
                if (clientes[i] == null) { clientes[i] = c; return; }
            clientes = Arrays.copyOf(clientes, clientes.length + Math.max(4, clientes.length / 2));
            insertarCliente(c);
        }

        /**
         * Actualiza nombre y/o tipo de un cliente existente.
         * @return true si se actualizó, false si no se encontró el ID.
         */
        boolean actualizarCliente(int id, String nom, TipoCliente t) {
            for (int i = 0; i < clientes.length; i++) {
                var c = clientes[i];
                if (c != null && c.getId() == id) {
                    if (Check.texto(nom)) c.setNombre(nom.trim());
                    if (t != null) c.setTipo(t);
                    return true;
                }
            }
            return false;
        }

        /**
         * Baja lógica de cliente (deja hueco null en el arreglo).
         */
        boolean bajaCliente(int id) {
            for (int i = 0; i < clientes.length; i++)
                if (clientes[i] != null && clientes[i].getId() == id) {
                    clientes[i] = null;
                    return true;
                }
            return false;
        }

        /**
         * Compacta el arreglo eliminando los huecos null intermedios.
         */
        void compactarClientes() {
            Cliente[] n = new Cliente[clientes.length];
            int k = 0;
            for (Cliente c : clientes) if (c != null) n[k++] = c;
            clientes = n;
        }

        /** Busca cliente por ID. */
        Cliente buscarClientePorId(int id) {
            for (Cliente c : clientes)
                if (c != null && c.getId() == id) return c;
            return null;
        }

        Cliente[] getClientes() { return clientes; }

        /* ====== EVENTOS (lista) ====== */

        /**
         * Crea un evento nuevo validando que no exceda el plano base.
         */
        Evento crearEvento(String nombre, int filas, int columnas, double precio) {
            Check.require(filas >= 1 && filas <= baseFilas, "Filas 1-" + baseFilas);
            Check.require(columnas >= 1 && columnas <= baseColumnas, "Columnas 1-" + baseColumnas);
            Evento e = new Evento(nextEventoId++, nombre, filas, columnas, precio);
            eventos.add(e);
            return e;
        }

        /** Obtiene evento por ID (o null si no existe). */
        Evento obtenerEventoPorId(int id) {
            for (Evento e : eventos) if (e.getId() == id) return e;
            return null;
        }

        /** Renombra evento si existe. */
        boolean renombrarEvento(int id, String n) {
            var e = obtenerEventoPorId(id);
            if (e == null) return false;
            if (Check.texto(n)) e.setNombre(n.trim());
            return true;
        }

        /** Cambia precio base si el evento existe. */
        boolean cambiarPrecioEvento(int id, double p) {
            var e = obtenerEventoPorId(id);
            if (e == null) return false;
            e.setPrecioBase(p);
            return true;
        }

        /**
         * Elimina un evento solo si no tiene ventas asociadas (seguridad
         * referencial mínima sin sistema de tickets persistentes).
         */
        boolean eliminarEventoSinVentas(int id) {
            var e = obtenerEventoPorId(id);
            if (e == null || !e.getVentas().isEmpty()) return false;
            return eventos.remove(e);
        }

        List<Evento> getEventos() { return eventos; }

        /* ====== DISPONIBILIDAD ====== */

        /** Total de asientos del subplano del evento. */
        int total(Evento e) { return e.getFilas() * e.getColumnas(); }

        /** Total de asientos ocupados por las ventas del evento. */
        int ocupados(Evento e) {
            int s = 0;
            for (Venta v : e.getVentas()) s += v.getAsientosIds().length;
            return s;
        }

        /** Asientos libres restantes. */
        int libres(Evento e) { return total(e) - ocupados(e); }

        /* ====== VENTAS ====== */

        /**
         * Ejecuta una venta validando:
         * - existencia de evento y cliente,
         * - cantidad válida y <= límite,
         * - pertenencia de cada asiento al subplano del evento,
         * - que no haya IDs duplicados en la selección,
         * - que todos estén libres y haya stock.
         * Si se viola la invariante de ocupación, hace rollback.
         */
        Venta venderEntradas(int eventoId, int clienteId, int[] ids) {
            Evento e = obtenerEventoPorId(eventoId);
            Check.require(e != null, "Evento inexistente");
            Check.require(Check.existeCliente(clientes, clienteId), "Cliente inexistente");
            Check.require(ids != null && ids.length > 0, "Sin asientos");
            Check.require(ids.length <= MAX_ASIENTOS_POR_VENTA,
                          "Maximo por venta: " + MAX_ASIENTOS_POR_VENTA);
            Check.require(ids.length <= libres(e), "No hay suficientes libres");

            // Verificación de pertenencia y rango para cada ID
            for (int id : ids) {
                Asiento a = Check.buscar(asientosBase, id);
                Check.require(a != null, "Asiento ID invalido: " + id);
                Check.require(a.getFila() < e.getFilas() && a.getColumna() < e.getColumnas(),
                        "Asiento " + id + " fuera del plano del evento");
            }

            // Verificar duplicados en la selección
            Set<Integer> unicos = new HashSet<>();
            for (int x : ids) Check.require(unicos.add(x), "ID repetido: " + x);

            // Verificar ocupación actual
            Check.require(Check.libres(e, ids), "Alguno ya ocupado");

            // Cálculos monetarios
            Cliente c = buscarClientePorId(clienteId);
            double bruto = e.getPrecioBase() * ids.length;
            double desc  = Math.round(bruto * DescuentoService.factor(c.getTipo()) * 100.0) / 100.0;
            double neto  = Math.round((bruto - desc) * 100.0) / 100.0;

            // Persistir venta en arreglo + asociarla al evento
            Venta v = new Venta(nextVentaId++, eventoId, clienteId, Arrays.copyOf(ids, ids.length),
                                LocalDateTime.now(), bruto, desc, neto);
            insertarVenta(v); e.getVentas().add(v);

            // Validación de invariante (no duplicidad post-venta)
            if (!Check.invariante(e)) {
                eliminarVenta(v.getId());     // rollback en estructura global
                e.getVentas().remove(v);      // rollback en evento
                throw new IllegalStateException("Violacion de invariante de ocupacion");
            }
            return v;
        }

        /** Inserta la venta y expande arreglo si es necesario. */
        private void insertarVenta(Venta v) {
            for (int i = 0; i < ventas.length; i++)
                if (ventas[i] == null) { ventas[i] = v; return; }
            ventas = Arrays.copyOf(ventas, ventas.length + Math.max(4, ventas.length / 2));
            insertarVenta(v);
        }

        /**
         * Elimina una venta por ID de la estructura global y
         * la quita también de la lista de ventas del evento.
         */
        boolean eliminarVenta(int id) {
            boolean rem = false;
            for (int i = 0; i < ventas.length; i++)
                if (ventas[i] != null && ventas[i].getId() == id) {
                    ventas[i] = null; rem = true; break;
                }
            if (!rem) return false;
            for (Evento e : eventos) e.getVentas().removeIf(v -> v.getId() == id);
            return true;
        }

        /* ====== RENDER ASCII ====== */

        // Códigos ANSI: reset, verde, rojo, cian, negrita
        private static final String R = "\u001B[0m", G = "\u001B[32m", D = "\u001B[31m",
                                    C = "\u001B[36m", B = "\u001B[1m";

        /**
         * Render de ocupación (O/X) para el subplano del evento.
         * Usa bordes ASCII (+ - |). Aplica colores si están activos.
         */
        String planoAscii(int eventoId) {
            Evento e = obtenerEventoPorId(eventoId);
            if (e == null) return "Evento no encontrado.";
            int F = e.getFilas(), K = e.getColumnas();

            StringBuilder sb = new StringBuilder();
            sb.append(B).append("Plano - ").append(e.getNombre()).append(R)
              .append(" | Precio: ").append(e.getPrecioBase())
              .append(" | Libres: ").append(libres(e)).append("/").append(total(e)).append("\n");

            // encabezado de columnas
            sb.append("    ");
            for (int c = 1; c <= K; c++) sb.append(String.format("%3d", c));
            sb.append("\n");

            // borde superior
            sb.append("   ").append("+").append("-".repeat(K * 3)).append("+").append("\n");

            // filas
            for (int f = 0; f < F; f++) {
                char letra = (char) ('A' + f);
                sb.append(" ").append(letra).append(" ").append("|");
                for (int c = 0; c < K; c++) {
                    int id = asientosBase[f * K + c].getId();
                    boolean oc = Check.ocupado(e, id);
                    String simb = oc ? "X" : "O";
                    String o = USE_COLORS ? (oc ? D : G) : "";
                    String r = USE_COLORS ? R : "";
                    sb.append(" ").append(o).append(simb).append(r).append(" ");
                }
                sb.append("|").append("\n");
            }

            // borde inferior y leyenda
            sb.append("   ").append("+").append("-".repeat(K * 3)).append("+").append("\n");
            sb.append((USE_COLORS ? C : "")).append("Leyenda: ").append((USE_COLORS ? R : ""))
              .append((USE_COLORS ? G : "")).append("O Libre ").append((USE_COLORS ? R : ""))
              .append((USE_COLORS ? D : "")).append("X Ocupado").append((USE_COLORS ? R : "")).append("\n");
            return sb.toString();
        }

        /**
         * Render de mapa de IDs (numérico) para orientar al usuario al
         * seleccionar por ID. No muestra ocupación.
         */
        String planoConIds(int eventoId) {
            Evento e = obtenerEventoPorId(eventoId);
            if (e == null) return "Evento no encontrado.";
            int F = e.getFilas(), K = e.getColumnas();

            StringBuilder sb = new StringBuilder();
            sb.append("Plano con IDs - ").append(e.getNombre()).append("\n");

            // encabezado de columnas
            sb.append("     ");
            for (int c = 1; c <= K; c++) sb.append(String.format("%4d", c));
            sb.append("\n");

            // borde superior
            sb.append("   ").append("+").append("-".repeat(K * 4)).append("+").append("\n");

            // filas
            for (int f = 0; f < F; f++) {
                char letra = (char) ('A' + f);
                sb.append(" ").append(letra).append(" ").append("|");
                for (int c = 0; c < K; c++) {
                    int id = asientosBase[f * K + c].getId();
                    sb.append(String.format("%4d", id));
                }
                sb.append("|").append("\n");
            }

            // borde inferior y regla de cálculo
            sb.append("   ").append("+").append("-".repeat(K * 4)).append("+").append("\n");
            sb.append("ID = (filaIndex * columnas + columna), comenzando en 1.\n");
            return sb.toString();
        }

        /**
         * Busca un bloque de N asientos contiguos libres en una fila concreta.
         * @return lista con los IDs seleccionados o lista vacía si no hay.
         */
        List<Integer> contiguosEnFila(int eventoId, int filaIndex, int n) {
            Evento e = obtenerEventoPorId(eventoId);
            if (e == null) return List.of();
            int K = e.getColumnas();
            List<Integer> win = new ArrayList<>();
            int streak = 0;

            for (int c = 0; c < K; c++) {
                int id = asientosBase[filaIndex * K + c].getId();
                if (!Check.ocupado(e, id)) {
                    win.add(id);
                    streak++;
                    if (streak == n) return new ArrayList<>(win);
                } else {
                    win.clear(); streak = 0;
                }
            }
            return List.of();
        }

        /**
         * Reporte breve de un evento (ventas, ocupación y libres).
         */
        String reporte(int eventoId) {
            Evento e = obtenerEventoPorId(eventoId);
            if (e == null) return "Evento no encontrado.";
            int occ = ocupados(e), tot = total(e);
            double p = (tot == 0) ? 0.0 : (100.0 * occ / tot);
            return "Evento: " + e.getNombre() + " | Ventas: " + e.getVentas().size() +
                   " | Ocupados: " + occ + "/" + tot + String.format(" (%.1f%%)", p) +
                   " | Libres: " + libres(e);
        }

        /* ====== HELPERS ETIQUETA/ID ====== */

        /**
         * Convierte etiqueta (ej. "A3") a ID de asiento válido para el evento.
         * @return ID (>=1) o -1 si es inválido / fuera del plano del evento.
         */
        int idDesdeEtiqueta(int eventoId, String etiqueta) {
            if (etiqueta == null || etiqueta.length() < 2) return -1;
            etiqueta = etiqueta.trim().toUpperCase(Locale.ROOT);
            char filaChar = etiqueta.charAt(0);
            String numStr = etiqueta.substring(1).trim();
            if (!numStr.matches("\\d+")) return -1;

            int col = Integer.parseInt(numStr);
            if (col < 1) return -1;

            Evento e = obtenerEventoPorId(eventoId);
            if (e == null) return -1;

            int fila = filaChar - 'A';
            if (fila < 0 || fila >= e.getFilas()) return -1;
            if (col > e.getColumnas()) return -1;

            // Mapeo estable y didáctico
            return fila * e.getColumnas() + col;
        }

        /**
         * Convierte ID de asiento a etiqueta (ej. 3 -> "A3") si pertenece
         * al subplano del evento; retorna "?" si está fuera o inválido.
         */
        String etiquetaDesdeId(int eventoId, int id) {
            Evento e = obtenerEventoPorId(eventoId);
            if (e == null) return "?";
            Asiento a = Check.buscar(asientosBase, id);
            if (a == null) return "?";
            if (a.getFila() >= e.getFilas() || a.getColumna() >= e.getColumnas()) return "?";
            return a.getEtiqueta();
        }
    }


    /* ================== APLICACIÓN / INTERFAZ CONSOLA ================== */

    /** Motor de negocio compartido por los menús. */
    private static TeatroMoroCore core;

    /** Punto de entrada. */
    public static void main(String[] args) {
        inicializar();
        menuPrincipal();
    }

    /**
     * Carga datos iniciales del sistema:
     * - Plano base 8x12 (96), precio base 5000.
     * - Tres clientes de ejemplo.
     */
    private static void inicializar() {
        core = new TeatroMoroCore(50, 8, 12, 200, 5000.0);
        core.altaCliente("Ana Perez",  TipoCliente.ESTUDIANTE);
        core.altaCliente("Luis Munoz", TipoCliente.TERCERA_EDAD);
        core.altaCliente("Maria Lopez",TipoCliente.GENERAL);
    }

    /**
     * Menú principal:
     *  1) Ver plano
     *  2) Vender entradas
     *  3) Gestión de clientes
     *  4) Gestión de eventos
     *  5) Reportes
     *  6) Pruebas rápidas
     *  0) Salir
     * Además: tecla oculta "7" que alterna colores ANSI.
     */
    private static void menuPrincipal() {
        while (true) {
            System.out.println("\n=== TEATRO MORO - Sistema de Ventas ===");
            System.out.println("1) Ver plano de asientos");
            System.out.println("2) Vender entradas (max " + MAX_ASIENTOS_POR_VENTA + ")");
            System.out.println("3) Gestion de clientes");
            System.out.println("4) Gestion de eventos");
            System.out.println("5) Reportes");
            System.out.println("6) Pruebas rapidas");
            System.out.println("0) Salir");
            System.out.print("Opcion [0-6]: ");

            String entrada = SC.nextLine().trim();

            // Tecla oculta: no aparece en el menú, pero permite alternar colores.
            if ("7".equals(entrada)) {
                USE_COLORS = !USE_COLORS;
                System.out.println("Colores: " + (USE_COLORS ? "ON" : "OFF"));
                continue;
            }

            int op;
            try {
                op = Integer.parseInt(entrada);
            } catch (Exception e) {
                System.out.println("Ingrese un numero entre 0 y 6.");
                continue;
            }
            if (op < 0 || op > 6) {
                System.out.println("Ingrese un numero entre 0 y 6.");
                continue;
            }

            switch (op) {
                case 1 -> verPlano();
                case 2 -> vender();
                case 3 -> menuClientes();
                case 4 -> menuEventos();
                case 5 -> menuReportes();
                case 6 -> pruebasRapidas();
                case 0 -> { System.out.println("Hasta luego."); return; }
            }
        }
    }


    /* ================== UTILIDADES I/O ================== */

    /**
     * Pide una opción numérica validada en un rango.
     */
    private static int pedirOpcion(String label, int min, int max) {
        while (true) {
            System.out.print(label + " [" + min + "-" + max + "]: ");
            String s = SC.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) throw new NumberFormatException();
                return v;
            } catch (Exception e) {
                System.out.println("Ingrese un numero entre " + min + " y " + max + ".");
            }
        }
    }

    /**
     * Pide entero con posibilidad de cancelar (enter = null).
     */
    private static Integer pedirIntCancelable(String label) {
        System.out.print(label + " (enter para cancelar): ");
        String s = SC.nextLine().trim();
        if (s.isEmpty()) return null;
        try { return Integer.parseInt(s); }
        catch (Exception e) {
            System.out.println("Ingrese un numero valido.");
            return pedirIntCancelable(label);
        }
    }

    /**
     * Pide texto no vacío (itera hasta obtener uno válido).
     */
    private static String pedirTextoNoVacio(String label) {
        while (true) {
            System.out.print(label);
            String s = SC.nextLine();
            if (s != null && !s.trim().isEmpty()) return s.trim();
            System.out.println("Texto vacio. Intente nuevamente.");
        }
    }

    /**
     * Pide un número decimal (double) validado.
     */
    private static double pedirDouble(String label) {
        while (true) {
            System.out.print(label);
            try { return Double.parseDouble(SC.nextLine().trim()); }
            catch (Exception e) { System.out.println("Ingrese numero decimal valido."); }
        }
    }


    /* ================== SELECCIÓN DE ENTIDADES ================== */

    /**
     * Muestra eventos y solicita uno válido (o null si se cancela).
     */
    private static Integer elegirEvento() {
        while (true) {
            System.out.println("Eventos:");
            for (var e : core.getEventos()) {
                int libres = core.libres(e);
                System.out.println("ID " + e.getId() + " - " + e.getNombre() +
                                   " [libres " + libres + "/" + e.capacidad() + "]");
            }
            Integer id = pedirIntCancelable("ID de evento (0 para salir)");
            if (id == null || id == 0) return null;
            if (core.obtenerEventoPorId(id) != null) return id;
            System.out.println("Evento no encontrado. Intente nuevamente.");
        }
    }

    /**
     * Muestra clientes y solicita uno válido (o null si se cancela).
     */
    private static Integer elegirCliente() {
        while (true) {
            System.out.println("Clientes:");
            for (Cliente c : core.getClientes())
                if (c != null) System.out.println("ID " + c.getId() + " - " + c.getNombre() + " (" + c.getTipo() + ")");
            Integer id = pedirIntCancelable("ID de cliente (0 para salir)");
            if (id == null || id == 0) return null;
            if (core.buscarClientePorId(id) != null) return id;
            System.out.println("Cliente no encontrado. Intente nuevamente.");
        }
    }


    /* ================== PARSERS DE ENTRADA ================== */

    /**
     * Convierte una cadena de etiquetas a lista de IDs.
     * Formatos admitidos:
     *   - Listas separadas por coma: "A3,A4,B2"
     *   - Rangos en la misma fila: "A3-A6"
     * Se ignoran tokens inválidos. El llamador valida la cantidad.
     */
    private static int[] parseEtiquetasLista(int eventoId, String input) {
        String[] toks = input.toUpperCase(Locale.ROOT).replace(" ", "").split(",");
        List<Integer> out = new ArrayList<>();
        for (String t : toks) {
            if (t.isEmpty()) continue;
            if (t.contains("-")) {
                String[] lr = t.split("-");
                if (lr.length != 2) continue;
                int idL = core.idDesdeEtiqueta(eventoId, lr[0]);
                int idR = core.idDesdeEtiqueta(eventoId, lr[1]);
                if (idL == -1 || idR == -1) continue;
                // Deben pertenecer a la MISMA fila y ser un rango creciente
                Asiento aL = Check.buscar(core.asientosBase, idL);
                Asiento aR = Check.buscar(core.asientosBase, idR);
                if (aL.getFila() != aR.getFila() || idL > idR) continue;
                for (int id = idL; id <= idR; id++) out.add(id);
            } else {
                int id = core.idDesdeEtiqueta(eventoId, t);
                if (id != -1) out.add(id);
            }
        }
        return out.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Convierte una cadena de IDs a lista de IDs.
     * Formatos admitidos:
     *   - Listas separadas por coma: "3,4,5"
     *   - Rangos: "3-6"
     * Se ignoran tokens inválidos. El llamador valida la cantidad.
     */
    private static int[] parseIdsLista(String input) {
        String[] toks = input.replace(" ", "").split(",");
        List<Integer> out = new ArrayList<>();
        for (String t : toks) {
            if (t.isEmpty()) continue;
            if (t.contains("-")) {
                String[] lr = t.split("-");
                if (lr.length != 2) continue;
                try {
                    int L = Integer.parseInt(lr[0]), R = Integer.parseInt(lr[1]);
                    if (L <= 0 || R < L) continue;
                    for (int x = L; x <= R; x++) out.add(x);
                } catch (Exception ignore) { /* token inválido -> se omite */ }
            } else {
                try { int v = Integer.parseInt(t); if (v > 0) out.add(v); }
                catch (Exception ignore) { /* token inválido -> se omite */ }
            }
        }
        return out.stream().mapToInt(i -> i).toArray();
    }


    /* ================== MENÚS: FLUJOS DE USO ================== */

    /**
     * Submenú de visualización del plano:
     *   1) Ocupación (O/X)
     *   2) Mapa de IDs
     *   3) Libres por fila
     */
    private static void verPlano() {
        Integer eventoId = elegirEvento();
        if (eventoId == null) return;

        while (true) {
            System.out.println("\n-- Ver plano --");
            System.out.println("1) Ocupacion (O/X)");
            System.out.println("2) Mapa de IDs");
            System.out.println("3) Ver libres por fila");
            System.out.println("0) Volver");
            int op = pedirOpcion("Opcion", 0, 3);
            if (op == 0) return;

            if (op == 1) {
                System.out.println(core.planoAscii(eventoId));
            } else if (op == 2) {
                System.out.println(core.planoConIds(eventoId));
            } else {
                // (3) Mostrar todas las butacas libres en una fila indicada
                var e = core.obtenerEventoPorId(eventoId);
                int fila = pedirOpcion("Fila (A=1,B=2,...)", 1, e.getFilas()) - 1;
                // Recorre la fila y lista libres
                List<Integer> all = new ArrayList<>();
                int K = e.getColumnas();
                for (int c = 0; c < K; c++) {
                    int id = core.asientosBase[fila * K + c].getId();
                    if (!Check.ocupado(e, id)) all.add(id);
                }
                List<String> etiquetas = new ArrayList<>();
                for (int id : all) etiquetas.add(core.etiquetaDesdeId(eventoId, id));
                System.out.println("Libres en fila " + (char) ('A' + fila) + ": " +
                                   (all.isEmpty() ? "(ninguno)" : etiquetas));
            }
        }
    }

    /**
     * Flujo de venta de entradas:
     *   - Selección de evento y cliente
     *   - Elección de método (etiquetas, IDs o contiguos)
     *   - Resumen de compra + confirmación
     *   - Emite la venta y re-render del plano
     */
    private static void vender() {
        Integer eventoId = elegirEvento();
        if (eventoId == null) return;

        Integer clienteId = elegirCliente();
        if (clienteId == null) return;

        // Muestras didácticas previas a la selección
        System.out.println(core.planoAscii(eventoId));
        System.out.println(core.planoConIds(eventoId));

        int cantidad = pedirOpcion("Cuantos asientos desea?", 1, MAX_ASIENTOS_POR_VENTA);

        System.out.println("Metodo de seleccion:");
        System.out.println("1) Por etiqueta (ej: A3,A4 o A3-A6)");
        System.out.println("2) Por ID (ej: 3,4,5 o 3-6)");
        System.out.println("3) Autocontiguos en una fila");
        int metodo = pedirOpcion("Opcion", 1, 3);

        int[] ids = new int[cantidad];

        if (metodo == 1) {
            // Lista/rango de etiquetas
            while (true) {
                String s = pedirTextoNoVacio("Etiquetas: ");
                int[] tmp = parseEtiquetasLista(eventoId, s);
                if (tmp.length != cantidad) {
                    System.out.println("Debe ingresar exactamente " + cantidad + " asientos.");
                    continue;
                }
                ids = tmp; break;
            }
        } else if (metodo == 2) {
            // Lista/rango de IDs
            while (true) {
                String s = pedirTextoNoVacio("IDs: ");
                int[] tmp = parseIdsLista(s);
                if (tmp.length != cantidad) {
                    System.out.println("Debe ingresar exactamente " + cantidad + " asientos.");
                    continue;
                }
                ids = tmp; break;
            }
        } else {
            // Búsqueda automática de contiguos en fila
            var e = core.obtenerEventoPorId(eventoId);
            int fila = pedirOpcion("Fila (A=1,B=2,...)", 1, e.getFilas()) - 1;
            List<Integer> pack = core.contiguosEnFila(eventoId, fila, cantidad);
            if (pack.isEmpty()) {
                System.out.println("No hay " + cantidad + " contiguos en esa fila.");
                return;
            }
            for (int i = 0; i < cantidad; i++) ids[i] = pack.get(i);
        }

        // Resumen previo a confirmar
        List<String> etiq = new ArrayList<>();
        for (int id : ids) etiq.add(core.etiquetaDesdeId(eventoId, id));
        var e = core.obtenerEventoPorId(eventoId);
        var c = core.buscarClientePorId(clienteId);
        double bruto = e.getPrecioBase() * ids.length;
        double desc  = Math.round(bruto * DescuentoService.factor(c.getTipo()) * 100.0) / 100.0;
        double neto  = Math.round((bruto - desc) * 100.0) / 100.0;

        System.out.println("Resumen:");
        System.out.println("- Evento: " + e.getNombre());
        System.out.println("- Cliente: " + c.getNombre() + " (" + c.getTipo() + ")");
        System.out.println("- Asientos: " + etiq + " (IDs " + Arrays.toString(ids) + ")");
        System.out.println("- Bruto: " + bruto + "  Descuento: " + desc + "  Total: " + neto);

        String conf = pedirTextoNoVacio("Confirmar venta? (S/N): ").toUpperCase(Locale.ROOT);
        if (!conf.startsWith("S")) { System.out.println("Venta cancelada."); return; }

        try {
            Venta v = core.venderEntradas(eventoId, clienteId, ids);
            System.out.println("Venta realizada: " + v);
            System.out.println(core.planoAscii(eventoId));
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    /**
     * Gestión de clientes (listar, crear, actualizar, eliminar, compactar).
     */
    private static void menuClientes() {
        while (true) {
            System.out.println("\n-- Gestion de Clientes --");
            System.out.println("1) Listar");
            System.out.println("2) Agregar");
            System.out.println("3) Actualizar");
            System.out.println("4) Eliminar");
            System.out.println("5) Compactar");
            System.out.println("0) Volver");
            int op = pedirOpcion("Opcion", 0, 5);

            switch (op) {
                case 1 -> { // Listar
                    for (Cliente c : core.getClientes())
                        if (c != null) System.out.println(c);
                }
                case 2 -> { // Agregar
                    String n = pedirTextoNoVacio("Nombre: ");
                    System.out.println("Tipo: 1) GENERAL 2) ESTUDIANTE 3) TERCERA_EDAD");
                    int t = pedirOpcion("Opcion", 1, 3);
                    TipoCliente tc = (t == 2) ? TipoCliente.ESTUDIANTE
                                              : (t == 3 ? TipoCliente.TERCERA_EDAD : TipoCliente.GENERAL);
                    try { System.out.println("Creado: " + core.altaCliente(n, tc)); }
                    catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
                }
                case 3 -> { // Actualizar
                    Integer id = elegirCliente(); if (id == null) break;
                    String n = pedirTextoNoVacio("Nuevo nombre: ");
                    System.out.println("Nuevo tipo: 1) GENERAL 2) ESTUDIANTE 3) TERCERA_EDAD 4) Sin cambio");
                    int t = pedirOpcion("Opcion", 1, 4);
                    TipoCliente nt = (t == 4) ? null
                                              : (t == 2 ? TipoCliente.ESTUDIANTE
                                                        : (t == 3 ? TipoCliente.TERCERA_EDAD : TipoCliente.GENERAL));
                    System.out.println(core.actualizarCliente(id, n, nt) ? "Actualizado." : "No encontrado.");
                }
                case 4 -> { // Eliminar
                    Integer id = elegirCliente(); if (id == null) break;
                    System.out.println(core.bajaCliente(id) ? "Eliminado." : "No encontrado.");
                }
                case 5 -> { // Compactar
                    core.compactarClientes();
                    System.out.println("Compactado.");
                }
                case 0 -> { return; }
            }
        }
    }

    /**
     * Gestión de eventos (listar, crear, renombrar, cambiar precio, eliminar).
     */
    private static void menuEventos() {
        while (true) {
            System.out.println("\n-- Gestion de Eventos --");
            System.out.println("1) Listar");
            System.out.println("2) Crear");
            System.out.println("3) Renombrar");
            System.out.println("4) Cambiar precio");
            System.out.println("5) Eliminar (sin ventas)");
            System.out.println("0) Volver");
            int op = pedirOpcion("Opcion", 0, 5);

            switch (op) {
                case 1 -> // Listar con libres/total
                    core.getEventos().forEach(e -> {
                        int libres = core.libres(e);
                        System.out.println(e + " [libres " + libres + "/" + e.capacidad() + "]");
                    });

                case 2 -> { // Crear
                    String n = pedirTextoNoVacio("Nombre: ");
                    int f = pedirOpcion("Filas (<= base)", 1, 26);
                    int k = pedirOpcion("Columnas (<= base)", 1, 50);
                    double p = pedirDouble("Precio base: ");
                    try { System.out.println("Creado: " + core.crearEvento(n, f, k, p)); }
                    catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
                }

                case 3 -> { // Renombrar
                    Integer id = elegirEvento(); if (id == null) break;
                    String n = pedirTextoNoVacio("Nuevo nombre: ");
                    System.out.println(core.renombrarEvento(id, n) ? "Renombrado." : "No encontrado.");
                }

                case 4 -> { // Cambiar precio
                    Integer id = elegirEvento(); if (id == null) break;
                    double p = pedirDouble("Nuevo precio: ");
                    System.out.println(core.cambiarPrecioEvento(id, p) ? "Actualizado." : "No encontrado.");
                }

                case 5 -> { // Eliminar (sin ventas)
                    Integer id = elegirEvento(); if (id == null) break;
                    System.out.println(core.eliminarEventoSinVentas(id)
                            ? "Eliminado."
                            : "No se puede eliminar (no existe o tiene ventas).");
                }

                case 0 -> { return; }
            }
        }
    }

    /**
     * Reporte sintetizado de un evento (ocupación, ventas, libres).
     */
    private static void menuReportes() {
        Integer id = elegirEvento(); if (id == null) return;
        System.out.println(core.reporte(id));
    }

    /**
     * Prueba rápida:
     *  - Pide un evento
     *  - Toma el primer cliente
     *  - Intenta vender IDs {1,2,3}
     *  - Muestra plano y reporte
     */
    private static void pruebasRapidas() {
        Integer id = elegirEvento(); if (id == null) return;
        Integer cli = null;
        for (Cliente c : core.getClientes()) if (c != null) { cli = c.getId(); break; }
        if (cli == null) { System.out.println("Sin clientes."); return; }

        try { core.venderEntradas(id, cli, new int[]{1, 2, 3}); }
        catch (Exception e) { System.out.println("Prueba: " + e.getMessage()); }

        System.out.println(core.planoAscii(id));
        System.out.println(core.reporte(id));
    }
}

