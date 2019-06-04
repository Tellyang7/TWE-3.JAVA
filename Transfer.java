package TopicEmbeddings;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.lang.String;


public class Transfer {
    public HashMap<String, Integer> word2id;//    public HashMap<Integer, String> id2word;
    public String wordMap;
    public String rawData;

    public Transfer(String word, String raw){
        rawData = raw;
        wordMap = word;
        word2id = new HashMap<>();
 //       id2word = new HashMap<>();
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
        File file = new File(path);

        try {
            if (!file.exists())
                file.createNewFile();    // 不存在，则创建文件
            bw = new BufferedWriter(new FileWriter(file));
            return bw;
        } catch (IOException io){
            io.printStackTrace();
        }
        return null;
    }

    public boolean loadWordMap(){
        try{
            BufferedReader br = getReader(wordMap);
            String str = br.readLine();
            System.out.println("find word size: " + str);

            //word map: word id
            while((str = br.readLine()) != null){
                String[] s = str.split("\\s+");
                if(s[0] != null)
                    word2id.put(s[0], Integer.parseInt(s[1]));
            }
            br.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }

    public boolean transformer(){
        String target = rawData.substring(0, rawData.lastIndexOf(".")) + ".txt";
        BufferedWriter bw = getWriter(target);
        try{
            BufferedReader br = getReader(rawData);
            String line = null;
            while((line = br.readLine()) != null){
                bw.write(replace(line) + "\n");
            }

            br.close();
            bw.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        return true;
    }


    public String replace(String line){
        String[] tokens = line.split("\\s+");
        List<String> str = new ArrayList<>();
        for(String word: tokens){
            String tmp = word.substring(0,word.lastIndexOf("_"));
            //System.out.println(tmp);

            int id = word2id.get(tmp);
            //int id = 11;
            str.add(String.valueOf(id) + ":" + word.substring(word.lastIndexOf("_") + 1));
            //System.out.println(pro);
        }
        //return String.join(" ", str);
        //StringUtils.join()
        return listToString(str," ");
    }

    public String listToString(List list, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args){
        Transfer tran = new Transfer(args[0],args[1]);
        if(tran.loadWordMap())
            System.out.println("load word size: " + tran.word2id.size());

        if(tran.transformer())
            System.out.println("transforming success.");

        System.out.println("over!!");


        /*
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        System.out.println(String.join(" ",list));
        */
    }
}
