package nl.tblid.login;


import com.dax.tools.dns.Answers;
import java.io.IOException;
import com.dax.tools.dns.DnsRecords;
import com.dax.tools.dns.Records;

public class DNS {

    /**
     * Create a new Cloud IoT Authentication wrapper using the default keystore and alias.
     */
    public DNS() {
    }

    public static String getDnsRecord(String domainName) {
        try {
            Records records = DnsRecords.findDnsRecords(domainName); //For example: google.com
            StringBuilder builder = new StringBuilder();
            if(records != null){
                for(Answers answers : records.getAnswers()){
                    builder.append(answers.getName()
                            +" " + answers.getTTL()
                            +" " + answers.getTypeName()
                            +" " + answers.getData());
                    builder.append("\n");
                }
            }
            return builder.toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }


}
