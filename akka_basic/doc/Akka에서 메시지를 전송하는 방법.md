# Akka에서 메시지를 전송하는 방법

### `forward` 메서드

- `forward` 메서드는 현재 액터가 메시지를 받은 후, 해당 메시지를 다른 액터에게 전달합니다.
- 전달되는 메시지는 이전 발신자(sender)와 동일한 발신자를 가지며, 현재 액터의 컨텍스트(context)가 유지됩니다.
- 이렇게 함으로써, 메시지를 전달한 액터가 이전 발신자의 역할을 계속할 수 있습니다.

### `tell` 메서드

- `tell` 메서드는 액터에게 새로운 메시지를 보내는 가장 일반적인 방법입니다.
- 메시지를 받는 액터의 경로는 메시지를 보낸 액터 자체(자기 자신)가 됩니다.
- 따라서, 메시지를 받는 액터는 이전 발신자의 정보를 알 수 없습니다. 이는 메시지의 발신자(sender)가 변경되는 것을 의미합니다.

### 예시

```java
printerActorRef.forward(new PrinterActor.PrintFinalResult(totalNumberOfWords), getContext());
```

- `printerActorRef`가 메시지를 받으면, 해당 메시지를 `PrinterActor`로 전달합니다.
- 전달된 메시지는 이전 발신자와 발신자 컨텍스트를 유지합니다.

```java
printerActorRef.tell(new PrinterActor.PrintFinalResult(totalNumberOfWords), getSelf());
```

- `printerActorRef`가 메시지를 받으면, 해당 메시지를 자기 자신에게 전달합니다.
- 따라서, 이전 발신자는 현재 액터(즉, `printerActorRef`)가 됩니다.

따라서, 메시지를 전달할 때 이전 발신자와 컨텍스트를 유지하고 싶을 때는 `forward`를 사용하고, 그렇지 않을 때는 `tell`을 사용합니다.