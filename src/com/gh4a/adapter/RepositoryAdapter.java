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
package com.gh4a.adapter;

import java.util.Locale;

import org.eclipse.egit.github.core.Repository;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

public class RepositoryAdapter extends RootAdapter<Repository> implements Filterable {
    public RepositoryAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_simple_3, null);
        Gh4Application app = Gh4Application.get(mContext);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvTitle.setTypeface(app.boldCondensed);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvDesc.setTypeface(app.regular);

        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.tvExtra.setTextAppearance(mContext, R.style.default_text_micro);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View view, Repository repository) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.tvTitle.setText(repository.getOwner().getLogin() + "/" + repository.getName());

        if (!StringUtils.isBlank(repository.getDescription())) {
            viewHolder.tvDesc.setVisibility(View.VISIBLE);
            viewHolder.tvDesc.setText(StringUtils.doTeaser(repository.getDescription()));
        } else {
            viewHolder.tvDesc.setVisibility(View.GONE);
        }

        String language = repository.getLanguage() != null
                ? repository.getLanguage() : mContext.getString(R.string.unknown);
        viewHolder.tvExtra.setText(mContext.getString(R.string.repo_search_extradata,
                language, Formatter.formatFileSize(mContext, repository.getSize() * 1000),
                repository.getForks(), repository.getWatchers()));
    }

    @Override
    protected boolean isFiltered(CharSequence filter, Repository repo) {
        String lcFilter = filter.toString().toLowerCase(Locale.getDefault());
        String name = repo.getName().toLowerCase(Locale.getDefault());
        return name.contains(lcFilter);
    }

    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;
    }
}