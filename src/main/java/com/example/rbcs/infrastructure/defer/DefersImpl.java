package com.example.rbcs.infrastructure.defer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 延迟执行某个任务
 */
@Component
@RequiredArgsConstructor
public class DefersImpl implements Defers {
    private final ApplicationEventPublisher eventPublisher;
    @Override
    public void deferOnTransactionComplete(Runnable runnable) {
        eventPublisher.publishEvent(new TransactionCompleteDeferEvent(runnable));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void onTransactionCompleteDeferEvent(TransactionCompleteDeferEvent event) {
        event.getRunnable().run();
    }

    public static class TransactionCompleteDeferEvent {
        @Getter
        private final Runnable runnable;

        public TransactionCompleteDeferEvent(Runnable runnable) {
            this.runnable = runnable;
        }
    }
}
