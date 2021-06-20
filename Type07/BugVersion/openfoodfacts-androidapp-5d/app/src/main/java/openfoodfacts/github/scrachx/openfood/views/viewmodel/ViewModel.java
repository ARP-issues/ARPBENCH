package openfoodfacts.github.scrachx.openfood.views.viewmodel;

import android.support.annotation.NonNull;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by Abdelali Eramli on 27/12/2017.
 */

public abstract class ViewModel {

    private CompositeDisposable subscriptions;

    public ViewModel() {
    }

    public void bind() {
        unbind();
        subscriptions = new CompositeDisposable();
        subscribe(subscriptions);
    }

    public void unbind() {
        if(subscriptions != null){
            subscriptions.clear();
            subscriptions = null;
        }
    }

    protected abstract void subscribe(@NonNull final CompositeDisposable subscriptions);


}
