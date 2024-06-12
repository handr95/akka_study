# akkaProject 코드 정리

## FirstActor

```cpp
package org.example;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class FirstActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(FirstActor.class);
    }

    @Override
    public void preStart() {
        log.info("Actor started");
    }

    @Override
    public void postStop() {
        log.info("Actor stopped");
    }

    // Messages will not be handled
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .build();
    }
}
```

## MyActor

```cpp
package org.example;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MyActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public void postStop() {
        //Akka가 Actor 중지할 때 자동으로 호출됩니다. Actor가 중지되고 있음을 나타내는 메시지를 로그에 기록합니다.
        log.info("Stopping actor {}", this);
    }

    public Receive createReceive() {
        return receiveBuilder()
                // 메시지가 정확히 "printit"인 경우를 매칭합니다.
                .matchEquals("printit", p -> {
                    // 메시지를 받으면 Actor는 자신의 주소를 콘솔에 출력하고 "Got Message"라는 메시지를 보낸 사람에게 다시 보냅니다.
                    System.out.println("The address of this actor is: " + getSelf());
                    getSender().tell("Got Message", getSelf());
                })
                .build();
    }
}
```

## PrinterActor

```cpp
package org.example;

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
        log.info("Starting PrinterActor {}", this);
    }

    @Override
    public void postStop() {
        log.info("Stopping PrinterActor {}", this);
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
```

## ReadingActor

```cpp
package org.example;

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
        log.info("Starting ReadingActor {}", this);
    }

    @Override
    public void postStop() {
        log.info("Stopping ReadingActor {}", this);
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
                    // printerActorRef.tell(new PrinterActor.PrintFinalResult(totalNumberOfWords), getSelf());

                })
                .build();
    }
}
```

### WordCounterActor

```cpp
package org.example;

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
        log.info("Starting WordCounterActor {}", this);
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
    private int countWordsFromLine(String line) throws Exception {

        if (line == null) {
            throw new IllegalArgumentException("The text to process can't be null!");
        }

        int numberOfWords = 0;
        String[] words = line.split(" ");
        for (String possibleWord : words) {
            if (possibleWord.trim().length() > 0) {
                numberOfWords++;
            }
        }
        return numberOfWords;
    }
}
```

### AkkaActorsUnitTest

```cpp
package org.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Kill;
import akka.actor.PoisonPill;
import akka.actor.Props;

import akka.actor.Terminated;
import akka.testkit.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static akka.pattern.PatternsCS.ask;
import static org.junit.jupiter.api.Assertions.*;

public class AkkaActorsUnitTest {

    private static ActorSystem system = null;

    @BeforeAll
    public static void setup() {
        // "test-system"이라는 이름의 새로운 배우 시스템을 생성합니다.
        system = ActorSystem.create("test-system");
    }

    @AfterAll
    public static void teardown() {
        // 배우 시스템을 종료하며, 1000 밀리초 이내에 중지되도록 합니다.
        TestKit.shutdownActorSystem(system, Duration.apply(1000, TimeUnit.MILLISECONDS), true);
        system = null;
    }

    @Test
    void givenAnActor_sendHimAMessageUsingTell() {
        // Akka에서 제공하는 테스트 유틸리티인 TestKit 프로브를 생성합니다.
        final TestKit probe = new TestKit(system);
        // 프로브의 자식으로 MyActor 타입의 새로운 배우를 생성합니다.
        ActorRef myActorRef = probe.childActorOf(Props.create(MyActor.class));
        // myActorRef 배우에게 "printit" 메시지를 보내고, 보낸 사람을 프로브로 지정합니다.
        myActorRef.tell("printit", probe.testActor());

        // 프로브가 기본 타임아웃 기간 내에 "Got Message" 메시지를 받았는지 확인합니다.
        probe.expectMsg("Got Message");
    }

    @Test
    void givenAnActor_sendHimAMessageUsingAsk() throws ExecutionException, InterruptedException {
        final TestKit probe = new TestKit(system);
        // WordCounterActor의 인스턴스를 생성하고 probe.childActorOf를 통해 해당 액터를 생성
        ActorRef wordCounterActorRef = probe.childActorOf(Props.create(WordCounterActor.class));

        // ask 메서드를 사용하여 CountWords 메시지를 액터에게 보내고, 결과를 CompletableFuture로 받습니다.
        CompletableFuture<Object> future =
                ask(wordCounterActorRef, new WordCounterActor.CountWords("this is a text"), 1000).toCompletableFuture();

        Integer numberOfWords = (Integer) future.get();
        assertEquals(4, (int) numberOfWords, "The actor should count 4 words");
    }

    @Test
    void givenAnActor_whenTheMessageIsNull_respondWithException() {
        final TestKit probe = new TestKit(system);
        ActorRef wordCounterActorRef = probe.childActorOf(Props.create(WordCounterActor.class));

        CompletableFuture<Object> future =
                ask(wordCounterActorRef, new WordCounterActor.CountWords(null), 1000).toCompletableFuture();

        /**
         * [INFO] [06/12/2024 16:21:01.126] [test-system-akka.actor.default-dispatcher-2] [akka://test-system/system/testActor-1/$a] Starting WordCounterActor org.example.WordCounterActor@70ec2b7d
         * [INFO] [06/12/2024 16:21:01.128] [test-system-akka.actor.default-dispatcher-2] [akka://test-system/system/testActor-1/$a] Received CountWords message from Actor[akka://test-system/temp/$a]
         * [ERROR] [06/12/2024 16:21:01.132] [test-system-akka.actor.default-dispatcher-2] [akka://test-system/system/testActor-1/$a] The text to process can't be null!
         * java.lang.IllegalArgumentException: The text to process can't be null!
         * 	at org.example.WordCounterActor.countWordsFromLine(WordCounterActor.java:61)
         * 	at org.example.WordCounterActor.lambda$createReceive$0(WordCounterActor.java:45)
         */
        try {
            future.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            assertTrue(e.getMessage().contains("The text to process can't be null!"), "Invalid error message");
        } catch (InterruptedException | TimeoutException e) {
            fail("Actor should respond with an exception instead of timing out !");
        }
    }

    /**
     * Akka 시스템을 생성하고, 텍스트를 처리하는 데 필요한 액터들을 생성하고 통신하는 과정을 보여줍니다.
     */
    @Test
    void givenAnAkkaSystem_countTheWordsInAText() {
    	ActorSystem system = ActorSystem.create("test-system");

        ActorRef myActorRef = system.actorOf(Props.create(MyActor.class), "my-actor");
        myActorRef.tell("printit", null);
        // 해당 액터를 명시적으로 중지(stop)합니다. 이는 해당 액터가 처리 중인 모든 메시지를 처리한 후에 중지됩니다.
        system.stop(myActorRef);
        // PoisonPill 메시지를 특정 액터에게 보내어 해당 액터를 중지합니다. PoisonPill은 액터에게 종료 요청을 보내는 특별한 메시지입니다.
        // ActorRef.noSender()를 사용하여 발신자(sender)를 지정하지 않습니다. 따라서 액터가 PoisonPill 메시지를 받았을 때, 응답을 보낼 발신자가 없게 됩니다.
        myActorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
        // Kill 메시지를 특정 액터에게 보내어 해당 액터를 강제로 종료합니다. Kill 메시지를 받은 액터는 즉시 종료됩니다.
        // 마찬가지로 ActorRef.noSender()를 사용하여 발신자를 지정하지 않습니다.
        myActorRef.tell(Kill.getInstance(), ActorRef.noSender());

        // ReadingActor를 생성하여 텍스트를 처리하도록 액터 시스템에 추가하고, ReadLines 메시지를 보내어 텍스트 처리를 시작합니다.
        ActorRef readingActorRef = system.actorOf(ReadingActor.props(TEXT), "readingActor");
        // ReadingActor에서 각 라인의 단어 수를 계산하는 동안 생성되는 각각의 WordCounterActor들이 비동기적으로 작동하며, 그 결과를 PrinterActor에게 보냅니다.
        readingActorRef.tell(new ReadingActor.ReadLines(), ActorRef.noSender());    //ActorRef.noSender() means the sender ref is akka://test-system/deadLetters

        //  Akka 액터 시스템을 종료하는 메서드입니다. 이 메서드는 현재 액터 시스템에 등록된 모든 액터들을 중지하고, 액터 시스템 자체를 종료
        Future<Terminated> terminateResponse = system.terminate();
    }
    
    private static String TEXT = "Lorem Ipsum is simply dummy text\n" +
            "of the printing and typesetting industry.\n" +
            "Lorem Ipsum has been the industry's standard dummy text\n" +
            "ever since the 1500s, when an unknown printer took a galley\n" +
            "of type and scrambled it to make a type specimen book.\n" +
            " It has survived not only five centuries, but also the leap\n" +
            "into electronic typesetting, remaining essentially unchanged.\n" +
            " It was popularised in the 1960s with the release of Letraset\n" +
            " sheets containing Lorem Ipsum passages, and more recently with\n" +
            " desktop publishing software like Aldus PageMaker including\n" +
            "versions of Lorem Ipsum.";

}
```