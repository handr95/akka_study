# Introduction to Spring with Akka

> [https://www.baeldung.com/akka-with-spring](https://www.baeldung.com/akka-with-spring)
> 

## **1. Introduction**

이 기사에서는 Akka를 Spring Framework와 통합하여 Akka 액터에 Spring 기반 서비스를 주입하는 데 중점을 둘 것입니다.

이 글을 읽기 전에 Akka의 기본 사항에 대한 사전 지식을 갖추는 것이 좋습니다.

## **2. Dependency Injection in Akka**

Akka는 Actor 동시성 모델을 기반으로 하는 강력한 애플리케이션 프레임워크입니다. 프레임워크는 Scala로 작성되었으므로 Java 기반 애플리케이션에서도 완벽하게 사용할 수 있습니다. 따라서 **Akka를 기존 Spring 기반 애플리케이션과 통합하거나** 단순히 Spring을 사용하여 Bean을 액터에 연결하려는 경우가 매우 많습니다.

Spring/Akka 통합의 문제는 Spring의 빈 관리와 Akka의 액터 관리 간의 차이에 있습니다. **액터는 일반적인 Spring 빈 라이프사이클과 다른 특정 라이프사이클을 갖습니다.**

또한 액터는 액터 자체(내부 구현 세부 사항이며 Spring에서 관리할 수 없음)와 클라이언트 코드에서 액세스할 수 있을 뿐만 아니라 직렬화 가능하고 다른 Akka 런타임 간에 이식 가능한 액터 참조로 분할됩니다.

다행히 Akka는 외부 종속성 주입 프레임워크를 매우 쉽게 사용할 수 있게 해주는 Akka 확장이라는 메커니즘을 제공합니다.

## **3. Dependencies**

pom.xml

```cpp
<properties>
    <spring.version>5.3.25</spring.version>
    <akka.version>2.4.14</akka.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>${spring.version}</version>
    </dependency>

    <dependency>
        <groupId>com.typesafe.akka</groupId>
        <artifactId>akka-actor_2.12</artifactId>
        <version>${akka.version}</version>
    </dependency>

</dependencies>
```

build.gradle

```cpp
dependencies {
    implementation "org.springframework:spring-context:${spring_version}"
    implementation "com.typesafe.akka:akka-actor_${scala_version}:${typesafe_akka_version}"

    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.1'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.1'
}
```

gradle.properties

```cpp
typesafe_akka_version=2.4.14
scala_version=2.12
spring_version=5.3.25
```

최신 버전의 [spring-context](https://mvnrepository.com/artifact/org.springframework/spring-context) 및 [akka-actor](https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor) 종속성을 확인하려면 Maven Central을 확인하세요.

그리고 akka-actor 종속성의 이름에 _2.11 접미사가 있다는 점에 주목하세요. 이는 이 버전의 Akka 프레임워크가 Scala 버전 2.11에 대해 구축되었음을 나타냅니다. 해당 버전의 Scala 라이브러리가 빌드에 전이적으로 포함됩니다.

## **4. Injecting Spring Beans into Akka Actors**

사람에게 인사를 보내서 사람의 이름에 답할 수 있는 단일 액터로 구성된 간단한 Spring/Akka 애플리케이션을 만들어 보겠습니다. 인사말 로직은 별도의 서비스로 추출됩니다. 우리는 이 서비스를 행위자 인스턴스에 자동 연결하려고 합니다. Spring 통합은 이 작업에 도움이 될 것입니다.

### **4.1. Defining an Actor and a Service**

행위자에 서비스 주입을 시연하기 위해 유형이 지정되지 않은 행위자로 정의된 간단한 클래스 GreetingActor(Akka의 UntypedActor 기본 클래스 확장)를 만듭니다. 모든 Akka 액터의 주요 메소드는 메시지를 수신하고 지정된 로직에 따라 처리하는 onReceive 메소드입니다.

우리의 경우 GreetingActor 구현은 메시지가 미리 정의된 Greet 유형인지 확인한 다음 Greet 인스턴스에서 사람의 이름을 가져온 다음 GreetingService를 사용하여 이 사람에 대한 인사말을 받고 보낸 사람에게 수신된 인사말 문자열로 응답합니다. 메시지가 알 수 없는 다른 유형인 경우 행위자의 미리 정의된 처리되지 않은 메서드로 전달됩니다.

```cpp
import akka.actor.UntypedActor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GreetingActor extends UntypedActor {

    private final GreetingService greetingService;

    public GreetingActor(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof Greet) {
            String name = ((Greet) message).getName();
            getSender().tell(greetingService.greet(name), getSelf());
        } else {
            unhandled(message);
        }
    }

    public static class Greet {
        private String name;

        public Greet(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }
}
```

Greet 메시지 유형은 이 액터 내부의 정적 내부 클래스로 정의되며 이는 좋은 사례로 간주됩니다. 허용되는 메시지 유형은 해당 행위자가 처리할 수 있는 메시지 유형에 대한 혼란을 피하기 위해 가능한 한 행위자에 가깝게 정의되어야 합니다.

또한 **Spring 어노테이션 @Component 및 @Scope를 확인하세요**. 이는 클래스를 프로토타입 범위가 있는 Spring 관리 Bean으로 정의합니다.

범위는 매우 중요합니다. 모든 Bean 검색 요청은 새로 생성된 인스턴스를 생성해야 하기 때문입니다. 이 동작은 Akka의 행위자 수명 주기와 일치하기 때문입니다. 다른 범위로 이 Bean을 구현하는 경우 Akka에서 액터를 다시 시작하는 일반적인 경우가 잘못 작동할 가능성이 높습니다.

마지막으로 GreetingService 인스턴스를 명시적으로 @Autowire할 필요가 없다는 점에 유의하세요. 이는 암시적 생성자 주입이라는 Spring 4.3의 새로운 기능으로 인해 가능합니다.

GreeterService의 구현은 매우 간단합니다. @Component 주석을 추가하여 Spring 관리 Bean으로 정의했습니다(기본 싱글톤 범위 사용).

```cpp
import org.springframework.stereotype.Component;

@Component
public class GreetingService {
    public String greet(String name) {
        return "Hello, " + name;
    }
}
```

### **4.2. Adding Spring Support via Akka Extension**

Spring을 Akka와 통합하는 가장 쉬운 방법은 Akka 확장을 사용하는 것입니다.

**확장은 행위자 시스템별로 생성된 싱글톤 인스턴스입니다.** 이는 마커 인터페이스 Extension을 구현하는 확장 클래스 자체와 일반적으로 AbstractExtensionId를 상속하는 확장 ID 클래스로 구성됩니다.

이 두 클래스는 밀접하게 결합되어 있으므로 ExtensionId 클래스 내에 중첩된 Extension 클래스를 구현하는 것이 좋습니다.

```cpp
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
}1/
```

1. AkkaSpringExtension은 확장 인스턴스인 SpringExt 객체의 생성을 설명하는 AbstractExtensionId 클래스의 단일 createExtension 메서드를 구현합니다.
    
    AkkaSpringExtension 클래스에는 유일한 인스턴스에 대한 참조를 보유하는 정적 필드 SPRING_EXTENSION_PROVIDER도 있습니다. SpringExtention이 싱글톤 클래스여야 함을 명시적으로 나타내기 위해 전용 생성자를 추가하는 것이 종종 의미가 있지만 명확성을 위해 이를 생략하겠습니다.
    
2.  정적 내부 클래스 SpringExt는 확장 자체입니다. Extension은 단순한 마커 인터페이스이므로 적합하다고 판단되는 대로 이 클래스의 내용을 정의할 수 있습니다.
    1. 우리의 경우 Spring ApplicationContext 인스턴스를 유지하기 위한 초기화 메소드가 필요합니다. 이 메소드는 확장 초기화마다 한 번만 호출됩니다.
    2. 또한 Props 객체를 생성하려면 props 메소드가 필요합니다. Props 인스턴스는 액터에 대한 청사진이며, 우리의 경우 Props.create 메소드는 SpringActorProducer 클래스와 이 클래스에 대한 생성자 인수를 받습니다. 이는 이 클래스의 생성자가 호출되는 인수입니다.
    3. props 메소드는 Spring이 관리하는 액터 참조가 필요할 때마다 실행됩니다.
3. 3. SpringActorProducer : Akka의 IndirectActorProducer 인터페이스를 구현하여 생성 및 actorClass 메서드를 구현하여 액터의 인스턴스화 프로세스를 재정의할 수 있습니다.

이미 짐작하셨겠지만 **직접 인스턴스화 대신 항상 Spring의 ApplicationContext에서 액터 인스턴스를 검색합니다.** 액터를 프로토타입 범위의 Bean으로 만들었으므로 생성 메소드를 호출할 때마다 액터의 새 인스턴스가 반환됩니다.

```cpp
import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import org.springframework.context.ApplicationContext;

public class SpringActorProducer implements IndirectActorProducer {
    private ApplicationContext applicationContext;
    private String beanActorName;

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
```

### **4.3. Putting It All Together**

이제 남은 유일한 일은 Spring에 모든 중첩 패키지와 함께 현재 패키지를 스캔하도록 지시하는 Spring 구성 클래스(@Configuration 주석으로 표시됨)를 생성하고(이는 @ComponentScan 주석으로 보장됨) Spring 컨테이너를 생성하는 것입니다. .

우리는 단지 하나의 추가 빈(ActorSystem 인스턴스)을 추가하고 이 ActorSystem에서 Spring 확장을 초기화하기만 하면 됩니다.

```cpp
import akka.actor.ActorSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class AppConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("akka-spring-demo");
        AkkaSpringExtension.SPRING_EXTENSION_PROVIDER.get(system).initialize(applicationContext);
        return system;
    }
}
```

### **4.4. Retrieving Spring-Wired Actors**

모든 것이 올바르게 작동하는지 테스트하기 위해 ActorSystem 인스턴스를 코드(Spring 관리 애플리케이션 코드 또는 Spring 기반 테스트)에 삽입하고, 확장을 사용하여 액터에 대한 Props 객체를 생성하고, 액터에 대한 참조를 검색할 수 있습니다. Props 객체를 통해 누군가에게 인사를 하려고 합니다:

```cpp
ActorRef greeter = system.actorOf(SPRING_EXTENSION_PROVIDER.get(system)
  .props("greetingActor"), "greeter");

FiniteDuration duration = FiniteDuration.create(1, TimeUnit.SECONDS);
Timeout timeout = Timeout.durationToTimeout(duration);

Future<Object> result = ask(greeter, new Greet("John"), timeout);

Assert.assertEquals("Hello, John", Await.result(result, duration));
```

여기서는 Scala의 Future 인스턴스를 반환하는 일반적인 akka.pattern.Patterns.ask 패턴을 사용합니다. 계산이 완료되면 Future는 GreetingActor.onMessasge 메소드에서 반환된 값으로 해결됩니다.

Scala의 Await.result 메소드를 Future에 적용하여 결과를 기다리거나, 더 바람직하게는 비동기 패턴으로 전체 애플리케이션을 구축할 수 있습니다.

## **5. Conclusion**

이 글에서는 Spring Framework를 Akka와 통합하고 Autowire Bean을 액터에 통합하는 방법을 보여주었습니다.

이 글의 소스 코드는 [GitHub](https://github.com/eugenp/tutorials/tree/master/akka-modules/spring-akka)에서 확인할 수 있습니다.

[예제 코드](https://www.notion.so/389c780549e64c2287d358c9414208da?pvs=21)