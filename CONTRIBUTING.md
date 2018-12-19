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
5. Constants 
- must be declared in a `@NoArgsConstructor(access=PRIVATE)` annotated class with a name representative of the type of constants. For example, the class `Tables` under the package `constants` would contain sql table names. 
- Constant variable names should be consistent throughout code base. For example, the text `egoUserPermissions` should be defined by the variable `EGO_USER_PERMISSION`.  
6. If a method is not stateful and not an interface/abstract method, then it should be static

#### Service Layer
1. Get * should always return Optional<T>
2. Find * should always return a Collection<T>

#### JPA
1. Entity member declarations should take the following presidence:
    1. @Id (identifier)    
    2. Non-relationship @Column
    3. @OneToOne
    4. @OneToMany
    5. @ManyToOne
    6. @ManyToMany
2. As explained in this [article](https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/), you should prefer bidirectional associations since they are more efficient than unidirectional ones in terms of SQL performance [source](https://vladmihalcea.com/merge-entity-collections-jpa-hibernate/)
3. Always lazy load for @OneToMany and @ManyToMany
4. Never use CascadeType.ALL or CascadeType.REMOVE becuase they are too destructive. Use CascadeType.MERGE and CascadeType.PERSIST instead
5. Name methods with `remove` indicating an entity was deleted
6. Name methods with `dissociate` indicating a child relationship with its parent will be destoryed
7. For performance reasons, @ManyToMany collections should be a Set as described [here](https://thoughts-on-java.org/association-mappings-bag-list-set/)
8. For performance reasons, @OneToMany collections should be a list as described [here](https://vladmihalcea.com/hibernate-facts-favoring-sets-vs-bags/)
9. In ManyToMany relationships, the JoinTable should only be defined on the **owning** side , and on the inverse side the `mappedBy` ManyToMany annotation parameter should be defined, as described [here](https://www.baeldung.com/hibernate-many-to-many)
    
### Testing

#### General
1. DB via Test Containers - no in-memory DB or OS specific services
2. No dependencies on any external services (ie. production micro-service)

##### Unit Testing

##### Integration Testing
