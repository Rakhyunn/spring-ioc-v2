# Spring IoC-V2

> 스프링 부트의 IoC 컨테이너를 직접 구현해보는 학습 프로젝트  
> V1의 하드코딩 개선하여 어노테이션 + 리플렉션 기반으로 자동화 구현

## 📌 프로젝트 개요 / Overview

V1에서는 Bean을 하드코딩으로 직접 등록했습니다.  
V2에서는 `@Service`, `@Repository` 같은 **어노테이션을 기반으로 Bean을 자동 감지**하고,  
**리플렉션(Reflection)** 으로 의존성을 파악하여 자동 주입하는 방식으로 리팩토링했습니다.


## 🆚 V1 vs V2 비교

| | V1 | V2 |
|---|---|---|
| Bean 등록 방식 | 하드코딩 | 어노테이션 자동 감지 |
| 의존성 주입 | 수동으로 순서 맞춰 생성 | 리플렉션으로 자동 파악 & 재귀 주입 |
| 새 클래스 추가 시 | ApplicationContext 직접 수정 필요 | 어노테이션만 붙이면 자동 등록 |
| 확장성 | ❌ 클래스가 늘어날수록 코드 증가 | ✅ 클래스가 늘어도 코드 변경 없음 |


## 🧠 V2 핵심 개념

### 어노테이션 (Annotation)

클래스에 메타데이터를 붙이는 방법으로, `@Retention(RUNTIME)` 이어야 실행 중에 리플렉션으로 읽을 수 있습니다.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)  // 실행 중에도 읽을 수 있음
@Component                           // 메타 어노테이션
public @interface Service { }
```

### 메타 어노테이션 (Meta Annotation)

어노테이션 위에 붙은 어노테이션입니다.  
`@Service`, `@Repository`, `@Configuration` 모두 내부에 `@Component` 어노테이션을 가지고 있습니다.

```
@Service    →  내부에 @Component 포함
@Repository →  내부에 @Component 포함
@Component  →  Bean 등록 대상의 기준
```

따라서 `@Service`나 `@Repository`가 붙은 클래스도 `@Component` 어노테이션 소지 여부로 감지해야 합니다.

```java
// @Component 직접 붙었거나
clazz.isAnnotationPresent(Component.class)

// @Service처럼 @Component를 품은 어노테이션이 붙었거나
annotation.annotationType().isAnnotationPresent(Component.class)
```

### 리플렉션 (Reflection)

런타임에 클래스 이름(문자열)만으로 클래스 구조를 분석하고 객체를 생성하는 Java 기능입니다.


```java
// 클래스 이름만으로 생성자 파라미터 파악 가능
Class<?> clazz = Class.forName("com.ll.domain.testPost.service.TestPostService");
Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
Parameter[] params = constructor.getParameters();
// → [TestPostRepository] : TestPostRepository가 필요하다는 것을 자동으로 파악!
```

---

## 🔴🟢🔵 TDD 사이클 / TDD Cycle

V2는 V1의 그린 코드를 **블루(리팩토링)** 단계로 발전시켰습니다.

### 변경된 테스트 코드

```java
// V1
applicationContext = new ApplicationContext();

// V2 - 패키지 지정 + init() 분리
applicationContext = new ApplicationContext("com.ll");
applicationContext.init();
```

## ⚙️ 구현 흐름 / Implementation Flow

```
① init() 호출
        ↓
② "com.ll" 패키지 하위 모든 .class 파일 탐색 (패키지 스캔)
        ↓
③ 각 클래스에 @Component 계열 어노테이션 있는지 확인 (어노테이션 필터링)
        ↓
④ Bean 이름 결정 : 클래스명 첫 글자 소문자 (util 패키지의 Ut.lcfirst 활용)
        ↓
⑤ 생성자 파라미터를 리플렉션으로 파악 → 재귀적으로 의존성 먼저 생성
        ↓
⑥ beans Map에 저장 (싱글톤 보장)
        ↓
⑦ genBean() 호출 시 Map에서 꺼내서 반환
```

## 🔍 단계별 구현 설명

### 1단계 - 패키지 스캔

`"com.ll"` 문자열로 하위의 모든 클래스 파일을 탐색합니다.

```java
public void init() {
    String path = basePackage.replace(".", "/");  // "com.ll" → "com/ll"
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    URL resource = classLoader.getResource(path); // 실제 폴더 경로 탐색
    File directory = new File(resource.toURI());

    scanDirectory(directory, basePackage);
}

private void scanDirectory(File directory, String packageName) {
    for (File file : directory.listFiles()) {
        if (file.isDirectory()) {
            // 폴더면 → 패키지명 누적하며 재귀 탐색
            scanDirectory(file, packageName + "." + file.getName());
        } else if (file.getName().endsWith(".class")) {
            // .class 파일이면 → 완전한 클래스 이름 조합
            String fullClassName = packageName + "." + file.getName().replace(".class", "");
        }
    }
}
```

### 2단계 - 어노테이션 필터링

`@Component` 계열 어노테이션이 붙은 클래스만 Bean 등록 대상으로 추립니다.

```java
private boolean isComponent(Class<?> clazz) {
    // @Component 직접 달린 경우
    if (clazz.isAnnotationPresent(Component.class)) return true;

    // @Service, @Repository 처럼 메타 어노테이션으로 @Component를 품은 경우
    for (Annotation annotation : clazz.getAnnotations()) {
        if (annotation.annotationType().isAnnotationPresent(Component.class)) return true;
    }

    return false;
}
```

### 3단계 - Bean 이름 결정

`Ut.str.lcfirst()`로 클래스명 첫 글자를 소문자로 변환합니다.

```
TestPostService       → "testPostService"
TestPostRepository    → "testPostRepository"
TestFacadePostService → "testFacadePostService"
```

### 4단계 - 재귀적 의존성 주입

생성자 파라미터를 리플렉션으로 파악하고, 재귀적으로 의존 객체를 먼저 생성합니다.
1. 생성자의 필요 파라미터를 찾음
2. 필요 파라미터가 없으면 바로 인스턴스를 생성하여 반환
3. 필요 파라미터가 있으면 기본 컨테이너로 활용할 맵에서 있으면 활용
4. 없다면 새로 생성하여 맵을 넣어 싱글톤 유지
5. 모든 파라미터를 만들고 생성자에 넣어 인스턴스를 생성하여 반환

```java
private Object createBean(Class<?> clazz) {
    Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
    Parameter[] parameters = constructor.getParameters();

    if (parameters.length == 0) {
        return constructor.newInstance(); // 의존성 없음 → 바로 생성
    }

    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
        String name = Ut.str.lcfirst(parameters[i].getType().getSimpleName());
        Object bean = beans.get(name);
        if (bean == null) {
            bean = createBean(parameters[i].getType()); // 없으면 재귀 생성
            beans.put(name, bean);
        }
        args[i] = bean;
    }

    return constructor.newInstance(args); // 의존성 주입하여 생성
}
```

재귀 흐름 예시:
- `TestFacadePostService`는 `TestPostService`와 `TestPostRepository`가 먼저 있어야 생성이 가능하므로 먼저 2개를 생성하여 주입
- `TestPostService`는 `TestPostRepository`가 있어야 생성 가능하지만 새로 객체를 생성하는 것이 아니라 `TestFacadePostService` 생성 시에 만든 객체를 beans 맵에서 꺼내 싱글톤 유지
```
createBean(TestFacadePostService)
    → 파라미터: [TestPostService, TestPostRepository]
    → createBean(TestPostService)
        → 파라미터: [TestPostRepository]
        → createBean(TestPostRepository)
            → 파라미터 없음 → new TestPostRepository() ✅
        → new TestPostService(testPostRepository) ✅
    → beans에 testPostRepository 이미 있음 → 재사용 ✅
    → new TestFacadePostService(testPostService, testPostRepository) ✅
```

## ✅ 테스트 결과 / Test Results

| 테스트 | 내용 | 결과 |
|--------|------|------|
| t1 | ApplicationContext 객체 생성 | ✅ |
| t2 | testPostService Bean 가져오기 | ✅ |
| t3 | 싱글톤 보장 | ✅ |
| t4 | testPostRepository Bean 가져오기 | ✅ |
| t5 | testPostService가 testPostRepository를 가지고 있는지 | ✅ |
| t6 | testFacadePostService가 testPostService, testPostRepository를 가지고 있는지 | ✅ |

---

## 💡 배운 점 & 회고

**하드코딩에서 자동화 개선**

V1에서 클래스가 3개일 때는 하드코딩이 가능했지만, 클래스가 늘어날수록 `ApplicationContext`를 계속 수정해야 하며 하드 코딩 형식이라 실수를 유발할 수 있다고 생각했습니다.
따라서 V2에서 어노테이션과 리플렉션을 도입하니 새 클래스를 추가할 때 `ApplicationContext`를 전혀 건드리지 않아도 됐습니다.

**리플렉션**

`Class.forName()`으로 클래스 이름(문자열)만 있어도 생성자 파라미터를 파악하고 객체를 생성할 수 있다는 것을 배웠습니다. 
스프링 부트가 내부적으로 이런 방식으로 동작한다는 것을 직접 구현하며 이해하게 됐습니다.

**재귀의 활용**

재귀를 이용하여 "없으면 만들고, 있으면 재사용"이라는 단순한 원칙으로 싱글톤과 의존성 주입을 동시에 해결할 수 있었습니다.

**메타 어노테이션**

어노테이션을 활용하여 해당 어노테이션의 존재 여부로 객체를 파악하고 생성하는 것을 구현해보면 스프링부트의 IoC 컨테이너 흐름을 파악할 수 있었습니다.
