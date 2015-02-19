package org.jboss.demo.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.jboss.demo.model.Beer;

/**
 * Backing bean for Beer entities.
 * <p/>
 * This class provides CRUD functionality for all Beer entities. It focuses
 * purely on Java EE 6 standards (e.g. <tt>&#64;ConversationScoped</tt> for
 * state management, <tt>PersistenceContext</tt> for persistence,
 * <tt>CriteriaBuilder</tt> for searches) rather than introducing a CRUD framework or
 * custom base class.
 */

@Named
@Stateful
@ConversationScoped
public class BeerBean implements Serializable
{

   private static final long serialVersionUID = 1L;

   /*
    * Support creating and retrieving Beer entities
    */

   private Long id;

   public Long getId()
   {
      return this.id;
   }

   public void setId(Long id)
   {
      this.id = id;
   }

   private Beer beer;

   public Beer getBeer()
   {
      return this.beer;
   }

   public void setBeer(Beer beer)
   {
      this.beer = beer;
   }

   @Inject
   private Conversation conversation;

   @PersistenceContext(unitName = "beer-rateing-persistance-unit", type = PersistenceContextType.EXTENDED)
   private EntityManager entityManager;

   public String create()
   {

      this.conversation.begin();
      this.conversation.setTimeout(1800000L);
      return "create?faces-redirect=true";
   }

   public void retrieve()
   {

      if (FacesContext.getCurrentInstance().isPostback())
      {
         return;
      }

      if (this.conversation.isTransient())
      {
         this.conversation.begin();
         this.conversation.setTimeout(1800000L);
      }

      if (this.id == null)
      {
         this.beer = this.example;
      }
      else
      {
         this.beer = findById(getId());
      }
   }

   public Beer findById(Long id)
   {

      return this.entityManager.find(Beer.class, id);
   }

   /*
    * Support updating and deleting Beer entities
    */

   public String update()
   {
      this.conversation.end();

      try
      {
         if (this.id == null)
         {
            this.entityManager.persist(this.beer);
            return "search?faces-redirect=true";
         }
         else
         {
            this.entityManager.merge(this.beer);
            return "view?faces-redirect=true&id=" + this.beer.getId();
         }
      }
      catch (Exception e)
      {
         FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
         return null;
      }
   }

   public String delete()
   {
      this.conversation.end();

      try
      {
         Beer deletableEntity = findById(getId());

         this.entityManager.remove(deletableEntity);
         this.entityManager.flush();
         return "search?faces-redirect=true";
      }
      catch (Exception e)
      {
         FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
         return null;
      }
   }

   /*
    * Support searching Beer entities with pagination
    */

   private int page;
   private long count;
   private List<Beer> pageItems;

   private Beer example = new Beer();

   public int getPage()
   {
      return this.page;
   }

   public void setPage(int page)
   {
      this.page = page;
   }

   public int getPageSize()
   {
      return 10;
   }

   public Beer getExample()
   {
      return this.example;
   }

   public void setExample(Beer example)
   {
      this.example = example;
   }

   public String search()
   {
      this.page = 0;
      return null;
   }

   public void paginate()
   {

      CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();

      // Populate this.count

      CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
      Root<Beer> root = countCriteria.from(Beer.class);
      countCriteria = countCriteria.select(builder.count(root)).where(
            getSearchPredicates(root));
      this.count = this.entityManager.createQuery(countCriteria)
            .getSingleResult();

      // Populate this.pageItems

      CriteriaQuery<Beer> criteria = builder.createQuery(Beer.class);
      root = criteria.from(Beer.class);
      TypedQuery<Beer> query = this.entityManager.createQuery(criteria
            .select(root).where(getSearchPredicates(root)));
      query.setFirstResult(this.page * getPageSize()).setMaxResults(
            getPageSize());
      this.pageItems = query.getResultList();
   }

   private Predicate[] getSearchPredicates(Root<Beer> root)
   {

      CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
      List<Predicate> predicatesList = new ArrayList<Predicate>();

      String name = this.example.getName();
      if (name != null && !"".equals(name))
      {
         predicatesList.add(builder.like(builder.lower(root.<String> get("name")), '%' + name.toLowerCase() + '%'));
      }
      String taste = this.example.getTaste();
      if (taste != null && !"".equals(taste))
      {
         predicatesList.add(builder.like(builder.lower(root.<String> get("taste")), '%' + taste.toLowerCase() + '%'));
      }
      int score = this.example.getScore();
      if (score != 0)
      {
         predicatesList.add(builder.equal(root.get("score"), score));
      }

      return predicatesList.toArray(new Predicate[predicatesList.size()]);
   }

   public List<Beer> getPageItems()
   {
      return this.pageItems;
   }

   public long getCount()
   {
      return this.count;
   }

   /*
    * Support listing and POSTing back Beer entities (e.g. from inside an
    * HtmlSelectOneMenu)
    */

   public List<Beer> getAll()
   {

      CriteriaQuery<Beer> criteria = this.entityManager
            .getCriteriaBuilder().createQuery(Beer.class);
      return this.entityManager.createQuery(
            criteria.select(criteria.from(Beer.class))).getResultList();
   }

   @Resource
   private SessionContext sessionContext;

   public Converter getConverter()
   {

      final BeerBean ejbProxy = this.sessionContext.getBusinessObject(BeerBean.class);

      return new Converter()
      {

         @Override
         public Object getAsObject(FacesContext context,
               UIComponent component, String value)
         {

            return ejbProxy.findById(Long.valueOf(value));
         }

         @Override
         public String getAsString(FacesContext context,
               UIComponent component, Object value)
         {

            if (value == null)
            {
               return "";
            }

            return String.valueOf(((Beer) value).getId());
         }
      };
   }

   /*
    * Support adding children to bidirectional, one-to-many tables
    */

   private Beer add = new Beer();

   public Beer getAdd()
   {
      return this.add;
   }

   public Beer getAdded()
   {
      Beer added = this.add;
      this.add = new Beer();
      return added;
   }
}
