/*
 * Copyright (C) 2005-2010 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */
package com.manydesigns.portofino.email;

import com.manydesigns.elements.fields.search.Criteria;
import com.manydesigns.elements.logging.LogUtil;
import com.manydesigns.elements.reflection.ClassAccessor;
import com.manydesigns.elements.reflection.JavaClassAccessor;
import com.manydesigns.portofino.PortofinoProperties;
import com.manydesigns.portofino.context.Context;
import com.manydesigns.portofino.systemModel.email.EmailBean;
import com.manydesigns.portofino.systemModel.users.User;

import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
*/


public class EmailTask extends TimerTask {
    public static final String copyright =
            "Copyright (c) 2005-2010, ManyDesigns srl";

    protected static final int N_THREADS=5;
    protected static ExecutorService outbox = Executors.newFixedThreadPool
            (N_THREADS);
    protected static final ConcurrentLinkedQueue<EmailSender> successQueue
            = new ConcurrentLinkedQueue<EmailSender>();
    protected static final ConcurrentLinkedQueue<EmailSender> rejectedQueue
            = new ConcurrentLinkedQueue<EmailSender>();
    protected final POP3Client client;
    protected static final Logger logger =
            LogUtil.getLogger(TimerTask.class);
    protected final Context context;


    public EmailTask(Context context) {
        this.context = context;

        //Setto il client smtp per il bouncing
        String smtpHost = PortofinoProperties.getProperties()
                .getProperty("mail.smtp.host", "127.0.0.1");
        String smtpProtocol = PortofinoProperties.getProperties()
                .getProperty("mail.smtp.protocol", "pop3");
        int smtpPort = Integer.parseInt(PortofinoProperties.getProperties()
                .getProperty("mail.smtp.port", "25"));
        String smtpLogin = PortofinoProperties.getProperties()
                .getProperty("mail.smtp.login");
        String smtpPassword = PortofinoProperties.getProperties()
                .getProperty("mail.smtp.password");
        boolean bounceEnabled = Boolean.parseBoolean(PortofinoProperties.getProperties()
                .getProperty("mail.bounce.enabled", "false"));
        boolean sslEnabled = Boolean.parseBoolean(PortofinoProperties.getProperties()
                .getProperty("mail.smtp.ssl.enabled", "false"));
        if (bounceEnabled &&
                smtpHost != null && smtpProtocol != null && smtpLogin != null
                && smtpPassword != null) {
            if (sslEnabled) {
                client = new POP3SSLClient(smtpHost, smtpProtocol, smtpPort, smtpLogin,
                        smtpPassword);
            } else {
                client = new POP3SimpleClient(smtpHost, smtpProtocol, smtpPort, smtpLogin,
                        smtpPassword);
            }
        } else {
            client = null;
        }
    }

    public static void stop() {
        outbox.shutdownNow();
    }


    public void run() {

        synchronized (this) {
            createQueue();
            checkBounce();
            manageSuccessAndRejected();
        }
    }

    public synchronized void createQueue() {
        try {
            ClassAccessor accessor = context.getTableAccessor(
                    EmailManager.EMAILQUEUE_TABLE);
            Criteria criteria = new Criteria(accessor);
            List<Object> emails = context.getObjects(
                    criteria.eq(accessor.getProperty("state"),
                            EmailManager.TOBESENT));
            for (Object obj : emails) {

                EmailSender emailSender = new EmailSender((EmailBean) obj);
            outbox.submit(emailSender);
            }
        } catch (NoSuchFieldException e) {
            LogUtil.warning(logger,"No state field in emailQueue",e);
        }
    }


    private synchronized void manageSuccessAndRejected() {

            while (!successQueue.isEmpty()) {
                EmailSender email = successQueue.poll();
                context.deleteObject(EmailManager.EMAILQUEUE_TABLE,
                        email.getEmailBean());
                context.commit("portofino");
            }


        while (!rejectedQueue.isEmpty()) {
            EmailSender email = rejectedQueue.poll();
            LogUtil.finestMF(logger, "Adding reject mail with id:"
                    + email.getEmailBean().getId());
            outbox.submit(email);
        }
    }

    private synchronized void checkBounce() {
        if (client != null) {
            Set<String> emails = client.read();
            for (String email : emails) {
                incrementBounce(email);
            }
        }
    }

    private void incrementBounce(String email) {
        try {
            ClassAccessor accessor = JavaClassAccessor.getClassAccessor(User.class);
            Criteria criteria = new Criteria(accessor);
            List<Object> users = context.getObjects(
                    criteria.gt(accessor.getProperty("email"), email));
            if (users.size()==0){
                LogUtil.warningMF(logger,"no user found for email {0}", email);
                return;
            }
            User user = (User) users.get(0);
            Integer value = user.getBounced();
            if (null==value){
                value = 1;
            } else {
                value++;
            }
            user.setBounced(value);
            context.saveObject("portofino.user_", user);
        } catch (NoSuchFieldException e) {
            LogUtil.warning(logger,"cannot increment bounce for user", e);
        }
    }
}
