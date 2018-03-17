package loanbroker;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import JMS.Communicator;
import model.bank.*;
import model.loan.LoanReply;
import model.loan.LoanRequest;
import org.apache.activemq.command.ActiveMQObjectMessage;


public class LoanBrokerFrame extends JFrame {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private DefaultListModel<JListLine> listModel = new DefaultListModel<JListLine>();
    private JList<JListLine> list;
    private static boolean isRunning = false;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    LoanBrokerFrame frame = new LoanBrokerFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * Create the frame.
     */
    public LoanBrokerFrame() {
        if (!isRunning) setupConnection();
        setTitle("Loan Broker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        GridBagLayout gbl_contentPane = new GridBagLayout();
        gbl_contentPane.columnWidths = new int[]{46, 31, 86, 30, 89, 0};
        gbl_contentPane.rowHeights = new int[]{233, 23, 0};
        gbl_contentPane.columnWeights = new double[]{1.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        gbl_contentPane.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
        contentPane.setLayout(gbl_contentPane);

        JScrollPane scrollPane = new JScrollPane();
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.gridwidth = 7;
        gbc_scrollPane.insets = new Insets(0, 0, 5, 5);
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 0;
        contentPane.add(scrollPane, gbc_scrollPane);

        list = new JList<JListLine>(listModel);
        scrollPane.setViewportView(list);
    }

    private JListLine getRequestReply(LoanRequest request) {

        for (int i = 0; i < listModel.getSize(); i++) {
            JListLine rr = listModel.get(i);
            if (rr.getLoanRequest() == request) {
                return rr;
            }
        }

        return null;
    }

    public void add(LoanRequest loanRequest) {

        listModel.addElement(new JListLine(loanRequest));
    }


    public void add(LoanRequest loanRequest, BankInterestRequest bankRequest) {
        JListLine rr = getRequestReply(loanRequest);
        if (rr != null && bankRequest != null) {
            rr.setBankRequest(bankRequest);
            list.repaint();
        }
    }

    public void add(LoanRequest loanRequest, BankInterestReply bankReply) {
        JListLine rr = getRequestReply(loanRequest);
        if (rr != null && bankReply != null) {
            rr.setBankReply(bankReply);
            ;
            list.repaint();
        }
    }

    public void setupConnection() {
        isRunning = true;
        Communicator.SetupReceiver("LoanRequest", "LoanRequest", new MessageListener() {

            @Override
            public void onMessage(Message msg) {
                try {
                    ActiveMQObjectMessage msgObject = (ActiveMQObjectMessage) msg;
                    LoanRequest loanRq = (LoanRequest) msgObject.getObject();
                    Destination replyTo = msg.getJMSReplyTo();
                    add(loanRq);
                    BankInterestRequest bankIntRq = new BankInterestRequest(loanRq.getAmount(), loanRq.getTime());
                    add(loanRq, bankIntRq);
                    Communicator.Request("BankInterestRequest", "BankInterestRequest", bankIntRq, new MessageListener() {

                        @Override
                        public void onMessage(Message msg) {
                            try {
                                ActiveMQObjectMessage msgObject = (ActiveMQObjectMessage) msg;
                                BankInterestReply bankRp = (BankInterestReply) msgObject.getObject();
                                LoanReply loanReply = new LoanReply(bankRp.getInterest(),bankRp.getQuoteId());
                                add(loanRq, bankRp);
                                Communicator.Reply("LoanReply",replyTo,loanReply);


                            } catch (JMSException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });
    }

//    public void awaitReplyFromBank(LoanRequest request, Destination listenDestination, Destination replyDestination) {
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        Communicator.SetupReceiver("BankInterestReply", listenDestination, new MessageListener() {
//
//            @Override
//            public void onMessage(Message msg) {
//                try {
//                    ActiveMQObjectMessage msgObject = (ActiveMQObjectMessage) msg;
//                    BankInterestReply bankRp = (BankInterestReply) msgObject.getObject();
//
//                    add(request, bankRp);
//
//                    Communicator.SendObject("LoanReply ", replyDestination, bankRp, null);
//
//
//                } catch (JMSException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }


}
