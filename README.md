<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>스프링부트 IoC 컨테이너 클론 - v2</title>
  <style>
    body { font-family: Arial, sans-serif; line-height: 1.6; padding: 20px; }
    code { background: #f4f4f4; padding: 2px 4px; border-radius: 4px; }
    pre { background: #f4f4f4; padding: 12px; border-radius: 6px; overflow-x: auto; }
    h1, h2 { color: #333; }
    .box { background: #eef; padding: 10px; border-left: 4px solid #55f; margin-bottom: 15px; }
  </style>
</head>

<h1> 스프링부트 IoC 컨테이너 클론 - v2 </h1>

<div class="box">
  <strong>핵심 개념:</strong><br>
  하드코딩을 제거하고, 어노테이션 기반 스캔 + Reflection을 활용하여<br>
  자동으로 Bean을 등록하고 의존성을 주입하는 IoC 컨테이너 구현
</div>

<h2>1️⃣ v1 vs v2 차이점</h2>

<pre>
v1
- 모든 Bean을 직접 new
- switch-case로 하드코딩
- 확장성 없음

v2
- @Component 기반 자동 스캔
- Reflection을 이용한 객체 생성
- 생성자 기반 DI 자동 처리
- 싱글톤 캐싱 지원
</pre>

<hr>

<h2>2️⃣ ApplicationContext 구조 (v2)</h2>

<pre>
public class ApplicationContext {

    private final Map&lt;String, Class&lt;?&gt;&gt; beanMap = new HashMap&lt;&gt;();
    private final Map&lt;String, Object&gt; beanStore = new HashMap&lt;&gt;();

    public ApplicationContext(String basePackage) {
        scan(basePackage);
    }

    private void scan(String basePackage) {
        Reflections reflections = new Reflections(basePackage);

        Set&lt;Class&lt;?&gt;&gt; components =
                reflections.getTypesAnnotatedWith(Component.class);

        for (Class&lt;?&gt; clazz : components) {
            String beanName =
                decapitalize(clazz.getSimpleName());

            beanMap.put(beanName, clazz);
        }
    }

    public &lt;T&gt; T genBean(String beanName) {
        try {

            if (beanStore.containsKey(beanName)) {
                return (T) beanStore.get(beanName);
            }

            Class&lt;?&gt; clazz = beanMap.get(beanName);

            Constructor&lt;?&gt; constructor =
                    clazz.getDeclaredConstructors()[0];

            Class&lt;?&gt;[] paramTypes =
                    constructor.getParameterTypes();

            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i &lt; paramTypes.length; i++) {
                String dependencyName =
                    decapitalize(paramTypes[i].getSimpleName());

                params[i] = genBean(dependencyName);
            }

            Object instance =
                    constructor.newInstance(params);

            beanStore.put(beanName, instance);

            return (T) instance;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
</pre>

<hr>

<h2>3️⃣ 동작 흐름</h2>

<pre>
ApplicationContext 생성
        ↓
Reflections로 패키지 스캔
        ↓
@Component 클래스 탐색
        ↓
beanMap 등록
        ↓
genBean 호출
        ↓
생성자 기반 DI 수행
        ↓
싱글톤 저장
</pre>

<hr>

<h2>4️⃣ 핵심 기술 요소</h2>

<ul>
  <li>@Component 어노테이션 기반 자동 등록</li>
  <li>Reflections 라이브러리 활용 패키지 스캔</li>
  <li>Reflection을 통한 생성자 호출</li>
  <li>재귀 호출을 통한 의존성 자동 생성</li>
  <li>싱글톤 캐싱 구조</li>
</ul>

<hr>

<h2>5️⃣ 예시 클래스</h2>

<pre>
@Component
public class TestPostRepository {
}

@Component
public class TestPostService {

    private final TestPostRepository repository;

    public TestPostService(TestPostRepository repository) {
        this.repository = repository;
    }
}
</pre>

<hr>

<h2>6️⃣ 후기</h2>

v1에서는 IoC의 개념을 이해하기 위해 모든 Bean을 하드코딩으로 생성하였다.

v2에서는 실제 Spring의 동작 원리를 참고하여  
어노테이션 기반 스캔 + Reflection을 통해 자동으로 Bean을 등록하고  
생성자 기반 DI를 수행하도록 개선하였다.

이를 통해 Spring IoC 컨테이너의 핵심 구조인  
“Bean 등록 → 의존성 해결 → 싱글톤 관리”의 흐름을 직접 구현해볼 수 있었다.

<hr>

<h2>3️⃣ 후기</h2>
- v2는 요구사항대로 빈 생성 시 하드코딩 방식을 사용하지 않고 작성하였습니다.
- 생성자가 필요한 경우에는 생성자의 개수를 파악하고 생성자를 만들어서 작성하였습니다.
- 이 작업을 사용해서 v3를 작업하겠습니다.

</body>
</html>
