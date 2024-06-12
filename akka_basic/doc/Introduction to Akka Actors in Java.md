# Introduction to Akka Actors in Java

> [https://www.baeldung.com/akka-actors-java](https://www.baeldung.com/akka-actors-java) 번역
> 

## 1. **Introduction**

Akka는 액터 모델을 활용하여 Java 또는 Scala를 사용하여 동시 및 분산 애플리케이션을 쉽게 개발할 수 있도록 도와주는 오픈 소스 라이브러리입니다.

이 튜토리얼에서는 액터 정의, 통신 방법, 액터 종료 방법과 같은 기본 기능을 소개합니다. 마지막 노트에서는 Akka로 작업할 때의 몇 가지 모범 사례도 소개합니다.

## 2. **The Actor Model**

액터 모델은 컴퓨터 과학 커뮤니티에서 새로운 개념은 아닙니다. 1973년 Carl Eddie Hewitt이 동시 계산을 처리하기 위한 이론적 모델로 처음 소개했습니다.

소프트웨어 업계에서 동시 및 분산 애플리케이션 구현의 함정을 깨닫기 시작하면서 실용적인 적용 가능성을 보이기 시작했습니다.

액터는 독립적인 계산 단위를 나타냅니다. 몇 가지 중요한 특징이 있습니다:

- 액터는 자신의 상태와 애플리케이션 로직의 일부를 캡슐화합니다.
- 액터는 비동기 메시지를 통해서만 상호 작용하고 직접적인 메서드 호출을 통해서는 상호 작용하지 않습니다.
- 각 액터에는 고유한 주소와 다른 액터가 메시지를 전달할 수 있는 메일박스가 있습니다.
액터는 메일박스에 있는 모든 메시지를 순차적으로 처리합니다(메일박스의 기본 구현은 FIFO 큐입니다).
- 액터 시스템은 트리와 같은 계층 구조로 구성됩니다.
- 액터는 다른 액터를 생성할 수 있고, 다른 액터에게 메시지를 보낼 수 있으며, 자신 또는 생성된 액터를 중지할 수 있습니다.

### **2.1. Advantages**

동기화, Lock 및 공유 메모리를 처리해야 하기 때문에 동시 애플리케이션을 개발하는 것은 어렵습니다. Akka 액터를 사용하면 Lock과 동기화 없이도 비동기 코드를 쉽게 작성할 수 있습니다.

메서드 호출 대신 메시지를 사용할 때의 장점 중 하나는 발신자 스레드가 다른 액터에 메시지를 보낼 때 반환 값을 기다리기 위해 차단되지 않는다는 것입니다. 수신 액터는 발신자에게 회신 메시지를 전송하여 결과에 응답합니다.

메시지 사용의 또 다른 큰 장점은 멀티스레드 환경에서 동기화에 대해 걱정할 필요가 없다는 것입니다. 모든 메시지가 순차적으로 처리되기 때문입니다.

Akka 액터 모델의 또 다른 장점은 오류 처리입니다. 액터를 계층 구조로 구성하면 각 액터가 부모 액터에 장애를 알리고 그에 따라 조치를 취할 수 있습니다. 부모 액터는 자식 액터를 중지하거나 다시 시작하도록 결정할 수 있습니다.

# **3. Setup**

To take advantage of the Akka actors we need to add the following dependency from [Maven Central](https://mvnrepository.com/search?q=akka-actor):

```cpp
<dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-actor_2.12</artifactId>
    <version>2.5.11</version>
</dependency>
```

```cpp
dependencies {
    implementation 'com.typesafe.akka:akka-actor_2.12:2.5.11'
}
```

# **4. Creating an Actor**

앞서 언급했듯이 액터는 계층 구조 시스템으로 정의됩니다. 공통 구성을 공유하는 모든 액터는 *ActorSystem.*로 정의됩니다.

지금은 기본 구성과 커스텀 이름으로 *ActorSystem*을 간단히 정의해 보겠습니다:

```cpp
import akka.actor.ActorSystem;

public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("test-system");
    }
}
```

아직 액터를 생성하지 않았지만 시스템에는 이미 3개의 메인 액터가 포함됩니다:

- 루트 가디언 액터는 이름에서 알 수 있듯이 액터 시스템 계층 구조의 루트를 나타내는 “/” 주소를 가집니다.
- 사용자 가디언 액터는 “/user” 주소를 갖습니다. 이것은 우리가 정의하는 모든 액터의 부모가 됩니다.
- 시스템 가디언 액터는 “/system” 주소를 갖습니다. Akka 시스템에서 내부적으로 정의한 모든 액터의 부모가 됩니다.

모든 Akka 액터는 AbstractActor 추상 클래스를 확장하고 다른 액터로부터 들어오는 메시지를 처리하기 위해 createReceive() 메서드를 구현합니다:

```cpp
import akka.actor.AbstractActor;

public class MyActor extends AbstractActor {
    public Receive createReceive() {
        return receiveBuilder().build();
    }
}
```

이것은 우리가 만들 수 있는 가장 기본적인 액터입니다. 다른 액터로부터 메시지를 수신할 수 있으며, 수신 빌더에 일치하는 메시지 패턴이 정의되어 있지 않으므로 메시지를 삭제합니다. 이 글의 뒷부분에서 메시지 패턴 매칭에 대해 설명하겠습니다.

이제 첫 번째 액터를 만들었으니 액터시스템에 포함시켜야 합니다:

```cpp
ActorRef readingActorRef = system.actorOf(Props.create(MyActor.class), "my-actor");
```

### **4.1. Actor Configuration**

Props 클래스에는 액터 구성이 포함되어 있습니다. 디스패처, 메일박스 또는 배포 구성과 같은 것을 구성할 수 있습니다. 이 클래스는 변경 불가능하므로 스레드에 안전하므로 새 액터를 만들 때 공유할 수 있습니다.

액터 오브젝트 내부에 Props 오브젝트 생성을 처리할 팩토리 메서드를 정의하는 것이 가장 권장되며 모범 사례로 간주됩니다.

예를 들어 텍스트 처리를 수행할 액터를 정의해 보겠습니다. 액터는 처리를 수행할 문자열 객체를 받습니다:

Props 클래스에는 액터 구성이 포함되어 있습니다. 디스패처, 메일박스 또는 배포 구성과 같은 것을 구성할 수 있습니다. 이 클래스는 변경 불가능하므로 스레드에 안전하므로 새 액터를 만들 때 공유할 수 있습니다.

액터 오브젝트 내부에 Props 오브젝트 생성을 처리할 팩토리 메서드를 정의하는 것이 가장 권장되며 모범 사례로 간주됩니다.

예를 들어 텍스트 처리를 수행할 액터를 정의해 보겠습니다. 액터는 처리를 수행할 문자열 객체를 받습니다:

```cpp
import akka.actor.AbstractActor;
import akka.actor.Props;

public class ReadingActor extends AbstractActor {
    private String text;

    public static Props props(String text) {
        return Props.create(ReadingActor.class, text);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }
    // ...
}
```

이제 이 유형의 액터 인스턴스를 생성하려면 props() 팩토리 메서드를 사용하여 생성자에 String 인수를 전달하기만 하면 됩니다:

```cpp
 ActorRef readingActorRef = system.actorOf(ReadingActor.props(TEXT), "readingActor");
```

이제 액터를 정의하는 방법을 알았으니 액터가 액터 시스템 내에서 어떻게 통신하는지 살펴봅시다.

## **5. Actor Messaging**

액터는 서로 상호 작용하기 위해 시스템 내 다른 액터와 메시지를 주고받을 수 있습니다. 이러한 메시지는 불변이라는 조건이 있는 모든 유형의 객체가 될 수 있습니다.

액터 클래스 내에서 메시지를 정의하는 것이 가장 좋습니다. 이렇게 하면 이해하기 쉬운 코드를 작성하고 액터가 처리할 수 있는 메시지를 파악하는 데 도움이 됩니다.

### **5.1. Sending Messages**

Akka 액터 시스템 내에서 메시지는 메서드를 사용하여 전송됩니다:

- *tell()*
- *ask()*
- *forward()*

**메시지를 보내고 싶지만 응답을 기대하지 않을 때는 tell() 메서드를 사용할 수 있습니다.** 이는 성능 관점에서 가장 효율적인 방법입니다:

```cpp
readingActorRef.tell(new ReadingActor.ReadLines(), ActorRef.noSender());
```

첫 번째 매개변수는 액터 주소 readingActorRef로 보내는 메시지를 나타냅니다.

두 번째 파라미터는 발신자가 누구인지 지정합니다. 메시지를 받는 액터가 발신자가 아닌 다른 액터(예: 보내는 액터의 부모)에게 응답을 보내야 할 때 유용합니다.

일반적으로 응답을 기대하지 않으므로 두 번째 매개변수를 null 또는 ActorRef.noSender()로 설정할 수 있습니다. **액터로부터 응답이 필요한 경우 ask() 메서드를 사용할 수 있습니다:**

```cpp
CompletableFuture<Object> future = ask(wordCounterActorRef, 
new WordCounterActor.CountWords(line), 1000).toCompletableFuture();
```

액터에 응답을 요청할 때 CompletionStage 객체가 반환되므로 처리가 차단되지 않습니다.

여기서 주의해야 할 매우 중요한 사실은 응답할 액터 내부의 오류 처리입니다. **예외를 포함하는 Future 객체를 반환하려면 발신자 액터에 Status.Failure 메시지를 보내야 합니다.**

이 작업은 액터가 메시지를 처리하는 동안 예외가 발생하여 ask() 호출이 시간 초과되고 로그에 예외에 대한 참조가 표시되지 않는 경우에는 자동으로 수행되지 않습니다:

```cpp
@Override
public Receive createReceive() {
    return receiveBuilder()
      .match(CountWords.class, r -> {
          try {
              int numberOfWords = countWordsFromLine(r.line);
              getSender().tell(numberOfWords, getSelf());
          } catch (Exception ex) {
              getSender().tell(
               new akka.actor.Status.Failure(ex), getSelf());
               throw ex;
          }
    }).build();
}
```

또한 tell()와 유사한 forward() 메서드도 있습니다. 차이점은 메시지를 보낼 때 메시지의 원래 발신자가 유지되므로 메시지를 전달하는 액터는 중개 액터 역할만 수행한다는 점입니다:

```cpp
printerActorRef.forward(
  new PrinterActor.PrintFinalResult(totalNumberOfWords), getContext());
```

### **5.2. Receiving Messages**

**각 액터는 수신되는 모든 메시지를 처리하는 createReceive() 메서드를 구현합니다.** receiveBuilder()는 스위치 문처럼 작동하여 수신된 메시지를 정의된 메시지 유형과 일치시키려고 시도합니다:

```cpp
public Receive createReceive() {
    return receiveBuilder().matchEquals("printit", p -> {
        System.out.println("The address of this actor is: " + getSelf());
    }).build();
}
```

**메시지가 수신되면 FIFO 대기열에 넣어져 메시지가 순차적으로 처리됩니다.**

## **6. Killing an Actor**

액터 사용이 끝나면 ActorRefFactory  인터페이스에서 stop() 메서드를 호출하여 액터를 중지할 수 있습니다:

```cpp
system.stop(myActorRef);
```

이 메서드를 사용하여 자식 액터 또는 액터 자체를 종료할 수 있습니다. 중지는 비동기적으로 수행되며 액터가 종료되기 전에 **현재 메시지 처리가 완료**된다는 점에 유의하세요**. 액터 메일함에는 더 이상 수신 메시지가 수락되지 않습니다.**

**부모 액터를 중지하면** 해당 액터에 의해 생성된 **모든 자식 액터에도 킬 신호를 보냅니다.**

액터 시스템이 더 이상 필요하지 않으면 액터 시스템을 종료하여 모든 리소스를 확보하고 메모리 누수를 방지할 수 있습니다:

```cpp
Future<Terminated> terminateResponse = system.terminate();
```

이렇게 하면 시스템 가디언 액터가 중지되므로 이 Akka 시스템에 정의된 모든 액터가 중지됩니다.

kill 할 Actor에게 PoisonPill 메시지를 보낼 수도 있습니다:

```cpp
myActorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
```

포이즌필 메시지는 다른 메시지와 마찬가지로 액터가 수신하여 대기열에 넣습니다. **액터는 포이즌필 메시지에 도달할 때까지 모든 메시지를 처리합니다.** 그래야만 액터가 종료 프로세스를 시작합니다.

액터를 종료하는 데 사용되는 또 다른 특수 메시지는 Kill 메시지입니다. 포이즌필과 달리, 이 메시지를 처리할 때 액터는 ActorKilledException을 던집니다:

```cpp
myActorRef.tell(Kill.getInstance(), ActorRef.noSender());
```

## **7. Conclusion**

이 글에서는 Akka 프레임워크의 기본 사항을 소개했습니다. 액터를 정의하는 방법, 액터가 서로 통신하는 방법, 액터를 종료하는 방법을 살펴보았습니다.

Akka로 작업할 때의 몇 가지 모범 사례로 마무리하겠습니다:

- 성능이 우려되는 경우 ask() 대신 tell() 사용
- ask()를 사용할 때는 항상 Failure 메시지를 전송하여 예외를 처리해야 합니다.
- 액터는 변경 가능한 상태를 공유해서는 안 됩니다.
- 액터가 다른 액터 내에서 선언되어서는 안됩니다.
- 액터는 더 이상 참조되지 않을 때 **자동으로 중지되지 않습니다**. 메모리 누수를 방지하기 위해 더 이상 필요하지 않은 액터는 명시적으로 소멸해야 합니다.
- 액터가 사용하는 메시지는 **항상 불변**이어야 합니다.

항상 그렇듯이 이 글의 소스 코드는 [GitHub](https://github.com/eugenp/tutorials/tree/master/akka-modules/akka-actors)에서 확인할 수 있습니다.
