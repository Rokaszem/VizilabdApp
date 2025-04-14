package hu.egyetem.vizilabdapp.ui.add;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hu.egyetem.vizilabdapp.R;

public class AddMatchActivity extends AppCompatActivity {

    private EditText dateEditText, cityEditText;
    private Spinner opponentTeamSpinner;
    private Button saveMatchButton;
    private DatabaseReference teamsReference, matchesReference;
    public static final int MENU_ID = R.id.action_add_match;
    private Spinner currentTeamSpinner; // Hazai csapat Spinner

    private Map<String, String> teamsMap; // Csapat ID -> Csapat név összerendelés


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_match);

        // Firebase referencia
        teamsReference = FirebaseDatabase.getInstance().getReference("teams");
        matchesReference = FirebaseDatabase.getInstance().getReference("matches");

        // UI elemek inicializálása
        dateEditText = findViewById(R.id.matchDateEditText);
        cityEditText = findViewById(R.id.matchCityEditText);
        opponentTeamSpinner = findViewById(R.id.opponentTeamSpinner);
        saveMatchButton = findViewById(R.id.saveMatchButton);
        currentTeamSpinner = findViewById(R.id.currentTeamSpinner);

        // Spinner adatok betöltése
        loadTeamsIntoSpinner();

        // Mentés esemény
        saveMatchButton.setOnClickListener(this::saveMatch);
    }

    private void loadTeamsIntoSpinner() {
        teamsReference.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ArrayList<String> teamNames = new ArrayList<>();
                    teamsMap = new HashMap<>();

                    for (DataSnapshot teamSnapshot : dataSnapshot.getChildren()) {
                        String teamId = teamSnapshot.getKey();
                        String teamName = teamSnapshot.child("name").getValue(String.class);

                        if (teamId != null && teamName != null) {
                            teamNames.add(teamName);
                            teamsMap.put(teamName, teamId); // Név -> ID térkép
                        }
                    }

                    // Adapter az aktuális csapat spinnerhez
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddMatchActivity.this, android.R.layout.simple_spinner_item, teamNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    currentTeamSpinner.setAdapter(adapter); // Beállítjuk az adattáblát a hazai csapat spinnerhez is
                    opponentTeamSpinner.setAdapter(adapter);    // Beállítjuk az ellenfélhez is
                } else {
                    Toast.makeText(AddMatchActivity.this, getString(R.string.error_no_available_team), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AddMatchActivity.this, getString(R.string.error_teams_load,databaseError.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveMatch(View view) {
        String date = dateEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();
        String currentTeamName = (String) currentTeamSpinner.getSelectedItem();
        String opponentName = (String) opponentTeamSpinner.getSelectedItem();

        if (date.isEmpty() || city.isEmpty() ||
                currentTeamName == null || currentTeamName.isEmpty() ||
                opponentName == null || opponentName.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_all_field_required), Toast.LENGTH_SHORT).show();
            return;
        }

        // Azonos csapat kiválasztása ellenőrzés
        if (currentTeamName.equals(opponentName)) {
            Toast.makeText(this, getString(R.string.error_cant_be_same_team), Toast.LENGTH_SHORT).show();
            return;
        }

        // Hazai és ellenfél ID-k lekérése
        String currentTeamId = teamsMap.get(currentTeamName);
        String opponentId = teamsMap.get(opponentName);

        // Mérkőzés azonosító generálása
        String matchId = matchesReference.push().getKey();
        if (matchId != null) {
            Map<String, Object> matchData = new HashMap<>();
            matchData.put("date", date);
            matchData.put("city", city);
            matchData.put("opponentId", opponentId);

            // Meccs mentése az aktuális csapathoz
            matchesReference.child(currentTeamId).child("matches").child(matchId)
                    .setValue(matchData)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(this, getString(R.string.error_team_save), Toast.LENGTH_SHORT).show();
                        }
                    });

            // Meccs mentése az ellenfélhez (fordított logikával)
            Map<String, Object> reverseMatchData = new HashMap<>(matchData);
            reverseMatchData.put("opponentId", currentTeamId); // Az ellenfélnél fordítva mentjük a hazai csapatot

            matchesReference.child(opponentId).child("matches").child(matchId)
                    .setValue(reverseMatchData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, getString(R.string.match_added), Toast.LENGTH_SHORT).show();
                            finish(); // Bezárjuk az Activity-t
                        } else {
                            Toast.makeText(this, getString(R.string.error_save_opponent), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}