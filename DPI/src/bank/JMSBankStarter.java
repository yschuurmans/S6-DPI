package bank;

import java.awt.*;

public class JMSBankStarter {

    public static final String[] BanksToStart = new String[]
            {
                    "Rabobank",
                    "ABNAMRO",
                    "ING",
            };

    public static void main(String[] args) {
        for (String bank : BanksToStart) {
            JMSBankFrame.main(new String[] {bank});
        }
    }
}
