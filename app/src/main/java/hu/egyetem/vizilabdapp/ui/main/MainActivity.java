package hu.egyetem.vizilabdapp.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import hu.egyetem.vizilabdapp.R;
import hu.egyetem.vizilabdapp.ui.add.AddMatchActivity;
import hu.egyetem.vizilabdapp.ui.add.AddTeamActivity;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference teamsReference;
    private LinearLayout teamsContainer;
    private boolean isUserAuthenticated;
    public static final int MENU_ID = R.id.action_view_teams;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        // Ellenőrizzük, hogy a felhasználó be van-e jelentkezve
        if (currentUser != null && !currentUser.isAnonymous()) {
            setupBottomNavigation(); // Navigáció beállítása
        }

        // UI elemek inicializálása
        teamsContainer = findViewById(R.id.teamsContainer);

        // Firebase Database referencia inicializálása
        teamsReference = FirebaseDatabase.getInstance().getReference("teams");

        // Csapatok betöltése
        loadTeamsData();
    }

    private void loadTeamsData() {
        if (teamsReference == null) {
            Toast.makeText(this, getString(R.string.error_no_db_connection), Toast.LENGTH_SHORT).show();
            return;
        }

        teamsReference.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    teamsContainer.removeAllViews(); // Korábbi nézetek törlése

                    for (DataSnapshot teamSnapshot : dataSnapshot.getChildren()) {
                        // Csapat adatok lekérése Firebase-ből
                        String teamId = teamSnapshot.getKey(); // Csapat azonosítója
                        String teamName = teamSnapshot.child("name").getValue(String.class);
                        String teamLogoUrl = teamSnapshot.child("logoUrl").getValue(String.class);

                        // Dinamikus csapat nézet létrehozása
                        addTeamView(teamId, teamName, teamLogoUrl);
                    }
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.error_teams_not_found), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, getString(R.string.error_query,databaseError.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTeamView(String teamId, String teamName, String teamLogoUrl) {
        // Külső layout: egy csapathoz
        LinearLayout teamLayout = new LinearLayout(this);
        teamLayout.setOrientation(LinearLayout.HORIZONTAL);
        teamLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        teamLayout.setPadding(0, 16, 0, 16);

        // Csapat logója (ImageView)
        androidx.appcompat.widget.AppCompatImageView teamLogoImageView = new androidx.appcompat.widget.AppCompatImageView(this);
        teamLogoImageView.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
        teamLogoImageView.setPadding(0, 0, 16, 0);
        com.squareup.picasso.Picasso.get().load(teamLogoUrl).into(teamLogoImageView);

        // Csapat neve (TextView)
        android.widget.TextView teamNameTextView = new android.widget.TextView(this);
        teamNameTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        teamNameTextView.setText(teamName);
        teamNameTextView.setTextSize(18);

        // Elemek hozzáadása a layouthoz
        teamLayout.addView(teamLogoImageView);
        teamLayout.addView(teamNameTextView);

        // Kattintás esemény hozzárendelése
        teamLayout.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, hu.egyetem.vizilabdapp.ui.details.TeamDetailsActivity.class);
            intent.putExtra("teamId", teamId); // Csapat ID átadása
            startActivity(intent);
        });

        // Csapat nézet hozzáadása a fő konténerhez
        teamsContainer.addView(teamLayout);
    }


    private int getCurrentMenuItemId() {
        try {
            // Az aktuális Activity osztály "MENU_ID" konstansának lekérdezése
            return (int) this.getClass().getField("MENU_ID").get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return -1; // Ha nincs definiált ID
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setVisibility(View.VISIBLE); // Navigációs menü megjelenítése

        // Menüpont aktív állapota
        int currentMenuItemId = getCurrentMenuItemId(); // Ellenőrzi az aktuális képernyő menü ID-jét
        bottomNavigationView.setSelectedItemId(currentMenuItemId); // Beállítja az aktívan megnyitott képernyőt.

        // Menüelemek kattintási eseményei
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // A már aktív képernyő menü gombjának tiltása (nincs újranyitás)
            if (itemId == currentMenuItemId) {
                return false; // Az aktuális képernyőhöz tartozó elem nincs kattintási eseményhez kötve.
            }

            if (itemId == R.id.action_view_teams) {
                // Navigáljon a főképernyőre (MainActivity)
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                return true;
            }else if (itemId == R.id.action_add_team) {
                // Csapat hozzáadása nézet megnyitása
                startActivity(new Intent(MainActivity.this, AddTeamActivity.class));
                return true;
            } else if (itemId == R.id.action_add_match) {
                // Mérkőzés hozzáadása nézet megnyitása
                startActivity(new Intent(MainActivity.this, AddMatchActivity.class));
                return true;
            } else {
                return false;
            }
        });
    }
}