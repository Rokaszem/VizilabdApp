package hu.egyetem.vizilabdapp.data.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import hu.egyetem.vizilabdapp.data.model.Team;

public class FirebaseRepository {

    private final DatabaseReference teamsRef;

    public FirebaseRepository() {
        // Hozzáférés a "teams" csomóponthoz
        teamsRef = FirebaseDatabase.getInstance().getReference("teams");
    }

    public void fetchTeams(FirebaseCallback<List<Team>> callback) {
        teamsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Team> teams = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    Team team = childSnapshot.getValue(Team.class);
                    teams.add(team);
                }
                callback.onSuccess(teams);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onFailure(error.toException());
            }
        });
    }

    public interface FirebaseCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}

