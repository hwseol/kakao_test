package com.example.mykacao

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class ListViewAdpater(val context : Context, val List : MutableList<ListLayout>) : BaseAdapter(){

    // View를 꾸며주는 부분
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

        // converView가 null이 아닐 경우 View를 재활용
        // 이 부분이 없다면, View를 리스트의 갯수만큼 호출해야 함
        var convertView = convertView
        if (convertView == null) {
            // list_view_item 을 가져온다
            convertView = LayoutInflater.from(parent?.context).inflate(R.layout.list_view_item, parent, false)
        }

        // List에 있는 데이터들을 하나씩 list_view_item의 textView의 아이디를 찾아서 넣어줌
        val title = convertView?.findViewById<TextView>(R.id.itemTextId)
        val list_item = List[position]
        title!!.text = list_item.name + "\n" + list_item.road
        return convertView

    }

    // 각각의 리스트 하나씩 가져오는 부분
    override fun getItem(position: Int): Any {
        return List[position]
    }

    // 리스트의 ID를 가져오는 부분
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // 리스트의 전체 크기
    override fun getCount(): Int {
        return List.size
    }

}