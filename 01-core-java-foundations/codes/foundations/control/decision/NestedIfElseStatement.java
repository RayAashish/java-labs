package foundations.control.decision;


public class NestedIfElseStatement {

    /**
     * @param n
     * suppose we are hiring an employee who should meet these three condtions
     * 1. Most have a bachelor degree
     * 2. Most have 80% above in bachelor degree
     * 3. Most have 2 years of experince
     */
    private static void nestedIfElseStatement(Applicant applicant){
        if (applicant.isDegree()){
            if (applicant.getPercentage() >= 80){
                if (applicant.getExperince() >= 2){
                    System.out.println("Congratulation Mr. " + applicant.getName() + ", You have been selected for our hiring process");
                } else {
                    System.out.println("Experince isn't enough");
                }
            } else {
                System.out.println("Percentage isn't enough");
            }
        } else {
            System.out.println("Sorry Mr. " + applicant.getName() + ", You are not meeting our eligibility criterio");
        }
    }
    public static void main(String[] args) {
        Applicant applicant1 = new Applicant();
        applicant1.setName("Aashish");
        applicant1.setDegree(true);
        applicant1.setExperince((byte) 0);
        applicant1.setPercentage(87.5);

        nestedIfElseStatement(applicant1);
    }
}

class Applicant{
    private String name;
    private boolean degree;
    private double percentage;
    private byte experince;

    public Applicant(){
        super();
    }
    
    public Applicant(String name, boolean degree, double percentage, byte experince) {
        super();
        this.name = name;
        this.degree = degree;
        this.percentage = percentage;
        this.experince = experince;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public boolean isDegree() {
        return degree;
    }
    public void setDegree(boolean degree) {
        this.degree = degree;
    }
    public double getPercentage() {
        return percentage;
    }
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
    public byte getExperince() {
        return experince;
    }
    public void setExperince(byte experince) {
        this.experince = experince;
    }
    
}