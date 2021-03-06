package com.plter.jus.auth.funcs.user;

import com.plter.jus.auth.Function;
import com.plter.jus.auth.tools.AttrTool;
import com.plter.jus.auth.tools.PasswordTool;
import com.plter.jus.db.DbConnection;
import com.plter.jus.db.entities.UsersEntity;
import com.plter.jus.errors.ErrorMessages;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by plter on 6/24/15.
 */
public class Login extends Function {


    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response) {

        do {

            String name = request.getParameter("name");
            if (name==null){
                request.setAttribute("errorMsg", ErrorMessages.NO_NAME);
                break;
            }
            String pass = request.getParameter("pass");
            if (pass==null){
                request.setAttribute("errorMsg", ErrorMessages.NO_PASS);
                break;
            }

            pass = PasswordTool.translatePassword(pass);

            UsersEntity user = null;

            Session session = DbConnection.openSession();
            List<UsersEntity> list = session.createCriteria(UsersEntity.class).add(Restrictions.eq("name", name)).add(Restrictions.eq("pass", pass)).list();
            if (list.size()>0){
                user = list.get(0);
            }
            if (user!=null){
                try {
                    Transaction transaction = session.beginTransaction();
                    user.setLastlogtime(Timestamp.valueOf(LocalDateTime.now()));
                    transaction.commit();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            session.close();

            if (user!=null){
                CurrentUser.setLogged(request, true);
                CurrentUser.setLoggedName(request, name);
                CurrentUser.setLoggedId(request, list.get(0).getId());

                String redirect = request.getParameter("redirect");
                if (redirect!=null){
                    try {
                        redirect = URLDecoder.decode(redirect,"UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }else {
                    redirect = request.getContextPath();
                }

                try {
                    response.sendRedirect(redirect);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                request.setAttribute("errorMsg", "Fail to login");
            }
        }while (false);

    }


    static public class CurrentUser {

        static private final String KEY_LOGGED = "logged";
        static private final String KEY_LOGGED_ID = "loggedId";
        static private final String KEY_LOGGED_NAME = "loggedName";

        static public boolean isLogged(HttpServletRequest request){
            return AttrTool.getSessionValue(request.getSession(), KEY_LOGGED, false);
        }

        static public void setLogged(HttpServletRequest request,boolean logState){
            request.getSession().setAttribute(KEY_LOGGED,true);
        }

        static public void setLoggedId(HttpServletRequest request,long id){
            request.getSession().setAttribute(KEY_LOGGED_ID, id);
        }

        static public long getLoggedId(HttpServletRequest request){
            return AttrTool.getSessionValue(request.getSession(), KEY_LOGGED_ID, 0L);
        }

        static public void setLoggedName(HttpServletRequest request,String name){
            request.getSession().setAttribute(KEY_LOGGED_NAME, name);
        }

        static public String getLoggedName(HttpServletRequest request){
            return AttrTool.getSessionValue(request.getSession(), KEY_LOGGED_NAME, "");
        }
    }
}
