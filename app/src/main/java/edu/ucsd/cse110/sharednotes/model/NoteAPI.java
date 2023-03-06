package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NoteAPI {
    // TODO: Implement the API using OkHttp! <RESOLVED>
    // TODO: - getNote (maybe getNoteAsync) <RESOLVED>
    // TODO: - putNote (don't need putNotAsync, probably) <RESOLVED>
    // TODO: Read the docs: https://square.github.io/okhttp/ <RESOLVED>
    // TODO: Read the docs: https://sharednotes.goto.ucsd.edu/docs <RESOLVED>

    private volatile static NoteAPI instance = null;

    private final OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     *
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @AnyThread
    public Future<String> echoAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();

        // We can use future.get(1, SECONDS) to wait for the result.
        return executor.submit(() -> echo(msg));
    }

    public Note getNote(String title) {
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("GET", null)
                .build();

        String body = "";
        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            body = response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(body);

        return Note.fromJSON(body);
    }

    public Note getNoteAsync(String title) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> getNote(title));
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void putNote(Note updatedNote) {
        var mediaType = MediaType.get("application/json; charset=utf-8");
        var requestBody = RequestBody.create(updatedNote.toJSON(), mediaType);
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + updatedNote.title)
                .method("PUT", requestBody)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            System.out.println(response.body().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void putNoteAsync(Note updatedNote) {
        var executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> putNote(updatedNote));
    }
}
