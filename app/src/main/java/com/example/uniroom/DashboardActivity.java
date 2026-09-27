package com.example.uniroom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uniroom.model.Models.*;
import com.example.uniroom.network.Client;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {
    Session s;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); s=new Session(this); if(!s.protect(this)){finish();return;}
        setContentView(R.layout.activity_dashboard);
        ((TextView)findViewById(R.id.welcome)).setText("Welcome, "+s.name()+"!");
        findViewById(R.id.schedule).setOnClickListener(v->startActivity(new Intent(this,ScheduleActivity.class)));
        findViewById(R.id.rooms).setOnClickListener(v->startActivity(new Intent(this,FindRoomActivity.class)));
        findViewById(R.id.roomCrud).setOnClickListener(v->startActivity(new Intent(this,RoomCrudActivity.class)));
        findViewById(R.id.profile).setOnClickListener(v->startActivity(new Intent(this,ProfileActivity.class)));
        findViewById(R.id.logout).setOnClickListener(v->logout());
        load();
    }
    void load(){Client.api().schedule(s.id()).enqueue(new Callback<ListResponse<Schedule>>(){
        public void onResponse(Call<ListResponse<Schedule>> c,Response<ListResponse<Schedule>> r){
            if(r.body()!=null&&r.body().data!=null&&!r.body().data.isEmpty()){Schedule x=r.body().data.get(0);((TextView)findViewById(R.id.nextText)).setText(x.subject_code+" • "+x.room_name+"\n"+x.day_of_week+" "+x.start_time+"–"+x.end_time);}
            else ((TextView)findViewById(R.id.nextText)).setText("No schedule available");
        }
        public void onFailure(Call<ListResponse<Schedule>> c,Throwable t){((TextView)findViewById(R.id.nextText)).setText("Schedule unavailable");}
    });}
    void logout(){s.clear();Toast.makeText(this,"Logged out successfully.",Toast.LENGTH_SHORT).show();Intent i=new Intent(this,LoginActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);}
}
