package com.study.akka_spring;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import org.springframework.context.ApplicationContext;

public class SpringActorProducer implements IndirectActorProducer {
    private final ApplicationContext applicationContext;
    private final String beanActorName;

    public SpringActorProducer(ApplicationContext applicationContext, String beanActorName) {
        this.applicationContext = applicationContext;
        this.beanActorName = beanActorName;
    }

    @Override
    public Actor produce() {
        return (Actor) applicationContext.getBean(beanActorName);
    }
    @Override
    public Class<? extends Actor> actorClass() {
        return (Class<? extends Actor>) applicationContext.getType(beanActorName);
    }
}