package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import ar.edu.utn.turnos.shared.notificacion.domain.ErrorEnvioNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionPort;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adaptador en memoria para pruebas y para el perfil {@code test}: no abre SMTP. Guarda los envíos
 * exitosos, cuenta todos los intentos (incluidos los fallidos) y permite forzar fallos y bloquear un
 * envío para verificar la exclusión mutua del planificador sin depender de tiempos reales.
 */
public class InMemoryNotificacionAdapter implements NotificacionPort {

    private final List<MensajeNotificacion> enviados = new CopyOnWriteArrayList<>();
    private final AtomicInteger intentos = new AtomicInteger();
    private final AtomicInteger fallosPendientes = new AtomicInteger();
    private volatile CountDownLatch envioIniciado;
    private volatile CountDownLatch continuarEnvio;

    @Override
    public void enviar(MensajeNotificacion mensaje) {
        intentos.incrementAndGet();
        esperarSiEstaBloqueado();
        if (consumirFalloPendiente()) {
            throw new ErrorEnvioNotificacion("Fallo simulado de envío");
        }
        enviados.add(mensaje);
    }

    /** Fuerza que los próximos {@code cantidad} envíos fallen antes de registrar el mensaje. */
    public void fallarProximos(int cantidad) {
        fallosPendientes.set(Math.max(cantidad, 0));
    }

    /** Cantidad de envíos exitosos registrados. */
    public List<MensajeNotificacion> enviados() {
        return List.copyOf(enviados);
    }

    /** Cantidad total de intentos, exitosos y fallidos. */
    public int intentos() {
        return intentos.get();
    }

    /** Deja el adaptador limpio entre pruebas. */
    public void limpiar() {
        enviados.clear();
        intentos.set(0);
        fallosPendientes.set(0);
        envioIniciado = null;
        continuarEnvio = null;
    }

    /**
     * Bloquea el próximo envío: avisa con {@code iniciado} cuando entró y espera {@code continuar}.
     * Sirve para sostener el lock de fila mientras otra ejecución del planificador intenta reclamarla.
     */
    public void bloquearProximoEnvio(CountDownLatch iniciado, CountDownLatch continuar) {
        this.envioIniciado = iniciado;
        this.continuarEnvio = continuar;
    }

    private void esperarSiEstaBloqueado() {
        CountDownLatch iniciado = envioIniciado;
        CountDownLatch continuar = continuarEnvio;
        if (iniciado == null || continuar == null) {
            return;
        }
        envioIniciado = null;
        continuarEnvio = null;
        iniciado.countDown();
        try {
            continuar.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorEnvioNotificacion("Envío interrumpido", e);
        }
    }

    private boolean consumirFalloPendiente() {
        return fallosPendientes.getAndUpdate(restantes -> restantes > 0 ? restantes - 1 : 0) > 0;
    }
}
