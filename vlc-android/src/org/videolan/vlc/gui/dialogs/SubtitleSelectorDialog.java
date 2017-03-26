package org.videolan.vlc.gui.dialogs;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;

import org.videolan.vlc.R;

import java.util.ArrayList;

/**
 * Created by habib on 3/26/17.
 */

public class SubtitleSelectorDialog extends DialogFragment {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private OnTrackClickListener trackClickListener;
    public static final String Disable = "Disable";

    public SubtitleSelectorDialog(){

    }

    public static SubtitleSelectorDialog newInstance (ArrayList<String> mTrackList, final int defaultSelected /*pass -1 to select nothing*/){
        SubtitleSelectorDialog myFragment = new SubtitleSelectorDialog();
        Bundle args = new Bundle();
        args.putStringArrayList("tracks",mTrackList);
        args.putInt("default",defaultSelected);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container , Bundle savedInstanceState){
        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);

        View v = inflater.inflate(R.layout.track_selector_dialog,container);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.track_selector_recycler_view);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        final ArrayList<String> trackList = getArguments().getStringArrayList("tracks");
        final int defaultSelected = getArguments().getInt("default");

        mAdapter = new TrackSelectorAdapter(trackList,defaultSelected);
        mRecyclerView.setAdapter(mAdapter);
        return v;
    }

    @Override
    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
    }

    public interface OnTrackClickListener{
        void onTrackClick(boolean isChecked /*is user checked it or unchecked with tapping again*/ , String path);
        void onTrackDelete(String deletedPath , ArrayList<String> newTracksList );
    }

    public void setOnTrackClickListener(OnTrackClickListener listener){
        trackClickListener = listener;
    }

    private class TrackSelectorAdapter extends RecyclerView.Adapter<TrackSelectorAdapter.ViewHolder>{

        public ArrayList<String> mTracks;
        public int mSelectedTrack = -1;

        public TrackSelectorAdapter (ArrayList<String> mTracks,final int defaultSelected){
            this.mTracks = mTracks;
            this.mSelectedTrack = defaultSelected;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_item,parent,false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            //with help of notifyItemRangeChanged and below line I can
            //Implement RadioGroup inside RecyclerView
            holder.selected.setChecked(position == mSelectedTrack);
            final String trackName = mTracks.get(position);
            holder.selected.setText(Uri.parse(trackName).getLastPathSegment());
        }

        @Override
        public int getItemCount() {
            return mTracks.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            public RadioButton selected;
            public ImageView   clearButton;

            public ViewHolder(View v){
                super(v);
                selected = (RadioButton) v.findViewById(R.id.track);
                clearButton = (ImageView) v.findViewById(R.id.clear_button);
                selected.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int clickedTrack = getAdapterPosition();
                        //select or unselect when tap it again
                        mSelectedTrack = (clickedTrack == mSelectedTrack) ? -1 :clickedTrack;
                        notifyItemRangeChanged(0, mTracks.size());

                        if(trackClickListener !=null)
                            if(mSelectedTrack > -1 )
                                trackClickListener.onTrackClick(true, mTracks.get(mSelectedTrack));
                            else
                                trackClickListener.onTrackClick(false, null);


                    }
                });
                clearButton.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        if(trackClickListener != null){
                            final int deletedTrackPosition = getAdapterPosition();
                            final String deletedPath = mTracks.remove(deletedTrackPosition);
                            trackClickListener.onTrackDelete(deletedPath , mTracks);
                            notifyItemRemoved(deletedTrackPosition);
                        }
                    }
                });

            }
        }

    }

}
