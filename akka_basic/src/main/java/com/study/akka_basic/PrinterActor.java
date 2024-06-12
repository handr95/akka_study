package com.study.akka_basic;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 *  최종 결과를 출력하는 액터
 */
public class PrinterActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(String text) {
        return Props.create(PrinterActor.class, text);
    }

    /**
     * 최종 결과를 프린팅하기 위한 메시지입니다.
     */
    public static final class PrintFinalResult {
        Integer totalNumberOfWords;

        public PrintFinalResult(Integer totalNumberOfWords) {
            this.totalNumberOfWords = totalNumberOfWords;
        }
    }

    @Override
    public void preStart() {
        log.info("Starting com.study.akka_basic.PrinterActor {}", this);
    }

    @Override
    public void postStop() {
        log.info("Stopping com.study.akka_basic.PrinterActor {}", this);
    }

    /**
     * PrintFinalResult 메시지를 받으면 결과를 프린팅합니다.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PrinterActor.PrintFinalResult.class,
                        r -> {
                            log.info("Received PrintFinalResult message from " + getSender());
                            log.info("The text has a total number of {} words", r.totalNumberOfWords);
                        })
                .build();
    }
}