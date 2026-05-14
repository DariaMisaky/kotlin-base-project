package com.daria.kotlinbase.shared.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A LiveData-like holder that emits an event exactly once per observer, even after
 * configuration changes. Used for one-shot side effects (navigation, snackbar, toast)
 * where StateFlow/LiveData replay would cause duplicate handling.
 */
open class LiveEvent<T> : MediatorLiveData<T>() {
    private val observers = mutableSetOf<ObserverWrapper<in T>>()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val wrapper = ObserverWrapper(observer)
        observers.add(wrapper)
        super.observe(owner, wrapper)
    }

    override fun removeObservers(owner: LifecycleOwner) {
        observers.clear()
        super.removeObservers(owner)
    }

    override fun removeObserver(observer: Observer<in T>) {
        if (observers.remove(observer as ObserverWrapper<*>)) {
            super.removeObserver(observer)
            return
        }
        val iterator = observers.iterator()
        while (iterator.hasNext()) {
            val wrapper = iterator.next()
            if (wrapper.observer == observer) {
                iterator.remove()
                super.removeObserver(wrapper)
                break
            }
        }
    }

    override fun setValue(t: T?) {
        observers.forEach { it.newValue() }
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
