package Gateway;

import model.loan.LoanReply;
import model.loan.LoanRequest;
import org.apache.activemq.command.ActiveMQObjectMessage;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

public abstract class LoanClientApplicationGateway {
    public void ApplyForLoan(LoanRequest loanRq) {
        MessageSenderGateway sender = new MessageSenderGateway("LoanRequest", "LoanRequest");
        MessageReceiverGateway receiver = sender.send(loanRq);
        receiver.AwaitReply(new MessageListener() {
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
