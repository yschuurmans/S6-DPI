package Gateway;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

public class MessageReceiverGateway {

    private String channel;
    private String thisDestinationString;

    private Connection connection; // to connect to the Gateway
    private Session session; // session for creating consumers

    private Destination receiveDestination; //reference to a queue/topic destination
    private MessageConsumer consumer = null; // for receiving messages

    public MessageReceiverGateway(String channel, String thisDestinationString) {
        this.channel = channel;
        this.thisDestinationString = thisDestinationString;
    }

    public MessageReceiverGateway() {
    }

    public void AwaitReply(MessageListener listener) {
        try {
            connection.start();
            consumer = session.createConsumer(receiveDestination);
            CreateListener(listener, consumer, 1);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void startConnection(MessageListener listener) {

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
            receiveDestination = (Destination) jndiContext.lookup(thisDestinationString);
            consumer = session.createConsumer(receiveDestination);

            connection.start(); // this is needed to start receiving messages

        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }

        CreateListener(listener, consumer, 0);

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
                            if (expectedReplyCount>0&& replyCount >= expectedReplyCount)
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
                            if (expectedReplyCount>0&& replyCount >= expectedReplyCount)
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

    public String getThisDestinationString() {
        return thisDestinationString;
    }

    public void setThisDestinationString(String thisDestinationString) {
        this.thisDestinationString = thisDestinationString;
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
