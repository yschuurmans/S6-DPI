package Gateway;

import bank.JMSBankStarter;
import model.bank.BankInterestReply;
import model.bank.BankInterestRequest;
import model.loan.LoanReply;
import model.loan.LoanRequest;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.activemq.memory.list.MessageList;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        HashMap<String, BankInterestReply> bankReplies = new HashMap<>();
        HashMap<String, String> bankKeys = new HashMap<>();

        for (String s : JMSBankStarter.BanksToStart) {
            bankKeys.put(s, UUID.randomUUID().toString());
        }

        for (int i = 0; i < JMSBankStarter.BanksToStart.length; i++) {
        }

        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    ActiveMQObjectMessage msgObject = (ActiveMQObjectMessage) message;
                    BankInterestReply bankIntRp = (BankInterestReply) msgObject.getObject();
                    System.out.println(message.getJMSCorrelationID() + " : " + bankIntRp.toString());
                    if(bankKeys.containsKey(bankIntRp.getQuoteId()) && bankKeys.get(bankIntRp.getQuoteId()).equals(((ActiveMQObjectMessage) message).getCorrelationId())) {
                        bankReplies.put(message.getJMSCorrelationID(), bankIntRp);
                        if (bankReplies.size() >= JMSBankStarter.BanksToStart.length)
                            returnBestToClient(bankReplies, loanRq, replyDestination);
                    }

                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        };


        for (String b : JMSBankStarter.BanksToStart) {
            String correlationID = bankKeys.get(b);
            MessageSenderGateway sender = new MessageSenderGateway("BankInterestRequest_" + b);
            sender.send(bankIntRq, correlationID);
            MessageReceiverGateway receiver = new MessageReceiverGateway(correlationID);
            receiver.awaitReply(listener);
        }
    }

    private void returnBestToClient(HashMap<String, BankInterestReply> bankReplies, LoanRequest loanRq, String replyDestination) {
        double lowestInterest = Double.MAX_VALUE;
        BankInterestReply lowestReply = new BankInterestReply();
        for (BankInterestReply bankReply : bankReplies.values()) {
            if (bankReply.getInterest() < lowestInterest) {
                lowestReply = bankReply;
                lowestInterest = bankReply.getInterest();
            }
        }

        LoanReply loanReply = onBankInterestReply(loanRq, lowestReply);
        replyLoanRequest(loanReply, replyDestination);
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
