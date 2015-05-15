package spelin.language_models;

/**
 * Implementation of stupid backoff
 * Created by Steven on 2015-02-22.
 */
public class StupidBackoffSmoothing extends BackoffSmoothing {

    public StupidBackoffSmoothing() {
        super(1.0);
    }
}
