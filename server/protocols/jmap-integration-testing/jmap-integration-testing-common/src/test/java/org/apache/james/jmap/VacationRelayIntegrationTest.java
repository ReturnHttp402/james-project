/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap;

import static org.hamcrest.Matchers.equalTo;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.smtp.SMTPClient;
import org.apache.james.GuiceJamesServer;
import org.apache.james.dnsservice.api.InMemoryDNSService;
import org.apache.james.jmap.api.vacation.AccountId;
import org.apache.james.jmap.api.vacation.VacationPatch;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.store.probe.MailboxProbe;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.probe.DataProbe;
import org.apache.james.util.docker.Images;
import org.apache.james.util.docker.SwarmGenericContainer;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.FakeSmtpHelper;
import org.apache.james.utils.JmapGuiceProbe;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.wait.HostPortWaitStrategy;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import com.jayway.restassured.RestAssured;

public abstract class VacationRelayIntegrationTest {

    private static final String DOMAIN = "mydomain.tld";
    private static final String USER = "benwa";
    private static final String USER_WITH_DOMAIN = USER + '@' + DOMAIN;
    private static final String PASSWORD = "secret";
    private static final String REASON = "Message explaining my wonderful vacations";

    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final int SMTP_PORT = 1025;
    private static final int REST_SMTP_SINK_PORT = 25;

    @Rule
    public final SwarmGenericContainer fakeSmtp = new SwarmGenericContainer(Images.FAKE_SMTP)
        .withExposedPorts(REST_SMTP_SINK_PORT)
        .waitingFor(new HostPortWaitStrategy());


    private ConditionFactory calmlyAwait;
    private GuiceJamesServer guiceJamesServer;
    private JmapGuiceProbe jmapGuiceProbe;

    protected abstract void await();

    protected abstract GuiceJamesServer getJmapServer();

    protected abstract InMemoryDNSService getInMemoryDns();

    @Before
    public void setUp() throws Exception {

        InetAddress containerIp = InetAddress.getByName(fakeSmtp.getContainerIp());
        getInMemoryDns()
            .registerRecord("yopmail.com", containerIp, "yopmail.com");

        guiceJamesServer = getJmapServer();
        guiceJamesServer.start();

        DataProbe dataProbe = guiceJamesServer.getProbe(DataProbeImpl.class);
        dataProbe.addDomain(DOMAIN);
        dataProbe.addUser(USER_WITH_DOMAIN, PASSWORD);
        MailboxProbe mailboxProbe = guiceJamesServer.getProbe(MailboxProbeImpl.class);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USER_WITH_DOMAIN, DefaultMailboxes.SENT);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USER_WITH_DOMAIN, DefaultMailboxes.INBOX);
        await();

        jmapGuiceProbe = guiceJamesServer.getProbe(JmapGuiceProbe.class);

        RestAssured.requestSpecification = FakeSmtpHelper.requestSpecification(fakeSmtp.getContainerIp());

        Duration slowPacedPollInterval = Duration.FIVE_HUNDRED_MILLISECONDS;
        calmlyAwait = Awaitility
            .with()
            .pollInterval(slowPacedPollInterval)
            .and()
            .pollDelay(slowPacedPollInterval).await();

        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> fakeSmtp.tryConnect(25));
    }

    @After
    public void teardown() {
        guiceJamesServer.stop();
    }

    @Test
    public void forwardingAnEmailShouldWork() throws Exception {
        jmapGuiceProbe.modifyVacation(AccountId.fromString(USER_WITH_DOMAIN), VacationPatch
            .builder()
            .isEnabled(true)
            .textBody(REASON)
            .build());

        String externalMail = "ray@yopmail.com";

        SMTPClient smtpClient = new SMTPClient();
        smtpClient.connect(LOCALHOST_IP, SMTP_PORT);
        smtpClient.helo(DOMAIN);
        smtpClient.setSender(externalMail);
        smtpClient.rcpt("<" + USER_WITH_DOMAIN + ">");
        smtpClient.sendShortMessageData("content");

        calmlyAwait.atMost(1, TimeUnit.MINUTES)
            .until(() ->
                FakeSmtpHelper.isReceived(response -> response
                    .body("[0].from", equalTo(USER_WITH_DOMAIN))
                    .body("[0].to[0]", equalTo(externalMail))
                    .body("[0].text", equalTo(REASON))));
    }
}
