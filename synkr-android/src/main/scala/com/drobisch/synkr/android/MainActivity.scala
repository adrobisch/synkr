package com.drobisch.synkr.android

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.ViewGroup
import android.widget.TextView


class FileViewHolder(val view: TextView) extends RecyclerView.ViewHolder(view)

class MainActivity extends AppCompatActivity {
  // allows accessing `.value` on TR.resource.constants
  implicit val context = this

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    // type ascription is required due to SCL-10491
    val vh: TypedViewHolder.main = TypedViewHolder.setContentView(this, TR.layout.main)

    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    vh.file_list.setHasFixedSize(true);

    vh.file_list.setLayoutManager(new LinearLayoutManager(this))

    vh.file_list.setAdapter(new RecyclerView.Adapter[FileViewHolder] {
      override def getItemCount: Int = 2

      override def onBindViewHolder(vh: FileViewHolder, i: Int): Unit = {
        vh.view.setText("foo")
      }

      override def onCreateViewHolder(viewGroup: ViewGroup, i: Int): FileViewHolder = {
        new FileViewHolder(new TextView(context))
      }
    })

  }
}