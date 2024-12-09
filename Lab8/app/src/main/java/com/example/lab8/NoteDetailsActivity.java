package com.example.lab8;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class NoteDetailsActivity extends AppCompatActivity {
    private FragmentNoteDetails detailsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_details);

        // Получаем заметку из Intent
        Note note = getIntent().getParcelableExtra("note");
        if (note == null) {
            finish();
            return;
        }

        // Создаём фрагмент и передаём данные заметки
        detailsFragment = new FragmentNoteDetails();
        Bundle bundle = new Bundle();
        bundle.putParcelable("note", note);
        detailsFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailsFragmentContainer, detailsFragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        Log.d("NoteDetailsActivity", "onBackPressed called");

        Intent resultIntent = new Intent();
        if (detailsFragment != null) {
            Note updatedNote = detailsFragment.getUpdatedNote();
            Log.d("NoteDetailsActivity", "Updated note to return: " + updatedNote.getTitle());
            resultIntent.putExtra("updatedNote", updatedNote);
        } else {
            Log.e("NoteDetailsActivity", "detailsFragment is null");
        }
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }

}