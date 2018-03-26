package Gateway;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.Properties;

public class MessageSenderGateway {

    String channel;
    Destination destination;
    Session session;
    Connection connection;

    MessageProducer producer; // for sending messages


//    MessageConsumer consumer;
//
//    Destination replyTo;

    public MessageSenderGateway(String channel) {
        this.channel = channel;
        this.destination = JNDILookup(channel);
    }

    public void send(Serializable message) {
        send(message, null);
    }

    public void send(Serializable message, String correlationID) {
        MessageReceiverGateway receiever = new MessageReceiverGateway();
        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");

            props.put(("queue." + channel), channel);

            Context jndiContext = new InitialContext(props);
            ActiveMQConnectionFactory connectionFactory = (ActiveMQConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            connectionFactory.setTrustAllPackages(true);
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // connect to the sender destination
            producer = session.createProducer(destination);
            // create a text message
            Message msg = session.createObjectMessage(message);

            if (false){//customReceiver == null) {
                receiever.setConnection(connection);
                receiever.setSession(session);
                receiever.setReceiveDestination(session.createTemporaryQueue());
                msg.setJMSReplyTo(receiever.getReceiveDestination());
                producer.send(msg);
            } else {
                msg.setJMSCorrelationID(correlationID);
                producer.send(msg);
            }
        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }
    }

    public static Destination JNDILookup(String channel) {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");
        props.put(("queue." + channel), channel);

        try {
            Context jndiContext = new InitialContext(props);
            return (Destination) jndiContext.lookup(channel);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return null;
    }


}
