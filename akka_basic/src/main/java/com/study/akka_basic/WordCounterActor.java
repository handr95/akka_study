package com.study.akka_basic;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * 주어진 문자열에서 단어 수를 계산하는 액터
 */
public class WordCounterActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    /**
     * 액터가 처리할 메시지를 나타냅니다.
     */
    public static final class CountWords {
        String line;

        public CountWords(String line) {
            this.line = line;
        }
    }

    /**
     * 액터가 시작될 때 자동으로 호출. 로그 메시지를 기록합니다.
     */
    @Override
    public void preStart() {
        log.info("Starting com.study.akka_basic.WordCounterActor {}", this);
    }

    /**
     * CountWords 메시지를 받으면 countWordsFromLine 메서드를 호출하여 단어 수를 계산한 후,
     * 결과를 메시지를 보낸 액터에게 응답합니다.
     *
     * @return 액터가 수신할 메시지와 해당 메시지를 처리하는 방법 반환
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CountWords.class, r -> {
                    try {
                        log.info("Received CountWords message from " + getSender());
                        int numberOfWords = countWordsFromLine(r.line);
                        getSender().tell(numberOfWords, getSelf());
                    } catch (Exception ex) {
                        getSender().tell(new akka.actor.Status.Failure(ex), getSelf());
                        throw ex;
                    }
                })
                .build();
    }

    /**
     * 주어진 문자열을 단어로 분리하고, 각 단어의 길이가 0보다 큰 경우 단어 수를 증가시킵니다.
     */
    private int countWordsFromLine(String line) {

        if (line == null) {
            throw new IllegalArgumentException("The text to process can't be null!");
        }

        int numberOfWords = 0;
        String[] words = line.split(" ");
        for (String possibleWord : words) {
            if (!possibleWord.trim().isEmpty()) {
                numberOfWords++;
            }
        }
        return numberOfWords;
    }
}