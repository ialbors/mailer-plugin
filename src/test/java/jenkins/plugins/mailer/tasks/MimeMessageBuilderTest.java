/*
 * The MIT License
 *
 * Copyright (c) 2013-2014, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.mailer.tasks;

import hudson.tasks.Mailer;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.mock_javamail.Mailbox;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class MimeMessageBuilderTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setup() {
        JenkinsLocationConfiguration.get().setAdminAddress("tom.aaaa@gmail.com");
        Mailer.descriptor().setReplyToAddress("tom.aaaa@gmail.com");
    }

    @Test
    public void test_construction() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder();

        messageBuilder.addRecipients("tom.xxxx@gmail.com, tom.yyyy@gmail.com");
        MimeMessage mimeMessage = messageBuilder.buildMimeMessage();

        // check from and reply-to
        Address[] from = mimeMessage.getFrom();
        Assert.assertNotNull(from);
        Assert.assertEquals(1, from.length);
        Assert.assertEquals("tom.aaaa@gmail.com", from[0].toString());
        Address[] replyTo = mimeMessage.getReplyTo();
        Assert.assertNotNull(from);
        Assert.assertEquals(1, replyTo.length);
        Assert.assertEquals("tom.aaaa@gmail.com", replyTo[0].toString());

        // check the recipient list...
        Address[] allRecipients = mimeMessage.getAllRecipients();
        Assert.assertNotNull(allRecipients);
        Assert.assertEquals(2, allRecipients.length);
        Assert.assertEquals("tom.xxxx@gmail.com", allRecipients[0].toString());
        Assert.assertEquals("tom.yyyy@gmail.com", allRecipients[1].toString());

        // Make sure we can regen the instance identifier public key
        String encodedIdent = mimeMessage.getHeader("X-Instance-Identity")[0];
        byte[] image = Base64.decodeBase64(encodedIdent);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(image));
        Assert.assertNotNull(publicKey);
    }

    @Test
    public void test_send() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder();

        messageBuilder
                .addRecipients("tom.xxxx@jenkins.com")
                .setSubject("Hello")
                .setBody("Testing email");

        MimeMessage mimeMessage = messageBuilder.buildMimeMessage();

        Mailbox.clearAll();
        Transport.send(mimeMessage);

        Mailbox mailbox = Mailbox.get("tom.xxxx@jenkins.com");
        Assert.assertEquals(1, mailbox.getNewMessageCount());
        Message message = mailbox.get(0);
        Assert.assertEquals("Hello", message.getSubject());
        Assert.assertEquals("Testing email", ((MimeMultipart)message.getContent()).getBodyPart(0).getContent().toString());
    }
}
