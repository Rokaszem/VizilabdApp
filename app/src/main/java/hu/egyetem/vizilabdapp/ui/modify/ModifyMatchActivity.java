package hu.egyetem.vizilabdapp.ui.modify;

import android.content.Intent;
import android.os.Bundle;
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

public class ModifyMatchActivity extends AppCompatActivity {

    private EditText dateEditText, cityEditText;
    private Spinner opponentTeamSpinner;
    private Button saveChangesButton;
    private DatabaseReference teamsReference, matchesReference;
    private Map<String, String> teamsMap; // Csapatnév -> Csapat ID térkép
    private String teamId, oldDate, oldCity, oldOpponentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_match);

        // Firebase referencia
        teamsReference = FirebaseDatabase.getInstance().getReference("teams");
        matchesReference = FirebaseDatabase.getInstance().getReference("matches");

        // UI elemek inicializálása
        dateEditText = findViewById(R.id.matchDateEditText);
        cityEditText = findViewById(R.id.matchCityEditText);
        opponentTeamSpinner = findViewById(R.id.opponentTeamSpinner);
        saveChangesButton = findViewById(R.id.saveChangesButton);

        // Intentből kapott adatok
        teamId = getIntent().getStringExtra("teamId");
        oldDate = getIntent().getStringExtra("date");
        oldCity = getIntent().getStringExtra("city");
        oldOpponentName = getIntent().getStringExtra("opponentName");

        // Spinner adatok betöltése
        loadTeamsIntoSpinner(() -> {
            // Adatok előzetes feltöltése
            dateEditText.setText(oldDate);
            cityEditText.setText(oldCity);

            if (oldOpponentName != null) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) opponentTeamSpinner.getAdapter();
                int position = adapter.getPosition(oldOpponentName);
                opponentTeamSpinner.setSelection(position);
            }
        });

        // Módosítás mentése gomb
        saveChangesButton.setOnClickListener(v -> saveChanges());
    }

    private void loadTeamsIntoSpinner(Runnable onComplete) {
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

                    // Spinner feltöltése adatokkal
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ModifyMatchActivity.this, android.R.layout.simple_spinner_item, teamNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    opponentTeamSpinner.setAdapter(adapter);

                    // Hívjuk meg az onComplete műveletet, hogy az alapadatokat beállítsuk
                    if (onComplete != null) {
                        onComplete.run();
                    }
                } else {
                    Toast.makeText(ModifyMatchActivity.this,getString( R.string.error_no_available_teams), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ModifyMatchActivity.this, getString(R.string.error_,databaseError.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveChanges() {
        String newDate = dateEditText.getText().toString().trim();
        String newCity = cityEditText.getText().toString().trim();
        String newOpponentName = (String) opponentTeamSpinner.getSelectedItem();
        String newOpponentId = teamsMap.get(newOpponentName);

        if (newDate.isEmpty() || newCity.isEmpty() || newOpponentName == null || newOpponentName.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_all_fileds_required), Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase módosítása
        deleteMatch(teamId, oldDate, oldCity, teamsMap.get(oldOpponentName), task -> {
            if (task.isSuccessful()) {
                String matchId = matchesReference.push().getKey();
                Map<String, Object> matchData = new HashMap<>();
                matchData.put("date", newDate);
                matchData.put("city", newCity);
                matchData.put("opponentId", newOpponentId);

                matchesReference.child(teamId).child("matches").child(matchId)
                        .setValue(matchData)
                        .addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Map<String, Object> reverseMatchData = new HashMap<>(matchData);
                                reverseMatchData.put("opponentId", teamId);

                                matchesReference.child(newOpponentId).child("matches").child(matchId)
                                        .setValue(reverseMatchData)
                                        .addOnCompleteListener(opponentTask -> {
                                            if (opponentTask.isSuccessful()) {
                                                Toast.makeText(ModifyMatchActivity.this,getString( R.string.match_modified), Toast.LENGTH_SHORT).show();
                                                Intent resultIntent = new Intent();
                                                resultIntent.putExtra("isModified", true); // Jelöljük, hogy történt-e módosítás
                                                setResult(RESULT_OK, resultIntent);
                                                finish(); // Zárjuk be az Activity-t
                                            } else {
                                                Toast.makeText(ModifyMatchActivity.this, getString(R.string.error_updateing_opponent), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(ModifyMatchActivity.this, getString(R.string.error_updateing_match), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(ModifyMatchActivity.this,getString( R.string.error_deleting_old_match), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMatch(String teamId, String date, String city, String opponentId, com.google.android.gms.tasks.OnCompleteListener<Void> onCompleteListener) {
        DatabaseReference teamMatchesRef = FirebaseDatabase.getInstance().getReference("matches").child(teamId).child("matches");

        teamMatchesRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot matchSnapshot : dataSnapshot.getChildren()) {
                    String matchDate = matchSnapshot.child("date").getValue(String.class);
                    String matchCity = matchSnapshot.child("city").getValue(String.class);
                    String matchOpponentId = matchSnapshot.child("opponentId").getValue(String.class);

                    if (matchDate.equals(date) && matchCity.equals(city) && matchOpponentId.equals(opponentId)) {
                        matchSnapshot.getRef().removeValue().addOnCompleteListener(onCompleteListener);
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ModifyMatchActivity.this, getString(R.string.error_,databaseError.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }
}