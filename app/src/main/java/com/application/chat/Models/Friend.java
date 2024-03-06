package com.application.chat.Models;

public class Friend {
    private String friendId;
    private int messageCount;

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
    private boolean typing;

    public Friend(){}
    public Friend(String friendId) {
        this.friendId = friendId;
    }
    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public int getMessageCount() {
        return messageCount;
    }
    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}
