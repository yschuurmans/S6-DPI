package Gateway;

import model.loan.LoanReply;
import model.loan.LoanRequest;
import org.apache.activemq.command.ActiveMQObjectMessage;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.UUID;

public abstract class LoanClientApplicationGateway {
    public void ApplyForLoan(LoanRequest loanRq) {
        MessageSenderGateway sender = new MessageSenderGateway("LoanRequest");
        String correlationID = UUID.randomUUID().toString();
                sender.send(loanRq,correlationID);
        MessageReceiverGateway receiver = new MessageReceiverGateway(correlationID);
        receiver.awaitReply(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    ActiveMQObjectMessage msgObject = (ActiveMQObjectMessage) message;
                    LoanReply loanRp = null;
                    loanRp = (LoanReply) msgObject.getObject();
                    onLoanReply(loanRq, loanRp);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onLoanReply(LoanRequest loanRq, LoanReply loanRp) {
    }
}
