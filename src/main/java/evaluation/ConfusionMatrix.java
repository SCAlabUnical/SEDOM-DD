package evaluation;

public class ConfusionMatrix {

    long TP, FP, TN, FN;


    public ConfusionMatrix() {
    }

    public ConfusionMatrix(long TP, long FP, long TN, long FN) {
        this.TP = TP;
        this.FP = FP;
        this.TN = TN;
        this.FN = FN;
    }


    public long getTP() {
        return TP;
    }

    public void setTP(long TP) {
        this.TP = TP;
    }

    public long getFP() {
        return FP;
    }

    public void setFP(long FP) {
        this.FP = FP;
    }

    public long getTN() {
        return TN;
    }

    public void setTN(long TN) {
        this.TN = TN;
    }

    public long getFN() {
        return FN;
    }

    public void setFN(long FN) {
        this.FN = FN;
    }

    public double getPrecisionP() {
        double tp = getTP();
        double fp = getFP();
        if (tp + fp == 0)
            return 0;
        return tp / (tp + fp);
    }

    public double getRecallP() {
        double tp = getTP();
        double fn = getFN();
        if (tp + fn == 0)
            return 0;
        return tp / (tp + fn);
    }

    public double getF1P() {
        double recallP = getRecallP();
        double precisionP = getPrecisionP();
        if (recallP + precisionP == 0)
            return 0;
        return 2.0 * recallP * precisionP / (recallP + precisionP);
    }

    @Override
    public String toString() {
        return "ConfusionMatrix{" +
                "TP=" + TP +
                ", FP=" + FP +
                ", TN=" + TN +
                ", FN=" + FN +
                '}' +
                "\nPrecisionP=" + this.getPrecisionP() +
                "\tRecallP=" + this.getRecallP() +
                "\tF1P=" + this.getF1P();
    }
}
