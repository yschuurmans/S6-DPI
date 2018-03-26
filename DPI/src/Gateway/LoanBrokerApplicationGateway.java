package Gateway;

import model.bank.BankInterestReply;
import model.bank.BankInterestRequest;
import model.loan.LoanReply;
import model.loan.LoanRequest;
import org.apache.activemq.command.ActiveMQObjectMessage;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.HashMap;
import java.util.UUID;

public abstract class LoanBrokerApplicationGateway {

    private HashMap<String, Destination> Clients = new HashMap<>();

    public void listenForLoanRequests() {
        MessageReceiverGateway receiver = new MessageReceiverGateway("LoanRequest");
        receiver.startConnection(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    ActiveMQObjectMessage msgObject = (ActiveMQObjectMessage) message;
                    LoanRequest loanRq = (LoanRequest) msgObject.getObject();
                    BankInterestRequest bankIntRq = onLoanRequestReceieved(loanRq);
                    requestBankInterest(loanRq, bankIntRq, message.getJMSCorrelationID());
                } catch (JMSException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public BankInterestRequest onLoanRequestReceieved(LoanRequest loanRq) {
        return null;
    }

    public void requestBankInterest(LoanRequest loanRq, BankInterestRequest bankIntRq, String replyDestination) {
        MessageSenderGateway sender = new MessageSenderGateway( "BankInterestRequest");
        String correlationID = UUID.randomUUID().toString();
        sender.send(bankIntRq, correlationID);
        MessageReceiverGateway receiver = new MessageReceiverGateway(correlationID);

        receiver.awaitReply(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    ActiveMQObjectMessage msgObject = (ActiveMQObjectMessage) message;
                    BankInterestReply bankIntRp = (BankInterestReply) msgObject.getObject();
                    LoanReply loanReply = onBankInterestReply(loanRq, bankIntRp);
                    replyLoanRequest(loanReply, replyDestination);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public LoanReply onBankInterestReply(LoanRequest loanRq, BankInterestReply loanRp) {
        return null;
    }

    public void replyLoanRequest(LoanReply loanReply, String replyDestination) {
        MessageSenderGateway sender = new MessageSenderGateway(replyDestination);
        sender.send(loanReply);
        System.out.println(loanReply);
    }
}
