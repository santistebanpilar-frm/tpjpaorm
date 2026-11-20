package com.tp.facturacion;

import com.tp.facturacion.model.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Date;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            // 1) Usuario (primero, porque toda la auditoria lo referencia)
            Usuario admin = new Usuario();
            admin.setUsuario("admin");
            admin.setClave("1234");
            admin.setNombre("Admin");
            admin.setApellido("Sistema");
            em.persist(admin);

            // 2) Entidades de soporte (sin cascada: se persisten una por una)
            PuntoVenta puntoVenta = new PuntoVenta();
            puntoVenta.setNumero(1);
            puntoVenta.setDescripcion("Casa central");
            puntoVenta.setTipoEmision("CAE");
            puntoVenta.setDomicilioComercial("San Martin 123, Mendoza");
            auditar(puntoVenta, admin);
            em.persist(puntoVenta);

            Rubro rubro = new Rubro();
            rubro.setDenominacion("Informatica");
            rubro.setCodigo(10);
            auditar(rubro, admin);
            em.persist(rubro);

            Marca marca = new Marca();
            marca.setDenominacion("Logitech");
            marca.setCodigo(20);
            auditar(marca, admin);
            em.persist(marca);

            Articulo articulo = new Articulo();
            articulo.setRubro(rubro);
            articulo.setMarca(marca);
            articulo.setCodigo("ART-001");
            articulo.setDenominacion("Mouse inalambrico");
            auditar(articulo, admin);
            em.persist(articulo);

            ListaPrecio listaPrecio = new ListaPrecio();
            listaPrecio.setCodigo("LP-01");
            listaPrecio.setDenominacion("Lista minorista");
            auditar(listaPrecio, admin);
            em.persist(listaPrecio);

            ListaPrecioArticulo lpa = new ListaPrecioArticulo();
            lpa.setListaPrecio(listaPrecio);
            lpa.setArticulo(articulo);
            lpa.setPrecioVenta(15000.0);
            auditar(lpa, admin);
            em.persist(lpa);

            // 3) Cabecera de la factura
            FacturaVenta factura = new FacturaVenta();
            factura.setNumero(1L);
            factura.setFechaEmision(new Date());
            factura.setPuntoVenta(puntoVenta);
            factura.setEstado("EMITIDA");
            auditar(factura, admin);

            // 4) Detalles asociados bidireccionalmente (agregarDetalle setea detalle.factura)
            double total = 0;
            for (int i = 1; i <= 2; i++) {
                FacturaVentaDetalle d = new FacturaVentaDetalle();
                d.setListaPrecioArticulo(lpa);
                d.setDescripcion(articulo.getDenominacion());
                d.setCantidad(i);
                d.setPrecioUnitario(lpa.getPrecioVenta());
                d.setPorcentajeBonificacion(0);
                d.setImporteNeto(d.getCantidad() * d.getPrecioUnitario());
                d.setImporteIva(d.getImporteNeto() * 0.21);
                d.setImporteSubtotal(d.getImporteNeto() + d.getImporteIva());
                factura.agregarDetalle(d);
                total += d.getImporteSubtotal();
            }
            factura.setImporteTotal(total);
            factura.setImporteCobrado(0);
            factura.setImporteSaldo(total);

            // 5) REQUISITO CLAVE: un unico persist de la cabecera.
            //    CascadeType.ALL inserta tambien todos los detalles.
            em.persist(factura);

            em.getTransaction().commit();
            System.out.println(">>> Factura id=" + factura.getId()
                    + " guardada con " + factura.getDetalles().size() + " detalles.");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }

    /** Completa los campos obligatorios de auditoria. */
    private static void auditar(AuditoriaApp entidad, Usuario usuario) {
        Date ahora = new Date();
        entidad.setFechaAlta(ahora);
        entidad.setFechaModificacion(ahora);
        entidad.setUsuarioCarga(usuario);
        entidad.setUsuarioModificacion(usuario);
    }
}
