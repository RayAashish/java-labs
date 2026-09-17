package java_oops.inheritance;

import java.sql.Date;
import java.util.List;

public class Inheritance {
    public static void main(String[] args) {
        EducationLoan eduLoan = new EducationLoan();
    }
}

class Account{
    private String name;
    private Long accountNo;
    private Date dob;
    public Account(String name, Long accountNo, Date dob) {
        this.name = name;
        this.accountNo = accountNo;
        this.dob = dob;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Long getAccountNo() {
        return accountNo;
    }
    public void setAccountNo(Long accountNo) {
        this.accountNo = accountNo;
    }
    public Date getDob() {
        return dob;
    }
    public void setDob(Date dob) {
        this.dob = dob;
    }
}

class EducationLoan extends Account{

    public EducationLoan(String name, Long accountNo, Date dob, List<String> requiredDocuments) {
        super(name, accountNo, dob);
        this.requiredDocuments = requiredDocuments;
    }


    private List<String> requiredDocuments;

    public List<String> getRequiredDocuments() {
        return requiredDocuments;
    }

    public void setRequiredDocuments(List<String> requiredDocuments) {
        this.requiredDocuments = requiredDocuments;
    }
}