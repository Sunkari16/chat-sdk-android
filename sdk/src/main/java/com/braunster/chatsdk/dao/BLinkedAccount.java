package com.braunster.chatsdk.dao;

import com.braunster.chatsdk.dao.DaoSession;
import de.greenrobot.dao.DaoException;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table BLINKED_ACCOUNT.
 */
public class BLinkedAccount extends Entity  {

    private String entityID;
    /** Not-null value. */
    private String authentication_id;
    private String user;

    /** Used to resolve relations */
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    private transient BLinkedAccountDao myDao;

    private BUser bUser;
    private String bUser__resolvedKey;


    public BLinkedAccount() {
    }

    public BLinkedAccount(String entityID) {
        this.entityID = entityID;
    }

    public BLinkedAccount(String entityID, String authentication_id, String user) {
        this.entityID = entityID;
        this.authentication_id = authentication_id;
        this.user = user;
    }

    /** called by internal mechanisms, do not call yourself. */
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getBLinkedAccountDao() : null;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    /** Not-null value. */
    public String getAuthentication_id() {
        return authentication_id;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setAuthentication_id(String authentication_id) {
        this.authentication_id = authentication_id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /** To-one relationship, resolved on first access. */
    public BUser getBUser() {
        String __key = this.user;
        if (bUser__resolvedKey == null || bUser__resolvedKey != __key) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            BUserDao targetDao = daoSession.getBUserDao();
            BUser bUserNew = targetDao.load(__key);
            synchronized (this) {
                bUser = bUserNew;
            	bUser__resolvedKey = __key;
            }
        }
        return bUser;
    }

    public void setBUser(BUser bUser) {
        synchronized (this) {
            this.bUser = bUser;
            user = bUser == null ? null : bUser.getEntityID();
            bUser__resolvedKey = user;
        }
    }

    /** Convenient call for {@link AbstractDao#delete(Object)}. Entity must attached to an entity context. */
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.delete(this);
    }

    /** Convenient call for {@link AbstractDao#update(Object)}. Entity must attached to an entity context. */
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.update(this);
    }

    /** Convenient call for {@link AbstractDao#refresh(Object)}. Entity must attached to an entity context. */
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.refresh(this);
    }

}
