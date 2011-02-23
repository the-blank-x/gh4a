/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.api.v2.schema.Repository;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.RepositoryService;

/**
 * The Repository activity.
 */
public class RepositoryActivity extends BaseActivity implements OnClickListener {

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The bundle. */
    protected Bundle mBundle;

    /** The load network task. */
    private LoadNetworkTask mLoadNetworkTask;

    /** The forks loaded. */
    private boolean mForksLoaded;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.repository);
        setUpActionBar();

        mBundle = getIntent().getExtras().getBundle(Constants.DATA_BUNDLE);

        // comes from repo search, activity, etc
        if (mBundle != null) {
            fillData();
        }
        // comes from when user click on forked from link
        else {
            Bundle bundle = getIntent().getExtras();
            String username = bundle.getString(Constants.Repository.REPO_OWNER);
            String repoName = bundle.getString(Constants.Repository.REPO_NAME);
            mBundle = new Bundle();
            mBundle.putString(Constants.Repository.REPO_OWNER, username);
            mBundle.putString(Constants.Repository.REPO_NAME, repoName);
            mLoadingDialog = LoadingDialog.show(this, true, true);
        }

        new LoadRepositoryInfoTask(this).execute();

        mLoadNetworkTask = (LoadNetworkTask) getLastNonConfigurationInstance();

        if (mLoadNetworkTask == null) {
            mLoadNetworkTask = new LoadNetworkTask(this);
            mLoadNetworkTask.execute();
        }
        else {
            mLoadNetworkTask.attach(this);
            if (AsyncTask.Status.FINISHED == mLoadNetworkTask.getStatus()) {
                try {
                    fillWatchers(mLoadNetworkTask.get());
                }
                catch (InterruptedException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                }
                catch (ExecutionException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Fill data into UI components.
     */
    public void fillData() {
        ImageButton btnBranches = (ImageButton) findViewById(R.id.btn_branches);
        btnBranches.setOnClickListener(this);

        ImageButton btnTags = (ImageButton) findViewById(R.id.btn_tags);
        btnTags.setOnClickListener(this);

        ImageButton btnPullRequests = (ImageButton) findViewById(R.id.btn_pull_requests);
        btnPullRequests.setOnClickListener(this);

        Button btnWatchers = (Button) findViewById(R.id.btn_watchers);
        btnWatchers.setOnClickListener(this);

        Button btnForks = (Button) findViewById(R.id.btn_forks);
        btnForks.setOnClickListener(this);

        Button btnOpenIssues = (Button) findViewById(R.id.btn_open_issues);
        btnOpenIssues.setOnClickListener(this);

        TextView tvOwner = (TextView) findViewById(R.id.tv_login);
        tvOwner.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                getApplicationContext().openUserInfoActivity(RepositoryActivity.this,
                        mBundle.getString(Constants.Repository.REPO_OWNER), null);
            }
        });
        TextView tvRepoName = (TextView) findViewById(R.id.tv_name);
        TextView tvParentRepo = (TextView) findViewById(R.id.tv_parent);
        TextView tvDesc = (TextView) findViewById(R.id.tv_desc);
        TextView tvUrl = (TextView) findViewById(R.id.tv_url);
        TextView tvLanguage = (TextView) findViewById(R.id.tv_language);
        
        tvOwner.setText(mBundle.getString(Constants.Repository.REPO_OWNER));
        tvRepoName.setText(mBundle.getString(Constants.Repository.REPO_NAME));
        if (mBundle.getBoolean(Constants.Repository.REPO_IS_FORKED)) {
            tvParentRepo.setVisibility(View.VISIBLE);
            if (mBundle.getString(Constants.Repository.REPO_SOURCE) != null) {
                tvParentRepo.setText("forked from "
                        + mBundle.getString(Constants.Repository.REPO_PARENT));
                tvParentRepo.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        String[] repoPart = mBundle.getString(Constants.Repository.REPO_SOURCE)
                                .split("/");
                        getApplicationContext().openRepositoryInfoActivity(RepositoryActivity.this,
                                repoPart[0], repoPart[1]);
                    }
                });
            }
        }
        else {
            tvParentRepo.setVisibility(View.GONE);
        }
        tvDesc.setText(mBundle.getString(Constants.Repository.REPO_DESC));
        
        if (mBundle.getString(Constants.Repository.REPO_LANGUANGE) != null) {
            tvLanguage.setText(getResources().getString(R.string.repo_language) 
                    + " " + mBundle.getString(Constants.Repository.REPO_LANGUANGE));
            tvLanguage.setVisibility(View.VISIBLE);
        }
        else {
            tvLanguage.setVisibility(View.GONE);
        }
        
        tvUrl.setText(mBundle.getString(Constants.Repository.REPO_URL));

        btnWatchers.setText(String.valueOf(mBundle.getInt(Constants.Repository.REPO_WATCHERS)));

        if (!mForksLoaded) {
            btnForks.setText(String.valueOf(mBundle.getInt(Constants.Repository.REPO_FORKS)));
        }

        if (mBundle.getBoolean(Constants.Repository.REPO_HAS_ISSUES)) {
            btnOpenIssues.setText(String.valueOf(mBundle
                    .getInt(Constants.Repository.REPO_OPEN_ISSUES)));
        }
        else {
            RelativeLayout rlOpenIssues = (RelativeLayout) findViewById(R.id.rl_open_issues);
            rlOpenIssues.setVisibility(View.GONE);
        }
    }

    /**
     * An asynchronous task that runs on a background thread to load repository
     * info.
     */
    private static class LoadRepositoryInfoTask extends AsyncTask<Void, Integer, Repository> {

        /** The target. */
        private WeakReference<RepositoryActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load repository info task.
         * 
         * @param activity the activity
         */
        public LoadRepositoryInfoTask(RepositoryActivity activity) {
            mTarget = new WeakReference<RepositoryActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Repository doInBackground(Void... arg0) {
            try {
                RepositoryActivity activity = mTarget.get();
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                RepositoryService repositoryService = factory.createRepositoryService();
                return repositoryService.getRepository(activity.mBundle
                        .getString(Constants.Repository.REPO_OWNER), activity.mBundle
                        .getString(Constants.Repository.REPO_NAME));
            }
            catch (GitHubException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                mException = true;
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Repository result) {
            RepositoryActivity activity = mTarget.get();
            if (activity.mLoadingDialog != null && activity.mLoadingDialog.isShowing()) {
                activity.mLoadingDialog.dismiss();
            }

            if (mException) {
                activity.showError();
            }
            else {
                activity.mBundle = activity.getApplicationContext().populateRepository(result);
                activity.fillData();
            }
        }
    }

    /**
     * An asynchronous task that runs on a background thread to load network.
     */
    private static class LoadNetworkTask extends AsyncTask<Void, Integer, Integer> {

        /** The target. */
        private WeakReference<RepositoryActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load network task.
         * 
         * @param activity the activity
         */
        public LoadNetworkTask(RepositoryActivity activity) {
            attach(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            // skip loading if this is not a forked repo
            if (!mTarget.get().mBundle.getBoolean(Constants.Repository.REPO_IS_FORKED)) {
                cancel(false);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Integer doInBackground(Void... arg0) {
            try {
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                RepositoryService repositoryService = factory.createRepositoryService();
                return repositoryService.getForks(
                        mTarget.get().mBundle.getString(Constants.Repository.REPO_OWNER),
                        mTarget.get().mBundle.getString(Constants.Repository.REPO_NAME)).size();
            }
            catch (GitHubException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                mException = true;
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Integer result) {
            if (mException) {
                mTarget.get().showError();
            }
            else {
                mTarget.get().fillWatchers(result);
            }
        }

        /**
         * Detach.
         */
        private void detach() {
            mTarget = null;
        }

        /**
         * Attach.
         * 
         * @param activity the activity
         */
        private void attach(RepositoryActivity activity) {
            this.mTarget = new WeakReference<RepositoryActivity>(activity);
        }

    }

    /**
     * Fill watchers into UI components.
     * 
     * @param result the result
     */
    protected void fillWatchers(int result) {
        Button btnForks = (Button) findViewById(R.id.btn_forks);
        btnForks.setText(String.valueOf(result));
        mForksLoaded = true;
    }

    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
        case R.id.btn_branches:
            getApplicationContext().openBranchListActivity(this,
                    mBundle.getString(Constants.Repository.REPO_OWNER),
                    mBundle.getString(Constants.Repository.REPO_NAME), R.id.btn_branches);
            break;
        case R.id.btn_tags:
            getApplicationContext().openTagListActivity(this,
                    mBundle.getString(Constants.Repository.REPO_OWNER),
                    mBundle.getString(Constants.Repository.REPO_NAME), R.id.btn_tags);
            break;
        case R.id.btn_commits:
            getApplicationContext().openBranchListActivity(this,
                    mBundle.getString(Constants.Repository.REPO_OWNER),
                    mBundle.getString(Constants.Repository.REPO_NAME), R.id.btn_commits);
            break;
        case R.id.btn_pull_requests:
            getApplicationContext().openPullRequestListActivity(this,
                    mBundle.getString(Constants.Repository.REPO_OWNER),
                    mBundle.getString(Constants.Repository.REPO_NAME),
                    Constants.Issue.ISSUE_STATE_OPEN);
            break;
        case R.id.btn_watchers:
            getWatchers(view);
            break;
        case R.id.btn_forks:
            getNetworks(view);
            break;
        case R.id.btn_open_issues:
            getApplicationContext().openIssueListActivity(this,
                    mBundle.getString(Constants.Repository.REPO_OWNER),
                    mBundle.getString(Constants.Repository.REPO_NAME),
                    Constants.Issue.ISSUE_STATE_OPEN);
        default:
            break;
        }
    }

    /**
     * Gets the watchers when Watchers button clicked.
     * 
     * @param view the view
     * @return the watchers
     */
    public void getWatchers(View view) {
        Intent intent = new Intent().setClass(RepositoryActivity.this, WatcherListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mBundle
                .getString(Constants.Repository.REPO_OWNER));
        intent.putExtra(Constants.Repository.REPO_NAME, mBundle
                .getString(Constants.Repository.REPO_NAME));
        startActivity(intent);
    }

    /**
     * Gets the networks when Networkd button clicked.
     * 
     * @param view the view
     * @return the networks
     */
    public void getNetworks(View view) {
        Intent intent = new Intent().setClass(RepositoryActivity.this, ForkListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mBundle
                .getString(Constants.Repository.REPO_OWNER));
        intent.putExtra(Constants.Repository.REPO_NAME, mBundle
                .getString(Constants.Repository.REPO_NAME));
        startActivity(intent);
    }

    /**
     * Gets the open issues when Open Issues button clicked.
     * 
     * @param view the view
     * @return the open issues
     */
    public void getOpenIssues(View view) {
        Intent intent = new Intent().setClass(RepositoryActivity.this, IssueListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mBundle
                .getString(Constants.Repository.REPO_OWNER));
        intent.putExtra(Constants.Repository.REPO_NAME, mBundle
                .getString(Constants.Repository.REPO_NAME));
        intent.putExtra(Constants.Issue.ISSUE_STATE, Constants.Issue.ISSUE_STATE_OPEN);
        startActivity(intent);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    public void onSaveInstanceState(Bundle outState) {
        Log.v(Constants.LOG_TAG, this.getLocalClassName() + " onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    public void onStart() {
        super.onStart();
        Log.v(Constants.LOG_TAG, this.getLocalClassName() + "onStart");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onRestart()
     */
    public void onRestart() {
        super.onRestart();
        Log.v(Constants.LOG_TAG, this.getLocalClassName() + "onReStart");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    public void onResume() {
        super.onResume();
        Log.v(Constants.LOG_TAG, this.getLocalClassName() + "onResume");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    public void onPause() {
        super.onPause();
        Log.v(Constants.LOG_TAG, this.getLocalClassName() + "onPause");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onStop()
     */
    public void onStop() {
        super.onStart();
        Log.v(Constants.LOG_TAG, this.getLocalClassName() + "onStop");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    public void onDestroy() {
        super.onDestroy();
        Log.v(Constants.LOG_TAG, this.getLocalClassName() + "onDestroy");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        Log.v(Constants.LOG_TAG, this.getLocalClassName() + " onRetain");
        mLoadNetworkTask.detach();
        return mLoadNetworkTask;
    }
}