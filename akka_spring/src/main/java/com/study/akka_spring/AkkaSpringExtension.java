package com.study.akka_spring;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.Props;
import org.springframework.context.ApplicationContext;

public class AkkaSpringExtension extends AbstractExtensionId<AkkaSpringExtension.SpringExt> {
    public static final AkkaSpringExtension SPRING_EXTENSION_PROVIDER = new AkkaSpringExtension();

    @Override
    public SpringExt createExtension(ExtendedActorSystem system) {
        return new SpringExt();
    }

    public static class SpringExt implements Extension {
        private volatile ApplicationContext applicationContext;
        public void initialize(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }
        public Props props(String actorBeanName) {
            return Props.create(SpringActorProducer.class, applicationContext, actorBeanName);
        }
    }
}