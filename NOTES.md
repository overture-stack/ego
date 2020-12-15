Notes
------

## 1. Reason for using the `@Type` hibernate annotation

#### Problem
In the entity `Policy`, the field `accessLevel` of type `AccessLevel` (which is an enum), the `@Type` annotation is used, which is a hibernate specific and not JPA annotation. The goal is to minimize or eliminate use of hibernate specific syntax, and just use JPA related code.

#### Solutions
The goal is to map the enum to the database. In the end, solution 3 was chosen.

##### 1. Middleware Level Enum Handling without Hibernate Annotations
Set the type of the field in the database to a `VARCHAR` and only use the `@Enumerated` JPA annotation
Pros:
    - Hibernate will handle the logic of converting an AccessLevel to a string. This means Enum conversion is handled by the middleware naturally.
    - Simple and clean solution using only JPA annotations
Cons:
    - Enum type is represented as an Enum at the application level but as a VARCHAR at the database level. If someone was to backdoor the database and update the `accessLevel` of a policy, they could potentially break the application. There is no safeguard outside of hibernate/JPA

##### 2. Application Level Enum Handling
Set the type of the field in the postgres database to a `AccessLevelType` and in the Java DAO, represent the field as a `String`. The application level (i.e the service layers) will manage the conversion of the Policies `String` accessLevel field to the `AccessLevel` Java enum type. Hibernate will pass the string to the postgres database, and since the url ends with `?stringtype=unspecified`, postgres will cast the string to the Database enum type
Pros:
    - No need for Hibernate annotations
    - Since conversions are done manually at application layer, functionality is more obvious and cleaner
Cons:
    - Its manual, meaning, if the developer forgets to check that the conversion from `AccessLevel->String` and `String->AccessLevel` is correct, a potentially wrong string value will be passed from hibernate to postrgres, resulting in a postgres error


##### 3. Middleware Level Enum Handling WITH Hibernate Annotations
Follow the instructions from this [blog post](https://vladmihalcea.com/the-best-way-to-map-an-enum-type-with-jpa-and-hiberate/) under the heading `Mapping a Java Enum to a database-specific Enumarated column type`. This results in the use of the `@Type` hibernate specific annotation for the `Policy` entity. This annotation modifies the way hibernate process the `accessLevel` field.
Pros:
    - All processing is done at the middleware (hibernate) and the developer does not need to add any extra code at the application layer. 
    - Hibernate properly process the Java enum type to a Postgres enum type. This means **BOTH** the java code (the `Policy` entity) and the Policy table in postgres are types and protected from values outside the enumeration. 
    - Almost no developer effort and minimizes developer mistakes
Cons:
    - The `Policy` entity is using a hibernate annotation with a custom `PostgresSQLEnumType` processor to assist hibernate in supporting Postgres enum types. 


