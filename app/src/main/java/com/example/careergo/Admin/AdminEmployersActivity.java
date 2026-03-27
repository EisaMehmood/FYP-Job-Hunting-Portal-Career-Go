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
import com.example.careergo.Adapters.EmployersAdapter;
import com.example.careergo.Model.Employer;
import com.example.careergo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class AdminEmployersActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText etSearchEmployer;
    private RecyclerView rvEmployers;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private DatabaseReference mDatabase;
    private EmployersAdapter employersAdapter;
    private List<Employer> employerList;
    private List<Employer> filteredEmployerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_employers);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        fetchEmployers();
        setupSearch();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearchEmployer = findViewById(R.id.etSearchEmployer);
        rvEmployers = findViewById(R.id.rvEmployers);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        employerList = new ArrayList<>();
        filteredEmployerList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        employersAdapter = new EmployersAdapter(filteredEmployerList,this);
        rvEmployers.setLayoutManager(new LinearLayoutManager(this));
        rvEmployers.setAdapter(employersAdapter);
    }

    private void fetchEmployers() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("users").orderByChild("role").equalTo("Employer")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        employerList.clear();
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            Employer employer = userSnapshot.getValue(Employer.class);
                            if (employer != null) {
                                employer.setId(userSnapshot.getKey());

                                // Fetch company profile
                                mDatabase.child("companyProfiles").child(userSnapshot.getKey())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot companySnapshot) {
                                                if (companySnapshot.exists()) {
                                                    employer.setCompanyName(companySnapshot.child("companyName").getValue(String.class));
                                                    employer.setIndustry(companySnapshot.child("industry").getValue(String.class));
                                                    employersAdapter.notifyDataSetChanged();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError error) {}
                                        });

                                employerList.add(employer);
                            }
                        }
                        filteredEmployerList.clear();
                        filteredEmployerList.addAll(employerList);
                        employersAdapter.notifyDataSetChanged();

                        progressBar.setVisibility(View.GONE);
                        updateEmptyState();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        tvEmptyState.setText("Error loading employers");
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setupSearch() {
        etSearchEmployer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEmployers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterEmployers(String query) {
        filteredEmployerList.clear();
        if (query.isEmpty()) {
            filteredEmployerList.addAll(employerList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Employer employer : employerList) {
                if ((employer.getCompanyName() != null && employer.getCompanyName().toLowerCase().contains(lowerCaseQuery)) ||
                        (employer.getFirstName() != null && employer.getFirstName().toLowerCase().contains(lowerCaseQuery)) ||
                        (employer.getLastName() != null && employer.getLastName().toLowerCase().contains(lowerCaseQuery)) ||
                        (employer.getEmail() != null && employer.getEmail().toLowerCase().contains(lowerCaseQuery)) ||
                        (employer.getIndustry() != null && employer.getIndustry().toLowerCase().contains(lowerCaseQuery))) {
                    filteredEmployerList.add(employer);
                }
            }
        }
        employersAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredEmployerList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvEmployers.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvEmployers.setVisibility(View.VISIBLE);
        }
    }
}