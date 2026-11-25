/** TimeCapsule (Domain: Sci-Fi)
Description: A digital vault meant to store messages or data that should remain sealed until a specific future date.
Key Fields: sealedData (String, private), unlockYear (int, private).
Why it fits: Prevents direct access to sealedData. The getter getContents() must check the current year against unlockYear before returning the data.
*/
package oops.c03_encapsulation;

import java.time.*;
import java.time.temporal.ChronoUnit;

public class TinyProject {
    public static void main(String[] args) {
        TimeCapsule capsule = new TimeCapsule();
        LocalDate dateTime = LocalDate.now();
        capsule.showSecreteMessage(dateTime);
    }
}

class TimeCapsule{
    private final String secreteMessage = "Secrete Message";
    private LocalDate date = LocalDate.of(2030, 8, 24);


    void showSecreteMessage(LocalDate date){
        if (this.date.equals(date)){
            System.out.println(this.secreteMessage);
        } else {
            System.out.println("No. of Days Remaining to Open Secrete Message : " + ChronoUnit.DAYS.between(date, this.date));
        }
    }

}