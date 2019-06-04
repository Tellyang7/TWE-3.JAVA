package TopicEmbeddings;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;

public class Prop {
    public String wordMap = "";
    public String inputPath = "in";
    public String outputPath = "out";

    public boolean sg= false; //true:使用Skip-gram模型
    public boolean hs = false; //true:使用层次Softmax模型，false:使用negative sampling

    /**
     * 训练多少个特征
     */
    public int layer1 = 200; // word
    public int layer2 = 200; // topic
    /**
     * 对低频词进行过滤处理
     */
    public int min_count = 5;

    /**
     * 上下文窗口大小
     */
    public int window = 5;
    /**
     * 负采样时，负样本的个数
     */
    public int neg = 5;
    /**
     * 对高频词进行处理，当该词出现的词频概率大于sample时则以一定的概率将其舍弃
     */
    public double sample = 1e-3;
    public double alpha = 0.025;
    public double weight_alpha = 0.1;

    public Prop(String path){
        init(path);
    }

    public void init(String path) {

        Properties pro = new Properties();
        try {
            // C:\DataCenter\Study\Project\IDEA\TopicWordEmbedding\src\
            InputStream in = new BufferedInputStream(new FileInputStream(path));
            pro.load(in);

            Iterator<String> it = pro.stringPropertyNames().iterator();

            while(it.hasNext()){
                String key = it.next();
                String value = pro.getProperty(key);

                if(key.equals("wordMap")){
                    wordMap = value;
                }
                if(key.equals("inputPath")){
                    inputPath = value;
                }
                if(key.equals("outputPath"))
                    outputPath = value;

                if(key.equals("sg"))
                    sg = Boolean.parseBoolean(value);

                if(key.equals("hs"))
                    hs = Boolean.parseBoolean(value);

                if(key.equals("layer1"))
                    layer1 = Integer.parseInt(value);
                if(key.equals("layer2"))
                    layer2 = Integer.parseInt(value);

                if(key.equals("window"))
                    window = Integer.parseInt(value);
                if(key.equals("neg"))
                    neg = Integer.parseInt(value);

                if(key.equals("min_count"))
                    min_count = Integer.parseInt(value);

                if(key.equals("alpha")){
                    alpha = Double.parseDouble(value);
                }
                if(key.equals("weight"))
                    weight_alpha = Double.parseDouble(value);

            }
            System.out.println("--- read properties ---");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void show(){
        System.out.println("input path : " + inputPath);
        System.out.println("output path: " + outputPath);
        if(sg)
            System.out.println("model is Skip-gram");
        else
            System.out.println("model is C_bow");

        if(!hs)
            System.out.println("optimization is Negative Sampling");
        else
            System.out.println("optimization is Hierarchical SoftMax");

        if(neg != 0)
            System.out.println("negative sampling: " + neg);

        System.out.println("minimum of count: " + min_count);
        System.out.println("size of window: " + window);
        System.out.println("the dimension of word : " + layer1);
        System.out.println("the dimension of topic: " + layer2);

        System.out.println("other parameters ->");
        System.out.println("sample->" + sample + " alpha->" + alpha + " weight_alpha->" + weight_alpha);
    }

    public static void main(String[] args){
        Prop pro = new Prop("src//TopicEmbeddings//options.properties");
        //pro.init();
        pro.show();
    }
}
