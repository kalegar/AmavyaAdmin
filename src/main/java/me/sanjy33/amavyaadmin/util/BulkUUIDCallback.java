package me.sanjy33.amavyaadmin.util;

import java.util.List;
import java.util.UUID;

public interface BulkUUIDCallback {

    void onFinished(List<String> names, List<UUID> uuids);
}
