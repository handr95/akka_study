# Future<Terminated> terminateResponse = system.terminate();

`system.terminate()`은 Akka 액터 시스템을 종료하는 메서드입니다. 이 메서드는 현재 액터 시스템에 등록된 모든 액터들을 중지하고, 액터 시스템 자체를 종료합니다.

### 동작 방식

1. `system.terminate()`을 호출하면 액터 시스템이 종료됩니다.
2. 액터 시스템은 현재 등록된 모든 액터들에게 `PoisonPill` 메시지를 보내어 각 액터를 종료하도록 요청합니다.
3. 모든 액터가 종료되면, 액터 시스템은 자체를 종료하고 종료 완료를 나타내는 `Future<Terminated>`을 반환합니다.

### 반환 값

- `Future<Terminated>`: 액터 시스템의 종료 상태를 나타내는 Future입니다.
    - 종료가 완료되면 `Terminated` 객체를 포함한 Future가 완료됩니다.
    - 종료가 실패하면 예외가 발생합니다.

### 사용 예시

```java
Future<Terminated> terminateResponse = system.terminate();

// 액터 시스템 종료가 완료되기를 기다립니다.
try {
    terminateResponse.get();
    System.out.println("Actor system terminated successfully.");
} catch (InterruptedException | ExecutionException e) {
    System.out.println("Failed to terminate actor system: " + e.getMessage());
}
```

위 코드에서는 `terminate()` 메서드를 호출하여 액터 시스템을 종료하고, `terminateResponse.get()`을 호출하여 액터 시스템이 완전히 종료될 때까지 기다립니다. 종료가 완료되면 "Actor system terminated successfully."가 출력됩니다. 종료에 실패하면 예외가 발생하고, "Failed to terminate actor system: [예외 메시지]"가 출력됩니다.

이러한 방식으로 액터 시스템을 종료하면 자원을 해제하고 프로세스를 정리할 수 있습니다.