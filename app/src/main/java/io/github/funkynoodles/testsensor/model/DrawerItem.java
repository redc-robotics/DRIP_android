package io.github.funkynoodles.testsensor.model;

/**
 * Created by Louis on 9/13/2016.
 * Model for item in drawer
 */
public class DrawerItem {

    private String title;
    private String count = "10";
    private boolean isCounterVisible = false;
    public DrawerItem(){}
    public DrawerItem(String title){
        this.title = title;
    }
    public DrawerItem(String title, boolean isCounterVisible, String count){
        this.title = title;
        this.isCounterVisible = isCounterVisible;
        this.count = count;
    }

    public String getTitle(){
        return this.title;
    }

    public String getCount(){
        return this.count;
    }

    public boolean getCounterVisibility(){
        return this.isCounterVisible;
    }

    public void setTitle(String title){
        this.title = title;
    }


    public void setCount(String count){
        this.count = count;
    }

    public void setCounterVisibility(boolean isCounterVisible){
        this.isCounterVisible = isCounterVisible;
    }
}
