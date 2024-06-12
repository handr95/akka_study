package com.study.akka_basic;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static akka.pattern.PatternsCS.ask;

/**
 * 텍스트를 받아서 각 라인마다 단어 수를 계산하는 액터
 */
public class ReadingActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private String text;

    public ReadingActor(String text) {
        this.text = text;
    }

    public static Props props(String text) {
        return Props.create(ReadingActor.class, text);
    }

    /**
     * ReadingActor에게 텍스트를 처리하도록 알리는 메시지입니다.
     */
    public static final class ReadLines {
    }

    @Override
    public void preStart() {
        log.info("Starting com.study.akka_basic.ReadingActor {}", this);
    }

    @Override
    public void postStop() {
        log.info("Stopping com.study.akka_basic.ReadingActor {}", this);
    }

    /**
     * ReadLines 메시지를 받으면 텍스트를 라인 단위로 분할하고,
     * 각 라인마다 WordCounterActor를 생성하여 단어 수를 계산하도록 합니다.
     * @return
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ReadLines.class, r -> {

                    log.info("Received ReadLines message from " + getSender());

                    String[] lines = text.split("\n");
                    List<CompletableFuture> futures = new ArrayList<>();

                    // 각각의 WordCounterActor로부터 CompletableFuture를 받아서, 각 라인의 단어 수를 비동기적으로 기다립니다.
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        ActorRef wordCounterActorRef = getContext().actorOf(Props.create(WordCounterActor.class), "word-counter-" + i);

                        // 모든 라인의 CompletableFuture를 합하여 총 단어 수를 계산합니다.
                        CompletableFuture<Object> future =
                                ask(wordCounterActorRef, new WordCounterActor.CountWords(line), 1000).toCompletableFuture();
                        futures.add(future);
                    }

                    Integer totalNumberOfWords = futures.stream()
                            .map(CompletableFuture::join)
                            .mapToInt(n -> (Integer) n)
                            .sum();

                    // 마지막으로 PrinterActor에게 최종 결과를 전달합니다.
                    ActorRef printerActorRef = getContext().actorOf(Props.create(PrinterActor.class), "Printer-Actor");
                    printerActorRef.forward(new PrinterActor.PrintFinalResult(totalNumberOfWords), getContext());
                    // printerActorRef.tell(new com.study.akka_basic.PrinterActor.PrintFinalResult(totalNumberOfWords), getSelf());

                })
                .build();
    }
}