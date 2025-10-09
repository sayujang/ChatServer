package com.chat;

import java.util.ArrayList;
import java.util.List;

public class checks {
    public static void main(String arg[])
    {
        String a="sayuj";
        System.out.println(a.substring(0,3));
        List<Integer> b=new ArrayList<>();
        b.add(5);
        b.add(5);
        b.add(5);
        b.add(5);
        for( int i=0; i<b.size();i++)
        {
            b.remove(i);
        }
        System.out.println(b);
        // for(int i=b.size()-1; i>=0;i--)
        // {
        //     b.remove(i);
        // }
        // System.out.println(b);
    }

    
}
