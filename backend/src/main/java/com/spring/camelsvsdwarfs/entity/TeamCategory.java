package com.spring.camelsvsdwarfs.entity;

public enum TeamCategory {
    DUO(2),
    TRIO(3),
    QUARTET(4);

    private final int maxMembers;

    TeamCategory(int maxMembers){
        this.maxMembers=maxMembers;
    }

    public int getMaxMembers(){
        return maxMembers;
    }
}
