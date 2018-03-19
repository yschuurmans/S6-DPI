package Gateway;

import messaging.requestreply.RequestReply;
import model.bank.BankInterestReply;
import model.bank.BankInterestRequest;
import org.apache.activemq.command.ActiveMQObjectMessage;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.HashMap;

public abstract class LoanBankApplicationGateway {

    private HashMap<String, Destination> Clients = new HashMap<>();

    public void listenForBankInterestRequest() {
        MessageReceiverGateway receiver = new MessageReceiverGateway("BankInterestRequest", "BankInterestRequest");
        receiver.startConnection(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    ActiveMQObjectMessage msgObject = (ActiveMQObjectMessage) message;
                    BankInterestRequest bankIntRq = (BankInterestRequest) msgObject.getObject();
                    bankIntRq.setReplyDestination(message.getJMSReplyTo());
                    onBankInterestRequestReceived(bankIntRq);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onBankInterestRequestReceived(BankInterestRequest bankRq) {}

    public void replyLoanRequest(RequestReply rr) {
        MessageSenderGateway sender = new MessageSenderGateway("BankInterestReply",((BankInterestRequest) rr.getRequest()).getReplyDestination());
        sender.send((BankInterestReply)rr.getReply());
    }
}
