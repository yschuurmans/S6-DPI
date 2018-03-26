package Gateway;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.memory.list.MessageList;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

public class MessageReceiverGateway {

    private String channel;

    private Connection connection; // to connect to the Gateway
    private Session session; // session for creating consumers

    private Destination receiveDestination; //reference to a queue/topic destination
    private MessageConsumer consumer = null; // for receiving messages

    public MessageReceiverGateway(String channel) {
        this.channel = channel;
    }

    public MessageReceiverGateway() {
    }

    public void awaitReply(MessageListener listener) {
        startConnection(1, listener);
    }

    public void startConnection(MessageListener listener) {
        startConnection(0, listener);
    }

    public void startConnection(int expectedReplyCount, MessageListener listener) {

        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");

            // connect to the Destination called “myFirstChannel”
            // queue or topic: “queue.myFirstDestination” or “topic.myFirstDestination”
            props.put(("queue." + channel), channel);

            Context jndiContext = new InitialContext(props);
            ActiveMQConnectionFactory connectionFactory = (ActiveMQConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            connectionFactory.setTrustAllPackages(true);
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // connect to the receiver destination
            receiveDestination = (Destination) jndiContext.lookup(channel);
            consumer = session.createConsumer(receiveDestination);

            connection.start(); // this is needed to start receiving messages

        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }

        CreateListener(listener, consumer, expectedReplyCount);

    }

    private static void CreateListener(MessageListener listener, MessageConsumer consumer, int expectedReplyCount) {
        try {
            if (listener != null)
                consumer.setMessageListener(new MessageListener() {
                    private int replyCount;

                    @Override
                    public void onMessage(Message message) {
                        try {
                            listener.onMessage(message);
                            replyCount++;
                            if (expectedReplyCount > 0 && replyCount >= expectedReplyCount)
                                consumer.close();
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }
                });
            else {
                consumer.setMessageListener(new MessageListener() {
                    private int replyCount;

                    @Override
                    public void onMessage(Message msg) {
                        try {
                            System.out.println("received message: " + msg);
                            replyCount++;
                            if (expectedReplyCount > 0 && replyCount >= expectedReplyCount)
                                consumer.close();
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }


    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Destination getReceiveDestination() {
        return receiveDestination;
    }

    public void setReceiveDestination(Destination receiveDestination) {
        this.receiveDestination = receiveDestination;
    }

    public MessageConsumer getConsumer() {
        return consumer;
    }

    public void setConsumer(MessageConsumer consumer) {
        this.consumer = consumer;
    }
}
