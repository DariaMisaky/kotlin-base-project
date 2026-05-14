package com.daria.kotlinbase.shared.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A LiveData-like holder that emits an event exactly once per observer, even after
 * configuration changes. Used for one-shot side effects (navigation, snackbar, toast)
 * where plain LiveData replay would cause duplicate handling.
 */
open class LiveEvent<T> : MediatorLiveData<T>() {
    private val wrappers = mutableListOf<Pair<LifecycleOwner, ObserverWrapper<in T>>>()

    override fun observe(
        owner: LifecycleOwner,
        observer: Observer<in T>,
    ) {
        val wrapper = ObserverWrapper(observer)
        wrappers.add(owner to wrapper)
        super.observe(owner, wrapper)
    }

    override fun removeObservers(owner: LifecycleOwner) {
        wrappers.removeAll { it.first === owner }
        super.removeObservers(owner)
    }

    override fun removeObserver(observer: Observer<in T>) {
        val iterator = wrappers.iterator()
        while (iterator.hasNext()) {
            val (_, wrapper) = iterator.next()
            if (wrapper === observer || wrapper.observer === observer) {
                iterator.remove()
                super.removeObserver(wrapper)
                return
            }
        }
        super.removeObserver(observer)
    }

    override fun setValue(t: T?) {
        wrappers.forEach { (_, wrapper) -> wrapper.newValue() }
        super.setValue(t)
    }

    private class ObserverWrapper<T>(val observer: Observer<in T>) : Observer<T> {
        private val pending = AtomicBoolean(false)

        override fun onChanged(value: T) {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(value)
            }
        }

        fun newValue() {
            pending.set(true)
        }
    }
}

class MutableLiveEvent<T> : LiveEvent<T>() {
    public override fun setValue(t: T?) {
        super.setValue(t)
    }

    public override fun postValue(value: T) {
        super.postValue(value)
    }
}
