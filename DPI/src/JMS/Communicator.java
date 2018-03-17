package JMS;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.Properties;

public class Communicator {

    public static Message Request(String channel, String destination, Serializable message, MessageListener listener) {
        Destination dest = JNDILookup(destination, channel);
        return Request(channel, dest, message, listener);
    }

    public static Message Request(String channel, Destination destination, Serializable message, MessageListener listener) {
        Connection connection; // to connect to the ActiveMQ
        Session session; // session for creating messages, producers and

        Destination sendDestination; // reference to a queue/topic destination
        MessageProducer producer; // for sending messages
        MessageConsumer consumer;
        Destination replyTo;

        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
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

            // connect to the sender destination
            producer = session.createProducer(destination);
            // create a text message
            Message msg = session.createObjectMessage(message);

            if (listener != null) {
                connection.start();
                replyTo = session.createTemporaryQueue();
                msg.setJMSReplyTo(replyTo);

                // send the message
                producer.send(msg);

                consumer = session.createConsumer(replyTo);
                CreateListener(listener, consumer);
            } else {
                // send the message
                producer.send(msg);
            }
            return msg;//.replace(":", "L").replace("-","D");//.substring(0, msg.getJMSMessageID().lastIndexOf("-"));
        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void Reply(String channel, Destination destination, Serializable message) {
        Request(channel, destination, message, null);
    }

    public static void SetupReceiver(String channel, String destination, MessageListener listener) {
        Connection connection; // to connect to the JMS
        Session session; // session for creating consumers

        Destination receiveDestination; //reference to a queue/topic destination
        MessageConsumer consumer = null; // for receiving messages

        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");

            // connect to the Destination called “myFirstChannel”
            // queue or topic: “queue.myFirstDestination” or “topic.myFirstDestination”
            props.put(("queue."+channel), channel);

            Context jndiContext = new InitialContext(props);
            ActiveMQConnectionFactory connectionFactory = (ActiveMQConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            connectionFactory.setTrustAllPackages(true);
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // connect to the receiver destination
            receiveDestination = (Destination) jndiContext.lookup(destination);
            consumer = session.createConsumer(receiveDestination);

            connection.start(); // this is needed to start receiving messages

        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }

        CreateListener(listener, consumer);

    }

//    public static Destination SetupReceiver(String channel, MessageListener listener) {
//        Connection connection; // to connect to the JMS
//        Session session; // session for creating consumers
//
//        Destination receiveDestination; //reference to a queue/topic destination
//        MessageConsumer consumer = null; // for receiving messages
//
//        try {
//            Properties props = new Properties();
//            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
//                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
//            props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");
//
//            // connect to the Destination called “myFirstChannel”
//            // queue or topic: “queue.myFirstDestination” or “topic.myFirstDestination”
//            props.put(("queue." + channel), channel);
//
//            Context jndiContext = new InitialContext(props);
//            ActiveMQConnectionFactory connectionFactory = (ActiveMQConnectionFactory) jndiContext
//                    .lookup("ConnectionFactory");
//            connectionFactory.setTrustAllPackages(true);
//            connection = connectionFactory.createConnection();
//            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//
//            // connect to the receiver destination
//            consumer = session.createConsumer(destination);
//
//            connection.start(); // this is needed to start receiving messages
//
//        } catch (NamingException | JMSException e) {
//            e.printStackTrace();
//        }
//
//
//        CreateListener(listener, consumer);
//
//    }

    private static void CreateListener(MessageListener listener, MessageConsumer consumer) {
        try {
            if (listener != null)
                consumer.setMessageListener(listener);
            else {
                consumer.setMessageListener(new MessageListener() {

                    @Override
                    public void onMessage(Message msg) {
                        System.out.println("received message: " + msg);
                    }
                });
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public static Destination JNDILookup(String destinationName, String channel) {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");
        props.put(("queue." + channel), channel);

        try {
            Context jndiContext = new InitialContext(props);
            return (Destination) jndiContext.lookup(destinationName);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Destination getTemporaryQueue(String channel) {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");

        // connect to the Destination called “myFirstChannel”
        // queue or topic: “queue.myFirstDestination” or “topic.myFirstDestination”
        props.put(("queue." + channel), channel);

        Context jndiContext = null;
        try {
            jndiContext = new InitialContext(props);
            ActiveMQConnectionFactory connectionFactory = (ActiveMQConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            connectionFactory.setTrustAllPackages(true);
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            return session.createTemporaryQueue();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return null;
    }


}
