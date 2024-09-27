package nikeno.Tenki.viewbinding;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import nikeno.Tenki.R;
import nikeno.Tenki.view.TextTableView;

public final class ActivityMainBinding  {
    public final TextView      todayHeader;
    public final TextView      tomorrowHeader;
    public final TextView      weekHeader;
    public final TextTableView todayTable2;
    public final TextTableView tomorrowTable2;
    public final TextTableView weekTable2;
    public final TextView      time;
    public final View          progress;
    public final View          errorGroup;

    public ActivityMainBinding(ViewGroup v) {
        todayHeader = v.findViewById(R.id.todayHeader);
        tomorrowHeader = v.findViewById(R.id.tomorrowHeader);
        weekHeader = v.findViewById(R.id.weekHeader);
        todayTable2 = v.findViewById(R.id.today2);
        tomorrowTable2 =v.findViewById(R.id.tomorrow2);
        weekTable2 = v.findViewById(R.id.week2);
        time = v.findViewById(R.id.time);
        progress = v.findViewById(android.R.id.progress);
        errorGroup = v.findViewById(R.id.errorGroup);
    }
}
