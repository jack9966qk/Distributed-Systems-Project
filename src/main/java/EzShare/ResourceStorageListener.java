package EzShare;

/**
 * Listener for ResourceStorage, will be called every time a new resource is added
 * Created by Jack on 6/5/2017.
 */
public interface ResourceStorageListener {
    public void onResourceAdded(Resource resource);
}
