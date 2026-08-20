package polar.ru.api.storages.implement;

import java.util.ArrayList;
import java.util.List;
import polar.ru.polar;

public class StaffStorage {
    private final List<String> staffs = new ArrayList<String>();

    public void add(String staff) {
        if (!staff.isEmpty()) {
            this.staffs.add(staff);
            this.save();
        }
    }

    public void remove(String staff) {
        this.staffs.remove(staff);
        this.save();
    }

    public void clear() {
        this.staffs.clear();
        this.save();
    }

    public boolean isStaff(String staff) {
        return this.staffs.contains(staff);
    }

    public boolean isEmpty() {
        return this.staffs.isEmpty();
    }

    private void save() {
        try {
            polar.INSTANCE.configStorage.saveGlobals();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
    public List<String> getStaffs() {
        return this.staffs;
    }
}

