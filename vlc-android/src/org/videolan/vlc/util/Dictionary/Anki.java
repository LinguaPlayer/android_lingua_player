package org.videolan.vlc.util.Dictionary;

import android.content.Context;
import android.content.SharedPreferences;
import com.ichi2.anki.api.AddContentApi;
import com.ichi2.anki.api.NoteInfo;
import java.util.Map;



/**
 * Created by habib on 9/18/17.
 */

public class Anki {

    private AddContentApi mApi;
    private static final int AD_PERM_REQUEST = 0;
    private static final String DECK_REF_DB = "com.ichi2.anki.api.decks";
    private static final String MODEL_REF_DB = "com.ichi2.anki.api.models";

    public static final String DECK_NAME = "Lingua Player";
    public static final String MODEL_NAME = "ir.habibkazemi.linguaplayer";


    public Anki(Context context) {
            mApi = new AddContentApi(context);
    }

    private Long findDeckIdByName(String deckName,Context context) {
        SharedPreferences decksDb = context.getSharedPreferences(DECK_REF_DB, Context.MODE_PRIVATE);
        // Look for deckName in the deck list
        Long did = getDeckId(deckName);
        if (did != null) {
            // If the deck was found then return it's id
            return did;
        } else {
            // Otherwise try to check if we have a reference to a deck that was renamed and return that
            did = decksDb.getLong(deckName, -1);
            if (did != -1 && mApi.getDeckName(did) != null) {
                return did;
            } else {
                // If the deck really doesn't exist then return null
                return null;
            }
        }
    }

    private Long getDeckId(String deckName) {
            Map<Long, String> deckList = mApi.getDeckList();
            for (Map.Entry<Long, String> entry : deckList.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(deckName)) {
                    return entry.getKey();
                }
            }
        return null;
    }

    private long getDeckId(Context context) {
        Long did = findDeckIdByName(DECK_NAME, context);
        if (did == null) {
            did = mApi.addNewDeck(DECK_NAME);
            storeDeckReference(DECK_NAME, did, context);
        }
        return did;
    }

    public void storeDeckReference(String deckName, long deckId, Context context) {
        final SharedPreferences decksDb = context.getSharedPreferences(DECK_REF_DB, Context.MODE_PRIVATE);
        decksDb.edit().putLong(deckName, deckId).apply();
    }



    public Long addCardsToAnkiDroid(String front, String back, Context context) throws AnkiStoragePermissionError{
        //Hack: check if ankidroid has storage permission
        try {
            mApi.getDeckList();
        }catch (IllegalStateException e){
            throw new AnkiStoragePermissionError("Anki storage permission not granted");
        }
        long deckId =getDeckId(context);
        long modelId = mApi.addNewBasicModel(MODEL_NAME);

        long noteId = mApi.addNote(modelId, deckId, new String[] {front, back}, null);
        return noteId;
    }

    public boolean ifExist(long noteId) {
        NoteInfo note = mApi.getNote(noteId);
        return note != null;
    }


}

