/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service.impl;

import dao.impl.PassengerDao;
import java.util.List;
import model.Passenger;

/**
 *
 * @author USER
 */
public class PassengerService {

    private final PassengerDao dao = new PassengerDao();

    public Passenger buscarPorDocumento(String documento) {
        return dao.findByDocument(documento);
    }

    public List<Passenger> listarPasajeros() {
        return dao.listAll();
    }

    public boolean crear(Passenger p) {
        return dao.insert(p);
    }

    public boolean actualizar(Passenger p) {
        return dao.update(p);
    }

    public boolean eliminar(int id) {
        return dao.delete(id);
    }

    public boolean existeDocumento(String doc, Integer excl) {
        return dao.existsByDocument(doc, excl);
    }

    public List<Passenger> buscar(String q) {
        return dao.search(q);
    }

}
