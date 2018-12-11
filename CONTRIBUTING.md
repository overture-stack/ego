# Contributing

When contributing to this repository, please first discuss the change you wish to make via issue,
email, or any other method with the owners of this repository before making a change. 

Please note we have a code of conduct, please follow it in all your interactions with the project.

## Code Standards

#### General
1. Do not use field injection (ie. `@Value`, `@Autowired`)
    - Instead use an `@Autowired` or `@Value` annotated constructor
    - This helps to improves testability
    - Helps to decouple from Spring
    - If your constructor is feeling messy or too big - you are probably overloading the class you are working on
2. If a class is dependent on more than 3 constructor arguments, a _single_ config class should encapsulate those arguments while
 implementing a builder pattern (ie. Lombok `@Builder` annotation)
3. Do not use any implementation specific JPA code (ie. Hibernate-only annotations)
    - Exception for when no alternative functionality exists (ie. Postgres JSON field search)
4. All of our code is auto-formatted to Google Java Format using the [fmt-maven-plugin](https://mvnrepository.com/artifact/com.coveo/fmt-maven-plugin) on build:
```xml
<plugin>
    <groupId>com.coveo</groupId>
    <artifactId>fmt-maven-plugin</artifactId>
    <version>${FMT_MVN_PLG.VERSION}</version>
    <executions>
        <execution>
            <goals>
                <goal>format</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
5. *Always use the ICGC JPA repository

#### Service Layer
1. Get * should always return Optional<T>
2. Find * should always return a Collection<T>

### Testing

#### General
1. DB via Test Containers - no in-memory DB or OS specific services
2. No dependencies on any external services (ie. production micro-service)

##### Unit Testing

##### Integration Testing