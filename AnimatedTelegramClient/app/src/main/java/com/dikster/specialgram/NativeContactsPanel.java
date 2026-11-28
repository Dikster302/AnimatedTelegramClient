package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Native Java contacts screen with incremental search and TDLib contact rows. */
public final class NativeContactsPanel extends LinearLayout {
    public interface Listener {
        void onQuery(String query);
        void onOpen(TgContact contact);
    }

    private final TextView count;
    private final EditText search;
    private final ListView list;
    private final BaseAdapter adapter;
    private final TextView empty;
    private List<TgContact> contacts = new ArrayList<>();
    private Listener listener;
    private boolean changingText;
    private int textColor, mutedColor, accentColor, panelColor;

    public NativeContactsPanel(Context context, List<TgContact> contacts, String query,
                               int background, int text, int muted, int accent, int panel,
                               Listener listener) {
        super(context);
        setOrientation(VERTICAL);
        this.textColor=text; this.mutedColor=muted; this.accentColor=accent; this.panelColor=panel;
        setBackgroundColor(background);
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(12), dp(18), dp(12));
        TextView title = text("Контакты", textColor, 18, true);
        header.addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        count = text("0", mutedColor, 13, false);
        header.addView(count);
        addView(header);

        search = new EditText(context);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(textColor);
        search.setHintTextColor(mutedColor);
        search.setHint("Поиск контактов");
        search.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable input = new GradientDrawable();
        input.setColor(panelColor);
        input.setCornerRadius(dp(24));
        search.setBackground(input);
        LayoutParams searchParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(48));
        searchParams.leftMargin=dp(12); searchParams.rightMargin=dp(12);
        searchParams.topMargin=dp(5); searchParams.bottomMargin=dp(8);
        addView(search, searchParams);

        android.widget.FrameLayout content = new android.widget.FrameLayout(context);
        list = new ListView(context);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(android.R.color.transparent);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return contacts.size(); }
            @Override public Object getItem(int position) { return contacts.get(position); }
            @Override public long getItemId(int position) { return contacts.get(position).getId(); }
            @Override public boolean hasStableIds() { return true; }
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                return createContactRow(contacts.get(position));
            }
        };
        list.setAdapter(adapter);
        content.addView(list, new android.widget.FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        empty = text("Контакты не найдены", mutedColor, 14, false);
        empty.setGravity(Gravity.CENTER);
        content.addView(empty, new android.widget.FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(content, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (!changingText && NativeContactsPanel.this.listener != null)
                    NativeContactsPanel.this.listener.onQuery(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        update(contacts, query, background, text, muted, accent, panel, listener);
    }

    public void update(List<TgContact> values, String query, int background,
                       int text, int muted, int accent, int panel, Listener callback) {
        this.listener=callback;
        boolean styleChanged = textColor != text || mutedColor != muted
                || accentColor != accent || panelColor != panel;
        this.textColor=text; this.mutedColor=muted; this.accentColor=accent; this.panelColor=panel;
        setBackgroundColor(background);
        if (!query.equals(search.getText().toString())) {
            changingText=true;
            search.setText(query);
            search.setSelection(search.length());
            changingText=false;
        }
        List<TgContact> next = values == null ? new ArrayList<>() : new ArrayList<>(values);
        count.setText(String.valueOf(next.size()));
        if (styleChanged || !next.equals(contacts)) {
            contacts=next;
            adapter.notifyDataSetChanged();
        }
        empty.setVisibility(contacts.isEmpty() ? VISIBLE : GONE);
    }

    private View createContactRow(TgContact contact) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(9), dp(16), dp(9));
        NativeAvatarView avatar = new NativeAvatarView(getContext(), "",
                first(contact.getName()), contact.getId(), 48, contact.isOnline(),
                false, false, panelColor);
        row.addView(avatar, new LayoutParams(dp(48), dp(48)));
        LinearLayout details = new LinearLayout(getContext());
        details.setOrientation(VERTICAL);
        LayoutParams detailsParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        detailsParams.leftMargin=dp(13);
        row.addView(details, detailsParams);
        LinearLayout titleRow = new LinearLayout(getContext());
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(contact.getName(), textColor, 15, true);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleRow.addView(name);
        if (contact.isPremium()) titleRow.addView(text("  ✦", 0xFFB58CFF, 12, false));
        details.addView(titleRow);
        String subtitle = contact.isOnline() ? "в сети" :
                !empty(contact.getStatus()) ? contact.getStatus() :
                !empty(contact.getUsername()) ? "@" + contact.getUsername() : contact.getPhone();
        details.addView(text(subtitle, contact.isOnline() ? 0xFF35C759 : mutedColor, 12, false));
        row.setOnClickListener(v -> { if (listener != null) listener.onOpen(contact); });
        View divider = new View(getContext());
        divider.setBackgroundColor(0x99253341);
        LayoutParams dp = new LayoutParams(LayoutParams.MATCH_PARENT, dp(1));
        dp.leftMargin=dp(76);
        row.addView(divider, dp);
        return row;
    }

    private TextView text(String value,int color,int size,boolean bold) {
        TextView view=new TextView(getContext());
        view.setText(value == null ? "" : value); view.setTextColor(color); view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }
    private String first(String value) { return value == null || value.isEmpty() ? "?" : value.substring(0,1).toUpperCase(); }
    private boolean empty(String value) { return value == null || value.trim().isEmpty(); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
