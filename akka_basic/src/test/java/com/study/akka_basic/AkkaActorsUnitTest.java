package com.study.akka_basic;

import akka.actor.*;
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


class AkkaActorsUnitTest {

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
        // 프로브의 자식으로 com.study.akka_basic.MyActor 타입의 새로운 배우를 생성합니다.
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
         * [INFO] [06/12/2024 16:21:01.126] [test-system-akka.actor.default-dispatcher-2] [akka://test-system/system/testActor-1/$a] Starting com.study.akka_basic.WordCounterActor org.example.com.study.akka_basic.WordCounterActor@70ec2b7d
         * [INFO] [06/12/2024 16:21:01.128] [test-system-akka.actor.default-dispatcher-2] [akka://test-system/system/testActor-1/$a] Received CountWords message from Actor[akka://test-system/temp/$a]
         * [ERROR] [06/12/2024 16:21:01.132] [test-system-akka.actor.default-dispatcher-2] [akka://test-system/system/testActor-1/$a] The text to process can't be null!
         * java.lang.IllegalArgumentException: The text to process can't be null!
         * 	at org.example.com.study.akka_basic.WordCounterActor.countWordsFromLine(com.study.akka_basic.WordCounterActor.java:61)
         * 	at org.example.com.study.akka_basic.WordCounterActor.lambda$createReceive$0(com.study.akka_basic.WordCounterActor.java:45)
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

       /* ActorRef myActorRef = system.actorOf(Props.create(MyActor.class), "my-actor");
        myActorRef.tell("printit", null);
        // 해당 액터를 명시적으로 중지(stop)합니다. 이는 해당 액터가 처리 중인 모든 메시지를 처리한 후에 중지됩니다.
        system.stop(myActorRef);
        // PoisonPill 메시지를 특정 액터에게 보내어 해당 액터를 중지합니다. PoisonPill은 액터에게 종료 요청을 보내는 특별한 메시지입니다.
        // ActorRef.noSender()를 사용하여 발신자(sender)를 지정하지 않습니다. 따라서 액터가 PoisonPill 메시지를 받았을 때, 응답을 보낼 발신자가 없게 됩니다.
        myActorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
        // Kill 메시지를 특정 액터에게 보내어 해당 액터를 강제로 종료합니다. Kill 메시지를 받은 액터는 즉시 종료됩니다.
        // 마찬가지로 ActorRef.noSender()를 사용하여 발신자를 지정하지 않습니다.
        myActorRef.tell(Kill.getInstance(), ActorRef.noSender());*/


        // ReadingActor를 생성하여 텍스트를 처리하도록 액터 시스템에 추가하고, ReadLines 메시지를 보내어 텍스트 처리를 시작합니다.
        ActorRef readingActorRef = system.actorOf(ReadingActor.props(TEXT), "readingActor");
        // ReadingActor에서 각 라인의 단어 수를 계산하는 동안 생성되는 각각의 WordCounterActor들이 비동기적으로 작동하며, 그 결과를 PrinterActor에게 보냅니다.
        readingActorRef.tell(new ReadingActor.ReadLines(), ActorRef.noSender());    //ActorRef.noSender() means the sender ref is akka://test-system/deadLetters

        /*//  Akka 액터 시스템을 종료하는 메서드입니다. 이 메서드는 현재 액터 시스템에 등록된 모든 액터들을 중지하고, 액터 시스템 자체를 종료
        Future<Terminated> terminateResponse = system.terminate();*/
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