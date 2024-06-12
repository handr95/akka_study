package com.study.akka_basic;

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
                    log.info("The address of this actor is: " + getSelf());
                    getSender().tell("Got Message", getSelf());
                })
                .build();
    }
}