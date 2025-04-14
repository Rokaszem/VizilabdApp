package hu.egyetem.vizilabdapp.ui.details;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import hu.egyetem.vizilabdapp.R;
import hu.egyetem.vizilabdapp.ui.modify.ModifyMatchActivity;

public class TeamDetailsActivity extends AppCompatActivity {

    private LinearLayout matchesContainer;
    private DatabaseReference matchesReference, teamsReference;
    private Map<String, String> teamsMap; // Csapatnév -> Csapat ID térkép
    private static final int MODIFY_MATCH_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_details);

        matchesContainer = findViewById(R.id.matchesContainer);

        // A csapat ID átvétele az Intentből
        String teamId = getIntent().getStringExtra("teamId");
        if (teamId == null || teamId.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_team_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Firebase referenciák
        matchesReference = FirebaseDatabase.getInstance().getReference("matches").child(teamId).child("matches");
        teamsReference = FirebaseDatabase.getInstance().getReference("teams");

        // Töltse be a csapatok térképét
        loadTeams();

        // Mérkőzések betöltése
        loadMatches();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Ellenőrizzük, hogy ModifyMatchActivity-ből tértünk vissza
        if (requestCode == MODIFY_MATCH_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra("isModified", false)) {
                // Újrarendereljük az Activity-t, hogy az adatok frissek legyenek
                recreate(); // Ez garantálja, hogy az Activity újra lefut, és friss adatok kerülnek a Firebase-ről
            }
        }
    }

    private void loadMatches() {
        // Tisztítsuk meg a nézetet, hogy ne legyen duplikáció
        matchesContainer.removeAllViews();

        matchesReference.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    HashSet<String> processedMatches = new HashSet<>(); // Az egyedi meccsek tárolása ellenőrzéshez

                    for (DataSnapshot matchSnapshot : dataSnapshot.getChildren()) {
                        String date = matchSnapshot.child("date").getValue(String.class);
                        String city = matchSnapshot.child("city").getValue(String.class);
                        String opponentId = matchSnapshot.child("opponentId").getValue(String.class);

                        // Egyedi kulcs alapján ellenőrizzük a duplikációt
                        String matchKey = date + "|" + city + "|" + opponentId;
                        if (processedMatches.contains(matchKey)) {
                            continue; // Már feldolgozott meccs
                        }
                        processedMatches.add(matchKey); // Adjuk hozzá a kulcsot

                        // Ellenfél adatok betöltése
                        loadOpponentData(opponentId, date, city);
                    }
                } else {
                    Toast.makeText(TeamDetailsActivity.this, getString(R.string.error_matches_not_found), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(TeamDetailsActivity.this, getString(R.string.error_,databaseError.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTeams() {
        teamsReference.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    teamsMap = new HashMap<>();

                    for (DataSnapshot teamSnapshot : dataSnapshot.getChildren()) {
                        String teamId = teamSnapshot.getKey();
                        String teamName = teamSnapshot.child("name").getValue(String.class);

                        if (teamId != null && teamName != null) {
                            teamsMap.put(teamName, teamId); // Térképbe betöltjük: Csapatnév -> Csapat ID
                        }
                    }
                } else {
                    Toast.makeText(TeamDetailsActivity.this, getString(R.string.error_teams_not_found), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(TeamDetailsActivity.this, getString(R.string.error_,databaseError.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOpponentData(String opponentId, String date, String city) {
        teamsReference.child(opponentId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot opponentSnapshot) {
                if (opponentSnapshot.exists()) {
                    String opponentName = opponentSnapshot.child("name").getValue(String.class);
                    String opponentLogoUrl = opponentSnapshot.child("logoUrl").getValue(String.class);

                    // Dinamikus mérkőzés nézet hozzáadása
                    addMatchView(date, city, opponentName, opponentLogoUrl);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(TeamDetailsActivity.this, getString(R.string.error_,databaseError.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMatchFromTeam(String teamId, String date, String city, String opponentId, com.google.android.gms.tasks.OnCompleteListener<Void> onCompleteListener) {
        DatabaseReference teamMatchesRef = FirebaseDatabase.getInstance().getReference("matches").child(teamId).child("matches");

        teamMatchesRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot matchSnapshot : dataSnapshot.getChildren()) {
                    String matchDate = matchSnapshot.child("date").getValue(String.class);
                    String matchCity = matchSnapshot.child("city").getValue(String.class);
                    String matchOpponentId = matchSnapshot.child("opponentId").getValue(String.class);

                    if (matchDate.equals(date) && matchCity.equals(city) && matchOpponentId.equals(opponentId)) {
                        matchSnapshot.getRef().removeValue().addOnCompleteListener(onCompleteListener); // Törlési művelet szinkronizálva
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(TeamDetailsActivity.this, getString(R.string.error_,databaseError.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMatch(String date, String city, String opponentName) {
        String currentTeamId = getIntent().getStringExtra("teamId");
        String opponentId = teamsMap.get(opponentName); // Az ellenfél ID-jének lekérése a teamsMap-ből

        if (currentTeamId == null || opponentId == null) {
            Toast.makeText(this, getString(R.string.error_delete_match), Toast.LENGTH_SHORT).show();
            return;
        }

        // Törlés az aktuális csapat adatainál
        deleteMatchFromTeam(currentTeamId, date, city, opponentId, task -> {
            if (task.isSuccessful()) {
                // Törlés az ellenfélnél
                deleteMatchFromTeam(opponentId, date, city, currentTeamId, opponentTask -> {
                    if (opponentTask.isSuccessful()) {
                        // Törlés sikeres, most frissítjük a nézetet
                        Toast.makeText(this, getString(R.string.match_deleted), Toast.LENGTH_SHORT).show();
                        loadMatches(); // Újratöltjük a meccseket
                    } else {
                        Toast.makeText(this, getString(R.string.error_load_opponent), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, getString(R.string.error_load_current_team), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openModifyMatchActivity(String date, String city, String opponentName) {
        // Intent az ModifyMatchActivity megnyitásához
        Intent intent = new Intent(TeamDetailsActivity.this, hu.egyetem.vizilabdapp.ui.modify.ModifyMatchActivity.class);
        intent.putExtra("teamId", getIntent().getStringExtra("teamId")); // Az aktuális csapat ID-je
        intent.putExtra("date", date); // A meccs dátuma
        intent.putExtra("city", city); // A meccs városa
        intent.putExtra("opponentName", opponentName); // Ellenfél neve
        startActivity(intent);
    }

    private void addMatchView(String date, String city, String opponentName, String opponentLogoUrl) {
        // Hozzunk létre egy új LinearLayout-ot a nézetekhez
        LinearLayout matchLayout = new LinearLayout(this);
        matchLayout.setOrientation(LinearLayout.VERTICAL);
        matchLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        matchLayout.setPadding(16, 16, 16, 16);

        // Ellenfél logója
        ImageView opponentLogoImageView = new ImageView(this);
        opponentLogoImageView.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
        opponentLogoImageView.setPadding(0, 0, 16, 0);
        Picasso.get().load(opponentLogoUrl).into(opponentLogoImageView);

        // Szöveges adatok (Dátum, Város, Ellenfél neve)
        TextView dateTextView = new TextView(this);
        dateTextView.setText("Időpont: " + date);
        TextView cityTextView = new TextView(this);
        cityTextView.setText("Város: " + city);
        TextView opponentNameTextView = new TextView(this);
        opponentNameTextView.setText("Ellenfél: " + opponentName);

        // Módosítás gomb
        Button modifyButton = new Button(this);
        modifyButton.setText("Módosítás");
        modifyButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        modifyButton.setOnClickListener(view -> {
            Intent intent = new Intent(TeamDetailsActivity.this, ModifyMatchActivity.class);
            intent.putExtra("teamId", getIntent().getStringExtra("teamId"));
            intent.putExtra("date", date);
            intent.putExtra("city", city);
            intent.putExtra("opponentName", opponentName);
            startActivityForResult(intent, MODIFY_MATCH_REQUEST); // Az eredmény figyelése
        });

        // Törlés gomb
        Button deleteButton = new Button(this);
        deleteButton.setText("Törlés");
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        deleteButton.setOnClickListener(view -> deleteMatch(date, city, opponentName)); // A törlés logikája már meglévő

        // Nézetek hozzáadása: Logó, szöveges adatok, gombok
        matchLayout.addView(opponentLogoImageView);
        matchLayout.addView(dateTextView);
        matchLayout.addView(cityTextView);
        matchLayout.addView(opponentNameTextView);
        matchLayout.addView(modifyButton); // Módosítás gomb
        matchLayout.addView(deleteButton); // Törlés gomb

        matchesContainer.addView(matchLayout);
    }
}