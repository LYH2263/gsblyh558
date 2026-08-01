-- Categories
INSERT INTO question_categories (id, name, description) VALUES 
(1, 'Java基础', 'Java基础知识测试'),
(2, 'Spring框架', 'Spring Framework核心概念')
ON CONFLICT (id) DO NOTHING;

-- Questions (Java基础: 5 Single, 5 Multi, 5 TF, 5 Fill, 5 Short)
-- Java Single Choice
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(1, 'SINGLE_CHOICE', 'Java中，int类型占用多少字节？', '["2", "4", "8", "16"]', 'B', 'Java中int类型固定占用4个字节', 1, NOW()),
(1, 'SINGLE_CHOICE', 'Java中，以下哪个关键字用于定义接口？', '["class", "interface", "implements", "abstract"]', 'B', 'interface用于定义接口', 1, NOW()),
(1, 'SINGLE_CHOICE', '以下哪个不是Java的基本数据类型？', '["int", "float", "String", "boolean"]', 'C', 'String是引用类型', 1, NOW()),
(1, 'SINGLE_CHOICE', 'System.out.println("5" + 2) 的输出结果是？', '["7", "52", "Error", "None"]', 'B', '字符串连接', 2, NOW()),
(1, 'SINGLE_CHOICE', 'ArrayList和LinkedList的区别，以下说法错误的是？', '["ArrayList基于数组", "LinkedList基于链表", "ArrayList随机访问快", "LinkedList读取速度总是比ArrayList快"]', 'D', 'LinkedList随机访问慢', 3, NOW());

-- Java Multi Choice
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(1, 'MULTI_CHOICE', '以下哪些是Java的访问修饰符？', '["public", "protected", "friend", "private"]', 'A,B,D', 'friend是C++的关键字', 2, NOW()),
(1, 'MULTI_CHOICE', 'Java中集合框架的接口有哪些？', '["List", "Set", "Map", "Collection"]', 'A,B,C,D', '都是核心接口', 2, NOW()),
(1, 'MULTI_CHOICE', '关于异常处理，以下哪些关键字是正确的？', '["try", "catch", "throw", "throws"]', 'A,B,C,D', '都是异常处理关键字', 2, NOW()),
(1, 'MULTI_CHOICE', '以下哪些是Object类的方法？', '["toString", "equals", "hashCode", "clone"]', 'A,B,C,D', '都是Object的方法', 2, NOW()),
(1, 'MULTI_CHOICE', 'Java 8的新特性包括哪些？', '["Lambda表达式", "Stream API", "Optional类", "局部变量类型推断(var)"]', 'A,B,C', 'var是Java 10引入的', 3, NOW());

-- Java True/False
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(1, 'TRUE_FALSE', 'Java是面向对象的编程语言吗？', NULL, 'T', '纯面向对象', 1, NOW()),
(1, 'TRUE_FALSE', 'String类是可变的吗？', NULL, 'F', 'String是不可变的', 1, NOW()),
(1, 'TRUE_FALSE', 'Java支持多重继承吗？', NULL, 'F', 'Java类不支持多重继承，接口支持多重继承', 2, NOW()),
(1, 'TRUE_FALSE', 'HashMap是线程安全的吗？', NULL, 'F', 'HashMap非线程安全，HashTable或ConcurrentHashMap是', 2, NOW()),
(1, 'TRUE_FALSE', 'GC是垃圾回收的缩写吗？', NULL, 'T', 'Garbage Collection', 1, NOW());

-- Java Fill in Blank
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(1, 'FILL_IN_BLANK', 'Java的入口方法是public static void ____(String[] args)', NULL, 'main', 'main方法', 1, NOW()),
(1, 'FILL_IN_BLANK', '定义常量的关键字是 ____', NULL, 'final', 'final修饰常量', 1, NOW()),
(1, 'FILL_IN_BLANK', 'Java编译后的字节码文件后缀是 .____', NULL, 'class', 'javac生成.class文件', 1, NOW()),
(1, 'FILL_IN_BLANK', 'Java中所有类的父类是 ____', NULL, 'Object', 'Object是根类', 1, NOW()),
(1, 'FILL_IN_BLANK', '用于捕获异常的块是 try-____', NULL, 'catch', 'try-catch结构', 1, NOW());

-- Java Short Answer
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(1, 'SHORT_ANSWER', '请简述封装的概念。', NULL, '{"standard": "将对象的状态信息隐藏在对象内部，不允许外部程序直接访问对象内部信息，而是通过该类所提供的方法来实现对内部信息的操作和访问。", "keywords": [{"text": "隐藏", "weight": 30}, {"text": "内部信息", "weight": 30}, {"text": "方法操作", "weight": 40}]}', '封装是面向对象编程（OOP）的核心支柱。它通过将数据（属性）和操作数据的代码（方法）绑定在一起，并对外隐藏实现细节，从而增强了代码的安全性和可维护性。用户只能通过受控的公共接口（如Getter/Setter）与对象交互，防止了非法数据篡改。', 3, NOW()),
(1, 'SHORT_ANSWER', 'String, StringBuilder, StringBuffer的区别？', NULL, '{"standard": "String不可变；StringBuilder可变，线程不安全，效率高；StringBuffer可变，线程安全，效率低。", "keywords": [{"text": "不可变", "weight": 30}, {"text": "可变", "weight": 20}, {"text": "线程不安全", "weight": 25}, {"text": "线程安全", "weight": 25}]}', '三者主要在可变性和线程安全上有所区别。String是final修饰的，每次修改都会创建新对象；StringBuilder和StringBuffer都是在原对象上操作，其中StringBuffer通过synchronized关键字实现了线程安全，适合多线程环境，而StringBuilder在单线程下性能更优。', 3, NOW()),
(1, 'SHORT_ANSWER', '什么是多态？', NULL, '{"standard": "同一个行为具有多个不同表现形式或形态的能力。在Java中体现在重写和重载。", "keywords": [{"text": "不同表现形式", "weight": 40}, {"text": "重写", "weight": 30}, {"text": "重载", "weight": 30}]}', '多态允许一个接口代表多个类的行为。它通过继承（或实现）和方法重写来实现运行时多态。这种机制降低了代码间的耦合度，使得程序具有极高的灵活性和可扩展性，是实现“解耦”的重要手段。', 3, NOW()),
(1, 'SHORT_ANSWER', 'ArrayList和LinkedList的区别？', NULL, '{"standard": "ArrayList基于动态数组，查询快增删慢；LinkedList基于双向链表，查询慢增删快。", "keywords": [{"text": "动态数组", "weight": 25}, {"text": "双向链表", "weight": 25}, {"text": "查询快", "weight": 25}, {"text": "查询慢", "weight": 25}]}', 'ArrayList内部使用连续的内存空间，支持随机访问，因此索引查询极快，但在中间插入或删除需移动大量元素；LinkedList则由分散的节点组成，每次增删只需修改指针，不需要移动数据，因此在频繁增删的场景下表现更佳。', 3, NOW()),
(1, 'SHORT_ANSWER', 'Error和Exception的区别？', NULL, '{"standard": "Error是程序无法处理的错误，如OOM；Exception是程序可以处理的异常。", "keywords": [{"text": "无法处理", "weight": 40}, {"text": "程序可以处理", "weight": 40}, {"text": "异常", "weight": 20}]}', '它们都继承自Throwable。Error通常指严重的系统级问题（如栈溢出或内存溢出），JVM一般会选择终止程序；而Exception分为受检异常（必须捕获）和非受检异常，通常是由程序逻辑引起的，可以通过try-catch机制进行恢复，保证程序继续运行。', 3, NOW());

-- Questions (Spring框架: 5 Single, 5 Multi, 5 TF, 5 Fill, 5 Short)
-- Spring Single Choice
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(2, 'SINGLE_CHOICE', 'Spring的核心特性是什么？', '["IoC和AOP", "MVC", "JPA", "Security"]', 'A', 'IoC和AOP', 3, NOW()),
(2, 'SINGLE_CHOICE', 'Spring Boot的主程序注解是？', '["@SpringBootApplication", "@SpringApplication", "@EnableAutoConfiguration", "@Configuration"]', 'A', '组合注解', 2, NOW()),
(2, 'SINGLE_CHOICE', '默认的Bean作用域是？', '["prototype", "singleton", "request", "session"]', 'B', '单例', 2, NOW()),
(2, 'SINGLE_CHOICE', 'Spring MVC的前端控制器是？', '["FacesServlet", "DispatcherServlet", "ActionServlet", "SpringServlet"]', 'B', 'DispatcherServlet', 3, NOW()),
(2, 'SINGLE_CHOICE', '用于注入依赖的注解是？', '["@Autowired", "@Inject", "@Resource", "以上都是"]', 'D', '都支持', 2, NOW());

-- Spring Multi Choice
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(2, 'MULTI_CHOICE', 'Spring AOP常用的通知类型有？', '["@Before", "@After", "@Around", "@AfterReturning"]', 'A,B,C,D', '全部都是', 3, NOW()),
(2, 'MULTI_CHOICE', 'Spring Bean的作用域包括？', '["singleton", "prototype", "request", "session"]', 'A,B,C,D', '常用作用域', 3, NOW()),
(2, 'MULTI_CHOICE', 'Spring Boot支持的配置文件格式有？', '["properties", "yml", "yaml", "xml"]', 'A,B,C', 'xml已不常用', 2, NOW()),
(2, 'MULTI_CHOICE', 'Spring事务传播行为包括？', '["REQUIRED", "REQUIRES_NEW", "SUPPORTS", "MANDATORY"]', 'A,B,C,D', '全部都是', 4, NOW()),
(2, 'MULTI_CHOICE', '常用的Spring Cloud组件有？', '["Eureka", "Gateway", "Nacos", "OpenFeign"]', 'A,B,C,D', '全部都是', 3, NOW());

-- Spring True/False
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(2, 'TRUE_FALSE', 'Spring默认情况下Bean是多例的。', NULL, 'F', '默认是单例(singleton)', 2, NOW()),
(2, 'TRUE_FALSE', 'Spring Boot内置了Tomcat容器。', NULL, 'T', '默认内置Tomcat', 1, NOW()),
(2, 'TRUE_FALSE', 'AOP只能通过XML配置。', NULL, 'F', '现在主要使用注解配置', 2, NOW()),
(2, 'TRUE_FALSE', 'Spring MVC和Spring Boot是竞争关系。', NULL, 'F', 'Spring Boot包含了Spring MVC', 1, NOW()),
(2, 'TRUE_FALSE', 'MyBatis可以和Spring无缝集成。', NULL, 'T', '常用集成方案', 1, NOW());

-- Spring Fill in the Blank
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(2, 'FILL_IN_BLANK', 'Spring的两大核心思想是IoC和_____。', NULL, 'AOP', '核心特性', 2, NOW()),
(2, 'FILL_IN_BLANK', 'Spring Boot默认的配置文件名是_____。', NULL, 'application', '默认配置', 1, NOW()),
(2, 'FILL_IN_BLANK', '在Spring中，用于声明一个类为配置类的注解是_____。', NULL, '@Configuration', '配置注解', 2, NOW()),
(2, 'FILL_IN_BLANK', 'Spring Cloud中实现负载均衡的组件是_____。', NULL, 'Ribbon', '负载均衡', 3, NOW()),
(2, 'FILL_IN_BLANK', 'Spring中Bean的默认作用域名称是_____。', NULL, 'singleton', '单例', 2, NOW());

-- Spring Short Answer
INSERT INTO questions (category_id, type, content, options, answer, analysis, difficulty, create_time) VALUES
(2, 'SHORT_ANSWER', '什么是IoC？', NULL, '{"standard": "控制反转，将对象的创建和依赖关系的管理交给Spring容器，而不是在代码中硬编码。", "keywords": [{"text": "控制反转", "weight": 40}, {"text": "Spring容器", "weight": 30}, {"text": "依赖关系", "weight": 30}]}', 'IoC（Inversion of Control）是Spring的核心。它改变了传统由开发者手动new对象的方式，转而由容器统一管理对象的生命周期 and 装配逻辑。这极大地降低了类与类之间的耦合度，使得系统更加模块化，也更利于进行单元测试和维护。', 3, NOW()),
(2, 'SHORT_ANSWER', '什么是AOP？', NULL, '{"standard": "面向切面编程，用于将横切关注点（如日志、事务）与业务逻辑分离。", "keywords": [{"text": "面向切面", "weight": 40}, {"text": "横切关注点", "weight": 30}, {"text": "业务逻辑分离", "weight": 30}]}', 'AOP（Aspect Oriented Programming）是OOP的补充。它允许开发者定义“切面”来捕捉那些散布在多个对象中的公共行为（如权限检查、性能监控）。通过这种方式，核心业务代码可以保持简洁，而横切逻辑则被集中管理，提高了代码的重用性和清晰度。', 3, NOW()),
(2, 'SHORT_ANSWER', 'Spring Bean的生命周期？', NULL, '{"standard": "实例化 -> 属性赋值 -> 初始化 -> 使用 -> 销毁。", "keywords": [{"text": "实例化", "weight": 20}, {"text": "属性赋值", "weight": 20}, {"text": "初始化", "weight": 20}, {"text": "使用", "weight": 20}, {"text": "销毁", "weight": 20}]}', 'Bean的生命周期由Spring容器精细控制。首先进行类加载和实例化，接着通过依赖注入完成属性赋值。之后会调用各类Aware接口及初始化方法（如@PostConstruct）。在容器关闭前，还会执行销毁回调（如PreDestroy），确保资源如数据库连接、线程池等能被正确释放。', 4, NOW()),
(2, 'SHORT_ANSWER', 'Spring Boot Starter的作用？', NULL, '{"standard": "提供一站式的依赖管理，自动引入相关功能的jar包，简化配置。", "keywords": [{"text": "一站式", "weight": 30}, {"text": "依赖管理", "weight": 40}, {"text": "简化配置", "weight": 30}]}', 'Starter是Spring Boot“约定优于配置”理念的体现。它将特定功能（如Web、Data-JPA）所需的全部依赖打包在一起，开发者只需引入一个Starter，系统就会自动配置好所需的底层环境和Bean，极大地缩短了从零搭建项目的时间，降低了版本冲突的风险。', 2, NOW()),
(2, 'SHORT_ANSWER', '@RestController和@Controller的区别？', NULL, '{"standard": "@RestController = @Controller + @ResponseBody，直接返回数据而不是视图。", "keywords": [{"text": "@ResponseBody", "weight": 50}, {"text": "返回数据", "weight": 30}, {"text": "视图", "weight": 20}]}', '@Controller主要用于传统的MVC架构，其方法返回值通常被视图解析器处理为页面路径。而@RestController则是专为RESTful Web服务设计的，它会自动在所有处理方法上添加@ResponseBody注解，确保返回的对象能被Jackson等序列化工具直接转换为JSON或XML发送给客户端。', 2, NOW());

-- Reset sequences (Postgres specific)
SELECT setval('question_categories_id_seq', (SELECT MAX(id) FROM question_categories));
SELECT setval('questions_id_seq', (SELECT MAX(id) FROM questions));
SELECT setval('exams_id_seq', (SELECT MAX(id) FROM exams));
SELECT setval('exam_questions_id_seq', (SELECT MAX(id) FROM exam_questions));
