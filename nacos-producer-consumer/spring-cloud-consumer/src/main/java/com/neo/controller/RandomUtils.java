package com.neo.controller;

import java.util.*;

/**
 *
 * Long 转32位数字
 *
 * */
public class RandomUtils {

//    private static final char[] r = new char[]{'q', 'w', 'e', '8', 's', '2', 'd', 'z', 'x', '9', 'c', '7', 'p', '5', 'k', '3', 'm', 'j', 'u', 'f', 'r', '4', 'v', 'y', 't', 'n', '6', 'b', 'g', 'h'};
//
//    /**
//     * 定义一个字符用来补全邀请码长度（该字符前面是计算出来的邀请码，后面是用来补全用的）
//     */
//    private static final char b = 'a';
//
//    /**
//     * 进制长度
//     */
//    private static final int binLen = r.length;
//
//    /**
//     * 邀请码长度
//     */
//    private static final int s = 6;
//
//
//    /**
//     * 补位字符串
//     */
//    private static final String BUMA = "atgsghj";


    public static void main(String[] args) {

        String code = idToCode(System.currentTimeMillis());
        System.out.println(code);
        System.out.println(codeToId(code));

        Map<String,Long> chongfucheckUniq = new HashMap<String,Long>();
        Map<String,Long> chongfucheck = new HashMap<String,Long>();
        Integer chongfuUniq =0 ;
        Integer chongfu =0 ;

        Long start=System.currentTimeMillis();
        Long max=start+500000;
        for(long i=start;i<max;i++){
            String code1 = getRandomString3(5);

            if(!chongfucheckUniq.containsKey(code1)){
                chongfucheckUniq.put(code1,1L);
            }else{
                chongfuUniq++;
            }


            System.out.println("还有="+ (max-i) +"codeUniq="+code1);

        }

        System.out.println("循环完毕，总循环"+(max-start)+"次,chongfuUniq="+chongfuUniq);
    }

    /**

     自定义进制(0,1没有加入,容易与o,l混淆)，数组顺序可进行调整增加反推难度，A用来补位因此此数组不包含A，共31个字符。
     */
    private static final char[] BASE = new char[]{'H', 'V', 'E', '8', 'S', '2', 'D', 'Z', 'X', '9', 'C', '7', 'P',
            '5', 'I', 'K', '3', 'M', 'J', 'U', 'F', 'R', '4', 'W', 'Y', 'L', 'T', 'N', '6', 'B', 'G', 'Q'};
    /**

     A补位字符，不能与自定义重复
     */
    private static final char SUFFIX_CHAR = 'A';
    /**

     进制长度
     */
    private static final int BIN_LEN = BASE.length;
    /**

     生成邀请码最小长度
     */
    private static final int CODE_LEN = 6;
    /**

     ID转换为邀请码

     @param id

     @return
     */
    public static String idToCode(Long id) {
        char[] buf = new char[BIN_LEN];
        int charPos = BIN_LEN;

        // 当id除以数组长度结果大于0，则进行取模操作，并以取模的值作为数组的坐标获得对应的字符
        while (id / BIN_LEN > 0) {
            int index = (int) (id % BIN_LEN);
            buf[--charPos] = BASE[index];
            id /= BIN_LEN;
        }

        buf[--charPos] = BASE[(int) (id % BIN_LEN)];
        // 将字符数组转化为字符串
        String result = new String(buf, charPos, BIN_LEN - charPos);

        // 长度不足指定长度则随机补全
        int len = result.length();
        if (len < CODE_LEN) {
            StringBuilder sb = new StringBuilder();
            sb.append(SUFFIX_CHAR);
            Random random = new Random();
            // 去除SUFFIX_CHAR本身占位之后需要补齐的位数
            for (int i = 0; i < CODE_LEN - len - 1; i++) {
                sb.append(BASE[random.nextInt(BIN_LEN)]);
            }

            result += sb.toString();
        }

        return result;
    }

    /**

     邀请码解析出ID

     基本操作思路恰好与idToCode反向操作。

     @param code

     @return
     */
    public static Long codeToId(String code) {
        char[] charArray = code.toCharArray();
        long result = 0L;
        for (int i = 0; i < charArray.length; i++) {
            int index = 0;
            for (int j = 0; j < BIN_LEN; j++) {
                if (charArray[i] == BASE[j]) {
                    index = j;
                    break;
                }
            }

            if (charArray[i] == SUFFIX_CHAR) {
                break;
            }

            if (i > 0) {
                result = result * BIN_LEN + index;
            } else {
                result = index;
            }
        }

        return result;

    }


    /**
     * 生成出租车邀请码
     * */
    public static String genRandomNum(){
        List<String> randoms = new ArrayList<>();
        int  maxNum = 36;
        int i;
        int count = 0;
        char[] str = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
                'L', 'M', 'N','P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
                'X', 'Y', 'Z', '2', '3', '4', '5', '6', '7', '8', '9' };
        StringBuffer pwd = new StringBuffer("");
        Random r = new Random();
        while(count < 5){
            i = Math.abs(r.nextInt(maxNum));
            if (i >= 0 && i < str.length) {
                pwd.append(str[i]);
                count ++;
            }
        }
        String code = "D" + pwd.toString();
        if (randoms.contains(code)) {
            return genRandomNum();
        }else {
            randoms.add(code);
            return code;
        }
    }






    public static String getRandomString(int length) {
        // 定义一个字符串（A-Z，a-z，0-9）,把l,1,0,o,O几个容易搞混的去掉,即57位；
        String str = "zxcvbnmkjhgfdsaqwertyuipQWERTYUIPASDFGHJKLZXCVBNM23456789";
        // 由Random生成随机数
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        // 长度为几就循环几次
        for (int i = 0; i < length; ++i) {
            // 产生0-61的数字
            int number = random.nextInt(57);
            // 将产生的数字通过length次承载到sb中
            sb.append(str.charAt(number));
        }
        // 将承载的字符转换成字符串
        return sb.toString();
    }

    public static String getRandomString3(int length) {
        // 定义一个字符串（A-Z去掉O，2-9）,33位；
        String str = "QWERTYUIPASDFGHJKLZXCVBNM23456789";
        // 由Random生成随机数
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        // 长度为几就循环几次
        for (int i = 0; i < length; ++i) {
            // 产生0-61的数字
            int number = random.nextInt(str.length());
            // 将产生的数字通过length次承载到sb中
            sb.append(str.charAt(number));
        }
        // 将承载的字符转换成字符串
        return sb.toString();
    }

    /**
     * 第二种方法
     */
    public static String getRandomString2(int length) {
        // 产生随机数
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        // 循环length次
        for (int i = 0; i < length; i++) {
            // 产生0-2个随机数，既与a-z，A-Z，0-9三种可能
            int number = random.nextInt(3);
            long result = 0;
            switch (number) {
                // 如果number产生的是数字0；
                case 0:
                    // 产生A-Z的ASCII码
                    result = Math.round(Math.random() * 25 + 65);
                    // 将ASCII码转换成字符
                    sb.append(String.valueOf((char) result));
                    break;
                case 1:
                    // 产生a-z的ASCII码
                    result = Math.round(Math.random() * 25 + 97);
                    sb.append(String.valueOf((char) result));
                    break;
                case 2:
                    // 产生0-9的数字
                    sb.append(String.valueOf(new Random().nextInt(10)));
                    break;
            }
        }
        return sb.toString();
    }





}
