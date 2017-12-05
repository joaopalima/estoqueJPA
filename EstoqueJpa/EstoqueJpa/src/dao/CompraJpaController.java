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
import model.Compra;
import model.DetalheCompra;
import model.Produto;

/**
 *
 * @author jorge
 */
public class CompraJpaController implements Serializable {

    public CompraJpaController() {
        this.emf = Persistence.createEntityManagerFactory("EstoqueJpaPU");
    }

    public CompraJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Compra compra) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            em.persist(compra);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    public void createDetalheCompra(DetalheCompra detalhe) {
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

    public void edit(Compra compra) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            compra = em.merge(compra);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = compra.getId();
                if (findCompra(id) == null) {
                    throw new NonexistentEntityException("A compra com id " + id + " não existe mais.");
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
            Compra compra;
            try {
                compra = em.getReference(Compra.class, id);
                compra.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("A compra com id " + id + " não existe mais.", enfe);
            }
            em.remove(compra);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Compra> findCompraEntities() {
        return findCompraEntities(true, -1, -1);
    }

    public List<Compra> findCompraEntities(int maxResults, int firstResult) {
        return findCompraEntities(false, maxResults, firstResult);
    }

    private List<Compra> findCompraEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Compra.class));
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

    public Compra findCompra(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Compra.class, id);
        } finally {
            em.close();
        }
    }

    public int getCompraCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Compra> rt = cq.from(Compra.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
    
    public void adicionarProduto(Compra compra, Produto produto, Integer quantidade) {
      
        
//Criando o detalhe de compra
        DetalheCompra dc = new DetalheCompra();
        dc.setCompra(compra);
        dc.setProduto(produto);
        dc.setQuantidade(quantidade);
        dc.setValorTotal(produto.getPrecoVenda());
        
        this.createDetalheCompra(dc);
        
        //Atualizando o estoque 
        produto.adicionarEstoque(quantidade);
        
        try {
            new ProdutoJpaController().edit(produto);
        } catch (Exception ex) {
            Logger.getLogger(CompraJpaController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Atualizando o valor total da nota fiscal
        compra.atualizarValorTotal(produto.getPrecoVenda(), quantidade);
        
        try {
            this.edit(compra);
        } catch (Exception ex) {
            Logger.getLogger(CompraJpaController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
    }
    
    
    public List<DetalheCompra> getDetalhesDeUmaCompra(Compra compra) {
        EntityManager em = getEntityManager();
        
        Query q = em.createQuery("Select dc from DetalheCompra dc where dc.compra.id = :id");
        q.setParameter("id", compra.getId());
        
        return q.getResultList();
    }

}
