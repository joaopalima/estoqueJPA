/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import dao.exceptions.NonexistentEntityException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import model.DetalheVenda;
import model.Produto;
import model.Venda;

/**
 *
 * @author jorge
 */
public class VendaJpaController implements Serializable {

    public VendaJpaController() {
        this.emf = Persistence.createEntityManagerFactory("EstoqueJpaPU");
    }

    public VendaJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Venda venda) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            em.persist(venda);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void createDetalheVenda(DetalheVenda detalhe) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            em.persist(detalhe);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Venda venda) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            venda = em.merge(venda);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = venda.getId();
                if (findVenda(id) == null) {
                    throw new NonexistentEntityException("A venda com id " + id + " não existe mais.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Long id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Venda venda;
            try {
                venda = em.getReference(Venda.class, id);
                venda.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("A venda com id " + id + " não existe mais.", enfe);
            }
            em.remove(venda);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Venda> findVendaEntities() {
        return findVendaEntities(true, -1, -1);
    }

    public List<Venda> findVendaEntities(int maxResults, int firstResult) {
        return findVendaEntities(false, maxResults, firstResult);
    }

    private List<Venda> findVendaEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Venda.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Venda findVenda(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Venda.class, id);
        } finally {
            em.close();
        }
    }

    public int getVendaCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Venda> rt = cq.from(Venda.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

    public void adicionarProduto(Venda venda, Produto produto, Integer quantidade) {

        DetalheVenda dv = new DetalheVenda();
        dv.setVenda(venda);
        dv.setProduto(produto);
        dv.setQuantidade(quantidade);
        dv.setValorTotal(produto.getPrecoVenda());

        this.createDetalheVenda(dv);

        //Atualizando o estoque 
        produto.removerEstoque(quantidade);

        try {
            new ProdutoJpaController().edit(produto);
        } catch (Exception ex) {
            Logger.getLogger(VendaJpaController.class.getName()).log(Level.SEVERE, null, ex);
        }

        venda.atualizarValorTotal(produto.getPrecoVenda(), quantidade);

        try {
            this.edit(venda);
        } catch (Exception ex) {
            Logger.getLogger(VendaJpaController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<DetalheVenda> getDetalheDeUmaVenda(Venda venda){
    
        EntityManager em = getEntityManager();
        Query q = em.createQuery("Select dv from DetalheVenda dv where dv.venda.id = :id");
        q.setParameter("id", venda.getId());

        return q.getResultList();
    }
}

