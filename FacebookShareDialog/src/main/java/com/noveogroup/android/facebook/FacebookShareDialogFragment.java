/*
 * Copyright (c) 2013 Noveo Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Except as contained in this notice, the name(s) of the above copyright holders
 * shall not be used in advertising or otherwise to promote the sale, use or
 * other dealings in this Software without prior written authorization.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.noveogroup.android.facebook;

import java.io.Serializable;

import com.facebook.FacebookException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * User: rzhilich
 * Date: 27.11.2013
 */
public class FacebookShareDialogFragment extends DialogFragment implements Session.StatusCallback {

    private static final String EXTRA_PAYLOAD = "payload";
    private static final String FRAGMENT_TAG = "facebook_share_dialog";
    private static final String PACKAGE_FACEBOOK = "com.facebook.katana";
    private Payload payload;
    private UiLifecycleHelper uiHelper;

    public static FacebookShareDialogFragment newInstance(final Payload payload) {
        if (payload == null) {throw new IllegalArgumentException("payload can't be null");}

        final FacebookShareDialogFragment fragment = new FacebookShareDialogFragment();
        final Bundle arguments = new Bundle();
        arguments.putSerializable(EXTRA_PAYLOAD, payload);
        fragment.setArguments(arguments);
        return fragment;
    }

    private static boolean isAppPresent(final Context context) {
        try {
            final PackageManager manager = context.getPackageManager();
            if (manager != null) {
                manager.getApplicationInfo(PACKAGE_FACEBOOK, 0);
                return true;
            }
        } catch (PackageManager.NameNotFoundException ignore) {}
        return false;
    }

    public Payload getPayload() {
        return payload;
    }

    private void onHandleArguments(final Bundle arguments) {
        if (arguments.containsKey(EXTRA_PAYLOAD)) {
            payload = (Payload)arguments.getSerializable(EXTRA_PAYLOAD);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_PAYLOAD, payload);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null) {onHandleArguments(arguments);}

        setShowsDialog(false);

        uiHelper = new UiLifecycleHelper(getActivity(), this);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            onHandleArguments(savedInstanceState);
        } else {
            final Session session = Session.getActiveSession();
            if (session != null && session.isOpened()) {
                share();
            } else {
                Session.openActiveSession(getActivity(), this, true, this);
            }
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onComplete(final FacebookDialog.PendingCall pendingCall, final Bundle data) {
                onShareFinished();
            }

            @Override
            public void onError(final FacebookDialog.PendingCall pendingCall, final Exception error,
                final Bundle data) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        uiHelper.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
        uiHelper = null;
    }

    private void onShareFinished() {
        dismiss();
    }

    private void share() {
        if (isAppPresent(getActivity())) {
            final FacebookDialog facebookDialog = new FacebookDialog.ShareDialogBuilder(getActivity())
                .setFragment(this)
                .setCaption(payload.getCaption())
                .setDescription(payload.getDescription())
                .setLink(payload.getLink())
                .setName(payload.getName())
                .setPicture(payload.getPicture())
                .build();
            uiHelper.trackPendingDialogCall(facebookDialog.present());
        } else {
            new FacebookWebDialogFragment().show(getChildFragmentManager(), null);
        }
    }

    @Override
    public void show(final FragmentManager manager, final String tag) {
        final Fragment fragment = manager.findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null) {
            manager.beginTransaction()
                   .remove(fragment)
                   .commit();
            manager.executePendingTransactions();
        }
        super.show(manager, tag != null ? tag : FRAGMENT_TAG);
    }

    @Override
    public int show(final FragmentTransaction transaction, final String tag) {
        return super.show(transaction, tag != null ? tag : FRAGMENT_TAG);
    }

    @Override
    public void call(final Session session, final SessionState state, final Exception exception) {
        if (session.isOpened()) {
            share();
        }
    }

    public static final class Payload implements Serializable {
        private String link;
        private String caption;
        private String description;
        private String name;
        private String picture;

        public String getLink() {
            return link;
        }

        public void setLink(final String link) {
            this.link = link;
        }

        public String getCaption() {
            return caption;
        }

        public void setCaption(final String caption) {
            this.caption = caption;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getPicture() {
            return picture;
        }

        public void setPicture(final String picture) {
            this.picture = picture;
        }
    }

    public static class FacebookWebDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final FacebookShareDialogFragment parentFragment = (FacebookShareDialogFragment)getParentFragment();
            final Payload payload = parentFragment.getPayload();
            return new WebDialog.FeedDialogBuilder(getActivity(), Session.getActiveSession())
                .setCaption(payload.getCaption())
                .setDescription(payload.getDescription())
                .setLink(payload.getLink())
                .setName(payload.getName())
                .setPicture(payload.getPicture())
                .setOnCompleteListener(new WebDialog.OnCompleteListener() {
                    @Override
                    public void onComplete(final Bundle values, final FacebookException error) {
                        parentFragment.onShareFinished();
                    }
                })
                .build();
        }
    }

}
