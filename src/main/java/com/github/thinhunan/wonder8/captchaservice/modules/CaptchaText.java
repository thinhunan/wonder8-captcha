package com.github.thinhunan.wonder8.captchaservice.modules;

public final class CaptchaText {
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public CaptchaText(String text, int x, int y, int fontSize) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.fontSize = fontSize;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    String text;
    int x;
    int y;
    int fontSize;
    byte[] data;

    @Override
    public String toString() {
        return "CaptchaText{" +
                "text='" + text + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", size=" + fontSize +
                '}';
    }

}
