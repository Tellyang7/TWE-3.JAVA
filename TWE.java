package TopicEmbeddings;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class TWE {
    public HashMap<Integer, String> id2word;
    public HashMap<String, Integer> content2id; //word:topic
    //public HashMap<Integer, Integer> id2content; // content id to word id
    public HashMap<Integer, String> id2content;
    public ArrayList<Vector<int[]>> corpus;
    // the corpus: list -> docs; vec -> content; int[] -> word, topic, id
    public int topic;
    public int words;

    public TWE(String wm, String path, String output){
        id2word = new HashMap<>();
        content2id = new HashMap<>();
        id2content = new HashMap<>();
        corpus = new ArrayList<>();

        if(loadWordMap(wm))
            System.out.println("load word map success!");
        int[] tmp = genData(path);
        topic = tmp[0];
        words = tmp[1];

        if(words == id2word.size())
            System.out.println("load files success!");

        saveCorpus(corpus, output + File.separator + "data.tmp");
    }

    public TWE(TWE twe){
        this.words = twe.words;
        this.topic = twe.topic;

        id2word = new HashMap<>(twe.id2word);
        content2id = new HashMap<>(twe.content2id);
        id2content = new HashMap<>(twe.id2content);
        corpus = new ArrayList<>(twe.corpus);
    }

    public boolean loadWordMap(String wm){

        try{
            BufferedReader br = getReader(wm);
            String str = br.readLine();
            System.out.println("size: " + str);

            //word map: word id
            while((str = br.readLine()) != null){
                String[] s = str.split("\\s+");
                if(s[0] != null)
                    id2word.put(Integer.parseInt(s[1]), s[0]);
            }
            br.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }

    public int[] genData(String path){
        int cnt = 0;
        int max_word = 0;
        int max_topic = 0;
        int[] stat = new int[2];

        try{
            BufferedReader br = getReader(path);
            String str = null;
            while((str = br.readLine()) != null){
                String[] words = str.trim().split("\\s+");
                Vector<int[]> sentence = new Vector<>();
                for(String tmp : words){
                    //word:topic
                    String[] content = tmp.split(":");
                    int word = Integer.parseInt(content[0]);
                    int topic = Integer.parseInt(content[1]);

                    if(word > max_word)
                        max_word = word;
                    if(topic > max_topic)
                        max_topic = topic;

                    if(!content2id.containsKey(tmp)){
                        content2id.put(tmp, cnt);
                        id2content.put(cnt, tmp);
                        cnt += 1;
                    }

                    int [] s = {word, topic, content2id.get(tmp)};
                    sentence.add(s);
                }
                corpus.add(sentence);
            }

            br.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        max_topic += 1;
        max_word += 1;

        stat[0] = max_topic;
        stat[1] = max_word;

        System.out.println("topic number:" + max_topic);
        System.out.println("word number:" + max_word);
        System.out.println("total vocab:" + content2id.size());

        return stat;
    }

    public static BufferedReader getReader(String path){
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(new File(path)));
            return br;
        } catch (FileNotFoundException io){
            io.printStackTrace();
        }
        return null;
    }

    public static BufferedWriter getWriter(String path){
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter(new File(path)));
            return bw;
        } catch (IOException io){
            io.printStackTrace();
        }
        return null;
    }

    public static boolean saveCorpus(List<Vector<int[]>> corpus, String path) {
        BufferedWriter bw = getWriter(path);
        try {
            for (int i = 0; i < corpus.size(); i++) {
                Vector<int[]> tmp = corpus.get(i);
                for (int[] content : tmp) {
                    String str = String.valueOf(content[0] + " " + content[1] + " " + content[2] + ",");
                    bw.write(str);
                }
                bw.write("\n");
            }
            bw.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
        System.out.println("save corpus successful!");
        return true;
    }

    public static void main(String args[]){
        try{
            TWE twe = new TWE("./wordMap.txt", "./tm.topic_assign", "./");
            System.out.println(twe.id2word.size());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
