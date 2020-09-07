package com.college.task;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class Lists extends AppCompatActivity {

    private DatabaseReference uidref, boardref, cardref, due_dateref, fileref, descref, archived;
    private Button add_acttachment;
    private StorageReference storageRef;
    private String path;
    private ProgressDialog progressDialog;
    private RecyclerView recyclerView;
    private ArrayList<cards> cardlist;
    private cardsadapter cardsadapter;
    private String boardname;
    private final String TITLE = "TITLE";
    private final String DUE = "DUE";
    private final String DESC = "DESC";
    private final String ARCHIVED = "ARCHIVED";
    private final String True = "true";
    private final String False = "false";
    private int archivedcount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lists);

        boardname = Objects.requireNonNull(getIntent().getStringExtra("Task_name")).trim();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setTitle(boardname.toUpperCase());

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference(user.getUid());

        final TextView Archived_count = findViewById(R.id.Archived_count);
        recyclerView = findViewById(R.id.recycle);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        uidref = FirebaseDatabase.getInstance().getReference().child(user.getUid());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
        }



        DatabaseReference rootref = uidref.child(boardname);
        rootref.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int childerncount = (int) dataSnapshot.getChildrenCount();
                cardlist = new ArrayList<>();
                if (childerncount <= 1) {
                    Toast.makeText(Lists.this, "No cards", Toast.LENGTH_SHORT).show();
                } else {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        String due = ds.child(DUE).getValue(String.class);
                        String title = ds.child(TITLE).getValue(String.class);
                        String desc = ds.child(DESC).getValue(String.class);
                        String archived = ds.child(ARCHIVED).getValue(String.class);
                        assert archived != null;
                        if (title != null) {
                            if (!archived.equals(True)) {
                                cards card = new cards(due, title, desc);
                                cardlist.add(card);
                            } else {
                                archivedcount++;
                            }
                        }
                    }
                    Archived_count.setText("Archived (" + archivedcount + ")");
                    cardsadapter = new cardsadapter(cardlist);
                    recyclerView.setAdapter(cardsadapter);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        Archived_count.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (archivedcount == 0) {
                    Toast.makeText(Lists.this, "No archived items found", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(Lists.this, archived.class);
                    intent.putExtra("Task_name", boardname);
                    startActivity(intent);
                }
            }

        });



        findViewById(R.id.add_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Connection.isInternetAvailable(Lists.this)) {
            Snackbar.make(recyclerView, "No connection", 3000).show();
        }
    }

    public class cardsadapter extends RecyclerView.Adapter<cardsadapter.holder> {
        ArrayList<cards> cardslist;

        cardsadapter(ArrayList<cards> cardslist) {
            this.cardslist = cardslist;
        }

        @NonNull
        @Override
        public holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cards, parent, false);
            return new holder(view);

        }

        @Override
        public void onBindViewHolder(@NonNull final holder holder, final int position) {

            holder.desc.setText(cardslist.get(position).getDESC());
            holder.time.setText(cardslist.get(position).getDUE());
            holder.title.setText(cardslist.get(position).getTITLE());
            holder.archive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    progressDialog.show();
                    progressDialog.setMessage("Please wait..");
                    boardref = uidref.child(Objects.requireNonNull(boardname)).child(cardslist.get(position).getTITLE());
                    archived = boardref.child(ARCHIVED);
                    cardlist.remove(position);
                    notifyItemRemoved(position);
                    cardsadapter.notifyDataSetChanged();
                    Snackbar.make(recyclerView, "Archived", 3000).show();
                    archived.setValue(True).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            progressDialog.dismiss();
                            recreate();
                        }
                    });
                }
            });


        }

        @Override
        public int getItemCount() {
            return cardslist.size();
        }

        class holder extends RecyclerView.ViewHolder {
            TextView desc, time, title;
            ImageButton archive;


            holder(@NonNull View itemView) {
                super(itemView);
                desc = itemView.findViewById(R.id.desc);
                time = itemView.findViewById(R.id.due);
                title = itemView.findViewById(R.id.title);
                archive = itemView.findViewById(R.id.archive);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menulist, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            super.onBackPressed();
            return (true);
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void createnotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Task";
            String desc = "Chanel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("notify", name, importance);
            channel.setDescription(desc);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            Objects.requireNonNull(notificationManager).createNotificationChannel(channel);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void broadcast(String finalDue_time1) {
        Intent intent = new Intent(Lists.this, ReminderBroadcast.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(Lists.this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
        Date mDate = null;
        try {
            mDate = sdf.parse(finalDue_time1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assert mDate != null;
        long timeInMilliseconds = mDate.getTime();
        Objects.requireNonNull(alarmManager).set(AlarmManager.RTC_WAKEUP, timeInMilliseconds, pendingIntent);
    }

    private void showDialog() {
        final MaterialDatePicker.Builder datepicker = MaterialDatePicker.Builder.datePicker();

        datepicker.setTitleText("Select Date");
        final MaterialDatePicker materialDatePicker = datepicker.build();


        final Dialog builder = new Dialog(Lists.this);
        builder.setCancelable(false);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);

        builder.setContentView(R.layout.add_card);

        final int[] hours = new int[1];
        final int[] min = new int[1];
        final EditText card_name = builder.findViewById(R.id.card_name);
        final EditText description = builder.findViewById(R.id.card_desc);
        final LinearLayout datePickerLayout = builder.findViewById(R.id.date_picker_actions);
        final TextView datetext = builder.findViewById(R.id.date);
        Button done = builder.findViewById(R.id.done);
        final ImageButton cancle = builder.findViewById(R.id.cancel_button);

        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.dismiss();
            }
        });
        datePickerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                materialDatePicker.show(getSupportFragmentManager(), "DATE_PICKER");
            }

        });
        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Object selection) {

                final String date = "Due : " + materialDatePicker.getHeaderText();
                Calendar mcurrentTime = Calendar.getInstance();
                final int[] hour = {mcurrentTime.get(Calendar.HOUR_OF_DAY)};
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker = new TimePickerDialog(Lists.this, new TimePickerDialog.OnTimeSetListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        datetext.setText(date + " " + selectedHour + ":" + selectedMinute);
                        hours[0] = selectedHour;
                        min[0] = selectedMinute;
                    }

                }, hour[0], minute, false);
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();

            }
        });


        done.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {

                final String card_na = card_name.getText().toString().trim().toUpperCase();
                String due_time = datetext.getText().toString();
                final String desc = description.getText().toString().trim();
                boardref = uidref.child(Objects.requireNonNull(boardname)).child(card_na);
                cardref = boardref.child(TITLE);
                due_dateref = boardref.child(DUE);
                descref = boardref.child(DESC);
                archived = boardref.child(ARCHIVED);
                if (card_na.isEmpty()) {
                    Toast.makeText(Lists.this, "Please enter card name", Toast.LENGTH_SHORT).show();
                } else if (!due_time.contains("Due")) {
                    Toast.makeText(Lists.this, "Please select due time", Toast.LENGTH_SHORT).show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        createnotification();
                    }

                    try {
                        final String finalDue_time = due_time;
                                        due_dateref.setValue(finalDue_time);
                                        descref.setValue(desc);
                                        broadcast(finalDue_time);
                                        archived.setValue(False);
                                        cardref.setValue(card_na.trim()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                recreate();
                                            }
                                        });
                    } catch (Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(Lists.this, "Something wrong", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        builder.show();
    }

}
