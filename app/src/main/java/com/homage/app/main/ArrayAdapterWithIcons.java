package com.homage.app.main;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class ArrayAdapterWithIcons extends ArrayAdapter<String> {

    private List<Drawable> images;

    public ArrayAdapterWithIcons(Context context, List<String> items, List<Drawable> images) {
        super(context, android.R.layout.select_dialog_item, items);
        this.images = images;
    }

//    public ArrayAdapterWithIcons(Context context, String[] items, Drawable[] images) {
//        super(context, android.R.layout.select_dialog_item, items);
//        this.images = Arrays.asList(images);
//    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        Drawable image = images.get(position);
        textView.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
        textView.setCompoundDrawablePadding(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics()));
        return view;
    }

}
