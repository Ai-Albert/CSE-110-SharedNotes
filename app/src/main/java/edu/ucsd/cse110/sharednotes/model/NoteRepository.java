package edu.ucsd.cse110.sharednotes.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NoteRepository {
    private final NoteDao dao;
    Map<String, ScheduledExecutorService> executors;
    Map<String, ScheduledFuture<?>> futures;
    Map<String, MutableLiveData<Note>> liveNotes;
    Map<String, MediatorLiveData<Note>> notes;

    public NoteRepository(NoteDao dao) {
        this.dao = dao;
        this.executors = new HashMap<>();
        this.futures = new HashMap<>();
        this.liveNotes = new HashMap<>();
        this.notes = new HashMap<>();
    }

    // Synced Methods
    // ==============

    /**
     * This is where the magic happens. This method will return a LiveData object that will be
     * updated when the note is updated either locally or remotely on the server. Our activities
     * however will only need to observe this one LiveData object, and don't need to care where
     * it comes from!
     *
     * This method will always prefer the newest version of the note.
     *
     * @param title the title of the note
     * @return a LiveData object that will be updated when the note is updated locally or remotely.
     */
    public LiveData<Note> getSynced(String title) {
        var note = new MediatorLiveData<Note>();

        Observer<Note> updateFromRemote = theirNote -> {
            var ourNote = note.getValue();
            if (theirNote == null) return; // do nothing
            if (ourNote == null || ourNote.updatedAt < theirNote.updatedAt) {
                upsertLocal(theirNote);
            }
        };

        // If we get a local update, pass it on.
        note.addSource(getLocal(title), note::postValue);
        // If we get a remote update, update the local version (triggering the above observer)
        note.addSource(getRemote(title), updateFromRemote);

        return note;
    }

    public void upsertSynced(Note note) {
        upsertLocal(note);
        upsertRemote(note);
    }

    // Local Methods
    // =============

    public LiveData<Note> getLocal(String title) {
        return dao.get(title);
    }

    public LiveData<List<Note>> getAllLocal() {
        return dao.getAll();
    }

    public void upsertLocal(Note note) {
        note.updatedAt = System.currentTimeMillis();
        dao.upsert(note);
    }

    public void deleteLocal(Note note) {
        dao.delete(note);
    }

    public boolean existsLocal(String title) {
        return dao.exists(title);
    }

    // Remote Methods
    // ==============

    public LiveData<Note> getRemote(String title) {
        // TODO: Implement getRemote! <RESOLVED>
        // TODO: Set up polling background thread (MutableLiveData?) <RESOLVED>
        // TODO: Refer to TimerService from https://github.com/DylanLukes/CSE-110-WI23-Demo5-V2. <RESOLVED>

        // Start by fetching the note from the server _once_ and feeding it into MutableLiveData.
        // Then, set up a background thread that will poll the server every 3 seconds.

        // You may (but don't have to) want to cache the LiveData's for each title, so that
        // you don't create a new polling thread every time you call getRemote with the same title.
        // You don't need to worry about killing background threads.
        registerNoteListener(title);
        return notes.get(title);
    }

    private void registerNoteListener(String title) {
        var executor = executors.containsKey(title) ? executors.get(title) : Executors.newSingleThreadScheduledExecutor();
        var liveNote = new MutableLiveData<Note>();
        var future = executor.schedule(() -> liveNote.postValue(NoteAPI.provide().getNoteAsync(title)), 3, TimeUnit.SECONDS);

        var note = new MediatorLiveData<Note>();
        note.addSource(liveNote, note::postValue);

        executors.put(title, executor);
        liveNotes.put(title, liveNote);
        futures.put(title, future);
        notes.put(title, note);
    }

    public void upsertRemote(Note note) {
        // TODO: Implement upsertRemote! <RESOLVED>

        NoteAPI.provide().putNoteAsync(note);
    }
}
