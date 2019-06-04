package TopicEmbeddings;

public class Vocab implements Comparable<Vocab>{
    public int id;
    public int freq;
    public double prob;


    public Vocab(int i, int j){
        this.id = i;
        this.freq = j;
        this.prob = 1.0;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    public void setProb(double d){
        this.prob = d;
    }

    @Override
    public int compareTo(Vocab word) {
        //int flag = this.id.compareTo(word.id);
        if (this.freq > word.freq) {
            return -1;
        } else if (this.freq == word.freq) {
           return 0;
        } else { // <
            return 1;
        }
    }
}
