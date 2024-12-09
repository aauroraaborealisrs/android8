package com.example.lab8;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private NotesDatabase db; // Инициализация базы данных
    private NotesDao notesDao; // DAO для работы с заметками
    private List<Note> notes = new ArrayList<>(); // Хранение списка заметок
    private FragmentNoteList noteListFragment; // Фрагмент для отображения списка заметок

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_main);

        // Настройка отступов для системных панелей
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Установка Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Инициализация базы данных и DAO
        db = NotesDatabase.getInstance(this);
        notesDao = db.notesDao();

        // Создание фрагмента для отображения списка заметок
        noteListFragment = new FragmentNoteList();

        fillDatabaseWithSampleNotes();

        // Загружаем заметки из базы данных
        loadNotesFromDatabase();

        // Настраиваем режим работы (телефон/планшет)
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (findViewById(R.id.detailsFragmentContainer) != null) {
            // Планшетный режим
            noteListFragment.setOnNoteClickListener(note -> {
                FragmentNoteDetails detailsFragment = new FragmentNoteDetails();

                // Передача данных заметки
                Bundle bundle = new Bundle();
                bundle.putParcelable("note", note);
                detailsFragment.setArguments(bundle);

                // Обновление заметки через Listener
//                detailsFragment.setOnNoteUpdatedListener(updatedNote -> {
//                    updateNoteInDatabase(updatedNote);
//                });

                detailsFragment.setOnNoteUpdatedListener(updatedNote -> {
                    if (updatedNote == null) {
                        new Thread(this::loadNotesFromDatabase).start(); // Перезагружаем список
                        // Если заметка удалена, убираем детальный фрагмент
                        if (findViewById(R.id.detailsFragmentContainer) != null) {
                            getSupportFragmentManager().beginTransaction()
                                    .remove(detailsFragment)
                                    .commit();
                        } else {
                            // Если это телефонный режим, просто завершаем активность
                            finish();
                        }
                    } else {
                        new Thread(() -> {
                            if (updatedNote.getId() == 0) {
                                notesDao.insert(updatedNote);
                            } else {
                                notesDao.update(updatedNote);
                            }
                            loadNotesFromDatabase();
                        }).start();
                    }
                });


                // Замена фрагмента деталей
                fragmentManager.beginTransaction()
                        .replace(R.id.detailsFragmentContainer, detailsFragment)
                        .commit();
            });

            // Отображение списка заметок
            fragmentManager.beginTransaction()
                    .replace(R.id.listFragmentContainer, noteListFragment)
                    .commit();
        } else {
            // Телефонный режим
            noteListFragment.setOnNoteClickListener(note -> {
                Intent intent = new Intent(this, NoteDetailsActivity.class);
                intent.putExtra("note", note);
                startActivityForResult(intent, 100);
            });

            // Отображение списка заметок
            fragmentManager.beginTransaction()
                    .replace(R.id.mainFragmentContainer, noteListFragment)
                    .commit();
        }
    }

    public void loadNotesFromDatabase() {
        Log.d("депрессия", "loadNotesFromDatabase called.");

        new Thread(() -> {
            notes = notesDao.getAllNotes(); // Получаем заметки из базы
            Log.d("MainActivity", "Загруженные заметки: " + notes.size());
            for (Note note : notes) {
                Log.d("MainActivity", "Заметка: " + note.getTitle() + ", " + note.getContent());
            }
            runOnUiThread(() -> {
                noteListFragment.setNotes(notes);
                if (noteListFragment.getAdapter() != null) {
                    noteListFragment.getAdapter().notifyDataSetChanged();
                } else {
                    Log.e("MainActivity", "Адаптер не инициализирован.");
                }
            });
        }).start();
    }


    private void updateNoteInDatabase(Note updatedNote) {
        new Thread(() -> {
            notesDao.update(updatedNote); // Обновляем заметку в базе данных
            loadNotesFromDatabase(); // Обновляем список
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Note updatedNote = data.getParcelableExtra("updatedNote");
            if (updatedNote == null) {
                // Если заметка удалена
                loadNotesFromDatabase();
            } else {
                // Если заметка обновлена
                updateNoteInDatabase(updatedNote);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            // Создаем новую заметку
            Note newNote = new Note("", "");

            FragmentNoteDetails detailsFragment = new FragmentNoteDetails();
            Bundle bundle = new Bundle();
            bundle.putParcelable("note", newNote);
            bundle.putBoolean("isEditMode", true);
            detailsFragment.setArguments(bundle);

            // Устанавливаем слушатель для сохранения
            detailsFragment.setOnNoteUpdatedListener(savedNote -> {
                new Thread(() -> {
                    if (savedNote.getId() == 0) {
                        Log.d("депрессия", " 158 insert called with note: " + savedNote.getTitle());
                        notesDao.insert(savedNote);
                        Log.d("MainActivity", "insert called with note: " + savedNote.getTitle());
                    } else {
                        notesDao.update(savedNote);
                    }
                    loadNotesFromDatabase();
                }).start();
            });

            // Отображение фрагмента
            int containerId = (findViewById(R.id.mainFragmentContainer) != null)
                    ? R.id.mainFragmentContainer
                    : R.id.detailsFragmentContainer;

            getSupportFragmentManager().beginTransaction()
                    .replace(containerId, detailsFragment)
                    .addToBackStack(null)
                    .commit();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private void fillDatabaseWithSampleNotes() {
        new Thread(() -> {
            List<Note> existingNotes = notesDao.getAllNotes();
            Log.d("MainActivity", "Существующие заметки в БД: " + existingNotes.size());
            if (existingNotes.isEmpty()) {
                notesDao.insert(new Note("Пример заметки 1", "Содержимое заметки 1"));
                notesDao.insert(new Note("Пример заметки 2", "Содержимое заметки 2"));
                notesDao.insert(new Note("Пример заметки 3", "Содержимое заметки 3"));
                Log.d("MainActivity", "База данных успешно заполнена тестовыми заметками.");
            } else {
                Log.d("MainActivity", "База данных уже содержит заметки.");
            }
        }).start();
    }

}

