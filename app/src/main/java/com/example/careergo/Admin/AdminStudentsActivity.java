package com.example.careergo.Admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.careergo.Adapters.StudentsAdapter;
import com.example.careergo.Model.Student;
import com.example.careergo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminStudentsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText etSearchStudent;
    private RecyclerView rvStudents;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private DatabaseReference mDatabase;
    private StudentsAdapter studentsAdapter;
    private List<Student> studentList;
    private List<Student> filteredStudentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_students);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        fetchStudents();
        setupSearch();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearchStudent = findViewById(R.id.etSearchStudent);
        rvStudents = findViewById(R.id.rvStudents);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        studentList = new ArrayList<>();
        filteredStudentList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Students");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        studentsAdapter = new StudentsAdapter(filteredStudentList, this);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(studentsAdapter);
    }

    private void fetchStudents() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                studentList.clear();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    // Check if user is a student/job seeker
                    String userType = userSnapshot.child("userType").getValue(String.class);
                    String role = userSnapshot.child("role").getValue(String.class);

                    // Support both "userType" and "role" fields
                    boolean isStudent = "student".equalsIgnoreCase(userType) ||
                            "Job Seeker".equalsIgnoreCase(role) ||
                            "job_seeker".equalsIgnoreCase(userType);

                    if (isStudent) {
                        Student student = userSnapshot.getValue(Student.class);
                        if (student != null) {
                            student.setId(userSnapshot.getKey());
                            studentList.add(student);
                        }
                    }
                }

                filteredStudentList.clear();
                filteredStudentList.addAll(studentList);
                studentsAdapter.notifyDataSetChanged();

                progressBar.setVisibility(View.GONE);
                updateEmptyState();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setText("Error loading students: " + error.getMessage());
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupSearch() {
        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterStudents(String query) {
        filteredStudentList.clear();
        if (query.isEmpty()) {
            filteredStudentList.addAll(studentList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Student student : studentList) {
                boolean matches = false;

                // Check first name
                if (student.getFirstName() != null && student.getFirstName().toLowerCase().contains(lowerCaseQuery)) {
                    matches = true;
                }
                // Check last name
                else if (student.getLastName() != null && student.getLastName().toLowerCase().contains(lowerCaseQuery)) {
                    matches = true;
                }
                // Check email
                else if (student.getEmail() != null && student.getEmail().toLowerCase().contains(lowerCaseQuery)) {
                    matches = true;
                }
                // Check city
                else if (student.getCity() != null && student.getCity().toLowerCase().contains(lowerCaseQuery)) {
                    matches = true;
                }
                // Check state
                else if (student.getState() != null && student.getState().toLowerCase().contains(lowerCaseQuery)) {
                    matches = true;
                }

                if (matches) {
                    filteredStudentList.add(student);
                }
            }
        }
        studentsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredStudentList.isEmpty()) {
            if (studentList.isEmpty()) {
                tvEmptyState.setText("No students found");
            } else {
                tvEmptyState.setText("No students match your search");
            }
            tvEmptyState.setVisibility(View.VISIBLE);
            rvStudents.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvStudents.setVisibility(View.VISIBLE);
        }
    }
}