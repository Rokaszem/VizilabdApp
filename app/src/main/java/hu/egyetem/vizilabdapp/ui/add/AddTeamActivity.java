package hu.egyetem.vizilabdapp.ui.add;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import hu.egyetem.vizilabdapp.R;

public class AddTeamActivity extends AppCompatActivity {

    private EditText teamNameEditText, teamLogoEditText;
    private Button saveTeamButton;
    public static final int MENU_ID = R.id.action_add_team;

    private DatabaseReference teamsReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_team);

        // Firebase referencia
        teamsReference = FirebaseDatabase.getInstance().getReference("teams");

        // UI elemek inicializálása
        teamNameEditText = findViewById(R.id.teamNameEditText);
        teamLogoEditText = findViewById(R.id.teamLogoEditText);
        saveTeamButton = findViewById(R.id.saveTeamButton);

        // Mentés gomb esemény
        saveTeamButton.setOnClickListener(this::saveTeam);
    }

    private void saveTeam(View view) {
        String teamName = teamNameEditText.getText().toString().trim();
        String teamLogoUrl = teamLogoEditText.getText().toString().trim();

        if (teamName.isEmpty() || teamLogoUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_all_field_required), Toast.LENGTH_SHORT).show();
            return;
        }

        // Új csapat létrehozása az adatbázisban
        String teamId = teamsReference.push().getKey();
        if (teamId != null) {
            teamsReference.child(teamId).child("name").setValue(teamName);
            teamsReference.child(teamId).child("logoUrl").setValue(teamLogoUrl).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this,getString(R.string.team_added) , Toast.LENGTH_SHORT).show();
                    finish(); // Bezárjuk az Activity-t
                } else {
                    Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}