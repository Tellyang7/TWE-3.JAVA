package TopicEmbeddings;

import java.io.File;

public class Main {
    public static void main(String[] args){
        String str = "./options.properties";
        Prop pro = new Prop(str);
        pro.show();

        long start = System.currentTimeMillis();

        TWE twe = new TWE(pro.wordMap, pro.inputPath, pro.outputPath);
        TW2VEC tw2vec = new TW2VEC(twe);
        System.out.println("Time cost for creating TWE data object: " + (System.currentTimeMillis() - start));

        tw2vec.sg = pro.sg;//true:使用Skip-gram模型
        tw2vec.hs = pro.hs;//true:使用negative sampling
        tw2vec.window = pro.window;     //context-window size
        tw2vec.minimum = pro.min_count;  //low-frequency words
        tw2vec.layer1size = pro.layer1;  //dimension size of words
        tw2vec.topic_layer1size = pro.layer2; //dimension size of topic
        tw2vec.neg = pro.neg;   // negative sample size
        tw2vec.sample = pro.sample;   //high frequency words
        tw2vec.inPath = pro.inputPath;
        tw2vec.outPath = pro.outputPath;

        System.out.println(pro.inputPath);
        System.out.println(pro.outputPath);

        tw2vec.learn();
        System.out.println("Time cost: " + (System.currentTimeMillis() - start));
        System.out.println("Training Model is saved in +" + new File(pro.outputPath).getAbsolutePath());
        tw2vec.save(pro.outputPath);
    }
}
