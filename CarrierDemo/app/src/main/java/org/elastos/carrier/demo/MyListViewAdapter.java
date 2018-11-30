package org.elastos.carrier.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.elastos.carrier.UserInfo;

import java.util.List;

public class MyListViewAdapter extends ArrayAdapter {

    public MyListViewAdapter(Context context, int resource, List<PostItem> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        // 获取老师的数据
        PostItem item = (PostItem) getItem(position);

        // 创建布局
        View postView = LayoutInflater.from(getContext()).inflate(R.layout.post_item, parent, false);

        // 获取ImageView和TextView
        ImageView imageView = (ImageView) postView.findViewById(R.id.imageView_avatar);
        TextView addr = (TextView) postView.findViewById(R.id.textView_addr);
        TextView brief = (TextView) postView.findViewById(R.id.textView_brief);

        // 根据老师数据设置ImageView和TextView的展现
        //imageView.setImageResource(teacher.getImageId());
        addr.setText(item.name);
        brief.setText(item.msg);

        return postView;
    }
}
