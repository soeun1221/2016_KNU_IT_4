package com.knu.android.talktome;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by star on 2016. 5. 13..
 */
public class MessageAdapter extends ArrayAdapter<MessageItem> {

    ArrayList<MessageItem> messageList = new ArrayList<>();

    public MessageAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public void add(MessageItem object) {
        messageList.add(object);
    }

    @Override
    public int getCount() {
        return messageList.size();
    }

    @Override
    public MessageItem getItem(int position) {
        return messageList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            // inflator를 생성하여, chatting_message.xml을 읽어서 View객체로 생성한다.
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.message_item, parent, false);
        }

        // Array List에 들어 있는 채팅 문자열을 읽어
        MessageItem msg = messageList.get(position);

        // Inflater를 이용해서 생성한 View에, ChatMessage를 삽입한다.
        TextView msgText = (TextView) view.findViewById(R.id.messageTextView);
        msgText.setText(msg.getMessage());
        msgText.setTextColor(Color.parseColor("#000000"));

        return view;
    }
}
