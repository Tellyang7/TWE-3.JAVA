package TopicEmbeddings;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class TW2VEC {
    // self.vocab
    public HashMap<Integer, Vocab> id2vocab; // content id to vocab object
    // self.index2word
    public List<Integer> index2id; //index to vocab id
    public HashMap<Integer, Integer> vocab2index; // vocab id to index
    public HashMap<Integer, Integer> word2index;  // word id to index

    public boolean sg; // c bow or soft max
    public boolean hs; // not hs
    public int neg; //negative sampling number
    public int window; // context length
    public int minimum; // word count

    // for the high frequent words
    public double sample = 1e-3;
    public double alpha = 0.025;
    public double startingAlpha = alpha;

    public int tableSize = (int) 1e8; // when set 1e8, out of the memory, so maybe 1e7 more proper...
    public int[] table;
    public int EXP_TABLE_SIZE = 1000;
    public double[] expTable = new double[EXP_TABLE_SIZE];


    public int MAX_EXP = 6;
    public Random random = new Random();
    public DecimalFormat df = new DecimalFormat("#.##%");
    public DecimalFormat df1 = new DecimalFormat("#.000000");

    public int layer1size;
    public int topic_layer1size;
    public int layer2size;

    public double[][] syn1; // for hierarchical
    public double[][] syn1neg; // for negative
    public double[][] syn0; // for word
    public double[][] syn0topic; // for topic


    /**
     * learning rate self-adapt
     */
    public int wordCount = 0;
    public int lastWordCount = 0;
    public int wordCountActual = 0;
    public int trainWordsCount = 0;

    public TWE twe;
    public String inPath;
    public String outPath;

    public TW2VEC(TWE t){
        this.twe = new TWE(t);
        //createExpTable();
        id2vocab = new HashMap<>();
        vocab2index = new HashMap<>();
        word2index = new HashMap<>();
        index2id = new ArrayList<>();

        table = new int[tableSize];
    }

    public void learn(){
        long start = System.currentTimeMillis();
        //processing
        createExpTable();
        buildVocab();
        System.out.println("build the vocabulary：" + (System.currentTimeMillis() - start));

        preSampling();
        resetWeight();

        if (!hs) {
            initUniGramTable();
            System.out.println("creating table successful!" );
        } else{
            System.out.println("TODO: Hierarchical tree not created!");
        }

        // training
        start = System.currentTimeMillis();
        train();
        System.out.println("training process costs：" + (System.currentTimeMillis() - start));
    }

    /**
     * Pre compute the exp() table f(x) = x / (x + 1)
     */
    private void createExpTable() {
        for (int i = 0; i < EXP_TABLE_SIZE; i++) {
            expTable[i] = Math.exp(((i / (double) EXP_TABLE_SIZE * 2 - 1) * MAX_EXP));
            expTable[i] = expTable[i] / (expTable[i] + 1);
        }
    }

    public void buildVocab(){
        //int number = 0;
        int totalWords = 0;
        ;

        HashMap<Integer, Vocab> vocab = new HashMap<>(); // content id to content
        for(int no = 0; no < twe.corpus.size(); no++){
            if(no % 10000 == 0)
                System.out.println("Progress at sentence " + no + ", processed " + totalWords);

            for(int[] content : twe.corpus.get(no)){
                totalWords += 1;
                int id = content[2];
                if(vocab.containsKey(id)){
                    int f = vocab.get(id).freq;
                    vocab.put(id, new Vocab(id,f + 1));
                }else{
                    vocab.put(id, new Vocab(id, 1));
                }
            }
        }
        System.out.println("Finally, processed " + totalWords);
        System.out.println("-------, we got " + vocab.size() + " (word + topic) ---- ");

        //int index = 0;
        //int num = 0;
        for(int key: vocab.keySet()){ // vocab id
            int f = vocab.get(key).freq;
            if(f >= minimum){
                //index = id2vocab.size();
                vocab2index.put(key, index2id.size()); // vocab is to index
                id2vocab.put(key, vocab.get(key));
                //get word id by vocab id
                int id = Integer.parseInt(twe.id2content.get(key).split(":")[0]);

                if(!word2index.keySet().contains(id)){
                    word2index.put(id, word2index.size());
                    //num ++;
                }
                index2id.add(key);

                //index ++;
                trainWordsCount += f;
            }
        }
        System.out.println("Vocabulary size is:  " + word2index.size() + " after removing those < " + minimum);
        System.out.println("Content size is:  " + vocab2index.size() + " after removing those < " + minimum);
    }


    /**
     * only for negative sampling: a weighted sampling table，
     * in which more frequent words will be longer in the corresponding index place
     */
    public void initUniGramTable() {
        double train_words_pow = 0;
        double power = 0.75;

        for (int m = 0; m < id2vocab.size(); m++)
            train_words_pow += Math.pow(id2vocab.get(index2id.get(m)).freq, power);

        int i = 0;

        double d1 = Math.pow(id2vocab.get(index2id.get(i)).freq, power) / train_words_pow;
        for (int n = 0; n < tableSize; n++) {
            table[n] = i;
            if (n / (double) tableSize > d1) {
                i++;
                d1 += Math.pow(id2vocab.get(index2id.get(i)).freq, power) / train_words_pow;
            }
            if (i >= id2vocab.size())
                i = id2vocab.size() - 1;
        }
    }

    public void preSampling(){
        double prob;
        double threshold = 0;
        double sum = 0;
        if(sample != 0){
            System.out.println("frequent words down sampling, threshold: " + sample);
            for(Vocab v : id2vocab.values()){
                sum += v.freq;
            }
            threshold = sample * sum;
        }
        for(Vocab vb : id2vocab.values()){
            if(sample != 0){
                prob = (Math.sqrt(vb.freq / threshold) + 1) * (threshold / vb.freq);
            }else
                prob = 1.0;
            vb.setProb(Math.min(prob, 1.0));
        }
    }

    public void resetWeight(){
        layer2size = layer1size + topic_layer1size;

        if(hs){
            syn1 = new double[id2vocab.size()][layer2size];
            for(int i = 0; i < id2vocab.size(); i++){
                for(int j = 0; j < layer2size; j++)
                    syn1[i][j] = 0;
            }
        }

        if(neg != 0){
            syn1neg = new double[id2vocab.size()][layer2size];
            for(int i = 0; i < id2vocab.size(); i++){
                for(int j = 0; j < layer2size; j++)
                    syn1neg[i][j] = 0;
            }
        }

        syn0 = new double[word2index.size()][layer1size];
        syn0topic = new double[twe.topic][topic_layer1size];

        for(int i = 0; i < word2index.size(); i++){
            for(int j = 0; j < layer1size; j++)
                syn0[i][j] = (random.nextDouble() - 0.5) / layer1size;
        }

        for(int i = 0; i < twe.topic; i++){
            for(int j = 0; j < topic_layer1size; j++)
                syn0topic[i][j] = (random.nextDouble() - 0.5) / topic_layer1size;
        }
    }

    public boolean save(String path){
        return saveWordVectors(path) && saveTopicVectors(path);
    }

    public boolean saveWordVectors(String path) {
        DecimalFormat df = new DecimalFormat("#.######");
        String path1;
        if(sg)
            path1 = path + File.separator + "word_vector_sg_" + layer1size;
        else
            path1 = path + File.separator + "word_vector_sg_" + topic_layer1size;
        File file = new File(path1);
        DataOutputStream dataOutputStream = null;
        BufferedWriter fw = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            try {
                outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file.getPath() + ".txt"),
                        "UTF-8");
                fw = new BufferedWriter(outputStreamWriter);
                dataOutputStream.writeInt(word2index.size());
                fw.write(word2index.size() + " ");
                dataOutputStream.writeInt(layer1size);
                fw.write(layer1size + "\n");

                double[] syn0tmp = null;
                for (int i = 0; i < twe.words; i++) {
                    if (!word2index.keySet().contains(i))
                        continue;

                    int a = word2index.get(i); // get corresponding index word and representations
                    //i is the right word id, a is the index (in parameter matrix)
                    syn0tmp = syn0[a];
                    String w = twe.id2word.get(i);

                    dataOutputStream.writeUTF(w);
                    fw.write(w + " ");
                    for (double d : syn0tmp) {
                        dataOutputStream.writeFloat(((Double) d).floatValue());
                        String vec = df.format(d);
                        fw.write(vec + " ");
                    }
                    fw.write("\n");
                }
            } finally {
                if (fw != null)
                    fw.close();
                if (dataOutputStream != null)
                    dataOutputStream.close();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean saveTopicVectors(String path){
        DecimalFormat df = new DecimalFormat("#.######");
        String path1;
        if(sg)
            path1 = path + File.separator + "topic_vector_sg_" + layer1size;
        else
            path1 = path + File.separator + "topic_vector_sg_" + topic_layer1size;
        File file = new File(path1);
        DataOutputStream dataOutputStream = null;
        BufferedWriter fw = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            try {
                outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file.getPath() + ".txt"),
                        "UTF-8");
                fw = new BufferedWriter(outputStreamWriter);
                dataOutputStream.writeInt(twe.topic);
                fw.write(twe.topic + " ");
                dataOutputStream.writeInt(topic_layer1size);
                fw.write(topic_layer1size + "\n");

                double[] syn0tmp = null;
                for (int i = 0; i < twe.topic; i++) {
                    String w = "topic" + i;
                    dataOutputStream.writeUTF(w);
                    fw.write(w + " ");

                    syn0tmp = syn0topic[i];
                    for (double d : syn0tmp) {
                        dataOutputStream.writeFloat(((Double) d).floatValue());
                        String vec = df.format(d);
                        fw.write(vec + " ");
                    }
                    fw.write("\n");
                }
            } finally {
                if (fw != null)
                    fw.close();
                if (dataOutputStream != null)
                    dataOutputStream.close();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void train(){
        long nextRandom = 5;

        for(Vector<int[]> sentence: twe.corpus){
            //learning rate self-adapt
            if (wordCount - lastWordCount > 10000) {
                double progress = (wordCountActual / (double) (trainWordsCount + 1));
                System.out.println("alpha:" + df1.format(alpha) + "\tProgress: " + df.format(progress));
                wordCountActual += wordCount - lastWordCount;
                lastWordCount = wordCount;
                alpha = startingAlpha * (1 - wordCountActual / (double) (trainWordsCount + 1));
                if (alpha < startingAlpha * 0.0001) {
                    alpha = startingAlpha * 0.0001;
                }
            }

            // prepare sentences
            List<Vocab> vocabs = new ArrayList<>();
            for(int[] content: sentence){
                Vocab currWord = id2vocab.get(content[2]); // Vocab id
                if(currWord == null)
                    continue;

                wordCount ++;
                if (sample > 0) {
                    double ran = (Math.sqrt(currWord.freq / (sample * trainWordsCount)) + 1) * (sample * trainWordsCount) / currWord.freq;
//                                nextRandom = nextRandom * 25214903917L + 11;
                    nextRandom = random.nextInt(tableSize) % tableSize;
                    if (ran < (nextRandom & 0xFFFF) / (double) 65536) {
                        continue;
                    }
                }
                vocabs.add(currWord);
            }

            for(int j = 0; j < vocabs.size(); j++){
                nextRandom = new Random().nextInt(100) % 100 + 1;//生成1-100之间的随机数
                if(hs){
                    System.out.println("no corresponding methods");
                }else {
                    if(sg){
                        skipGram(j, vocabs, neg, (int) nextRandom % window);
                    }else{
                        cBow(j, vocabs, neg, (int) nextRandom % window);
                    }
                }
            }
        }

        System.out.println("Vocab size: " + word2index.size());
        System.out.println("Words in train file: " + trainWordsCount);
        System.out.println("train Model successfully!");
    }


    public void cBow(int index, List<Vocab> sentence, int negative, int b){
        Vocab word = sentence.get(index);
        int a, c = 0;
        int label;
        long nextRandom = 5;
        Vocab target;
        Vocab last_word;

        double[] neu1e = new double[layer2size];// 误差项
        double[] neu1 = new double[layer2size];//  输入
        for(int i = 0; i < layer2size; i++){
            neu1e[i] = 0.0;
            neu1[i] = 0.0;
        }

        int cw = 0;
        for (a = b; a < window * 2 + 1 - b; a++) {
            if (a != window) {
                c = index - window + a;
                if (c < 0)
                    continue;
                if (c >= sentence.size())
                    continue;
                last_word = sentence.get(c);
                if (last_word == null)
                    continue;

                // warning, index is not word id after filtering by minimum
                int[] last_index = getContent(last_word.id);

                for (c = 0; c < layer1size; c++)
                    neu1[c] += syn0[last_index[0]][c];
                for (c = 0; c < topic_layer1size; c++)
                    neu1[c] += syn0topic[last_index[1]][c];

                cw++;
            }
        }
        if (cw > 0) {
            for (c = 0; c < layer2size; c++)
                neu1[c] /= cw;

            //Negative Sample
            for (int d = 0; d < negative + 1; d++) {
                if (d == 0) {//样本为正
                    target = word;
                    label = 1;
                } else {//样本为负
                    //负采样时，采用带权采样的机制，如果一个词是高频词时，那么其被采样到的概率就大一些
//                nextRandom = nextRandom * 25214903917L + 11;
//                int tempIndex = (int) (nextRandom >> 16) % tableSize;
                    //target = table[tempIndex]
                    int tempIndex = random.nextInt(Integer.MAX_VALUE) % tableSize;
                    target = id2vocab.get(index2id.get(table[tempIndex]));
                    if (target == null) {
                        target = id2vocab.get(index2id.get(table[(int) (nextRandom % (id2vocab.size() - 1) + 1)]));
                    }
                    if (target == word) {
                        continue;
                    }
                    label = 0;
                }
                //LeafNode out = target;
                double f = 0;
                double g = 0;
                int target_vocab_index = vocab2index.get(target.id);
                // Propagate hidden -> output
                for (c = 0; c < layer2size; c++)
                    f += neu1[c] * syn1neg[target_vocab_index][c];

                if (f <= -MAX_EXP)
                    g = (label - 0) * alpha;
                else if (f >= MAX_EXP)
                    g = (label - 1) * alpha;
                else {
                    //g = (label - expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;
                    g = (label - expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;
                    //f = expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / (double) MAX_EXP / 2))];
                    // 'g' is the gradient multiplied by the learning rate
                    //g = (label - f) * alpha;
                }
                for (c = 0; c < layer2size; c++) {
                    neu1e[c] += g * syn1neg[target_vocab_index][c];
                }
                // Learn weights hidden -> output
                for (c = 0; c < layer2size; c++) {
                    syn1neg[target_vocab_index][c] += g * neu1[c];
                }
            }

            for (a = b; a < window * 2 + 1 - b; a++) {
                if (a != window) {
                    c = index - window + a;
                    if (c < 0)
                        continue;
                    if (c >= sentence.size())
                        continue;
                    last_word = sentence.get(c);
                    if (last_word == null)
                        continue;

                    int[] last_index = getContent(last_word.id);

                    for (c = 0; c < layer1size; c++)
                        syn0[last_index[0]][c] += neu1e[c];
                    for (c = 0; c < topic_layer1size; c++)
                        syn0topic[last_index[1]][c] += neu1e[c];
                }
            }

        }
    }

    /**
     * Skip-gram模型
     * 使用Skip-gram模型的Negative sample
     *
     * @param index
     * @param sentence
     * @param negative
     * @param b
     */
    public void skipGram(int index, List<Vocab> sentence, int negative, int b) {
        Vocab word = sentence.get(index);
        int a, c = 0;
        int label;
//        long nextRandom = 5;
        Vocab target;

        Vocab last_word = null;

        for (a = b; a < window * 2 + 1 - b; a++) {
            if (a != window) {
                c = index - window + a;
                if (c < 0 || c >= sentence.size()) {
                    continue;
                }
                last_word = sentence.get(c);
                if (last_word == null)
                    continue;
                //LeafNode out = new LeafNode(target.name,target.freq,layerSize);
                // word is != word index after filtering, so do the vocabs
                int[] last_index = getContent(last_word.id);

                double[] neu1e = new double[layer2size];// 误差项
                for(int i = 0; i < layer2size; i++){
                    neu1e[i] = 0.0;
                }

                for (int d = 0; d < negative + 1; d++) {
                    if (d == 0) {//样本为正
                        target = word;
                        label = 1;
                    } else {//样本为负
//                        nextRandom = nextRandom * 25214903917L + 11;
//                        int tempIndex = (int) (nextRandom >> 16) % tableSize;
                        int tempIndex = new Random().nextInt(tableSize) % (tableSize + 1);
                        target =  id2vocab.get(index2id.get(table[tempIndex]));
                        if (target == null) {
                            target =  id2vocab.get(index2id.get(table[(int) (tempIndex % (id2vocab.size() - 1) + 1)]));
                        }
                        if (target == word) {
                            continue;
                        }
                        label = 0;
                    }
                    double f = 0;
                    double g;
                    int target_vocab_index = vocab2index.get(target.id);

                    // Propagate hidden -> output
                    for (c = 0; c < layer1size; c++)
                        f += syn0[last_index[0]][c] * syn1neg[target_vocab_index][c];
                    for (c = 0; c < topic_layer1size; c++)
                        f += syn0topic[last_index[1]][c] * syn1neg[target_vocab_index][layer1size + c];


                    if (f <= -MAX_EXP)
                        g = (label - 0) * alpha;
                    else if (f >= MAX_EXP)
                        g = (label - 1) * alpha;
                    else {
                        //f = expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / (double) MAX_EXP / 2))];
                        // 'g' is the gradient multiplied by the learning rate
                        //g = (label - f) * alpha;
                        g = (label - expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;
                    }


                    for (c = 0; c < layer2size; c++) {
                        neu1e[c] += g * syn1neg[target_vocab_index][c];
                    }
                    // Learn weights hidden -> output
                    for (c = 0; c < layer1size; c++) {
                        syn1neg[target_vocab_index][c] += g * syn0[last_index[0]][c];
                    }
                    for (c = 0; c < topic_layer1size; c++) {
                        syn1neg[target_vocab_index][c + layer1size] += g * syn0[last_index[1]][c];
                    }
                }
                // Learn weights input -> hidden
                for (int j = 0; j < layer1size; j++) {
                    syn0[last_index[0]][j] += neu1e[j];
                }
                for (int i = 0; i < topic_layer1size; i++){
                    syn0topic[last_index[1]][i] += neu1e[i + layer1size];
                }
            }
        }
    }

    public int[] getContent(int id){ // vocab id
        String[] content = twe.id2content.get(id).split(":");
        int word = Integer.parseInt(content[0]);
        int topic = Integer.parseInt(content[1]);
        int[] s = {word2index.get(word),topic};
        return s;
    }
}
